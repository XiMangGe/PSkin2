/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.pskin;

import com.pskin.AuthMeListener;
import com.pskin.JoinListener;
import com.pskin.MessageManager;
import com.pskin.MineskinEndpointManager;
import com.pskin.MySqlStorage;
import com.pskin.PSkinCommand;
import com.pskin.PSkinConfig;
import com.pskin.SkinApplier;
import com.pskin.SkinCache;
import com.pskin.SkinProviderManager;
import com.pskin.SkinResolveService;
import com.pskin.SourceLockManager;
import com.pskin.WebServer;
import com.pskin.WebSkinManager;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class PSkinPlugin
extends JavaPlugin {
    private PSkinConfig config;
    private MessageManager messages;
    private SkinProviderManager providerManager;
    private SkinApplier skinApplier;
    private SkinCache skinCache;
    private SourceLockManager sourceLockManager;
    private WebSkinManager webSkinManager;
    private WebServer webServer;
    private SkinResolveService resolveService;
    private MineskinEndpointManager mineskinEndpoints;
    private MySqlStorage storage;
    private File sharedDataDir;
    private boolean authmeActive;

    public boolean isAuthmeActive() {
        return this.authmeActive;
    }

    public MySqlStorage getStorage() {
        return this.storage;
    }

    public boolean isMySqlStorageActive() {
        return this.storage != null;
    }

    public String describeStorageMode() {
        if (this.storage != null) {
            return "MySQL\uff08" + this.storage.describe() + "\uff09";
        }
        if (this.sharedDataDir != null) {
            return "\u5171\u4eab\u76ee\u5f55\uff08" + this.sharedDataDir.getAbsolutePath() + "\uff09";
        }
        return "\u672c\u5730\u6587\u4ef6";
    }

    /**
     * 跨版本获取插件版本号：优先 Paper getPluginMeta()，回退 Bukkit getDescription()。
     */
    private String getPluginVersion() {
        try {
            Object meta = this.getClass().getMethod("getPluginMeta").invoke(this);
            return (String) meta.getClass().getMethod("getVersion").invoke(meta);
        } catch (Exception e) {
            try {
                return this.getDescription().getVersion();
            } catch (Exception e2) {
                return "unknown";
            }
        }
    }

    public void onEnable() {
        block21: {
            this.saveDefaultConfig();
            this.config = new PSkinConfig(this);
            this.messages = new MessageManager(this, this.config.getLanguage());
            this.storage = null;
            this.sharedDataDir = null;
            if (this.config.isMySqlStorage()) {
                MySqlStorage storage = new MySqlStorage(this.config, this.getLogger());
                if (storage.init()) {
                    this.storage = storage;
                    this.getLogger().info("\u5b58\u50a8\u6a21\u5f0f: MySQL\uff08" + storage.describe() + "\uff09\u591a\u670d\u6570\u636e\u5171\u4eab\u5df2\u542f\u7528");
                } else {
                    this.getLogger().severe("MySQL \u5b58\u50a8\u521d\u59cb\u5316\u5931\u8d25\uff0c\u5df2\u56de\u9000\u5230\u672c\u5730\u6587\u4ef6\u6a21\u5f0f\uff01\u591a\u670d\u6570\u636e\u5c06\u4e0d\u5171\u4eab\uff0c\u8bf7\u68c0\u67e5 storage.mysql \u914d\u7f6e\u3002");
                }
            } else {
                if (this.config.isMySqlTypeSelected()) {
                    this.getLogger().warning("storage.type \u5df2\u8bbe\u4e3a mysql\uff0c\u4f46 storage.mysql.enabled \u4e3a false\uff08\u6570\u636e\u5e93\u9ed8\u8ba4\u5173\u95ed\uff09\uff0c\u672c\u6b21\u4e0d\u8fde\u63a5\u6570\u636e\u5e93\u3002");
                }
                if (this.config.isSharedDirStorage()) {
                    File dir = this.config.resolveSharedDataDir();
                    try {
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        if (dir.isDirectory()) {
                            this.sharedDataDir = dir;
                            this.getLogger().info("\u5b58\u50a8\u6a21\u5f0f: \u5171\u4eab\u76ee\u5f55\uff08" + dir.getAbsolutePath() + "\uff09\u591a\u670d\u6570\u636e\u5171\u4eab\u5df2\u542f\u7528");
                            break block21;
                        }
                        this.getLogger().severe("\u5171\u4eab\u76ee\u5f55\u4e0d\u53ef\u7528\uff08\u4e0d\u662f\u6587\u4ef6\u5939\uff09: " + dir.getAbsolutePath() + "\uff0c\u5df2\u56de\u9000\u5230\u672c\u5730\u6587\u4ef6\u6a21\u5f0f\uff01");
                    }
                    catch (Exception e) {
                        this.getLogger().severe("\u5171\u4eab\u76ee\u5f55\u521b\u5efa/\u8bbf\u95ee\u5931\u8d25: " + e.getMessage() + "\uff0c\u5df2\u56de\u9000\u5230\u672c\u5730\u6587\u4ef6\u6a21\u5f0f\uff01");
                    }
                } else {
                    this.getLogger().info("\u5b58\u50a8\u6a21\u5f0f: \u672c\u5730\u6587\u4ef6\uff08\u591a\u670d\u5171\u4eab\u6570\u636e\u53ef\u5c06 storage.type \u8bbe\u4e3a shared-dir \u6216 mysql\uff09");
                }
            }
        }
        File dataDir = this.sharedDataDir != null ? this.sharedDataDir : this.getDataFolder();
        boolean sharedActive = this.sharedDataDir != null;
        this.skinCache = new SkinCache(this, this.config, this.storage, dataDir, sharedActive);
        this.mineskinEndpoints = new MineskinEndpointManager(this.config);
        this.providerManager = new SkinProviderManager(this, this.config, this.mineskinEndpoints);
        this.skinApplier = new SkinApplier(this, this.config);
        this.sourceLockManager = new SourceLockManager(this, this.storage, dataDir, sharedActive);
        this.webSkinManager = new WebSkinManager(this, this.storage, dataDir, sharedActive);
        this.providerManager.init();
        this.webServer = new WebServer(this, this.config, this.webSkinManager, this.skinCache, this.skinApplier, this.messages, this.mineskinEndpoints);
        this.resolveService = new SkinResolveService(this, this.config, this.providerManager, this.skinApplier, this.skinCache, this.webSkinManager, this.sourceLockManager, this.messages, this.webServer);
        this.getServer().getPluginManager().registerEvents((Listener)new JoinListener(this, this.config, this.resolveService), (Plugin)this);
        if (this.config.isAuthmeIntegration()) {
            this.authmeActive = new AuthMeListener(this, this.config, this.resolveService).tryRegister();
            if (this.authmeActive) {
                this.getLogger().info("AuthMe \u96c6\u6210\u5df2\u63a5\u7ba1\u8fdb\u670d\u76ae\u80a4\u5e94\u7528\uff08\u907f\u514d\u6a2a\u5e45\u91cd\u590d\u663e\u793a\u4e24\u6b21\uff09");
            }
        }
        this.webServer.start();
        PluginCommand cmd = this.getCommand("pskin");
        if (cmd != null) {
            PSkinCommand executor = new PSkinCommand(this, this.config, this.providerManager, this.skinApplier, this.skinCache, this.messages, this.sourceLockManager, this.webSkinManager, this.webServer, this.resolveService);
            cmd.setExecutor((CommandExecutor)executor);
            cmd.setTabCompleter((TabCompleter)executor);
        }
        this.getLogger().info("PSkin2 v" + this.getPluginVersion() + " \u5df2\u542f\u7528\uff08" + Bukkit.getVersion() + "\uff09");
        this.getLogger().info("\u8bed\u8a00: " + this.messages.getLanguage());
        this.getLogger().info("\u76ae\u80a4\u89e3\u6790\u89c4\u5219: \u6765\u6e90\u9501\u5b9a > \u7f51\u7ad9\u4e0a\u4f20(\u72ec\u5360) > \u624b\u52a8\u8bbe\u7f6e > " + String.join((CharSequence)" > ", this.config.getSkinPriority()) + " > \u9ed8\u8ba4\u76ae\u80a4");
        this.getLogger().info("\u57fa\u5ca9\u7248: \u81ea\u5b9a\u4e49\u76ae\u80a4=" + (this.config.isBedrockApplyCustomSkins() ? "\u5f00" : "\u5173") + "\uff0c\u8fdc\u7a0b\u76ae\u80a4\u6e90=" + (this.config.isBedrockRemoteSkins() ? "\u5f00" : "\u5173") + "\uff08\u524d\u7f00: " + this.config.getBedrockPrefix() + "\uff09");
        this.getLogger().info("LittleSkin API: " + this.config.getLittleSkinApiRoot());
        int endpointCount = this.mineskinEndpoints.getCount();
        if (endpointCount > 1) {
            this.getLogger().info("Mineskin \u7aef\u70b9: " + endpointCount + " \u4e2a\uff08\u5b98\u65b9 + " + (endpointCount - 1) + " \u955c\u50cf\uff0c\u81ea\u52a8\u5207\u6362\uff09");
        } else {
            this.getLogger().info("Mineskin \u7aef\u70b9: " + endpointCount + " \u4e2a\uff08\u955c\u50cf\u672a\u542f\u7528\uff09");
        }
        if (this.config.hasMineskinApiKey()) {
            this.getLogger().info("Mineskin API Key: \u5df2\u914d\u7f6e\uff08\u4e13\u5c5e\u9650\u6d41\u6c60\uff09");
        } else if (this.config.isWebEnabled()) {
            this.getLogger().warning("Mineskin API Key \u672a\u914d\u7f6e\uff0c\u8d70\u533f\u540d\u516c\u5171\u9650\u6d41\u6c60\uff0c\u7f51\u7ad9\u4e0a\u4f20\u9ad8\u5cf0\u671f\u5bb9\u6613\u63d0\u793a\u670d\u52a1\u7e41\u5fd9(429)\u3002");
            this.getLogger().warning("\u514d\u8d39\u7533\u8bf7\uff08\u7ea61\u5206\u949f\uff09: https://account.mineskin.org/keys \uff0c\u586b\u5230 config.yml \u7684 mineskin.api-key \u540e /pskin reload \u5373\u53ef");
        }
        this.getLogger().info("\u56fe\u7247\u6821\u9a8c: " + (this.config.isWebValidateImage() ? "\u5f00" : "\u5173"));
        if (this.config.isWebEnabled() && !this.config.hasWebAdminPassword()) {
            this.getLogger().info("\u7f51\u7ad9\u7ba1\u7406\u9875\u672a\u542f\u7528\uff08\u5728 config.yml \u8bbe\u7f6e web.admin-password \u540e /admin \u53ef\u7528\uff09");
        }
        this.getLogger().info("\u6765\u6e90\u9501\u5b9a: " + this.sourceLockManager.getCount() + " \u6761 | \u7f51\u7ad9\u76ae\u80a4: " + this.webSkinManager.getCount() + " \u4e2a");
        if (this.providerManager.isEmpty()) {
            this.getLogger().warning("\u672a\u542f\u7528\u4efb\u4f55\u76ae\u80a4\u6e90\uff0c\u8bf7\u68c0\u67e5 config.yml\uff01");
        }
    }

    public void onDisable() {
        if (this.webServer != null) {
            this.webServer.stop();
        }
        if (this.webSkinManager != null) {
            this.webSkinManager.save();
        }
        if (this.sourceLockManager != null) {
            this.sourceLockManager.save();
        }
        if (this.skinCache != null) {
            this.skinCache.saveCache();
        }
        if (this.storage != null) {
            this.storage.close();
        }
        this.getLogger().info("PSkin2 \u5df2\u5378\u8f7d");
    }

    public MineskinEndpointManager getMineskinEndpoints() {
        return this.mineskinEndpoints;
    }

    public SkinResolveService getResolveService() {
        return this.resolveService;
    }
}
