/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.TextComponent$Builder
 *  net.kyori.adventure.text.event.ClickEvent
 *  net.kyori.adventure.text.event.HoverEvent
 *  net.kyori.adventure.text.event.HoverEventSource
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.pskin;

import com.pskin.MessageManager;
import com.pskin.MineskinEndpointManager;
import com.pskin.PSkinConfig;
import com.pskin.PSkinPlugin;
import com.pskin.SkinApplier;
import com.pskin.SkinCache;
import com.pskin.SkinData;
import com.pskin.SkinFetchResult;
import com.pskin.SkinProviderManager;
import com.pskin.SkinResolveService;
import com.pskin.SourceLockManager;
import com.pskin.UrlSkinProvider;
import com.pskin.WebServer;
import com.pskin.WebSkinManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class PSkinCommand
implements CommandExecutor,
TabCompleter {
    private static final List<String> SOURCE_OPTIONS = List.of("mojang", "littleskin", "offline", "web", "reset");
    private final JavaPlugin plugin;
    private final PSkinConfig config;
    private final SkinProviderManager providerManager;
    private final SkinApplier skinApplier;
    private final SkinCache skinCache;
    private final MessageManager messages;
    private final SourceLockManager sourceLockManager;
    private final WebSkinManager webSkinManager;
    private final WebServer webServer;
    private final SkinResolveService resolveService;
    private final UrlSkinProvider urlSkinProvider;
    private final Map<UUID, Long> cooldowns = new HashMap<UUID, Long>();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public PSkinCommand(JavaPlugin plugin, PSkinConfig config, SkinProviderManager providerManager, SkinApplier skinApplier, SkinCache skinCache, MessageManager messages, SourceLockManager sourceLockManager, WebSkinManager webSkinManager, WebServer webServer, SkinResolveService resolveService) {
        this.plugin = plugin;
        this.config = config;
        this.providerManager = providerManager;
        this.skinApplier = skinApplier;
        this.skinCache = skinCache;
        this.messages = messages;
        this.sourceLockManager = sourceLockManager;
        this.webSkinManager = webSkinManager;
        this.webServer = webServer;
        this.resolveService = resolveService;
        this.urlSkinProvider = new UrlSkinProvider(config);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            this.sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "set": {
                this.handleSet(sender, args);
                break;
            }
            case "url": {
                this.handleUrl(sender, args);
                break;
            }
            case "source": {
                this.handleSource(sender, args);
                break;
            }
            case "delete": {
                this.handleDelete(sender, args);
                break;
            }
            case "web": {
                this.handleWeb(sender);
                break;
            }
            case "clear": {
                this.handleClear(sender, args);
                break;
            }
            case "update": {
                this.handleUpdate(sender, args);
                break;
            }
            case "info": {
                this.handleInfo(sender, args);
                break;
            }
            case "apply": {
                this.handleApply(sender, args);
                break;
            }
            case "reload": {
                this.handleReload(sender);
                break;
            }
            case "status": {
                this.handleStatus(sender);
                break;
            }
            case "help": {
                this.sendHelp(sender);
                break;
            }
            default: {
                this.messages.send(sender, "unknown-command", new Object[0]);
            }
        }
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            this.messages.send((CommandSender)player, "usage-set", new Object[0]);
            return;
        }
        if (this.checkCooldown(player)) {
            return;
        }
        String targetName = args[1];
        String playerName = player.getName();
        this.messages.send((CommandSender)player, "fetching", targetName);
        this.providerManager.resolveSkin(targetName).thenAccept(result -> Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (result.isSuccess()) {
                SkinData skinData = result.getSkinData();
                this.webSkinManager.remove(playerName);
                this.skinCache.put(playerName, skinData, true);
                this.skinApplier.applySkin(player, skinData);
                this.messages.send((CommandSender)player, "skin-applied", targetName, result.getSource());
            } else {
                this.sendFetchError((CommandSender)player, (SkinFetchResult)result, targetName);
            }
        }));
    }

    private void handleUrl(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            this.messages.send((CommandSender)player, "usage-url", new Object[0]);
            return;
        }
        if (this.checkCooldown(player)) {
            return;
        }
        String url = args[1];
        try {
            String scheme;
            URI uri = URI.create(url);
            String string = scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (uri.getHost() == null || !scheme.equals("http") && !scheme.equals("https")) {
                this.messages.send((CommandSender)player, "invalid-url", new Object[0]);
                return;
            }
        }
        catch (Exception e) {
            this.messages.send((CommandSender)player, "invalid-url", new Object[0]);
            return;
        }
        String playerName = player.getName();
        this.messages.send((CommandSender)player, "fetching-url", new Object[0]);
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            SkinFetchResult result = this.urlSkinProvider.fetchSkinFromUrl(url);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (result.isSuccess()) {
                    this.webSkinManager.remove(playerName);
                    this.skinCache.put(playerName, result.getSkinData(), true);
                    this.skinApplier.applySkin(player, result.getSkinData());
                    this.messages.send((CommandSender)player, "skin-applied-url", new Object[0]);
                } else {
                    this.sendUrlError((CommandSender)player, result);
                }
            });
        });
    }

    private void handleSource(CommandSender sender, String[] args) {
        String targetName;
        String sourceArg = null;
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                this.messages.send(sender, "usage-source", new Object[0]);
                return;
            }
            Player p = (Player)sender;
            targetName = p.getName();
        } else if (args.length == 2) {
            boolean isSourceArg;
            String a1 = args[1];
            boolean bl = isSourceArg = "reset".equalsIgnoreCase(a1) || SourceLockManager.normalizeSource(a1) != null || (this.config.isCustomYggdrasil(a1) && this.config.isCustomYggdrasilEnabled(a1));
            if (isSourceArg) {
                Player p = this.requirePlayer(sender);
                if (p == null) {
                    return;
                }
                targetName = p.getName();
                sourceArg = a1;
            } else {
                if (!sender.hasPermission("pskin.admin")) {
                    this.messages.send(sender, "usage-source", new Object[0]);
                    return;
                }
                targetName = a1;
            }
        } else {
            if (!sender.hasPermission("pskin.admin")) {
                this.messages.send(sender, "no-permission", new Object[0]);
                return;
            }
            targetName = args[1];
            sourceArg = args[2];
        }
        if (sourceArg == null) {
            String lock = this.sourceLockManager.get(targetName);
            if (lock == null) {
                this.messages.send(sender, "source-current-none", targetName);
                return;
            }
            this.messages.send(sender, "source-current", targetName, lock);
            return;
        }
        if ("reset".equalsIgnoreCase(sourceArg)) {
            this.sourceLockManager.reset(targetName);
            this.messages.send(sender, "source-reset", targetName);
            this.reapplyAfterLockChange(targetName);
            return;
        }
        String normalized = SourceLockManager.normalizeSource(sourceArg);
        if (normalized == null) {
            // 检查是否是自定义皮肤站名称
            if (this.config.isCustomYggdrasil(sourceArg) && this.config.isCustomYggdrasilEnabled(sourceArg)) {
                normalized = sourceArg.toLowerCase();
            }
        }
        if (normalized == null) {
            this.messages.send(sender, "source-invalid", new Object[0]);
            return;
        }
        this.sourceLockManager.set(targetName, normalized);
        this.messages.send(sender, "source-set", targetName, normalized);
        this.reapplyAfterLockChange(targetName);
    }

    private void reapplyAfterLockChange(String playerName) {
        Player target = Bukkit.getPlayerExact((String)playerName);
        if (target == null || !target.isOnline()) {
            return;
        }
        this.skinCache.invalidate(playerName);
        this.messages.send((CommandSender)target, "source-reapplying", new Object[0]);
        this.resolveService.resolveAndApply(target);
    }

    private void handleDelete(CommandSender sender, String[] args) {
        String target;
        boolean isAdmin = sender.hasPermission("pskin.admin");
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                this.messages.send(sender, "usage-delete", new Object[0]);
                return;
            }
            Player p = (Player)sender;
            target = p.getName();
        } else if (args.length == 2) {
            if (!isAdmin) {
                this.messages.send(sender, "no-permission", new Object[0]);
                return;
            }
            target = args[1];
        } else {
            this.messages.send(sender, "usage-delete", new Object[0]);
            return;
        }
        boolean skinRemoved = this.webSkinManager.remove(target);
        if (!skinRemoved) {
            this.messages.send(sender, "web-skin-notfound", target);
            return;
        }
        this.messages.send(sender, "web-skin-deleted", target);
        Player targetPlayer = Bukkit.getPlayerExact((String)target);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            this.resolveService.resolveAndApply(targetPlayer);
        }
    }

    private void handleWeb(CommandSender sender) {
        if (!this.config.isWebEnabled() || this.webServer == null) {
            this.messages.send(sender, "web-disabled", new Object[0]);
            return;
        }
        if (!this.config.isWebShowLink()) {
            this.messages.send(sender, "web-link-hidden", new Object[0]);
            return;
        }
        this.messages.send(sender, "web-address", new Object[0]);
        this.sendWebLink(sender);
    }

    private void sendWebLink(CommandSender sender) {
        if (this.webServer == null) {
            return;
        }
        String addr = this.webServer.getDisplayAddress();
        String url = addr.startsWith("http://") || addr.startsWith("https://") ? addr : "http://" + addr;
        try {
            Component link = ((TextComponent)Component.text((String)url).clickEvent(ClickEvent.openUrl((String)url))).decorate(TextDecoration.UNDERLINED);
            sender.sendMessage(link);
        }
        catch (Exception e) {
            sender.sendMessage(url);
        }
    }

    private void handleClear(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null) {
            return;
        }
        String target = player.getName();
        boolean isAdmin = player.hasPermission("pskin.admin");
        if (args.length >= 2) {
            if (isAdmin) {
                target = args[1];
            } else {
                this.messages.send(sender, "usage-clear", new Object[0]);
                return;
            }
        }
        this.skinCache.remove(target);
        this.webSkinManager.remove(target);
        this.messages.send((CommandSender)player, "skin-cleared", target);
        Player targetPlayer = Bukkit.getPlayerExact((String)target);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            this.resolveService.resolveAndApply(targetPlayer);
            this.messages.send((CommandSender)player, "skin-cleared-reapply", new Object[0]);
        }
    }

    private void handleUpdate(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (this.checkCooldown(player)) {
            return;
        }
        String name = player.getName();
        this.messages.send((CommandSender)player, "updating", new Object[0]);
        SkinData webSkin = this.webSkinManager.get(name);
        if (webSkin != null) {
            this.skinApplier.applySkin(player, webSkin);
            this.messages.send((CommandSender)player, "web-skin-applied", new Object[0]);
            return;
        }
        this.skinCache.invalidate(name);
        String lock = this.sourceLockManager.get(name);
        String lookupName = this.config.isBedrockPlayer(name) ? this.config.stripBedrockPrefix(name) : name;
        this.providerManager.resolveSkin(lookupName, lock).thenAccept(result -> Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (result.isSuccess()) {
                this.skinCache.put(name, result.getSkinData(), false);
                this.skinApplier.applySkin(player, result.getSkinData());
                this.messages.send((CommandSender)player, "update-success", result.getSource());
            } else {
                this.sendFetchError((CommandSender)player, (SkinFetchResult)result, lookupName);
            }
        }));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        SkinData data;
        String target;
        if (args.length >= 2 && sender.hasPermission("pskin.admin")) {
            target = args[1];
        } else if (sender instanceof Player) {
            Player p = (Player)sender;
            target = p.getName();
        } else {
            this.messages.send(sender, "usage-info", new Object[0]);
            return;
        }
        this.messages.send(sender, "info-header", new Object[0]);
        this.messages.send(sender, "info-player", target);
        String lock = this.sourceLockManager.get(target);
        this.messages.send(sender, "info-lock", lock == null ? this.messages.get("info-lock-auto", new Object[0]) : lock);
        if (this.webSkinManager.has(target)) {
            this.messages.send(sender, "info-web", new Object[0]);
        }
        if ((data = this.skinCache.get(target)) == null) {
            this.messages.send(sender, "info-none", new Object[0]);
            return;
        }
        this.messages.send(sender, "info-source", data.getDisplaySource());
        if (data.hasSignature()) {
            this.messages.send(sender, "info-signed", new Object[0]);
        } else {
            this.messages.send(sender, "info-unsigned", new Object[0]);
        }
        if (this.skinCache.isManual(target)) {
            this.messages.send(sender, "info-cached", new Object[0]);
        } else {
            this.messages.send(sender, "info-notcached", new Object[0]);
        }
    }

    private void handleApply(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pskin.admin")) {
            this.messages.send(sender, "no-permission", new Object[0]);
            return;
        }
        if (args.length < 2) {
            this.messages.send(sender, "usage-apply", new Object[0]);
            return;
        }
        String targetName = args[1];
        if (args.length >= 3) {
            this.messages.send(sender, "usage-apply", new Object[0]);
            return;
        }
        Player target = Bukkit.getPlayer((String)targetName);
        if (target == null || !target.isOnline()) {
            this.messages.send(sender, "player-not-online", targetName);
            return;
        }
        this.messages.send(sender, "applying", targetName);
        String lookupName = this.config.isBedrockPlayer(targetName) ? this.config.stripBedrockPrefix(targetName) : targetName;
        String lock = this.sourceLockManager.get(targetName);
        this.providerManager.resolveSkin(lookupName, lock).thenAccept(result -> Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (result.isSuccess()) {
                this.skinCache.put(targetName, result.getSkinData(), false);
                this.skinApplier.applySkin(target, result.getSkinData());
                this.messages.send(sender, "apply-success", targetName, result.getSource());
            } else {
                this.sendFetchError(sender, (SkinFetchResult)result, lookupName);
            }
        }));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("pskin.admin")) {
            this.messages.send(sender, "no-permission", new Object[0]);
            return;
        }
        this.config.reload();
        this.providerManager.init();
        this.messages.reload(this.config.getLanguage());
        if (this.webServer != null) {
            this.webServer.reloadLanguage();
        }
        this.messages.send(sender, "reloaded", new Object[0]);
        this.messages.send(sender, "language-current", this.messages.getLanguage());
        if (this.plugin instanceof PSkinPlugin) {
            boolean activeShared;
            PSkinPlugin pl = (PSkinPlugin)this.plugin;
            boolean configMysql = this.config.isMySqlStorage();
            boolean configShared = this.config.isSharedDirStorage();
            boolean bl = activeShared = !pl.isMySqlStorageActive() && this.config.isSharedDirStorage() && pl.describeStorageMode().contains("\u5171\u4eab\u76ee\u5f55");
            if (configMysql != pl.isMySqlStorageActive() || configShared != activeShared) {
                String configMode = configMysql ? "mysql" : (configShared ? "shared-dir" : "file");
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "\u26a0 \u5b58\u50a8\u6a21\u5f0f\u914d\u7f6e\u5df2\u6539\u4e3a " + configMode + "\uff0c\u4f46\u9700\u8981\u91cd\u542f\u670d\u52a1\u5668\u624d\u80fd\u751f\u6548\uff08\u5f53\u524d\u4ecd\u4e3a " + pl.describeStorageMode() + "\uff09");
            }
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "\u5b58\u50a8\u6a21\u5f0f storage.type: " + this.config.getStorageType() + String.valueOf(ChatColor.GRAY) + " \u5f53\u524d\u751f\u6548: " + pl.describeStorageMode());
            if (this.config.isMySqlTypeSelected() && !this.config.isMySqlEnabled()) {
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "\u26a0 storage.mysql.enabled \u4e3a false\uff08\u6570\u636e\u5e93\u9ed8\u8ba4\u5173\u95ed\uff09\uff0c\u8981\u7528 MySQL \u9700\u628a\u5b83\u6539\u4e3a true \u5e76\u91cd\u542f\u3002");
            }
        }
        String showLinkState = this.config.isWebShowLink() ? String.valueOf(ChatColor.GREEN) + "\u663e\u793a" : String.valueOf(ChatColor.GRAY) + "\u9690\u85cf";
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "\u7f51\u7ad9\u5165\u53e3\u663e\u793a web.show-link: " + showLinkState + String.valueOf(ChatColor.GRAY) + " | \u7f51\u7ad9\u529f\u80fd web.enabled: " + (this.config.isWebEnabled() ? "\u5f00\u542f" : "\u5173\u95ed"));
        String err = this.config.getConfigLoadError();
        if (err != null) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "\u26a0 config.yml \u6709\u8bed\u6cd5\u9519\u8bef\uff0c\u4f60\u7684\u4fee\u6539\u672a\u751f\u6548\uff0c\u5f53\u524d\u7528\u7684\u662f\u9ed8\u8ba4\u503c!");
            sender.sendMessage(String.valueOf(ChatColor.RED) + "  \u9519\u8bef: " + err);
            sender.sendMessage(String.valueOf(ChatColor.RED) + "  \u63d0\u793a: \u989c\u8272\u4ee3\u7801 & \u5f00\u5934\u7684\u884c\u5fc5\u987b\u7528\u53cc\u5f15\u53f7\u5305\u8d77\u6765\uff0c\u5982 - \"&6&lPSkin2\"");
        } else if (this.config.getMergedKeys() > 0) {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "\u5df2\u81ea\u52a8\u8865\u5168 config.yml \u7f3a\u5931\u7684 " + this.config.getMergedKeys() + " \u4e2a\u914d\u7f6e\u9879\uff08\u5df2\u4fdd\u5b58\u5230\u6587\u4ef6\uff0c\u53ef\u76f4\u63a5\u7f16\u8f91\uff09\u3002");
        }
        String joinState = this.config.isJoinNotifyEnabled() ? String.valueOf(ChatColor.GREEN) + "\u5f00\u542f" : String.valueOf(ChatColor.RED) + "\u5173\u95ed";
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "\u8fdb\u670d\u6a2a\u5e45 join-notify: " + joinState + String.valueOf(ChatColor.GRAY) + " (\u5f00\u59cb " + this.config.getJoinNotifyStart().size() + " \u884c / \u6210\u529f " + this.config.getJoinNotifySuccess().size() + " \u884c / \u5931\u8d25 " + this.config.getJoinNotifyFail().size() + " \u884c)");
        String feedbackState = this.config.isAutoApplyFeedback() ? String.valueOf(ChatColor.GREEN) + "\u5f00\u542f" : String.valueOf(ChatColor.RED) + "\u5173\u95ed";
        String webFeedbackState = this.config.isWebJoinFeedback() ? String.valueOf(ChatColor.GREEN) + "\u5f00\u542f" : String.valueOf(ChatColor.RED) + "\u5173\u95ed";
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "\u6307\u4ee4\u53cd\u9988 auto-apply-feedback: " + feedbackState + String.valueOf(ChatColor.GRAY) + " | \u7f51\u7ad9\u4e0a\u4f20\u53cd\u9988 web.join-feedback: " + webFeedbackState);
    }

    private void handleStatus(CommandSender sender) {
        MineskinEndpointManager endpoints;
        if (!sender.hasPermission("pskin.admin")) {
            this.messages.send(sender, "no-permission", new Object[0]);
            return;
        }
        this.messages.send(sender, "status-header", new Object[0]);
        this.messages.send(sender, "status-cached", this.skinCache.getCachedCount());
        this.messages.send(sender, "status-manual", this.skinCache.getManualCount());
        this.messages.send(sender, "status-web", this.webSkinManager.getCount());
        this.messages.send(sender, "status-locks", this.sourceLockManager.getCount());
        this.messages.send(sender, "status-providers", this.providerManager.getProviderNames());
        MineskinEndpointManager mineskinEndpointManager = endpoints = this.plugin instanceof PSkinPlugin ? ((PSkinPlugin)this.plugin).getMineskinEndpoints() : null;
        if (endpoints != null) {
            this.messages.send(sender, "status-mineskin", endpoints.getCurrent(), endpoints.getCount());
        }
        this.messages.send(sender, this.config.hasMineskinApiKey() ? "status-apikey-on" : "status-apikey-off", new Object[0]);
        this.messages.send(sender, "status-language", this.messages.getLanguage());
        if (this.config.isWebEnabled() && this.webServer != null) {
            this.messages.send(sender, "status-webaddr", this.webServer.getDisplayAddress());
        }
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            Player p = (Player)sender;
            return p;
        }
        this.messages.send(sender, "player-only", new Object[0]);
        return null;
    }

    private boolean checkCooldown(Player player) {
        if (player.hasPermission("pskin.bypasscooldown")) {
            return false;
        }
        int cooldown = this.config.getCommandCooldown();
        if (cooldown <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = this.cooldowns.get(player.getUniqueId());
        if (last != null && now - last < (long)cooldown * 1000L) {
            long remaining = ((long)cooldown * 1000L - (now - last) + 999L) / 1000L;
            this.messages.send((CommandSender)player, "cooldown", remaining);
            return true;
        }
        this.cooldowns.put(player.getUniqueId(), now);
        return false;
    }

    private void sendFetchError(CommandSender sender, SkinFetchResult result, String targetName) {
        switch (result.getStatus()) {
            case NOT_FOUND: {
                this.messages.send(sender, "skin-not-found", targetName);
                break;
            }
            case NETWORK_ERROR: {
                this.messages.send(sender, "fetch-network-error", new Object[0]);
                break;
            }
            case TIMEOUT: {
                this.messages.send(sender, "fetch-timeout", new Object[0]);
                break;
            }
            case RATE_LIMITED: {
                this.messages.send(sender, "fetch-rate-limited", new Object[0]);
                break;
            }
            case API_ERROR: {
                this.messages.send(sender, "fetch-api-error", new Object[0]);
                break;
            }
            case INVALID_RESPONSE: {
                this.messages.send(sender, "fetch-invalid-response", new Object[0]);
                break;
            }
            default: {
                this.messages.send(sender, "fetch-unknown-error", new Object[0]);
            }
        }
    }

    private void sendUrlError(CommandSender sender, SkinFetchResult result) {
        switch (result.getStatus()) {
            case NETWORK_ERROR: {
                this.messages.send(sender, "url-network-error", new Object[0]);
                break;
            }
            case TIMEOUT: {
                this.messages.send(sender, "url-timeout", new Object[0]);
                break;
            }
            case URL_RATE_LIMITED: {
                this.messages.send(sender, "url-rate-limited", new Object[0]);
                break;
            }
            case URL_INVALID_IMAGE: {
                this.messages.send(sender, "url-invalid-image", new Object[0]);
                break;
            }
            case URL_IMAGE_TOO_LARGE: {
                this.messages.send(sender, "url-image-too-large", new Object[0]);
                break;
            }
            default: {
                this.messages.send(sender, "url-failed", new Object[0]);
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        boolean admin = sender.hasPermission("pskin.admin");
        sender.sendMessage((Component)LEGACY.deserialize(this.messages.get("help-header", new Object[0])));
        sender.sendMessage((Component)LEGACY.deserialize(this.messages.get("help-tip", new Object[0])));
        sender.sendMessage((Component)LEGACY.deserialize(this.messages.get("help-section-player", new Object[0])));
        this.sendHelpEntry(sender, "set");
        this.sendHelpEntry(sender, "url");
        this.sendHelpEntry(sender, "source");
        this.sendHelpEntry(sender, "delete");
        this.sendHelpEntry(sender, "web");
        this.sendHelpEntry(sender, "clear");
        this.sendHelpEntry(sender, "update");
        this.sendHelpEntry(sender, "info");
        if (admin) {
            sender.sendMessage((Component)LEGACY.deserialize(this.messages.get("help-section-admin", new Object[0])));
            this.sendHelpEntry(sender, "apply");
            this.sendHelpEntry(sender, "status");
            this.sendHelpEntry(sender, "reload");
        }
        if (this.config.isWebEnabled() && this.webServer != null && this.config.isWebShowLink()) {
            sender.sendMessage((Component)LEGACY.deserialize(this.messages.get("help-footer-web", new Object[0])));
            this.sendWebLink(sender);
        }
    }

    private void sendHelpEntry(CommandSender sender, String cmd) {
        String cmdText = this.messages.get("help-" + cmd + "-cmd", new Object[0]);
        String desc = this.messages.get("help-" + cmd + "-desc", new Object[0]);
        String detail = this.messages.get("help-" + cmd + "-detail", new Object[0]);
        Component line = ((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)LEGACY.deserialize(" &e" + cmdText))).append((Component)LEGACY.deserialize(" &8- &7" + desc))).build();
        boolean hasArgs = cmdText.contains("<") || cmdText.contains("[");
        line = hasArgs ? line.clickEvent(ClickEvent.suggestCommand((String)cmdText)) : line.clickEvent(ClickEvent.runCommand((String)cmdText));
        Component hover = ((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)LEGACY.deserialize("&e&l" + cmdText))).append((Component)Component.newline())).append((Component)LEGACY.deserialize(detail))).append((Component)Component.newline())).append((Component)LEGACY.deserialize(this.messages.get(hasArgs ? "help-click-suggest" : "help-click-run", new Object[0])))).build();
        line = line.hoverEvent((HoverEventSource)HoverEvent.showText((Component)hover));
        sender.sendMessage(line);
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            ArrayList<String> subs = new ArrayList<String>(Arrays.asList("set", "url", "source", "delete", "web", "clear", "update", "info", "help"));
            if (sender.hasPermission("pskin.admin")) {
                subs.add("apply");
                subs.add("status");
                subs.add("reload");
            }
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub;
            switch (sub = args[0].toLowerCase()) {
                case "source": {
                    ArrayList<String> options = new ArrayList<String>(SOURCE_OPTIONS);
                    if (sender.hasPermission("pskin.admin")) {
                        Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(options::add);
                    }
                    return options.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
                case "delete": 
                case "apply": 
                case "clear": {
                    if (!sender.hasPermission("pskin.admin")) {
                        return new ArrayList<String>();
                    }
                    ArrayList<String> opts1 = new ArrayList<>();
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(opts1::add);
                    return opts1.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
                case "info": {
                    ArrayList<String> opts2 = new ArrayList<>();
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(opts2::add);
                    return opts2.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
            }
            return new ArrayList<String>();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("source") && sender.hasPermission("pskin.admin")) {
            return SOURCE_OPTIONS.stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }
        return new ArrayList<String>();
    }
}
