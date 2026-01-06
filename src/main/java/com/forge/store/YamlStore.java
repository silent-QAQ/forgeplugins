package com.forge.store;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.io.IOException;

// Storage utility for YAML files
public class YamlStore {
    private final Plugin plugin;
    public YamlStore(Plugin plugin) { this.plugin = plugin; }
    public YamlConfiguration load(String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.exists()) {
            if (plugin.getResource(name) != null) {
                plugin.saveResource(name, false);
            } else {
                try { f.getParentFile().mkdirs(); f.createNewFile(); } catch (IOException ignored) {}
            }
        }
        return YamlConfiguration.loadConfiguration(f);
    }
    public void save(String name, YamlConfiguration cfg) {
        File f = new File(plugin.getDataFolder(), name);
        try { cfg.save(f); } catch (IOException ignored) {}
    }
}
