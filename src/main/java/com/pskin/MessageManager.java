/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.pskin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageManager {
    private static final String DEFAULT_LANGUAGE = "zh_CN";
    private final JavaPlugin plugin;
    private final Map<String, String> messages = new HashMap<String, String>();
    private String language;

    public MessageManager(JavaPlugin plugin, String language) {
        this.plugin = plugin;
        this.language = this.normalize(language);
        this.load();
    }

    private String normalize(String lang) {
        if (lang == null || lang.isEmpty()) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = lang.replace('-', '_').trim();
        String lower = normalized.toLowerCase();
        if ("zh".equals(lower) || "zh_cn".equals(lower) || "zh_cn_cn".equals(lower) || "chinese".equals(lower)) return DEFAULT_LANGUAGE;
        if ("zh_tw".equals(lower) || "zh_hk".equals(lower) || "zh_hant".equals(lower)) return "zh_TW";
        if ("en".equals(lower) || "en_us".equals(lower) || "english".equals(lower)) return "en_US";
        if ("ja".equals(lower) || "ja_jp".equals(lower) || "japanese".equals(lower)) return "ja_JP";
        return normalized;
    }

    private void load() {
        File externalDir;
        this.messages.clear();
        Properties props = new Properties();
        boolean loaded = this.loadFromJar(props, this.language);
        if (!loaded) {
            this.plugin.getLogger().warning("\u672a\u627e\u5230\u8bed\u8a00 " + this.language + "\uff0c\u56de\u9000\u5230\u9ed8\u8ba4 zh_CN");
            this.language = DEFAULT_LANGUAGE;
            this.loadFromJar(props, this.language);
        }
        if (!(externalDir = new File(this.plugin.getDataFolder(), "lang")).exists()) {
            externalDir.mkdirs();
        }
        this.exportDefaultLangFiles(externalDir);
        File externalFile = new File(externalDir, this.language + ".properties");
        if (externalFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader((InputStream)new FileInputStream(externalFile), StandardCharsets.UTF_8);){
                props.load(reader);
            }
            catch (IOException e) {
                this.plugin.getLogger().warning("\u52a0\u8f7d\u5916\u90e8\u8bed\u8a00\u6587\u4ef6\u5931\u8d25: " + externalFile.getPath());
            }
        }
        for (String key : props.stringPropertyNames()) {
            this.messages.put(key, ChatColor.translateAlternateColorCodes((char)'&', (String)props.getProperty(key)));
        }
        this.plugin.getLogger().info("\u5df2\u52a0\u8f7d\u8bed\u8a00 " + this.language + "\uff08" + this.messages.size() + " \u6761\u6d88\u606f\uff09");
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private boolean loadFromJar(Properties props, String lang) {
        String resourcePath = "/lang/" + lang + ".properties";
        try (InputStream in = this.getClass().getResourceAsStream(resourcePath);){
            boolean bl2;
            if (in == null) {
                boolean bl32;
                boolean bl22 = false;
                boolean bl322 = false;
                boolean bl323 = false;
                boolean bl324 = false;
                boolean bl325 = false;
                boolean bl326 = false;
                boolean bl327 = false;
                boolean bl3 = bl32 = false;
                return bl3;
            }
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            boolean bl22 = true;
            boolean bl23 = true;
            boolean bl24 = true;
            boolean bl25 = true;
            boolean bl26 = true;
            boolean bl27 = true;
            boolean bl28 = true;
            boolean bl = bl2 = true;
            return bl;
        }
        catch (IOException e) {
            return false;
        }
    }

    private void exportDefaultLangFiles(File dir) {
        String[] langs = new String[]{DEFAULT_LANGUAGE, "zh_TW", "en_US", "ja_JP"};
        int exported = 0;
        for (String lang : langs) {
            File target = new File(dir, lang + ".properties");
            if (target.exists()) continue;
            try (InputStream in = this.getClass().getResourceAsStream("/lang/" + lang + ".properties");){
                if (in == null) continue;
                Files.copy(in, target.toPath(), new CopyOption[0]);
                ++exported;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (exported > 0) {
            this.plugin.getLogger().info("\u5df2\u91ca\u653e " + exported + " \u4e2a\u8bed\u8a00\u6587\u4ef6\u5230 lang/ \u76ee\u5f55\uff08\u53ef\u81ea\u884c\u4fee\u6539\u81ea\u5b9a\u4e49\u6d88\u606f\uff09");
        }
    }

    public String get(String key, Object ... args) {
        String msg = this.messages.getOrDefault(key, key);
        try {
            return String.format(msg, args);
        }
        catch (Exception e) {
            return msg;
        }
    }

    public void send(CommandSender sender, String key, Object ... args) {
        String prefix = this.messages.getOrDefault("prefix", "");
        sender.sendMessage(prefix + this.get(key, args));
    }

    public void reload(String newLanguage) {
        this.language = this.normalize(newLanguage);
        this.load();
    }

    public String getLanguage() {
        return this.language;
    }
}
