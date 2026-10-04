/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.pskin;

import com.pskin.PSkinConfig;
import com.pskin.SkinResolveService;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class AuthMeListener
implements Listener {
    private final JavaPlugin plugin;
    private final PSkinConfig config;
    private final SkinResolveService resolveService;
    private Class<? extends Event> authedEventClass;
    private Method getNameMethod;

    public AuthMeListener(JavaPlugin plugin, PSkinConfig config, SkinResolveService resolveService) {
        this.plugin = plugin;
        this.config = config;
        this.resolveService = resolveService;
    }

    public boolean tryRegister() {
        try {
            Class<?> raw = Class.forName("fr.xephi.authme.events.PlayerAuthedEvent");
            if (!Event.class.isAssignableFrom(raw)) {
                this.plugin.getLogger().info("\u672a\u68c0\u6d4b\u5230 AuthMe\uff08\u6216\u7248\u672c\u4e0d\u517c\u5bb9\uff09\uff0c\u5df2\u5173\u95ed AuthMe \u96c6\u6210");
                return false;
            }
            this.authedEventClass = raw.asSubclass(Event.class);
            this.getNameMethod = this.authedEventClass.getMethod("getPlayer", new Class[0]);
            this.plugin.getServer().getPluginManager().registerEvent(this.authedEventClass, (Listener)this, EventPriority.NORMAL, (listener, event) -> this.onAuthed(event), (Plugin)this.plugin);
            this.plugin.getLogger().info("\u5df2\u542f\u7528 AuthMe \u96c6\u6210\uff1a\u5c06\u5728\u73a9\u5bb6\u767b\u5f55\u9a8c\u8bc1\u901a\u8fc7\u540e\u91cd\u65b0\u5e94\u7528\u76ae\u80a4\uff0c\u907f\u514d\u88ab\u767b\u5f55\u91cd\u7f6e\u8986\u76d6");
            return true;
        }
        catch (Exception e) {
            this.plugin.getLogger().info("\u672a\u68c0\u6d4b\u5230 AuthMe\uff08\u6216\u7248\u672c\u4e0d\u517c\u5bb9\uff09\uff0c\u5df2\u5173\u95ed AuthMe \u96c6\u6210: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }

    @EventHandler
    public void onAuthed(Event event) {
        block4: {
            try {
                if (this.authedEventClass == null || !this.authedEventClass.isInstance(event)) {
                    return;
                }
                Object playerObj = this.getNameMethod.invoke(event, new Object[0]);
                if (!(playerObj instanceof Player)) {
                    return;
                }
                Player player = (Player)playerObj;
                Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                    if (player.isOnline()) {
                        this.resolveService.resolveAndApplyJoin(player);
                    }
                }, this.config.isAuthmeIntegration() ? 40L : 0L);
            }
            catch (Exception e) {
                if (!this.config.isDebug()) break block4;
                this.plugin.getLogger().warning("AuthMe \u767b\u5f55\u4e8b\u4ef6\u5904\u7406\u5f02\u5e38: " + e.getMessage());
            }
        }
    }
}
