package com.pskin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 皮肤应用器（跨版本兼容版）
 *
 * 所有 Paper 专属 API（PlayerProfile / ProfileProperty / getPlayerProfile / setPlayerProfile）
 * 均通过反射调用，并提供 Bukkit GameProfile 回退方案。
 * 即使未来 Paper 重命名或移除这些 API，插件也能优雅降级，无需重新编译。
 */
public class SkinApplier {
    private final JavaPlugin plugin;
    private final PSkinConfig config;
    private Method refreshPlayerMethod;
    private boolean refreshPlayerChecked;

    // 反射缓存
    private Method getProfileMethod;
    private Method setProfileMethod;
    private boolean profileMethodChecked;

    public SkinApplier(JavaPlugin plugin, PSkinConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void applySkin(Player player, SkinData skinData) {
        if (skinData == null || player == null || !player.isOnline()) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin) this.plugin, () -> {
            try {
                if (!player.isOnline()) {
                    return;
                }
                boolean strip = this.config.isStripSignature() && skinData.hasSignature();
                this.setTextures(player, skinData.getValue(),
                        (skinData.hasSignature() && !strip) ? skinData.getSignature() : null);
                if (!this.tryRefreshPlayer(player)) {
                    this.refreshForOthers(player);
                }
                if (this.config.isDebug()) {
                    this.plugin.getLogger().info("已应用 " + player.getName() + " 的皮肤，来源: " + skinData.getDisplaySource()
                            + "，签名: " + (skinData.hasSignature() ? "有" : "无")
                            + "，发送签名: " + (skinData.hasSignature() && !strip ? "是" : "否（已剥离）"));
                }
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "给 " + player.getName() + " 应用皮肤失败", e);
            }
        });
    }

    public void clearSkin(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin) this.plugin, () -> {
            try {
                if (!player.isOnline()) {
                    return;
                }
                this.setTextures(player, null, null);
                if (!this.tryRefreshPlayer(player)) {
                    this.refreshForOthers(player);
                }
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "清除 " + player.getName() + " 皮肤失败", e);
            }
        });
    }

    /**
     * 跨版本设置玩家 textures 属性。
     * 优先使用 Paper 的 getPlayerProfile/setPlayerProfile，回退到 Bukkit GameProfile。
     */
    private void setTextures(Player player, String value, String signature) throws Exception {
        this.ensureProfileMethods(player);
        Object profile = this.getProfileMethod.invoke(player);
        // profile 是 PlayerProfile (Paper) 或 GameProfile (Bukkit)
        Object propertyMap = profile.getClass().getMethod("getProperties").invoke(profile);
        // 移除旧的 textures
        try {
            // Paper PlayerProfile: Set<ProfileProperty>.removeIf
            propertyMap.getClass().getMethod("removeIf", java.util.function.Predicate.class)
                    .invoke(propertyMap, (java.util.function.Predicate<Object>) prop -> {
                        try {
                            return "textures".equals((String) prop.getClass().getMethod("getName").invoke(prop));
                        } catch (Exception e) {
                            return false;
                        }
                    });
        } catch (NoSuchMethodException e) {
            // GameProfile PropertyMap: removeAll("textures")
            try {
                propertyMap.getClass().getMethod("removeAll", Object.class).invoke(propertyMap, "textures");
            } catch (Exception ignored) {
            }
        }
        // 添加新纹理（如果 value != null）
        if (value != null) {
            Object newProp = this.createProperty("textures", value, signature);
            try {
                // Paper: Set.add(ProfileProperty)
                propertyMap.getClass().getMethod("add", Object.class).invoke(propertyMap, newProp);
            } catch (Exception e) {
                // GameProfile: PropertyMap.put("textures", Property)
                try {
                    propertyMap.getClass().getMethod("put", Object.class, Object.class).invoke(propertyMap, "textures", newProp);
                } catch (Exception e2) {
                    throw new RuntimeException("无法添加 textures 属性", e2);
                }
            }
        }
        // 写回 profile
        this.setProfileMethod.invoke(player, profile);
    }

    /**
     * 创建纹理属性对象。优先 Paper ProfileProperty，回退到 authlib Property。
     */
    private Object createProperty(String name, String value, String signature) throws Exception {
        // 尝试 Paper 的 ProfileProperty
        try {
            Class<?> ppClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
            if (signature != null) {
                return ppClass.getConstructor(String.class, String.class, String.class).newInstance(name, value, signature);
            }
            return ppClass.getConstructor(String.class, String.class).newInstance(name, value);
        } catch (ClassNotFoundException e) {
            // 回退到 authlib Property
            Class<?> propClass = Class.forName("com.mojang.authlib.properties.Property");
            if (signature != null) {
                return propClass.getConstructor(String.class, String.class, String.class).newInstance(name, value, signature);
            }
            return propClass.getConstructor(String.class, String.class).newInstance(name, value);
        }
    }

    private void ensureProfileMethods(Player player) {
        if (this.profileMethodChecked) {
            return;
        }
        this.profileMethodChecked = true;
        Class<?> pc = player.getClass();
        // 优先 Paper 的 getPlayerProfile/setPlayerProfile
        try {
            this.getProfileMethod = pc.getMethod("getPlayerProfile");
            this.setProfileMethod = pc.getMethod("setPlayerProfile",
                    Class.forName("com.destroystokyo.paper.profile.PlayerProfile"));
            this.plugin.getLogger().info("使用 Paper PlayerProfile API 应用皮肤");
            return;
        } catch (Exception ignored) {
        }
        // 回退到 Bukkit/Spigot 的 getProfile/setProfile
        try {
            this.getProfileMethod = pc.getMethod("getProfile");
            this.setProfileMethod = pc.getMethod("setProfile", Class.forName("com.mojang.authlib.GameProfile"));
            this.plugin.getLogger().info("使用 Bukkit GameProfile API 应用皮肤（Paper API 不可用）");
        } catch (Exception e) {
            this.plugin.getLogger().warning("无法找到 profile 读写方法，皮肤应用可能失败: " + e.getMessage());
        }
    }

    private boolean tryRefreshPlayer(Player player) {
        try {
            if (!this.refreshPlayerChecked) {
                this.refreshPlayerChecked = true;
                try {
                    this.refreshPlayerMethod = player.getClass().getDeclaredMethod("refreshPlayer");
                    this.refreshPlayerMethod.setAccessible(true);
                    this.plugin.getLogger().info("已找到 Paper refreshPlayer() 方法，将使用其刷新皮肤可见性");
                } catch (NoSuchMethodException e) {
                    this.plugin.getLogger().info("未找到 refreshPlayer() 方法，回退到延迟 hide/show 方式");
                }
            }
            if (this.refreshPlayerMethod != null) {
                this.refreshPlayerMethod.invoke(player);
                if (this.config.isDebug()) {
                    this.plugin.getLogger().info("已调用 refreshPlayer() 刷新 " + player.getName() + " 的皮肤可见性");
                }
                return true;
            }
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "调用 refreshPlayer() 失败，回退到延迟 hide/show", e);
        }
        return false;
    }

    private void refreshForOthers(Player player) {
        ArrayList<Player> others = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) {
                continue;
            }
            try {
                online.hidePlayer((Plugin) this.plugin, player);
                others.add(online);
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> {
            for (Player online : others) {
                try {
                    online.showPlayer((Plugin) this.plugin, player);
                } catch (Exception ignored) {
                }
            }
        }, 10L);
    }
}
