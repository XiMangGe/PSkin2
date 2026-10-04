package com.pskin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class MySqlStorage {
    private final PSkinConfig config;
    private final Logger logger;
    private final String url;
    private final Properties props;
    private final String prefix;
    private Connection conn;

    public MySqlStorage(PSkinConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        StringBuilder sb = new StringBuilder("jdbc:mariadb://").append(config.getMySqlHost()).append(":").append(config.getMySqlPort()).append("/").append(config.getMySqlDatabase());
        sb.append("?sslMode=").append(config.isMySqlUseSsl() ? "trust" : "disable");
        this.url = sb.toString();
        this.props = new Properties();
        this.props.setProperty("user", config.getMySqlUsername());
        this.props.setProperty("password", config.getMySqlPassword());
        String p = config.getMySqlTablePrefix().replaceAll("[^A-Za-z0-9_]", "");
        this.prefix = p.isEmpty() ? "pskin_" : p;
    }

    public boolean init() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            this.logger.severe("\u672a\u627e\u5230 MariaDB/MySQL \u9a71\u52a8\u7c7b: " + e.getMessage());
            return false;
        }
        try {
            this.createTables();
            return true;
        } catch (SQLException e) {
            this.logger.severe("MySQL \u8fde\u63a5/\u5efa\u8868\u5931\u8d25\uff08host=" + this.config.getMySqlHost() + ":" + this.config.getMySqlPort() + " db=" + this.config.getMySqlDatabase() + "\uff09: " + e.getMessage());
            return false;
        }
    }

    private void createTables() throws SQLException {
        Connection c = this.connection();
        try (Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.prefix + "web_skins (name VARCHAR(64) NOT NULL, value MEDIUMTEXT NULL, signature MEDIUMTEXT NULL, model VARCHAR(16) NULL, png LONGBLOB NULL, timestamp BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (name)) DEFAULT CHARSET=utf8mb4");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.prefix + "source_locks (name VARCHAR(64) NOT NULL, source VARCHAR(32) NULL, PRIMARY KEY (name)) DEFAULT CHARSET=utf8mb4");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.prefix + "cache (name VARCHAR(64) NOT NULL, value MEDIUMTEXT NULL, signature MEDIUMTEXT NULL, source VARCHAR(32) NULL, original_source VARCHAR(32) NULL, manual TINYINT NOT NULL DEFAULT 0, timestamp BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (name)) DEFAULT CHARSET=utf8mb4");
        }
    }

    private synchronized Connection connection() throws SQLException {
        if (this.conn == null || this.conn.isClosed()) {
            this.conn = DriverManager.getConnection(this.url, this.props);
        }
        return this.conn;
    }

    private <T> T run(String what, SqlOp<T> op) {
        try {
            return op.run(this.connection());
        } catch (SQLException first) {
            this.logger.warning("MySQL \u64cd\u4f5c\u5931\u8d25(" + what + ")\uff0c\u91cd\u8fde\u91cd\u8bd5: " + first.getMessage());
            this.closeQuietly();
            try {
                return op.run(this.connection());
            } catch (SQLException second) {
                this.logger.warning("MySQL \u91cd\u8bd5\u4ecd\u5931\u8d25(" + what + "): " + second.getMessage());
                this.closeQuietly();
                return null;
            }
        }
    }

    private void closeQuietly() {
        try {
            if (this.conn != null && !this.conn.isClosed()) {
                this.conn.close();
            }
        } catch (SQLException ignored) {
        }
        this.conn = null;
    }

    public synchronized void close() {
        this.closeQuietly();
    }

    public boolean isMySql() {
        return this.config.isMySqlStorage();
    }

    public String describe() {
        return this.config.getMySqlHost() + ":" + this.config.getMySqlPort() + "/" + this.config.getMySqlDatabase();
    }

    public SkinCache.CachedEntry loadCacheEntry(String name) {
        return this.run("loadCacheEntry", c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT value, signature, source, original_source, manual, timestamp FROM " + this.prefix + "cache WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        SkinCache.CachedEntry entry = new SkinCache.CachedEntry();
                        entry.value = rs.getString(1);
                        entry.signature = rs.getString(2);
                        entry.source = rs.getString(3);
                        entry.originalSource = rs.getString(4);
                        entry.manual = rs.getInt(5) != 0;
                        entry.timestamp = rs.getLong(6);
                        return entry;
                    }
                    return null;
                }
            }
        });
    }

    public boolean isCacheManual(String name) {
        Boolean manual = this.run("isCacheManual", c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT manual FROM " + this.prefix + "cache WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) != 0;
                }
            }
        });
        return manual != null && manual;
    }

    public void saveCacheEntry(String name, SkinCache.CachedEntry entry) {
        this.run("saveCacheEntry", c -> {
            try (PreparedStatement ps = c.prepareStatement("REPLACE INTO " + this.prefix + "cache (name, value, signature, source, original_source, manual, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, name);
                ps.setString(2, entry.value);
                ps.setString(3, entry.signature);
                ps.setString(4, entry.source);
                ps.setString(5, entry.originalSource);
                ps.setInt(6, entry.manual ? 1 : 0);
                ps.setLong(7, entry.timestamp > 0L ? entry.timestamp : System.currentTimeMillis());
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void importCacheEntry(String name, SkinCache.CachedEntry entry) {
        this.run("importCacheEntry", c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO " + this.prefix + "cache (name, value, signature, source, original_source, manual, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, name);
                ps.setString(2, entry.value);
                ps.setString(3, entry.signature);
                ps.setString(4, entry.source);
                ps.setString(5, entry.originalSource);
                ps.setInt(6, entry.manual ? 1 : 0);
                ps.setLong(7, entry.timestamp);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void deleteCacheEntry(String name) {
        this.run("deleteCacheEntry", c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + this.prefix + "cache WHERE name = ?")) {
                ps.setString(1, name);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public Map<String, SkinCache.CachedEntry> loadAllCacheEntries() {
        Map<String, SkinCache.CachedEntry> result = this.run("loadAllCacheEntries", c -> {
            LinkedHashMap<String, SkinCache.CachedEntry> map = new LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT name, value, signature, source, original_source, manual, timestamp FROM " + this.prefix + "cache");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SkinCache.CachedEntry entry = new SkinCache.CachedEntry();
                    entry.value = rs.getString(2);
                    entry.signature = rs.getString(3);
                    entry.source = rs.getString(4);
                    entry.originalSource = rs.getString(5);
                    entry.manual = rs.getInt(6) != 0;
                    entry.timestamp = rs.getLong(7);
                    map.put(rs.getString(1), entry);
                }
            }
            return map;
        });
        return result == null ? new LinkedHashMap<>() : result;
    }

    public int countCache(boolean manualOnly) {
        Integer count = this.run("countCache", c -> {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + this.prefix + "cache" + (manualOnly ? " WHERE manual = 1" : ""))) {
                rs.next();
                return rs.getInt(1);
            }
        });
        return count == null ? 0 : count;
    }

    public String loadLock(String name) {
        return this.run("loadLock", c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT source FROM " + this.prefix + "source_locks WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        });
    }

    public void saveLock(String name, String source) {
        this.run("saveLock", c -> {
            try (PreparedStatement ps = c.prepareStatement("REPLACE INTO " + this.prefix + "source_locks (name, source) VALUES (?, ?)")) {
                ps.setString(1, name);
                ps.setString(2, source);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void importLock(String name, String source) {
        this.run("importLock", c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO " + this.prefix + "source_locks (name, source) VALUES (?, ?)")) {
                ps.setString(1, name);
                ps.setString(2, source);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void deleteLock(String name) {
        this.run("deleteLock", c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + this.prefix + "source_locks WHERE name = ?")) {
                ps.setString(1, name);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public Map<String, String> loadAllLocks() {
        Map<String, String> result = this.run("loadAllLocks", c -> {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name, source FROM " + this.prefix + "source_locks")) {
                while (rs.next()) {
                    map.put(rs.getString(1), rs.getString(2));
                }
            }
            return map;
        });
        return result == null ? new LinkedHashMap<>() : result;
    }

    public int countLocks() {
        Integer count = this.run("countLocks", c -> {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + this.prefix + "source_locks")) {
                rs.next();
                return rs.getInt(1);
            }
        });
        return count == null ? 0 : count;
    }

    public WebSkinManager.WebEntry loadWebSkin(String name) {
        return this.run("loadWebSkin", c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT value, signature, model, timestamp FROM " + this.prefix + "web_skins WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        WebSkinManager.WebEntry entry = new WebSkinManager.WebEntry();
                        entry.value = rs.getString(1);
                        entry.signature = rs.getString(2);
                        entry.model = rs.getString(3);
                        if (entry.model == null || entry.model.isEmpty()) {
                            entry.model = "classic";
                        }
                        entry.timestamp = rs.getLong(4);
                        return entry;
                    }
                    return null;
                }
            }
        });
    }

    public byte[] loadWebPng(String name) {
        return this.run("loadWebPng", c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT png FROM " + this.prefix + "web_skins WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        byte[] png = rs.getBytes(1);
                        return (png == null || png.length == 0) ? null : png;
                    }
                    return null;
                }
            }
        });
    }

    public void saveWebSkin(String name, WebSkinManager.WebEntry entry, byte[] png) {
        this.run("saveWebSkin", c -> {
            try (PreparedStatement ps = c.prepareStatement("REPLACE INTO " + this.prefix + "web_skins (name, value, signature, model, png, timestamp) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, name);
                ps.setString(2, entry.value);
                ps.setString(3, entry.signature);
                ps.setString(4, entry.model == null ? "classic" : entry.model);
                if (png != null && png.length > 0) {
                    ps.setBytes(5, png);
                } else {
                    ps.setNull(5, -4);
                }
                ps.setLong(6, entry.timestamp > 0L ? entry.timestamp : System.currentTimeMillis());
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void importWebSkin(String name, WebSkinManager.WebEntry entry, byte[] png) {
        this.run("importWebSkin", c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO " + this.prefix + "web_skins (name, value, signature, model, png, timestamp) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, name);
                ps.setString(2, entry.value);
                ps.setString(3, entry.signature);
                ps.setString(4, entry.model == null ? "classic" : entry.model);
                if (png != null && png.length > 0) {
                    ps.setBytes(5, png);
                } else {
                    ps.setNull(5, -4);
                }
                ps.setLong(6, entry.timestamp);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void deleteWebSkin(String name) {
        this.run("deleteWebSkin", c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + this.prefix + "web_skins WHERE name = ?")) {
                ps.setString(1, name);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public Map<String, WebSkinManager.WebEntry> loadAllWebSkins() {
        Map<String, WebSkinManager.WebEntry> result = this.run("loadAllWebSkins", c -> {
            LinkedHashMap<String, WebSkinManager.WebEntry> map = new LinkedHashMap<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name, value, signature, model, timestamp FROM " + this.prefix + "web_skins")) {
                while (rs.next()) {
                    WebSkinManager.WebEntry entry = new WebSkinManager.WebEntry();
                    entry.value = rs.getString(2);
                    entry.signature = rs.getString(3);
                    entry.model = rs.getString(4);
                    if (entry.model == null || entry.model.isEmpty()) {
                        entry.model = "classic";
                    }
                    entry.timestamp = rs.getLong(5);
                    map.put(rs.getString(1), entry);
                }
            }
            return map;
        });
        return result == null ? new LinkedHashMap<>() : result;
    }

    public int countWebSkins() {
        Integer count = this.run("countWebSkins", c -> {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + this.prefix + "web_skins")) {
                rs.next();
                return rs.getInt(1);
            }
        });
        return count == null ? 0 : count;
    }

    private interface SqlOp<T> {
        T run(Connection c) throws SQLException;
    }
}
