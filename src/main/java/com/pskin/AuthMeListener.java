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
    private static final String[] EVENT_CLASS_NAMES = new String[]{
        "fr.xephi.authme.events.PlayerAuthedEvent",
        "fr.xephi.authme.events.LoginEvent"
    };
    private final JavaPlugin plugin;
    private final PSkinConfig config;
    private final SkinResolveService resolveService;
    private Class<? extends Event> authedEventClass;
    private Method getPlayerMethod;

    public AuthMeListener(JavaPlugin plugin, PSkinConfig config, SkinResolveService resolveService) {
        this.plugin = plugin;
        this.config = config;
        this.resolveService = resolveService;
    }

    public boolean tryRegister() {
        // 先用插件管理器检测 AuthMe 插件是否存在
        Plugin authme = Bukkit.getPluginManager().getPlugin("AuthMe");
        if (authme == null) {
            this.plugin.getLogger().info("未检测到 AuthMe 插件（plugins/ 目录中无 AuthMe.jar），已关闭 AuthMe 集成");
            return false;
        }
        if (!authme.isEnabled()) {
            this.plugin.getLogger().info("AuthMe 插件存在但未启用，已关闭 AuthMe 集成");
            return false;
        }
        String version = authme.getDescription().getVersion();
        this.plugin.getLogger().info("检测到 AuthMe v" + version + "，正在尝试注册登录事件...");

        // 尝试多个可能的事件类名
        for (String className : EVENT_CLASS_NAMES) {
            try {
                Class<?> raw = Class.forName(className);
                if (!Event.class.isAssignableFrom(raw)) {
                    continue;
                }
                this.authedEventClass = raw.asSubclass(Event.class);
                // 尝试获取玩家对象的方法：getPlayer()
                try {
                    this.getPlayerMethod = this.authedEventClass.getMethod("getPlayer");
                } catch (NoSuchMethodException e) {
                    this.plugin.getLogger().warning("AuthMe 事件 " + className + " 没有 getPlayer() 方法，跳过");
                    this.authedEventClass = null;
                    continue;
                }
                this.plugin.getServer().getPluginManager().registerEvent(this.authedEventClass, (Listener)this, EventPriority.NORMAL, (listener, event) -> this.onAuthed(event), (Plugin)this.plugin);
                this.plugin.getLogger().info("已启用 AuthMe 集成（事件: " + className + "）：将在玩家登录验证通过后重新应用皮肤，避免被登录重置覆盖");
                return true;
            } catch (ClassNotFoundException e) {
                // 继续尝试下一个类名
                if (this.config.isDebug()) {
                    this.plugin.getLogger().info("AuthMe 事件类 " + className + " 不存在，尝试下一个...");
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("注册 AuthMe 事件 " + className + " 失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
        this.plugin.getLogger().warning("检测到 AuthMe 插件，但未能找到兼容的登录事件类（尝试了 PlayerAuthedEvent / LoginEvent）。"
                + "请确认 AuthMe 版本为 5.x 或更高，并检查是否有其他插件修改了类加载。");
        return false;
    }

    @EventHandler
    public void onAuthed(Event event) {
        try {
            if (this.authedEventClass == null || !this.authedEventClass.isInstance(event)) {
                return;
            }
            Object playerObj = this.getPlayerMethod.invoke(event);
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
            if (this.config.isDebug()) {
                this.plugin.getLogger().warning("AuthMe 登录事件处理异常: " + e.getMessage());
            }
        }
    }
}
