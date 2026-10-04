/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.pskin;

import com.pskin.PSkinConfig;
import com.pskin.PSkinPlugin;
import com.pskin.SkinResolveService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class JoinListener
implements Listener {
    private final JavaPlugin plugin;
    private final PSkinConfig config;
    private final SkinResolveService resolveService;

    public JoinListener(JavaPlugin plugin, PSkinConfig config, SkinResolveService resolveService) {
        this.plugin = plugin;
        this.config = config;
        this.resolveService = resolveService;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!this.config.isAutoApplyOnJoin()) {
            return;
        }
        if (this.plugin instanceof PSkinPlugin && this.config.isAuthmeIntegration() && ((PSkinPlugin)this.plugin).isAuthmeActive()) {
            if (this.config.isDebug()) {
                this.plugin.getLogger().info("AuthMe \u96c6\u6210\u5df2\u542f\u7528\uff0c\u8fdb\u670d\u76ae\u80a4\u5e94\u7528\u4ea4\u7531\u767b\u5f55\u4e8b\u4ef6\u5904\u7406\uff08\u907f\u514d\u6a2a\u5e45\u91cd\u590d\uff09: " + event.getPlayer().getName());
            }
            return;
        }
        Player player = event.getPlayer();
        String name = player.getName();
        if (this.config.isBedrockPlayer(name) && !this.config.isBedrockApplyCustomSkins() && !this.config.isBedrockRemoteSkins()) {
            if (this.config.isDebug()) {
                this.plugin.getLogger().info("\u8df3\u8fc7\u57fa\u5ca9\u7248\u73a9\u5bb6\uff08\u7f51\u7ad9/\u624b\u52a8/\u8fdc\u7a0b\u76ae\u80a4\u5747\u5df2\u5173\u95ed\uff09: " + name);
            }
            return;
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (player.isOnline()) {
                this.resolveService.resolveAndApplyJoin(player);
            }
        }, (long)this.config.getJoinDelayTicks());
    }
}
