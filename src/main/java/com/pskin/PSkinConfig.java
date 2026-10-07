/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.InvalidConfigurationException
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.pskin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class PSkinConfig {
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final String[] REQUIRED_KEYS = new String[]{"language", "auto-apply-on-join", "auto-apply-feedback", "join-notify", "authme", "skin-priority", "providers", "mineskin", "web", "commands.cooldown-seconds", "user-agent", "storage", "storage.type", "storage.directory", "storage.mysql.enabled", "web.show-link", "web.auto-port"};
    private final JavaPlugin plugin;
    private String configLoadError;
    private int mergedKeys;
    private String language;
    private List<String> providerOrder;
    private List<String> skinPriority;
    private boolean mojangEnabled;
    private String mojangProfileUrl;
    private String mojangSessionUrl;
    private int mojangTimeout;
    private boolean littleSkinEnabled;
    private String littleSkinApiRoot;
    private int littleSkinTimeout;
    private boolean autoApplyOnJoin;
    private int joinDelayTicks;
    private boolean defaultSkinEnabled;
    private String defaultSkinName;
    private boolean bedrockEnabled;
    private String bedrockPrefix;
    private boolean bedrockApplyCustomSkins;
    private boolean bedrockRemoteSkins;
    private boolean webJoinFeedback;
    private boolean autoApplyFeedback;
    private boolean authmeIntegration;
    private boolean joinNotifyEnabled;
    private List<String> joinNotifyStart;
    private List<String> joinNotifySuccess;
    private List<String> joinNotifyFail;
    private boolean mineskinResignEnabled;
    private int mineskinTimeoutSeconds;
    private String mineskinApiKey;
    private String mineskinApiUrl;
    private boolean mineskinMirrorsEnabled;
    private List<String> mineskinMirrorUrls;
    private boolean stripSignature;
    private boolean webEnabled;
    private int webPort;
    private boolean webAutoPort;
    private boolean webShowLink;
    private String webBind;
    private String webPublicAddress;
    private String webLanguage;
    private String webTitle;
    private String webSubtitle;
    private String webAnnouncement;
    private String webFooter;
    private String webGalleryTitle;
    private String webThemeColor;
    private int webMaxUploadKb;
    private boolean webValidateImage;
    private int webUploadCooldownSeconds;
    private boolean webDebug;
    private String webAdminPassword;
    private boolean webFilesEnabled;
    private String webFilesFolder;
    private int commandCooldown;
    private int skinCacheMinutes;
    private String storageType;
    private String storageDirectory;
    private boolean mySqlEnabled;
    private String mySqlHost;
    private int mySqlPort;
    private String mySqlDatabase;
    private String mySqlUsername;
    private String mySqlPassword;
    private String mySqlTablePrefix;
    private boolean mySqlUseSsl;
    private String userAgent;
    private boolean refreshVisibility;
    private boolean debug;

    public PSkinConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.ensureUpToDate();
        this.load();
    }

    private void ensureUpToDate() {
        this.configLoadError = null;
        this.mergedKeys = 0;
        File file = new File(this.plugin.getDataFolder(), "config.yml");
        if (!file.isFile()) {
            return;
        }
        YamlConfiguration probe = new YamlConfiguration();
        try {
            probe.load(file);
        }
        catch (InvalidConfigurationException e) {
            this.configLoadError = e.getMessage();
            this.plugin.getLogger().log(Level.SEVERE, "======== config.yml \u5b58\u5728\u8bed\u6cd5\u9519\u8bef\uff0c\u672c\u6b21\u542f\u52a8\u5c06\u5ffd\u7565\u4f60\u7684\u4fee\u6539\u3001\u4f7f\u7528\u9ed8\u8ba4\u503c ========");
            this.plugin.getLogger().log(Level.SEVERE, "\u9519\u8bef\u8be6\u60c5: " + e.getMessage());
            this.plugin.getLogger().severe("\u5e38\u89c1\u539f\u56e0: \u989c\u8272\u4ee3\u7801 & \u5f00\u5934\u7684\u884c\u5fc5\u987b\u7528\u53cc\u5f15\u53f7\u5305\u8d77\u6765\uff0c\u4f8b\u5982 - \"&6&lPSkin2\"");
            this.plugin.getLogger().severe("\u4fee\u590d\u65b9\u6cd5: \u6309\u4e0a\u9762\u7684\u884c\u53f7\u6539\u6b63 config.yml\uff0c\u7136\u540e\u6267\u884c /pskin reload");
            this.plugin.getLogger().severe("===========================================================================");
            return;
        }
        catch (Exception e) {
            this.configLoadError = e.getMessage();
            this.plugin.getLogger().severe("\u8bfb\u53d6 config.yml \u5931\u8d25\uff0c\u5c06\u4f7f\u7528\u9ed8\u8ba4\u503c: " + e.getMessage());
            return;
        }
        try {
            int missing = 0;
            for (String key : REQUIRED_KEYS) {
                if (probe.contains(key)) continue;
                ++missing;
            }
            if (missing > 0) {
                this.plugin.reloadConfig();
                FileConfiguration cfg = this.plugin.getConfig();
                cfg.options().copyDefaults(true);
                this.plugin.saveConfig();
                this.mergedKeys = missing;
                this.plugin.getLogger().info("\u68c0\u6d4b\u5230 config.yml \u7f3a\u5c11 " + missing + " \u4e2a\u914d\u7f6e\u9879\uff08\u53ef\u80fd\u662f\u65e7\u7248\u672c\u9057\u7559\uff09\uff0c\u5df2\u81ea\u52a8\u8865\u5168\u5e76\u4fdd\u5b58\u3002");
                this.plugin.getLogger().info("\u73b0\u5728\u53ef\u4ee5\u76f4\u63a5\u5728 plugins/PSkin2/config.yml \u91cc\u627e\u5230\u5e76\u4fee\u6539\u8fd9\u4e9b\u914d\u7f6e\u4e86\u3002");
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u81ea\u52a8\u8865\u5168 config.yml \u7f3a\u5931\u914d\u7f6e\u9879\u65f6\u51fa\u9519: " + e.getMessage());
        }
    }

    public void load() {
        FileConfiguration cfg = this.plugin.getConfig();
        this.language = cfg.getString("language", "zh_CN");
        this.providerOrder = cfg.getStringList("providers.order");
        if (this.providerOrder.isEmpty()) {
            this.providerOrder = List.of("mojang", "littleskin");
        }
        this.skinPriority = cfg.getStringList("skin-priority");
        if (this.skinPriority.isEmpty()) {
            this.skinPriority = List.of("web", "manual", "mojang", "littleskin");
        }
        this.mojangEnabled = cfg.getBoolean("providers.mojang.enabled", true);
        this.mojangProfileUrl = cfg.getString("providers.mojang.profile-url", "https://api.mojang.com/users/profiles/minecraft/%s");
        this.mojangSessionUrl = cfg.getString("providers.mojang.session-url", "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false");
        this.mojangTimeout = cfg.getInt("providers.mojang.timeout-seconds", 10);
        this.littleSkinEnabled = cfg.getBoolean("providers.littleskin.enabled", true);
        this.littleSkinApiRoot = cfg.getString("providers.littleskin.api-root", "https://littleskin.cn/api/yggdrasil");
        this.littleSkinTimeout = cfg.getInt("providers.littleskin.timeout-seconds", 10);
        this.autoApplyOnJoin = cfg.getBoolean("auto-apply-on-join", true);
        this.joinDelayTicks = cfg.getInt("join-delay-ticks", 20);
        this.defaultSkinEnabled = cfg.getBoolean("offline.default-skin-enabled", false);
        this.defaultSkinName = cfg.getString("offline.default-skin-name", "MHF_Steve");
        this.bedrockEnabled = cfg.getBoolean("bedrock.enabled", true);
        this.bedrockPrefix = cfg.getString("bedrock.prefix", ".");
        this.bedrockApplyCustomSkins = cfg.getBoolean("bedrock.apply-custom-skins", true);
        this.bedrockRemoteSkins = cfg.getBoolean("bedrock.remote-skins", true);
        this.webJoinFeedback = cfg.getBoolean("web.join-feedback", true);
        this.autoApplyFeedback = cfg.getBoolean("auto-apply-feedback", true);
        this.authmeIntegration = cfg.getBoolean("authme.enabled", false);
        this.joinNotifyEnabled = cfg.getBoolean("join-notify.enabled", true);
        this.joinNotifyStart = cfg.getStringList("join-notify.start");
        if (!cfg.contains("join-notify.start")) {
            this.joinNotifyStart = List.of("          &8&m========================================", "      &6&lPSkin2 &7\u6b63\u5728\u4e3a\u4f60\u52a0\u8f7d\u76ae\u80a4...", "          &8&m========================================");
        }
        this.joinNotifySuccess = cfg.getStringList("join-notify.success");
        if (!cfg.contains("join-notify.success")) {
            this.joinNotifySuccess = List.of("  &8&m============&r &a&l\u2714 \u76ae\u80a4\u52a0\u8f7d\u5b8c\u6210 &r&8&m============", "   &7\u6765\u6e90: &f{source}", "  &8========= {server} \u00b7 Services provided by PSkin2 =========");
        }
        this.joinNotifyFail = cfg.getStringList("join-notify.fail");
        if (!cfg.contains("join-notify.fail")) {
            this.joinNotifyFail = List.of("  &8&m============&r &c&l\u2718 \u672a\u80fd\u52a0\u8f7d\u5230\u76ae\u80a4 &r&8&m============", "   &7\u5c06\u4fdd\u6301\u9ed8\u8ba4\u5916\u89c2", "  &8========= {server} \u00b7 Services provided by PSkin2 =========");
        }
        this.mineskinResignEnabled = cfg.getBoolean("mineskin.resign-enabled", true);
        this.mineskinTimeoutSeconds = cfg.getInt("mineskin.timeout-seconds", 30);
        String key = cfg.getString("mineskin.api-key", "");
        this.mineskinApiKey = key == null ? "" : key.trim();
        this.mineskinApiUrl = cfg.getString("mineskin.api-url", "https://api.mineskin.org");
        if (this.mineskinApiUrl == null || this.mineskinApiUrl.isEmpty()) {
            this.mineskinApiUrl = "https://api.mineskin.org";
        }
        if (this.mineskinApiUrl.endsWith("/")) {
            this.mineskinApiUrl = this.mineskinApiUrl.substring(0, this.mineskinApiUrl.length() - 1);
        }
        this.mineskinMirrorsEnabled = cfg.getBoolean("mineskin.mirrors.enabled", false);
        this.mineskinMirrorUrls = new ArrayList<String>();
        for (String url : cfg.getStringList("mineskin.mirrors.urls")) {
            if (url == null) continue;
            String trimmed = url.trim();
            if (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            if (trimmed.isEmpty() || this.mineskinMirrorUrls.contains(trimmed)) continue;
            this.mineskinMirrorUrls.add(trimmed);
        }
        this.stripSignature = cfg.getBoolean("strip-signature", false);
        this.webEnabled = cfg.getBoolean("web.enabled", true);
        this.webPort = cfg.getInt("web.port", 7505);
        this.webAutoPort = cfg.getBoolean("web.auto-port", true);
        this.webShowLink = cfg.getBoolean("web.show-link", true);
        this.webBind = cfg.getString("web.bind", "0.0.0.0");
        String pubAddr = cfg.getString("web.public-address", "");
        this.webPublicAddress = pubAddr == null ? "" : pubAddr.trim();
        this.webLanguage = cfg.getString("web.language", "zh_CN");
        this.webTitle = cfg.getString("web.title", "\u670d\u52a1\u5668\u76ae\u80a4\u4e0a\u4f20\u7f51\u7ad9");
        this.webSubtitle = cfg.getString("web.subtitle", "\u4e0a\u4f20\u4f60\u7684\u4e13\u5c5e\u76ae\u80a4\uff0c\u5168\u670d\u73a9\u5bb6\u53ef\u89c1");
        this.webAnnouncement = cfg.getString("web.announcement", "");
        this.webFooter = cfg.getString("web.footer", "Powered by PSkin2");
        this.webGalleryTitle = cfg.getString("web.gallery-title", "\u5168\u670d\u76ae\u80a4\u753b\u5eca");
        this.webThemeColor = cfg.getString("web.theme-color", "#4f8cff");
        if (this.webThemeColor == null || !COLOR_PATTERN.matcher(this.webThemeColor).matches()) {
            this.webThemeColor = "#4f8cff";
        }
        this.webMaxUploadKb = Math.max(32, cfg.getInt("web.max-upload-kb", 256));
        this.webValidateImage = cfg.getBoolean("web.validate-image", true);
        this.webUploadCooldownSeconds = Math.max(0, cfg.getInt("web.upload-cooldown-seconds", 60));
        this.webDebug = cfg.getBoolean("web.debug", false);
        String adminPwd = cfg.getString("web.admin-password", "");
        this.webAdminPassword = adminPwd == null ? "" : adminPwd.trim();
        this.webFilesEnabled = cfg.getBoolean("web.files-enabled", true);
        String wff = cfg.getString("web.files-folder", "Web");
        if (wff == null || wff.trim().isEmpty() || wff.contains("..") || wff.contains("/") || wff.contains("\\")) {
            this.webFilesFolder = "Web";
        } else {
            this.webFilesFolder = wff.trim();
        }
        this.commandCooldown = cfg.getInt("commands.cooldown-seconds", 30);
        this.skinCacheMinutes = cfg.getInt("skin-cache-minutes", 720);
        String storageType = cfg.getString("storage.type", "file");
        this.storageType = storageType == null || storageType.trim().isEmpty() ? "file" : storageType.trim().toLowerCase();
        String storageDir = cfg.getString("storage.directory", "");
        this.storageDirectory = storageDir == null ? "" : storageDir.trim();
        this.mySqlEnabled = cfg.getBoolean("storage.mysql.enabled", false);
        this.mySqlHost = cfg.getString("storage.mysql.host", "127.0.0.1");
        this.mySqlPort = cfg.getInt("storage.mysql.port", 3306);
        this.mySqlDatabase = cfg.getString("storage.mysql.database", "pskin");
        this.mySqlUsername = cfg.getString("storage.mysql.username", "pskin");
        String dbPwd = cfg.getString("storage.mysql.password", "");
        this.mySqlPassword = dbPwd == null ? "" : dbPwd;
        String tblPrefix = cfg.getString("storage.mysql.table-prefix", "pskin_");
        this.mySqlTablePrefix = tblPrefix == null || tblPrefix.trim().isEmpty() ? "pskin_" : tblPrefix.trim();
        this.mySqlUseSsl = cfg.getBoolean("storage.mysql.use-ssl", false);
        this.userAgent = cfg.getString("user-agent", "PSkin2/26.8.25.5");
        this.refreshVisibility = cfg.getBoolean("refresh-visibility", false);
        this.debug = cfg.getBoolean("debug", false);
    }

    public void reload() {
        this.ensureUpToDate();
        this.plugin.reloadConfig();
        this.load();
    }

    public String getConfigLoadError() {
        return this.configLoadError;
    }

    public int getMergedKeys() {
        return this.mergedKeys;
    }

    public String getLanguage() {
        return this.language;
    }

    public List<String> getProviderOrder() {
        return this.providerOrder;
    }

    public List<String> getSkinPriority() {
        return this.skinPriority;
    }

    public boolean isMojangEnabled() {
        return this.mojangEnabled;
    }

    public String getMojangProfileUrl() {
        return this.mojangProfileUrl;
    }

    public String getMojangSessionUrl() {
        return this.mojangSessionUrl;
    }

    public int getMojangTimeout() {
        return Math.max(3, this.mojangTimeout);
    }

    public boolean isLittleSkinEnabled() {
        return this.littleSkinEnabled;
    }

    public String getLittleSkinApiRoot() {
        return this.littleSkinApiRoot;
    }

    public int getLittleSkinTimeout() {
        return Math.max(3, this.littleSkinTimeout);
    }

    /**
     * 判断 providers.order 中的某个名字是否是自定义 Yggdrasil 皮肤站。
     * 配置示例:
     *   providers:
     *     order: [mojang, littleskin, myskin]
     *     myskin:
     *       type: yggdrasil
     *       enabled: true
     *       api-root: "https://mcskin.example.com/api/yggdrasil"
     *       timeout-seconds: 10
     */
    public boolean isCustomYggdrasil(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        if ("mojang".equals(lower) || "littleskin".equals(lower)) return false;
        String type = this.plugin.getConfig().getString("providers." + name + ".type", "");
        return "yggdrasil".equalsIgnoreCase(type);
    }

    public boolean isCustomYggdrasilEnabled(String name) {
        return this.plugin.getConfig().getBoolean("providers." + name + ".enabled", true);
    }

    public String getCustomYggdrasilApiRoot(String name) {
        String root = this.plugin.getConfig().getString("providers." + name + ".api-root", "");
        if (root != null && root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        return root;
    }

    public int getCustomYggdrasilTimeout(String name) {
        return Math.max(3, this.plugin.getConfig().getInt("providers." + name + ".timeout-seconds", 10));
    }

    public boolean isAutoApplyOnJoin() {
        return this.autoApplyOnJoin;
    }

    public int getJoinDelayTicks() {
        return Math.max(0, this.joinDelayTicks);
    }

    public boolean isDefaultSkinEnabled() {
        return this.defaultSkinEnabled;
    }

    public String getDefaultSkinName() {
        return this.defaultSkinName;
    }

    public boolean isBedrockEnabled() {
        return this.bedrockEnabled;
    }

    public String getBedrockPrefix() {
        return this.bedrockPrefix;
    }

    public boolean isBedrockApplyCustomSkins() {
        return this.bedrockApplyCustomSkins;
    }

    public boolean isBedrockPlayer(String name) {
        return this.bedrockEnabled && this.bedrockPrefix != null && !this.bedrockPrefix.isEmpty() && name.startsWith(this.bedrockPrefix);
    }

    public String stripBedrockPrefix(String name) {
        if (this.isBedrockPlayer(name) && name.length() > this.bedrockPrefix.length()) {
            return name.substring(this.bedrockPrefix.length());
        }
        return name;
    }

    public boolean isBedrockRemoteSkins() {
        return this.bedrockRemoteSkins;
    }

    public boolean isWebJoinFeedback() {
        return this.webJoinFeedback;
    }

    public boolean isAutoApplyFeedback() {
        return this.autoApplyFeedback;
    }

    public boolean isJoinNotifyEnabled() {
        return this.joinNotifyEnabled;
    }

    public List<String> getJoinNotifyStart() {
        return this.joinNotifyStart;
    }

    public List<String> getJoinNotifySuccess() {
        return this.joinNotifySuccess;
    }

    public List<String> getJoinNotifyFail() {
        return this.joinNotifyFail;
    }

    public boolean isAuthmeIntegration() {
        return this.authmeIntegration;
    }

    public boolean isWebValidateImage() {
        return this.webValidateImage;
    }

    public String getWebAdminPassword() {
        return this.webAdminPassword;
    }

    public boolean hasWebAdminPassword() {
        return this.webAdminPassword != null && !this.webAdminPassword.isEmpty();
    }

    public boolean isWebFilesEnabled() {
        return this.webFilesEnabled;
    }

    public String getWebFilesFolder() {
        return this.webFilesFolder;
    }

    public String getMineskinApiKey() {
        return this.mineskinApiKey;
    }

    public boolean hasMineskinApiKey() {
        return this.mineskinApiKey != null && !this.mineskinApiKey.isEmpty();
    }

    public boolean isMineskinResignEnabled() {
        return this.mineskinResignEnabled;
    }

    public int getMineskinTimeoutSeconds() {
        return Math.max(10, this.mineskinTimeoutSeconds);
    }

    public String getMineskinApiUrl() {
        return this.mineskinApiUrl;
    }

    public boolean isMineskinMirrorsEnabled() {
        return this.mineskinMirrorsEnabled;
    }

    public List<String> getMineskinMirrorUrls() {
        return this.mineskinMirrorUrls;
    }

    public List<String> getMineskinEndpoints() {
        ArrayList<String> endpoints = new ArrayList<String>();
        if (this.mineskinApiUrl != null && !this.mineskinApiUrl.isEmpty()) {
            endpoints.add(this.mineskinApiUrl);
        }
        if (this.mineskinMirrorsEnabled) {
            for (String url : this.mineskinMirrorUrls) {
                if (endpoints.contains(url)) continue;
                endpoints.add(url);
            }
        }
        if (endpoints.isEmpty()) {
            endpoints.add("https://api.mineskin.org");
        }
        return endpoints;
    }

    public boolean isStripSignature() {
        return this.stripSignature;
    }

    public boolean isWebEnabled() {
        return this.webEnabled;
    }

    public int getWebPort() {
        return this.webPort;
    }

    public boolean isWebAutoPort() {
        return this.webAutoPort;
    }

    public boolean isWebShowLink() {
        return this.webShowLink;
    }

    public String getWebBind() {
        return this.webBind == null || this.webBind.isEmpty() ? "0.0.0.0" : this.webBind;
    }

    public String getWebPublicAddress() {
        return this.webPublicAddress;
    }

    public String getWebLanguage() {
        return this.webLanguage == null || this.webLanguage.isEmpty() ? this.language : this.webLanguage;
    }

    public String getWebTitle() {
        return this.webTitle;
    }

    public String getWebSubtitle() {
        return this.webSubtitle;
    }

    public String getWebAnnouncement() {
        return this.webAnnouncement == null ? "" : this.webAnnouncement;
    }

    public String getWebFooter() {
        return this.webFooter;
    }

    public String getWebGalleryTitle() {
        return this.webGalleryTitle == null || this.webGalleryTitle.isEmpty() ? this.webTitle : this.webGalleryTitle;
    }

    public String getWebThemeColor() {
        return this.webThemeColor;
    }

    public int getWebMaxUploadKb() {
        return this.webMaxUploadKb;
    }

    public int getWebUploadCooldownSeconds() {
        return this.webUploadCooldownSeconds;
    }

    public boolean isWebDebug() {
        return this.webDebug;
    }

    public int getCommandCooldown() {
        return this.commandCooldown;
    }

    public int getSkinCacheMinutes() {
        return this.skinCacheMinutes;
    }

    public String getStorageType() {
        return this.storageType;
    }

    public boolean isMySqlStorage() {
        return "mysql".equalsIgnoreCase(this.storageType) && this.mySqlEnabled;
    }

    public boolean isMySqlTypeSelected() {
        return "mysql".equalsIgnoreCase(this.storageType);
    }

    public boolean isSharedDirStorage() {
        return "shared-dir".equalsIgnoreCase(this.storageType) && !this.storageDirectory.isEmpty();
    }

    public String getStorageDirectory() {
        return this.storageDirectory;
    }

    public File resolveSharedDataDir() {
        if (this.storageDirectory.isEmpty()) {
            return null;
        }
        File dir = new File(this.storageDirectory);
        return dir.isAbsolute() ? dir : new File(this.plugin.getDataFolder(), this.storageDirectory);
    }

    public boolean isMySqlEnabled() {
        return this.mySqlEnabled;
    }

    public String getMySqlHost() {
        return this.mySqlHost;
    }

    public int getMySqlPort() {
        return this.mySqlPort;
    }

    public String getMySqlDatabase() {
        return this.mySqlDatabase;
    }

    public String getMySqlUsername() {
        return this.mySqlUsername;
    }

    public String getMySqlPassword() {
        return this.mySqlPassword;
    }

    public String getMySqlTablePrefix() {
        return this.mySqlTablePrefix;
    }

    public boolean isMySqlUseSsl() {
        return this.mySqlUseSsl;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public boolean isRefreshVisibility() {
        return this.refreshVisibility;
    }

    public boolean isDebug() {
        return this.debug;
    }
}
