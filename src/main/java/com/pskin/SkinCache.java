/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.pskin;

import com.pskin.MySqlStorage;
import com.pskin.PSkinConfig;
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
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.java.JavaPlugin;

public class SkinCache {
    private final JavaPlugin plugin;
    private final PSkinConfig config;
    private final MySqlStorage db;
    private final Gson gson;
    private final boolean sharedDir;
    private final File dataDir;
    private final Map<String, SkinData> cache = new ConcurrentHashMap<String, SkinData>();
    private final Map<String, Long> manualSet = new ConcurrentHashMap<String, Long>();
    private File cacheFile;
    private long lastSyncMtime = -1L;

    public SkinCache(JavaPlugin plugin, PSkinConfig config, MySqlStorage db, File dataDir, boolean sharedDir) {
        this.plugin = plugin;
        this.config = config;
        this.db = db;
        this.sharedDir = sharedDir && db == null;
        this.dataDir = dataDir != null ? dataDir : plugin.getDataFolder();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cacheFile = new File(this.dataDir, "skin-cache.json");
        if (this.db != null) {
            this.importLegacyFile();
        } else if (this.sharedDir) {
            this.loadCacheFile(false);
            this.importLegacyLocalIntoShared();
        } else {
            this.loadCacheFile(false);
        }
    }

