/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.pskin;

import com.pskin.MessageManager;
import com.pskin.PSkinConfig;
import com.pskin.SkinApplier;
import com.pskin.SkinCache;
import com.pskin.SkinData;
import com.pskin.SkinFetchResult;
import com.pskin.SkinProviderManager;
import com.pskin.SourceLockManager;
import com.pskin.WebServer;
import com.pskin.WebSkinManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class SkinResolveService {
    private final JavaPlugin plugin;
    private final PSkinConfig config;
    private final SkinProviderManager providerManager;
    private final SkinApplier skinApplier;
    private final SkinCache skinCache;
    private final WebSkinManager webSkinManager;
    private final SourceLockManager sourceLockManager;
    private final MessageManager messages;
    private final WebServer webServer;

    public SkinResolveService(JavaPlugin plugin, PSkinConfig config, SkinProviderManager providerManager, SkinApplier skinApplier, SkinCache skinCache, WebSkinManager webSkinManager, SourceLockManager sourceLockManager, MessageManager messages, WebServer webServer) {
        this.plugin = plugin;
        this.config = config;
        this.providerManager = providerManager;
        this.skinApplier = skinApplier;
        this.skinCache = skinCache;
        this.webSkinManager = webSkinManager;
        this.sourceLockManager = sourceLockManager;
        this.messages = messages;
        this.webServer = webServer;
    }

    public void resolveAndApply(Player player) {
        this.resolveAndApply(player, ApplyType.ALL, false);
    }

    public void resolveAndApply(Player player, ApplyType type) {
        this.resolveAndApply(player, type, false);
    }

    public void resolveAndApplyJoin(Player player) {
        this.resolveAndApply(player, ApplyType.ALL, true);
    }

    private void resolveAndApply(Player player, ApplyType type, boolean joinBanner) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (type == null) {
            type = ApplyType.ALL;
        }
        ApplyType finalType = type;
        boolean banner = joinBanner;
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> this.resolveNow(player, finalType, banner));
        } else {
            this.resolveNow(player, finalType, banner);
        }
    }

    private void resolveNow(Player player, ApplyType type, boolean joinBanner) {
        SkinData manual;
        SkinData webSkin;
        String name = player.getName();
        boolean bedrock = this.config.isBedrockPlayer(name);
        String remoteName = bedrock ? this.config.stripBedrockPrefix(name) : name;
        String lock = this.sourceLockManager.get(name);
        if (joinBanner && this.config.isJoinNotifyEnabled() && player.isOnline()) {
            this.sendJoinBanner(player, this.config.getJoinNotifyStart(), name, "");
        }
        if (lock != null) {
            if ("offline".equals(lock)) {
                if (this.config.isDebug()) {
                    this.plugin.getLogger().info(name + " \u6765\u6e90\u9501\u5b9a\u4e3a offline\uff0c\u6e05\u9664\u76ae\u80a4");
                }
                this.skinApplier.clearSkin(player);
                if (joinBanner && this.config.isJoinNotifyEnabled() && player.isOnline()) {
                    this.sendJoinBanner(player, this.config.getJoinNotifyFail(), name, "offline");
                }
                this.applyDefaultSkin(player, name, type);
                return;
            }
            if ("web".equals(lock)) {
                SkinData webSkin2 = this.webSkinManager.get(name);
                if (webSkin2 != null) {
                    if (this.config.isDebug()) {
                        this.plugin.getLogger().info(name + " \u6765\u6e90\u9501\u5b9a\u4e3a web\uff0c\u4f7f\u7528\u7f51\u7ad9\u4e0a\u4f20\u76ae\u80a4");
                    }
                    this.skinApplier.applySkin(player, webSkin2);
                    if (joinBanner && this.config.isJoinNotifyEnabled() && player.isOnline()) {
                        this.sendJoinBanner(player, this.config.getJoinNotifySuccess(), name, "web");
                    }
                    if (this.config.isWebJoinFeedback() && player.isOnline()) {
                        this.messages.send((CommandSender)player, "join-web-applied", new Object[0]);
                    }
                } else {
                    if (this.config.isDebug()) {
                        this.plugin.getLogger().info(name + " \u6765\u6e90\u9501\u5b9a\u4e3a web\uff0c\u4f46\u65e0\u7f51\u7ad9\u76ae\u80a4\uff0c\u6e05\u9664\u76ae\u80a4");
                    }
                    this.skinApplier.clearSkin(player);
                    if (joinBanner && this.config.isJoinNotifyEnabled() && player.isOnline()) {
                        this.sendJoinBanner(player, this.config.getJoinNotifyFail(), name, "web");
                    }
                    this.applyDefaultSkin(player, name, type);
                }
                return;
            }
            if (this.config.isDebug()) {
                this.plugin.getLogger().info(name + " \u6765\u6e90\u9501\u5b9a\u4e3a " + lock + "\uff0c\u6309\u9501\u5b9a\u6765\u6e90\u67e5\u8be2\uff08\u67e5\u8be2\u540d: " + remoteName + "\uff09");
            }
            this.resolveRemote(player, remoteName, lock, name, type, joinBanner);
            return;
        }
        if ((!bedrock || this.config.isBedrockApplyCustomSkins()) && (webSkin = this.webSkinManager.get(name)) != null) {
            if (this.config.isDebug()) {
                this.plugin.getLogger().info(name + " \u4f7f\u7528\u7f51\u7ad9\u4e0a\u4f20\u76ae\u80a4\uff08\u72ec\u5360\uff0c\u4e0d\u67e5\u8be2\u8fdc\u7a0b\u76ae\u80a4\u6e90\uff09");
            }
            this.skinApplier.applySkin(player, webSkin);
            if (joinBanner && this.config.isJoinNotifyEnabled() && player.isOnline()) {
                this.sendJoinBanner(player, this.config.getJoinNotifySuccess(), name, "web");
            }
            if (this.config.isWebJoinFeedback() && player.isOnline()) {
                this.messages.send((CommandSender)player, "join-web-applied", new Object[0]);
            }
            return;
        }
        if ((!bedrock || this.config.isBedrockApplyCustomSkins()) && this.skinCache.isManual(name) && (manual = this.skinCache.get(name)) != null) {
            if (this.config.isDebug()) {
                this.plugin.getLogger().info(name + " \u4f7f\u7528\u624b\u52a8\u8bbe\u7f6e\u76ae\u80a4\uff08\u6765\u6e90: " + manual.getDisplaySource() + "\uff09");
            }
            this.skinApplier.applySkin(player, manual);
            if (joinBanner && this.config.isJoinNotifyEnabled() && player.isOnline()) {
                this.sendJoinBanner(player, this.config.getJoinNotifySuccess(), name, manual.getDisplaySource());
            }
            return;
        }
        if (!bedrock || this.config.isBedrockRemoteSkins()) {
            SkinData cached = this.skinCache.get(name);
            if (cached != null) {
                if (this.config.isDebug()) {
                    this.plugin.getLogger().info(name + " \u4f7f\u7528\u7f13\u5b58\u76ae\u80a4\uff08\u6765\u6e90: " + cached.getDisplaySource() + "\uff09");
                }
                this.skinApplier.applySkin(player, cached);
                if (joinBanner && this.config.isJoinNotifyEnabled() && player.isOnline()) {
                    this.sendJoinBanner(player, this.config.getJoinNotifySuccess(), name, cached.getDisplaySource());
                }
                return;
            }
            for (String entry : this.config.getSkinPriority()) {
                switch (entry.toLowerCase()) {
                    case "web": 
                    case "manual": {
                        break;
                    }
                    case "mojang": 
                    case "littleskin": {
                        SkinFetchResult result = this.providerManager.resolveSkin(remoteName, entry).join();
                        if (!result.isSuccess()) break;
                        this.skinCache.put(name, result.getSkinData(), false);
                        this.skinApplier.applySkin(player, result.getSkinData());
                        if (this.config.isDebug()) {
                            this.plugin.getLogger().info("\u81ea\u52a8\u5e94\u7528 " + name + " \u7684\u76ae\u80a4\uff08\u67e5\u8be2\u540d: " + remoteName + "\uff0c\u6765\u6e90: " + result.getSkinData().getDisplaySource() + "\uff09");
                        }
                        if (player.isOnline()) {
                            this.notifyJoinResult(player, joinBanner, name, result.getSkinData().getDisplaySource(), true);
                        }
                        return;
                    }
                }
            }
        }
        if (this.config.isDefaultSkinEnabled()) {
            if (joinBanner && this.config.isJoinNotifyEnabled() && player.isOnline()) {
                this.sendJoinBanner(player, this.config.getJoinNotifyFail(), name, "");
            }
            this.applyDefaultSkin(player, name, type);
        } else if (joinBanner && this.config.isJoinNotifyEnabled() && player.isOnline()) {
            this.sendJoinBanner(player, this.config.getJoinNotifyFail(), name, "");
        } else if (this.config.isDebug()) {
            this.plugin.getLogger().info("\u672a\u627e\u5230 " + name + " \u7684\u76ae\u80a4\uff0c\u4fdd\u6301\u9ed8\u8ba4");
        }
    }

    private void resolveRemote(Player player, String remoteName, String source, String name, ApplyType type, boolean joinBanner) {
        if (!player.isOnline()) {
            return;
        }
        SkinFetchResult result = this.providerManager.resolveSkin(remoteName, source).join();
        if (!player.isOnline()) {
            return;
        }
        if (result.isSuccess()) {
            this.skinCache.put(name, result.getSkinData(), false);
            this.skinApplier.applySkin(player, result.getSkinData());
            if (player.isOnline()) {
                this.notifyJoinResult(player, joinBanner, name, result.getSkinData().getDisplaySource(), true);
            }
        } else {
            this.skinApplier.clearSkin(player);
            if (joinBanner && this.config.isJoinNotifyEnabled() && player.isOnline()) {
                this.sendJoinBanner(player, this.config.getJoinNotifyFail(), name, source);
            }
            this.applyDefaultSkin(player, name, type);
        }
    }

    private void notifyJoinResult(Player player, boolean joinBanner, String name, String source, boolean success) {
        if (!player.isOnline()) {
            return;
        }
        if (joinBanner && this.config.isJoinNotifyEnabled()) {
            this.sendJoinBanner(player, success ? this.config.getJoinNotifySuccess() : this.config.getJoinNotifyFail(), name, source);
        } else if (success && this.config.isAutoApplyFeedback()) {
            this.messages.send((CommandSender)player, "join-auto-applied", source);
        }
    }

    private void sendJoinBanner(Player player, List<String> template, String name, String source) {
        if (template == null || template.isEmpty()) {
            return;
        }
        String playerName = name == null ? "" : name;
        String src = source == null || source.isEmpty() ? "?" : source;
        String server = this.plugin.getServer().getName();
        ArrayList<String> rendered = new ArrayList<String>(template.size());
        for (String line : template) {
            if (line == null) continue;
            rendered.add(ChatColor.translateAlternateColorCodes((char)'&', (String)line.replace("{player}", playerName).replace("{source}", src).replace("{server}", server)));
        }
        if (rendered.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage(rendered.toArray(new String[0]));
            }
        });
    }

    private void applyDefaultSkin(Player player, String name, ApplyType type) {
        String defaultName = this.config.getDefaultSkinName();
        if (defaultName == null || defaultName.isEmpty() || defaultName.equalsIgnoreCase(name)) {
            return;
        }
        this.providerManager.resolveSkin(defaultName).thenAccept(result -> {
            if (!player.isOnline() || !result.isSuccess()) {
                return;
            }
            this.skinApplier.applySkin(player, result.getSkinData());
            if (this.config.isDebug()) {
                this.plugin.getLogger().info("\u7ed9 " + name + " \u5e94\u7528\u9ed8\u8ba4\u76ae\u80a4 " + defaultName);
            }
        });
    }

    public void resolveAndApplyFresh(Player player) {
        if (player == null) {
            return;
        }
        this.skinCache.invalidate(player.getName());
        this.resolveAndApply(player);
    }

    public static enum ApplyType {
        SKIN,
        ALL;

    }
}
