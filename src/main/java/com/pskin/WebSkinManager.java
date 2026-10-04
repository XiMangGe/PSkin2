/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.pskin;

import com.pskin.MySqlStorage;
import com.pskin.SharedFileIO;
import com.pskin.SkinData;
import com.pskin.libs.gson.Gson;
import com.pskin.libs.gson.GsonBuilder;
import com.pskin.libs.gson.reflect.TypeToken;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.java.JavaPlugin;

public class WebSkinManager {
    private final JavaPlugin plugin;
    private final MySqlStorage db;
    private final Gson gson;
    private final boolean sharedDir;
    private final File dataDir;
    private final Map<String, WebEntry> skins = new ConcurrentHashMap<String, WebEntry>();
    private File dataFile;
    private File skinDir;
    private long lastSyncMtime = -1L;

    public WebSkinManager(JavaPlugin plugin, MySqlStorage db, File dataDir, boolean sharedDir) {
        this.plugin = plugin;
        this.db = db;
        this.sharedDir = sharedDir && db == null;
        this.dataDir = dataDir != null ? dataDir : plugin.getDataFolder();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.skinDir = new File(this.dataDir, "web-skins");
        if (!this.skinDir.exists()) {
            this.skinDir.mkdirs();
        }
        this.dataFile = new File(this.dataDir, "web-skins.json");
        if (this.db != null) {
            this.importLegacyFile();
        } else if (this.sharedDir) {
            this.load(false);
            this.importLegacyLocalIntoShared();
        } else {
            this.load(false);
        }
    }

