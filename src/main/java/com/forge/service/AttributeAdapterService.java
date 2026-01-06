package com.forge.service;

import com.forge.store.YamlStore;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AttributeAdapterService {
    private final YamlConfiguration cfg;
    public AttributeAdapterService(YamlStore store) { this.cfg = store.load("mappings.yml"); }
    @SuppressWarnings("deprecation")
    public void applyAttributes(ItemStack item, Map<String, Double> attrs) {
        if (item == null || attrs == null) return;
        Map<String, Mapping> maps = loadMappings();
        ItemMeta meta = item.getItemMeta();
        for (Map.Entry<String, Double> e : attrs.entrySet()) {
            String src = e.getKey();
            double val = e.getValue();
            Mapping m = maps.get(src);
            if (m == null) continue;
            double v = val * m.percent / 100.0;
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            if (m.target.startsWith("VANILLA:")) {
                String name = m.target.substring("VANILLA:".length());
                lore.add("§6原版属性 §7"+name+": §a"+String.format("%.2f", v));
            } else if (m.target.startsWith("SX:")) {
                lore.add("§bSX §7"+m.target.substring(3)+": §a"+String.format("%.2f", v));
            } else if (m.target.startsWith("AP:")) {
                lore.add("§dAP §7"+m.target.substring(3)+": §a"+String.format("%.2f", v));
            } else if (m.target.startsWith("APS:")) {
                lore.add("§9APS §7"+m.target.substring(4)+": §a"+String.format("%.2f", v));
            } else {
                lore.add("§7"+m.target+": §a"+String.format("%.2f", v));
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
    }
    private Map<String, Mapping> loadMappings() {
        Map<String, Mapping> out = new HashMap<>();
        if (cfg.isConfigurationSection("mappings")) {
            for (String k : cfg.getConfigurationSection("mappings").getKeys(false)) {
                String target = cfg.getString("mappings."+k+".target", "");
                double percent = cfg.getDouble("mappings."+k+".percent", 100);
                out.put(k, new Mapping(target, percent));
            }
        }
        return out;
    }
    static class Mapping {
        final String target;
        final double percent;
        Mapping(String t, double p) { this.target = t; this.percent = p; }
    }
}
