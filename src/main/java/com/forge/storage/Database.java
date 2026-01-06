package com.forge.storage;

import org.bukkit.plugin.Plugin;
import java.sql.*;

public class Database {
    private final Plugin plugin;
    private Connection connection;
    private boolean mysql;
    public Database(Plugin plugin) { this.plugin = plugin; init(); }
    private void init() {
        String type = plugin.getConfig().getString("database.type", "sqlite");
        try {
            if (type.equalsIgnoreCase("mysql")) {
                mysql = true;
                String host = plugin.getConfig().getString("database.mysql.host");
                int port = plugin.getConfig().getInt("database.mysql.port");
                String db = plugin.getConfig().getString("database.mysql.database");
                String user = plugin.getConfig().getString("database.mysql.user");
                String pass = plugin.getConfig().getString("database.mysql.password");
                connection = DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+db+"?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", user, pass);
            } else {
                mysql = false;
                String file = plugin.getConfig().getString("database.sqlite.file", "plugins/forge/data.db");
                connection = DriverManager.getConnection("jdbc:sqlite:" + file);
            }
            try (Statement st = connection.createStatement()) {
                String playerTable = "CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(64), forging_level INTEGER DEFAULT 0)";
                String tplTable = "CREATE TABLE IF NOT EXISTS player_templates (uuid VARCHAR(36), template VARCHAR(128), PRIMARY KEY(uuid, template))";
                String styleTable = "CREATE TABLE IF NOT EXISTS player_styles (uuid VARCHAR(36), style VARCHAR(128), PRIMARY KEY(uuid, style))";
                st.executeUpdate(playerTable);
                try { st.executeUpdate("ALTER TABLE players ADD COLUMN forging_level INTEGER DEFAULT 0"); } catch (SQLException ignored) {}
                st.executeUpdate(tplTable);
                st.executeUpdate(styleTable);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("数据库初始化失败"+e.getMessage());
        }
    }
    public Connection conn() { return connection; }
    public boolean isMysql() { return mysql; }
    public void close() { try { if (connection != null) connection.close(); } catch (SQLException ignored) {} }
}