    private void load(boolean quiet) {
        this.dataFile = new File(this.dataDir, "web-skins.json");
        if (!this.dataFile.exists()) {
            this.lastSyncMtime = 0L;
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(this.dataFile.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);){
            Type type = new TypeToken<Map<String, WebEntry>>(){}.getType();
            Map<?, ?> loaded = (Map<?, ?>)this.gson.fromJson((Reader)reader, type);
            if (loaded != null) {
                for (Map.Entry<?, ?> e : loaded.entrySet()) {
                    if (e.getKey() == null || e.getValue() == null) continue;
                    this.skins.put(((String)e.getKey()).toLowerCase(), (WebEntry)e.getValue());
                }
            }
            if (!quiet) {
                this.plugin.getLogger().info("\u5df2\u52a0\u8f7d\u7f51\u7ad9\u76ae\u80a4: " + this.skins.size() + " \u4e2a");
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u52a0\u8f7d\u7f51\u7ad9\u76ae\u80a4\u5931\u8d25: " + e.getMessage());
        }
        this.lastSyncMtime = this.dataFile.exists() ? SharedFileIO.mtime(this.dataFile) : 0L;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void syncFromDisk() {
        if (!this.sharedDir) {
            return;
        }
        long m = SharedFileIO.mtime(this.dataFile);
        if (m != this.lastSyncMtime) {
            WebSkinManager webSkinManager = this;
            synchronized (webSkinManager) {
                m = SharedFileIO.mtime(this.dataFile);
                if (m != this.lastSyncMtime) {
                    this.skins.clear();
                    this.load(true);
                }
            }
        }
    }

    private void importLegacyLocalIntoShared() {
        File legacyJson = new File(this.plugin.getDataFolder(), "web-skins.json");
        File legacyDir = new File(this.plugin.getDataFolder(), "web-skins");
        if (!legacyJson.isFile() || legacyJson.equals(this.dataFile)) {
            return;
        }
        int imported = 0;
        int importedPng = 0;
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(legacyJson.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);){
            Type type = new TypeToken<Map<String, WebEntry>>(){}.getType();
            Map<?, ?> loaded = (Map<?, ?>)this.gson.fromJson((Reader)reader, type);
            if (loaded != null) {
                for (Map.Entry<?, ?> e : loaded.entrySet()) {
                    if (e.getKey() == null || e.getValue() == null || ((WebEntry)e.getValue()).value == null) continue;
                    String key = ((String)e.getKey()).toLowerCase();
                    if (!this.skins.containsKey(key)) {
                        this.skins.put(key, (WebEntry)e.getValue());
                        ++imported;
                    }
                    File oldImg = new File(legacyDir, key + ".png");
                    File newImg = new File(this.skinDir, key + ".png");
                    if (!oldImg.isFile() || newImg.exists()) continue;
                    try {
                        Files.copy(oldImg.toPath(), newImg.toPath(), new CopyOption[0]);
                        ++importedPng;
                    }
                    catch (Exception exception) {}
                }
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u5408\u5e76\u672c\u5730\u7f51\u7ad9\u76ae\u80a4\u5230\u5171\u4eab\u76ee\u5f55\u5931\u8d25: " + e.getMessage());
            return;
        }
        if (imported > 0 || importedPng > 0) {
            this.save();
            if (legacyJson.renameTo(new File(this.plugin.getDataFolder(), "web-skins.json.imported"))) {
                this.plugin.getLogger().info("\u5df2\u628a\u672c\u5730\u7f51\u7ad9\u76ae\u80a4 " + imported + " \u4e2a\u5408\u5e76\u8fdb\u5171\u4eab\u76ee\u5f55\uff08\u56fe\u7247 " + importedPng + " \u5f20\uff0c\u539f json \u6539\u540d\u4e3a web-skins.json.imported\uff09");
            } else {
                this.plugin.getLogger().info("\u5df2\u628a\u672c\u5730\u7f51\u7ad9\u76ae\u80a4 " + imported + " \u4e2a\u5408\u5e76\u8fdb\u5171\u4eab\u76ee\u5f55\uff08\u56fe\u7247 " + importedPng + " \u5f20\uff09");
            }
        }
    }

    private void importLegacyFile() {
        int imported;
        block15: {
            if (!this.dataFile.isFile()) {
                this.plugin.getLogger().info("\u5df2\u8fde\u63a5\u7f51\u7ad9\u76ae\u80a4\uff08MySQL\uff09: " + this.db.countWebSkins() + " \u4e2a");
                return;
            }
            imported = 0;
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(this.dataFile.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);){
                Type type = new TypeToken<Map<String, WebEntry>>(){}.getType();
                Map<?, ?> loaded = (Map<?, ?>)this.gson.fromJson((Reader)reader, type);
                if (loaded == null) break block15;
                for (Map.Entry<?, ?> e : loaded.entrySet()) {
                    if (e.getKey() == null || e.getValue() == null || ((WebEntry)e.getValue()).value == null) continue;
                    byte[] png = null;
                    try {
                        File img = new File(this.skinDir, ((String)e.getKey()).toLowerCase() + ".png");
                        if (img.isFile()) {
                            png = Files.readAllBytes(img.toPath());
                        }
                    }
                    catch (Exception imgEx) {
                        png = null;
                    }
                    this.db.importWebSkin(((String)e.getKey()).toLowerCase(), (WebEntry)e.getValue(), png);
                    ++imported;
                }
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("\u5bfc\u5165\u65e7\u7f51\u7ad9\u76ae\u80a4\u5230 MySQL \u5931\u8d25: " + e.getMessage());
            }
        }
        if (this.dataFile.renameTo(new File(this.plugin.getDataFolder(), "web-skins.json.imported"))) {
            this.plugin.getLogger().info("\u5df2\u5bfc\u5165\u65e7\u7f51\u7ad9\u76ae\u80a4 " + imported + " \u4e2a\u5230 MySQL\uff08\u539f\u6587\u4ef6\u6539\u540d\u4e3a web-skins.json.imported\uff09");
        } else {
            this.plugin.getLogger().info("\u5df2\u5bfc\u5165\u65e7\u7f51\u7ad9\u76ae\u80a4 " + imported + " \u4e2a\u5230 MySQL\uff08\u539f\u6587\u4ef6\u4fdd\u7559\uff0c\u4f46\u4e0d\u4f1a\u518d\u5bfc\u5165\uff09");
        }
    }

    public void save() {
        if (this.db != null) {
            return;
        }
        try {
            this.dataDir.mkdirs();
            String json = this.gson.toJson(this.skins);
            if (this.sharedDir) {
                SharedFileIO.lockedWrite(this.dataFile, json);
            } else {
                SharedFileIO.atomicWrite(this.dataFile, json);
            }
            this.lastSyncMtime = SharedFileIO.mtime(this.dataFile);
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u4fdd\u5b58\u7f51\u7ad9\u76ae\u80a4\u5931\u8d25: " + e.getMessage());
        }
    }

    public SkinData get(String username) {
        String key = username.toLowerCase();
        if (this.db != null) {
            WebEntry entry = this.db.loadWebSkin(key);
            if (entry == null || entry.value == null) {
                return null;
            }
            return new SkinData(entry.value, entry.signature, "web");
        }
        this.syncFromDisk();
        WebEntry entry = this.skins.get(key);
        if (entry == null || entry.value == null) {
            return null;
        }
        return new SkinData(entry.value, entry.signature, "web");
    }

    public boolean has(String username) {
        if (this.db != null) {
            WebSkinManager.WebEntry entry = this.db.loadWebSkin(username.toLowerCase());
            return entry != null && entry.value != null;
        }
        this.syncFromDisk();
        return this.skins.containsKey(username.toLowerCase());
    }

    public void put(String username, SkinData skinData, byte[] pngBytes, String model) {
        String key = username.toLowerCase();
        if (this.db != null) {
            WebEntry entry = new WebEntry();
            entry.value = skinData.getValue();
            entry.signature = skinData.getSignature();
            entry.model = model == null ? "classic" : model;
            entry.timestamp = System.currentTimeMillis();
            this.db.saveWebSkin(key, entry, pngBytes);
            return;
        }
        this.syncFromDisk();
        WebEntry entry = new WebEntry();
        entry.value = skinData.getValue();
        entry.signature = skinData.getSignature();
        entry.model = model == null ? "classic" : model;
        entry.timestamp = System.currentTimeMillis();
        this.skins.put(key, entry);
        this.save();
        if (pngBytes != null) {
            try {
                File img = new File(this.skinDir, key + ".png");
                Files.write(img.toPath(), pngBytes, new OpenOption[0]);
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("\u4fdd\u5b58\u76ae\u80a4\u56fe\u7247\u5931\u8d25: " + e.getMessage());
            }
        }
    }

    public boolean remove(String username) {
        String key = username.toLowerCase();
        if (this.db != null) {
            boolean existed = this.db.loadWebSkin(key) != null;
            boolean bl = existed;
            if (existed) {
                this.db.deleteWebSkin(key);
            }
            return existed;
        }
        this.syncFromDisk();
        boolean removed = this.skins.remove(key) != null;
        boolean bl = removed;
        if (removed) {
            this.save();
            File img = new File(this.skinDir, key + ".png");
            if (img.exists()) {
                img.delete();
            }
        }
        return removed;
    }

    public File getSkinFile(String username) {
        File img = new File(this.skinDir, username.toLowerCase() + ".png");
        return img.exists() ? img : null;
    }

    public byte[] getSkinPng(String username) {
        String key = username.toLowerCase();
        if (this.db != null) {
            return this.db.loadWebPng(key);
        }
        try {
            File img = new File(this.skinDir, key + ".png");
            if (!img.isFile()) {
                return null;
            }
            return Files.readAllBytes(img.toPath());
        }
        catch (Exception e) {
            return null;
        }
    }

    public List<WebSkinInfo> getAll() {
        ArrayList<WebSkinInfo> list = new ArrayList<WebSkinInfo>();
        LinkedHashMap<String, WebEntry> sorted = new LinkedHashMap<String, WebEntry>();
        if (this.db != null) {
            for (Map.Entry<String, WebEntry> e2 : this.db.loadAllWebSkins().entrySet()) {
                sorted.put(e2.getKey().toLowerCase(), e2.getValue());
            }
        } else {
            this.syncFromDisk();
            this.skins.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> sorted.put((String)e.getKey(), (WebEntry)e.getValue()));
        }
        sorted.forEach((name, entry) -> list.add(new WebSkinInfo((String)name, new SkinData(entry.value, entry.signature, "web"), entry.model, entry.timestamp)));
        return list;
    }

    public int getCount() {
        if (this.db != null) {
            return this.db.countWebSkins();
        }
        this.syncFromDisk();
        return this.skins.size();
    }

    static class WebEntry {
        String value;
        String signature;
        String source = "web";
        String model = "classic";
        long timestamp;

        WebEntry() {
        }
    }

    public static class WebSkinInfo {
        public final String name;
        public final SkinData skinData;
        public final String model;
        public final long timestamp;

        WebSkinInfo(String name, SkinData skinData, String model, long timestamp) {
            this.name = name;
            this.skinData = skinData;
            this.model = model;
            this.timestamp = timestamp;
        }
    }
}
