package com.forge.storage;

import java.sql.*;
import java.util.*;

public class PlayerDao {
    private final Database db;
    public PlayerDao(Database db) { this.db = db; }
    public void ensure(UUID uuid, String name) {
        // Try update name first
        try (PreparedStatement ps = db.conn().prepareStatement("UPDATE players SET name=? WHERE uuid=?")) {
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                // Insert if not exists
                String sql = "INSERT INTO players(uuid, name, forging_level) VALUES(?, ?, 0)";
                try (PreparedStatement ins = db.conn().prepareStatement(sql)) {
                    ins.setString(1, uuid.toString());
                    ins.setString(2, name);
                    ins.executeUpdate();
                }
            }
        } catch (SQLException ignored) {}
    }
    public int getForgingLevel(UUID uuid) {
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT forging_level FROM players WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignored) {}
        return 0;
    }
    public void setForgingLevel(UUID uuid, int level) {
        try (PreparedStatement ps = db.conn().prepareStatement("UPDATE players SET forging_level=? WHERE uuid=?")) {
            ps.setInt(1, level);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }
    public void learnTemplate(UUID uuid, String template) {
        String sql = db.isMysql() ? "INSERT IGNORE INTO player_templates(uuid,template) VALUES(?,?)" : "INSERT OR IGNORE INTO player_templates(uuid,template) VALUES(?,?)";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, template);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }
    public void unlockTemplate(UUID uuid, String template) {
        learnTemplate(uuid, template);
    }
    public Set<String> getTemplates(UUID uuid) {
        Set<String> set = new HashSet<>();
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT template FROM player_templates WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(rs.getString(1));
            }
        } catch (SQLException ignored) {}
        return set;
    }
    public void learnStyle(UUID uuid, String style) {
        String sql = db.isMysql() ? "INSERT IGNORE INTO player_styles(uuid,style) VALUES(?,?)" : "INSERT OR IGNORE INTO player_styles(uuid,style) VALUES(?,?)";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, style);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }
    public Set<String> getStyles(UUID uuid) {
        Set<String> set = new HashSet<>();
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT style FROM player_styles WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(rs.getString(1));
            }
        } catch (SQLException ignored) {}
        return set;
    }
}
