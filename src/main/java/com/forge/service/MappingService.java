package com.forge.service;

import com.forge.store.YamlStore;
import org.bukkit.configuration.file.YamlConfiguration;

public class MappingService {
    private final YamlStore store;
    private final YamlConfiguration cfg;
    public MappingService(YamlStore store) { this.store = store; cfg = store.load("mappings.yml"); }
    public void map(String src, String dst, double percent) {
        cfg.set("mappings."+src+".target", dst);
        cfg.set("mappings."+src+".percent", percent);
        store.save("mappings.yml", cfg);
    }
}