    private void loadCacheFile(boolean quiet) {
        this.cacheFile = new File(this.dataDir, "skin-cache.json");
        if (!this.cacheFile.exists()) {
            this.lastSyncMtime = 0L;
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(this.cacheFile.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);){
            Type type = new TypeToken<Map<String, CachedEntry>>(){}.getType();
            Map<?, ?> entries = (Map<?, ?>)this.gson.fromJson((Reader)reader, type);
            if (entries != null) {
                long now = System.currentTimeMillis();
                long ttl = this.getTtlMillis();
                for (Map.Entry<?, ?> e : entries.entrySet()) {
                    boolean expired;
                    CachedEntry entry = (CachedEntry)e.getValue();
                    if (entry == null || entry.value == null) continue;
                    boolean bl = expired = !entry.manual && now - entry.timestamp >= ttl;
                    if (expired && ttl > 0L) continue;
                    this.cache.put(((String)e.getKey()).toLowerCase(), new SkinData(entry.value, entry.signature, entry.source, entry.originalSource));
                    if (!entry.manual) continue;
                    this.manualSet.put(((String)e.getKey()).toLowerCase(), entry.timestamp);
                }
            }
            if (!quiet) {
                this.plugin.getLogger().info("\u5df2\u52a0\u8f7d\u76ae\u80a4\u7f13\u5b58: " + this.cache.size() + " \u6761\uff08\u624b\u52a8\u8bbe\u7f6e " + this.manualSet.size() + " \u6761\uff09");
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u52a0\u8f7d\u76ae\u80a4\u7f13\u5b58\u5931\u8d25: " + e.getMessage());
        }
        this.lastSyncMtime = this.cacheFile.exists() ? SharedFileIO.mtime(this.cacheFile) : 0L;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void syncFromDisk() {
        if (!this.sharedDir) {
            return;
        }
        long m = SharedFileIO.mtime(this.cacheFile);
        if (m != this.lastSyncMtime) {
            SkinCache skinCache = this;
            synchronized (skinCache) {
                m = SharedFileIO.mtime(this.cacheFile);
                if (m != this.lastSyncMtime) {
                    this.cache.clear();
                    this.manualSet.clear();
                    this.loadCacheFile(true);
                }
            }
        }
    }

    private void importLegacyLocalIntoShared() {
        File legacy = new File(this.plugin.getDataFolder(), "skin-cache.json");
        if (!legacy.isFile() || legacy.equals(this.cacheFile)) {
            return;
        }
        int imported = 0;
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(legacy.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);){
            Type type = new TypeToken<Map<String, CachedEntry>>(){}.getType();
            Map<?, ?> entries = (Map<?, ?>)this.gson.fromJson((Reader)reader, type);
            long now = System.currentTimeMillis();
            long ttl = this.getTtlMillis();
            if (entries != null) {
                for (Map.Entry<?, ?> e : entries.entrySet()) {
                    String key;
                    boolean expired;
                    CachedEntry entry = (CachedEntry)e.getValue();
                    if (entry == null || entry.value == null || e.getKey() == null || (expired = !entry.manual && ttl > 0L && now - entry.timestamp >= ttl) || this.cache.containsKey(key = ((String)e.getKey()).toLowerCase())) continue;
                    this.cache.put(key, new SkinData(entry.value, entry.signature, entry.source, entry.originalSource));
                    if (entry.manual) {
                        this.manualSet.put(key, entry.timestamp);
                    }
                    ++imported;
                }
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u5408\u5e76\u672c\u5730\u76ae\u80a4\u7f13\u5b58\u5230\u5171\u4eab\u76ee\u5f55\u5931\u8d25: " + e.getMessage());
            return;
        }
        if (imported > 0) {
            this.saveCache();
            if (legacy.renameTo(new File(this.plugin.getDataFolder(), "skin-cache.json.imported"))) {
                this.plugin.getLogger().info("\u5df2\u628a\u672c\u5730\u76ae\u80a4\u7f13\u5b58 " + imported + " \u6761\u5408\u5e76\u8fdb\u5171\u4eab\u76ee\u5f55\uff08\u539f\u6587\u4ef6\u6539\u540d\u4e3a skin-cache.json.imported\uff09");
            } else {
                this.plugin.getLogger().info("\u5df2\u628a\u672c\u5730\u76ae\u80a4\u7f13\u5b58 " + imported + " \u6761\u5408\u5e76\u8fdb\u5171\u4eab\u76ee\u5f55");
            }
        }
    }

    private void importLegacyFile() {
        File legacy = new File(this.plugin.getDataFolder(), "skin-cache.json");
        if (!legacy.isFile()) {
            this.plugin.getLogger().info("\u5df2\u8fde\u63a5\u76ae\u80a4\u7f13\u5b58\uff08MySQL\uff09: " + this.db.countCache(false) + " \u6761");
            return;
        }
        int imported = 0;
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(legacy.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);){
            Type type = new TypeToken<Map<String, CachedEntry>>(){}.getType();
            Map<?, ?> entries = (Map<?, ?>)this.gson.fromJson((Reader)reader, type);
            long now = System.currentTimeMillis();
            long ttl = this.getTtlMillis();
            if (entries != null) {
                for (Map.Entry<?, ?> e : entries.entrySet()) {
                    boolean expired;
                    CachedEntry entry = (CachedEntry)e.getValue();
                    if (entry == null || entry.value == null || e.getKey() == null) continue;
                    boolean bl = expired = !entry.manual && ttl > 0L && now - entry.timestamp >= ttl;
                    if (expired) continue;
                    this.db.importCacheEntry(((String)e.getKey()).toLowerCase(), entry);
                    ++imported;
                }
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u5bfc\u5165\u65e7\u76ae\u80a4\u7f13\u5b58\u5230 MySQL \u5931\u8d25: " + e.getMessage());
        }
        if (legacy.renameTo(new File(this.plugin.getDataFolder(), "skin-cache.json.imported"))) {
            this.plugin.getLogger().info("\u5df2\u5bfc\u5165\u65e7\u76ae\u80a4\u7f13\u5b58 " + imported + " \u6761\u5230 MySQL\uff08\u539f\u6587\u4ef6\u6539\u540d\u4e3a skin-cache.json.imported\uff09");
        } else {
            this.plugin.getLogger().info("\u5df2\u5bfc\u5165\u65e7\u76ae\u80a4\u7f13\u5b58 " + imported + " \u6761\u5230 MySQL\uff08\u539f\u6587\u4ef6\u4fdd\u7559\uff0c\u4f46\u4e0d\u4f1a\u518d\u5bfc\u5165\uff09");
        }
    }

    public synchronized void saveCache() {
        if (this.db != null) {
            return;
        }
        try {
            this.dataDir.mkdirs();
            LinkedHashMap<String, CachedEntry> entries = new LinkedHashMap<String, CachedEntry>();
            long now = System.currentTimeMillis();
            long ttl = this.getTtlMillis();
            for (Map.Entry<String, SkinData> e : this.cache.entrySet()) {
                boolean expired;
                boolean manual = this.manualSet.containsKey(e.getKey());
                boolean bl = expired = !manual && ttl > 0L && now - e.getValue().getTimestamp() >= ttl;
                if (expired) continue;
                CachedEntry entry = new CachedEntry();
                entry.value = e.getValue().getValue();
                entry.signature = e.getValue().getSignature();
                entry.source = e.getValue().getSource();
                entry.originalSource = e.getValue().getOriginalSource();
                entry.timestamp = e.getValue().getTimestamp();
                entry.manual = manual;
                entries.put(e.getKey(), entry);
            }
            String json = this.gson.toJson(entries);
            if (this.sharedDir) {
                SharedFileIO.lockedWrite(this.cacheFile, json);
            } else {
                SharedFileIO.atomicWrite(this.cacheFile, json);
            }
            this.lastSyncMtime = SharedFileIO.mtime(this.cacheFile);
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u4fdd\u5b58\u76ae\u80a4\u7f13\u5b58\u5931\u8d25: " + e.getMessage());
        }
    }

    private long getTtlMillis() {
        return (long)this.config.getSkinCacheMinutes() * 60000L;
    }

    private SkinData fromEntry(CachedEntry entry) {
        if (entry == null || entry.value == null) {
            return null;
        }
        return new SkinData(entry.value, entry.signature, entry.source, entry.originalSource);
    }

    public SkinData get(String username) {
        String key = username.toLowerCase();
        if (this.db != null) {
            CachedEntry entry = this.db.loadCacheEntry(key);
            if (entry == null || entry.value == null) {
                return null;
            }
            long ttl = this.getTtlMillis();
            if (!entry.manual && ttl > 0L && System.currentTimeMillis() - entry.timestamp >= ttl) {
                this.db.deleteCacheEntry(key);
                return null;
            }
            return this.fromEntry(entry);
        }
        this.syncFromDisk();
        SkinData data = this.cache.get(key);
        if (data == null) {
            return null;
        }
        if (this.manualSet.containsKey(key)) {
            return data;
        }
        long ttl = this.getTtlMillis();
        if (ttl > 0L && System.currentTimeMillis() - data.getTimestamp() >= ttl) {
            this.cache.remove(key);
            return null;
        }
        return data;
    }

    public void put(String username, SkinData data, boolean manual) {
        if (data == null) {
            return;
        }
        String key = username.toLowerCase();
        if (this.db != null) {
            CachedEntry entry = new CachedEntry();
            entry.value = data.getValue();
            entry.signature = data.getSignature();
            entry.source = data.getSource();
            entry.originalSource = data.getOriginalSource();
            entry.manual = manual;
            entry.timestamp = System.currentTimeMillis();
            this.db.saveCacheEntry(key, entry);
            return;
        }
        this.syncFromDisk();
        this.cache.put(key, data);
        if (manual) {
            this.manualSet.put(key, System.currentTimeMillis());
        } else {
            this.manualSet.remove(key);
        }
        this.saveCache();
    }

    public void invalidate(String username) {
        String key = username.toLowerCase();
        if (this.db != null) {
            this.db.deleteCacheEntry(key);
            return;
        }
        this.syncFromDisk();
        this.cache.remove(key);
        this.manualSet.remove(key);
        this.saveCache();
    }

    public void remove(String username) {
        this.invalidate(username);
    }

    public boolean isManual(String username) {
        if (this.db != null) {
            return this.db.isCacheManual(username.toLowerCase());
        }
        this.syncFromDisk();
        return this.manualSet.containsKey(username.toLowerCase());
    }

    public int getCachedCount() {
        if (this.db != null) {
            return this.db.countCache(false);
        }
        this.syncFromDisk();
        return this.cache.size();
    }

    public int getManualCount() {
        if (this.db != null) {
            return this.db.countCache(true);
        }
        this.syncFromDisk();
        return this.manualSet.size();
    }

    public Map<String, SkinData> getAllEntries() {
        LinkedHashMap<String, SkinData> result = new LinkedHashMap<String, SkinData>();
        long now = System.currentTimeMillis();
        long ttl = this.getTtlMillis();
        if (this.db != null) {
            for (Map.Entry<String, CachedEntry> e : this.db.loadAllCacheEntries().entrySet()) {
                boolean expired;
                if (e.getValue() == null || e.getValue().value == null) continue;
                boolean bl = expired = !e.getValue().manual && ttl > 0L && now - e.getValue().timestamp >= ttl;
                if (expired) continue;
                result.put(e.getKey(), this.fromEntry(e.getValue()));
            }
            return result;
        }
        this.syncFromDisk();
        for (Map.Entry<String, SkinData> e : this.cache.entrySet()) {
            boolean manual = this.manualSet.containsKey(e.getKey());
            if (!manual && ttl > 0L && now - e.getValue().getTimestamp() >= ttl) continue;
            result.put(e.getKey(), e.getValue());
        }
        return result;
    }

    static class CachedEntry {
        String value;
        String signature;
        String source;
        String originalSource;
        long timestamp;
        boolean manual;

        CachedEntry() {
        }
    }
}
