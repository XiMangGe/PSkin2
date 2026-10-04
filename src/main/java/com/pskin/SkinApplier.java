package com.pskin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
    private boolean usePaperProfile;  // true=Paper PlayerProfile, false=Bukkit GameProfile

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
        Object propertyMap = profile.getClass().getMethod("getProperties").invoke(profile);

        // 移除旧的 textures 属性
        if (this.usePaperProfile) {
            // Paper: Set<ProfileProperty>，优先用 removeIf，失败则用迭代器
            if (!removeTexturesFromSet(propertyMap)) {
                throw new RuntimeException("无法移除旧的 textures 属性");
            }
        } else {
            // Bukkit: PropertyMap (Map<String, Collection<Property>>)
            try {
                propertyMap.getClass().getMethod("removeAll", Object.class).invoke(propertyMap, "textures");
            } catch (Exception ignored) {
                // 兜底：直接 remove
                try {
                    Map.class.getMethod("remove", Object.class).invoke(propertyMap, "textures");
                } catch (Exception ignored2) {
                }
            }
        }

        // 添加新纹理（如果 value != null）
        if (value != null) {
            Object newProp = this.createProperty("textures", value, signature);
            if (this.usePaperProfile) {
                // Paper: 通过 Set 接口的 add 方法（比具体类的 getMethod 更可靠）
                try {
                    Set.class.getMethod("add", Object.class).invoke(propertyMap, newProp);
                } catch (Exception e) {
                    throw new RuntimeException("无法添加 textures 属性到 Set", e);
                }
            } else {
                // Bukkit: 通过 Map 接口的 put 方法
                try {
                    Map.class.getMethod("put", Object.class, Object.class).invoke(propertyMap, "textures", newProp);
                } catch (Exception e) {
                    throw new RuntimeException("无法添加 textures 属性到 Map", e);
                }
            }
        }
        // 写回 profile
        this.setProfileMethod.invoke(player, profile);
    }

    /**
     * 从 Paper 的 Set<ProfileProperty> 中移除 textures 属性。
     * 先尝试 removeIf，失败则用迭代器遍历移除。
     */
    @SuppressWarnings("unchecked")
    private boolean removeTexturesFromSet(Object propertyMap) {
        // 方案1: removeIf
        try {
            propertyMap.getClass().getMethod("removeIf", java.util.function.Predicate.class)
                    .invoke(propertyMap, (java.util.function.Predicate<Object>) prop -> {
                        try {
                            return "textures".equals((String) prop.getClass().getMethod("getName").invoke(prop));
                        } catch (Exception e) {
                            return false;
                        }
                    });
            return true;
        } catch (Exception ignored) {
        }
        // 方案2: 迭代器遍历移除
        try {
            Iterator<Object> it = ((Set<Object>) propertyMap).iterator();
            while (it.hasNext()) {
                Object prop = it.next();
                String name = (String) prop.getClass().getMethod("getName").invoke(prop);
                if ("textures".equals(name)) {
                    it.remove();
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
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
            this.usePaperProfile = true;
            this.plugin.getLogger().info("使用 Paper PlayerProfile API 应用皮肤");
            return;
        } catch (Exception ignored) {
        }
        // 回退到 Bukkit/Spigot 的 getProfile/setProfile
        try {
            this.getProfileMethod = pc.getMethod("getProfile");
            this.setProfileMethod = pc.getMethod("setProfile", Class.forName("com.mojang.authlib.GameProfile"));
            this.usePaperProfile = false;
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
