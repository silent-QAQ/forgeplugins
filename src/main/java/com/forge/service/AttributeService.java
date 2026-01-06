package com.forge.service;

import com.forge.store.YamlStore;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;

public class AttributeService {
    private final YamlStore store;
    private final YamlConfiguration cfg;
    public AttributeService(YamlStore store) {
        this.store = store;
        cfg = store.load("attributes.yml");
    }
    public void create(String name, Material icon) {
        cfg.set("attributes."+name+".icon", icon.name());
        store.save("attributes.yml", cfg);
    }
    public Map<String, String> all() {
        Map<String, String> m = new HashMap<>();
        if (cfg.isConfigurationSection("attributes")) {
            for (String k : cfg.getConfigurationSection("attributes").getKeys(false)) {
                if (cfg.isString("attributes." + k)) {
                    m.put(k, cfg.getString("attributes." + k));
                } else {
                    m.put(k, cfg.getString("attributes." + k + ".icon"));
                }
            }
        }
        return m;
    }
}
