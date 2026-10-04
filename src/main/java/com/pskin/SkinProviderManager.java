/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.pskin;

import com.pskin.LittleSkinProvider;
import com.pskin.MineskinEndpointManager;
import com.pskin.MineskinResigner;
import com.pskin.MojangSkinProvider;
import com.pskin.PSkinConfig;
import com.pskin.SkinData;
import com.pskin.SkinFetchResult;
import com.pskin.SkinProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.bukkit.plugin.java.JavaPlugin;

public class SkinProviderManager {
    private final List<SkinProvider> providers = new ArrayList<SkinProvider>();
    private final PSkinConfig config;
    private final JavaPlugin plugin;
    private final MineskinResigner mineskinResigner;

    public SkinProviderManager(JavaPlugin plugin, PSkinConfig config) {
        this(plugin, config, new MineskinEndpointManager(config));
    }

    public SkinProviderManager(JavaPlugin plugin, PSkinConfig config, MineskinEndpointManager endpointManager) {
        this.plugin = plugin;
        this.config = config;
        this.mineskinResigner = new MineskinResigner(plugin.getLogger(), config, endpointManager);
    }

    public void init() {
        this.providers.clear();
        List<String> order = this.config.getProviderOrder();
        block8: for (String name : order) {
            switch (name.toLowerCase()) {
                case "mojang": {
                    if (!this.config.isMojangEnabled()) continue block8;
                    this.providers.add(new MojangSkinProvider(this.config));
                    this.plugin.getLogger().info("\u5df2\u6ce8\u518c\u76ae\u80a4\u6e90: Mojang (\u6b63\u7248)");
                    continue block8;
                }
                case "littleskin": {
                    if (!this.config.isLittleSkinEnabled()) continue block8;
                    this.providers.add(new LittleSkinProvider(this.config));
                    this.plugin.getLogger().info("\u5df2\u6ce8\u518c\u76ae\u80a4\u6e90: LittleSkin (" + this.config.getLittleSkinApiRoot() + ")");
                    continue block8;
                }
            }
            this.plugin.getLogger().warning("\u914d\u7f6e\u4e2d\u672a\u77e5\u7684\u76ae\u80a4\u6e90: " + name + "\uff08\u53ef\u7528: mojang, littleskin\uff09");
        }
        if (this.providers.isEmpty()) {
            this.plugin.getLogger().warning("\u6ca1\u6709\u542f\u7528\u4efb\u4f55\u76ae\u80a4\u6e90\uff01\u8bf7\u5728 config.yml \u7684 providers.order \u4e2d\u914d\u7f6e");
        }
    }

    public CompletableFuture<SkinFetchResult> resolveSkin(String username) {
        return this.resolveSkin(username, null);
    }

    public CompletableFuture<SkinFetchResult> resolveSkin(String username, String forcedSource) {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<SkinProvider> toTry = new ArrayList<SkinProvider>();
            if (forcedSource != null) {
                for (SkinProvider p : this.providers) {
                    if (!p.getName().equalsIgnoreCase(forcedSource)) continue;
                    toTry.add(p);
                    break;
                }
                if (toTry.isEmpty() && this.config.isDebug()) {
                    this.plugin.getLogger().warning("\u6765\u6e90\u9501\u5b9a\u4e3a " + forcedSource + "\uff0c\u4f46\u8be5\u76ae\u80a4\u6e90\u672a\u542f\u7528");
                }
            } else {
                toTry.addAll(this.providers);
            }
            SkinFetchResult lastError = SkinFetchResult.notFound();
            for (SkinProvider provider : toTry) {
                try {
                    SkinFetchResult result = provider.fetchSkin(username).join();
                    if (result != null && result.isSuccess()) {
                        if (this.config.isDebug()) {
                            this.plugin.getLogger().info("\u901a\u8fc7 " + provider.getName() + " \u89e3\u6790\u5230 " + username + " \u7684\u76ae\u80a4\uff08\u539f\u59cb\u6765\u6e90: " + result.getSkinData().getDisplaySource() + "\uff09");
                        }
                        if (this.config.isMineskinResignEnabled() && !"mojang".equalsIgnoreCase(result.getSkinData().getSource())) {
                            try {
                                SkinData resigned = this.mineskinResigner.resign(result.getSkinData());
                                if (resigned != null && resigned.hasSignature()) {
                                    if (this.config.isDebug()) {
                                        this.plugin.getLogger().info("Mineskin \u91cd\u7b7e\u6210\u529f\uff08\u539f\u59cb\u6765\u6e90: " + resigned.getDisplaySource() + "\uff09");
                                    }
                                    return SkinFetchResult.success(resigned);
                                }
                            }
                            catch (Exception e) {
                                this.plugin.getLogger().warning("Mineskin \u91cd\u7b7e\u5931\u8d25\uff0c\u4f7f\u7528\u539f\u59cb\u76ae\u80a4: " + e.getMessage());
                            }
                        }
                        return result;
                    }
                    if (result == null) continue;
                    lastError = result;
                    if (!this.config.isDebug() || result.getStatus() == SkinFetchResult.Status.NOT_FOUND) continue;
                    this.plugin.getLogger().warning("\u76ae\u80a4\u6e90 " + provider.getName() + " \u83b7\u53d6 " + username + " \u5931\u8d25: " + String.valueOf((Object)result.getStatus()));
                }
                catch (Exception e) {
                    if (this.config.isDebug()) {
                        this.plugin.getLogger().warning("\u76ae\u80a4\u6e90 " + provider.getName() + " \u5904\u7406 " + username + " \u5f02\u5e38: " + e.getMessage());
                    }
                    lastError = SkinFetchResult.networkError();
                }
            }
            if (lastError.getStatus() == SkinFetchResult.Status.NOT_FOUND) {
                return SkinFetchResult.notFound();
            }
            return lastError;
        });
    }

    public String getProviderNames() {
        return this.providers.stream().map(SkinProvider::getName).collect(Collectors.joining(", "));
    }

    public boolean isEmpty() {
        return this.providers.isEmpty();
    }
}
