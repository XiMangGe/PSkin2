/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.pskin;

import com.pskin.MySqlStorage;
import com.pskin.SharedFileIO;
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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.java.JavaPlugin;

public class SourceLockManager {
    public static final String MOJANG = "mojang";
    public static final String LITTLESKIN = "littleskin";
    public static final String OFFLINE = "offline";
    public static final String WEB = "web";
    private final JavaPlugin plugin;
    private final MySqlStorage db;
    private final Gson gson;
    private final boolean sharedDir;
    private final File dataDir;
    private final Map<String, String> locks = new ConcurrentHashMap<String, String>();
    private File lockFile;
    private long lastSyncMtime = -1L;

    public SourceLockManager(JavaPlugin plugin, MySqlStorage db, File dataDir, boolean sharedDir) {
        this.plugin = plugin;
        this.db = db;
        this.sharedDir = sharedDir && db == null;
        this.dataDir = dataDir != null ? dataDir : plugin.getDataFolder();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.lockFile = new File(this.dataDir, "source-locks.json");
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
        this.lockFile = new File(this.dataDir, "source-locks.json");
        if (!this.lockFile.exists()) {
            this.lastSyncMtime = 0L;
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(this.lockFile.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);){
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<?, ?> loaded = (Map<?, ?>)this.gson.fromJson((Reader)reader, type);
            if (loaded != null) {
                for (Map.Entry<?, ?> e : loaded.entrySet()) {
                    String normalized = SourceLockManager.normalizeSource((String)e.getValue());
                    if (e.getKey() == null || normalized == null) {
                        this.plugin.getLogger().warning("\u6765\u6e90\u9501\u5b9a\u6587\u4ef6\u4e2d\u5b58\u5728\u65e0\u6548\u6761\u76ee\uff0c\u5df2\u8df3\u8fc7: " + String.valueOf(e.getKey()) + " = " + String.valueOf(e.getValue()));
                        continue;
                    }
                    this.locks.put(((String)e.getKey()).toLowerCase(), normalized);
                }
            }
            if (!quiet) {
                this.plugin.getLogger().info("\u5df2\u52a0\u8f7d\u6765\u6e90\u9501\u5b9a: " + this.locks.size() + " \u6761");
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u52a0\u8f7d\u6765\u6e90\u9501\u5b9a\u5931\u8d25: " + e.getMessage());
        }
        this.lastSyncMtime = this.lockFile.exists() ? SharedFileIO.mtime(this.lockFile) : 0L;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void syncFromDisk() {
        if (!this.sharedDir) {
            return;
        }
        long m = SharedFileIO.mtime(this.lockFile);
        if (m != this.lastSyncMtime) {
            SourceLockManager sourceLockManager = this;
            synchronized (sourceLockManager) {
                m = SharedFileIO.mtime(this.lockFile);
                if (m != this.lastSyncMtime) {
                    this.locks.clear();
                    this.load(true);
                }
            }
        }
    }

    private void importLegacyLocalIntoShared() {
        File legacy = new File(this.plugin.getDataFolder(), "source-locks.json");
        if (!legacy.isFile() || legacy.equals(this.lockFile)) {
            return;
        }
        int imported = 0;
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(legacy.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);){
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<?, ?> loaded = (Map<?, ?>)this.gson.fromJson((Reader)reader, type);
            if (loaded != null) {
                for (Map.Entry<?, ?> e : loaded.entrySet()) {
                    String key;
                    String normalized = SourceLockManager.normalizeSource((String)e.getValue());
                    if (e.getKey() == null || normalized == null || this.locks.containsKey(key = ((String)e.getKey()).toLowerCase())) continue;
                    this.locks.put(key, normalized);
                    ++imported;
                }
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u5408\u5e76\u672c\u5730\u6765\u6e90\u9501\u5b9a\u5230\u5171\u4eab\u76ee\u5f55\u5931\u8d25: " + e.getMessage());
            return;
        }
        if (imported > 0) {
            this.save();
            if (legacy.renameTo(new File(this.plugin.getDataFolder(), "source-locks.json.imported"))) {
                this.plugin.getLogger().info("\u5df2\u628a\u672c\u5730\u6765\u6e90\u9501\u5b9a " + imported + " \u6761\u5408\u5e76\u8fdb\u5171\u4eab\u76ee\u5f55\uff08\u539f\u6587\u4ef6\u6539\u540d\u4e3a source-locks.json.imported\uff09");
            } else {
                this.plugin.getLogger().info("\u5df2\u628a\u672c\u5730\u6765\u6e90\u9501\u5b9a " + imported + " \u6761\u5408\u5e76\u8fdb\u5171\u4eab\u76ee\u5f55");
            }
        }
    }

    private void importLegacyFile() {
        if (!this.lockFile.isFile()) {
            this.plugin.getLogger().info("\u5df2\u8fde\u63a5\u6765\u6e90\u9501\u5b9a\uff08MySQL\uff09: " + this.db.countLocks() + " \u6761");
            return;
        }
        int imported = 0;
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(this.lockFile.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);){
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<?, ?> loaded = (Map<?, ?>)this.gson.fromJson((Reader)reader, type);
            if (loaded != null) {
                for (Map.Entry<?, ?> e : loaded.entrySet()) {
                    String normalized = SourceLockManager.normalizeSource((String)e.getValue());
                    if (e.getKey() == null || normalized == null) continue;
                    this.db.importLock(((String)e.getKey()).toLowerCase(), normalized);
                    ++imported;
                }
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u5bfc\u5165\u65e7\u6765\u6e90\u9501\u5b9a\u5230 MySQL \u5931\u8d25: " + e.getMessage());
        }
        if (this.lockFile.renameTo(new File(this.plugin.getDataFolder(), "source-locks.json.imported"))) {
            this.plugin.getLogger().info("\u5df2\u5bfc\u5165\u65e7\u6765\u6e90\u9501\u5b9a " + imported + " \u6761\u5230 MySQL\uff08\u539f\u6587\u4ef6\u6539\u540d\u4e3a source-locks.json.imported\uff09");
        } else {
            this.plugin.getLogger().info("\u5df2\u5bfc\u5165\u65e7\u6765\u6e90\u9501\u5b9a " + imported + " \u6761\u5230 MySQL\uff08\u539f\u6587\u4ef6\u4fdd\u7559\uff0c\u4f46\u4e0d\u4f1a\u518d\u5bfc\u5165\uff09");
        }
    }

    public void save() {
        if (this.db != null) {
            return;
        }
        try {
            this.dataDir.mkdirs();
            String json = this.gson.toJson(this.locks);
            if (this.sharedDir) {
                SharedFileIO.lockedWrite(this.lockFile, json);
            } else {
                SharedFileIO.atomicWrite(this.lockFile, json);
            }
            this.lastSyncMtime = SharedFileIO.mtime(this.lockFile);
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u4fdd\u5b58\u6765\u6e90\u9501\u5b9a\u5931\u8d25: " + e.getMessage());
        }
    }

    public static String normalizeSource(String source) {
        if (source == null) {
            return null;
        }
        String sl = source.toLowerCase().trim();
        if (MOJANG.equals(sl) || "premium".equals(sl) || "\u6b63\u7248".equals(sl) || "\u6b63".equals(sl)) return MOJANG;
        if (LITTLESKIN.equals(sl) || "little".equals(sl) || "ls".equals(sl) || "\u76ae\u80a4\u7ad9".equals(sl)) return LITTLESKIN;
        if (OFFLINE.equals(sl) || "none".equals(sl) || "default".equals(sl) || "\u79bb\u7ebf".equals(sl)) return OFFLINE;
        if (WEB.equals(sl) || "website".equals(sl) || "\u7f51\u7ad9".equals(sl) || "\u4e0a\u4f20".equals(sl)) return WEB;
        return null;
    }

    public String get(String username) {
        if (this.db != null) {
            return this.db.loadLock(username.toLowerCase());
        }
        this.syncFromDisk();
        return this.locks.get(username.toLowerCase());
    }

    public boolean isLocked(String username) {
        if (this.db != null) {
            return this.db.loadLock(username.toLowerCase()) != null;
        }
        this.syncFromDisk();
        return this.locks.containsKey(username.toLowerCase());
    }

    public void set(String username, String source) {
        String normalized = SourceLockManager.normalizeSource(source);
        if (normalized == null) {
            return;
        }
        if (this.db != null) {
            this.db.saveLock(username.toLowerCase(), normalized);
            return;
        }
        this.syncFromDisk();
        this.locks.put(username.toLowerCase(), normalized);
        this.save();
    }

    public void reset(String username) {
        if (this.db != null) {
            this.db.deleteLock(username.toLowerCase());
            return;
        }
        this.syncFromDisk();
        this.locks.remove(username.toLowerCase());
        this.save();
    }

    public int getCount() {
        if (this.db != null) {
            return this.db.countLocks();
        }
        this.syncFromDisk();
        return this.locks.size();
    }

    public Map<String, String> getAll() {
        if (this.db != null) {
            return this.db.loadAllLocks();
        }
        this.syncFromDisk();
        return this.locks;
    }
}
