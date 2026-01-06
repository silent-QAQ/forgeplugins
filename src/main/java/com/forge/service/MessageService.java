package com.forge.service;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class MessageService {
    private final YamlConfiguration lang;
    public MessageService(Plugin plugin) {
        File f = new File(plugin.getDataFolder(), "messages_zh.yml");
        if (!f.exists()) plugin.saveResource("messages_zh.yml", false);
        lang = YamlConfiguration.loadConfiguration(f);
    }
    @SuppressWarnings("deprecation")
    public String msg(String key) {
        String s = lang.getString(key, key);
        s = ChatColor.translateAlternateColorCodes('&', s);
        String p = ChatColor.translateAlternateColorCodes('&', lang.getString("prefix", ""));
        return p + s;
    }
}
