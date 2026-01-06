package com.forge.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class LoreUtil {
    @SuppressWarnings("deprecation")
    public static ItemStack name(ItemStack it, String name) {
        if (it == null) return null;
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        it.setItemMeta(meta);
        return it;
    }
    @SuppressWarnings("deprecation")
    public static ItemStack lore(ItemStack it, List<String> lines) {
        if (it == null) return null;
        ItemMeta meta = it.getItemMeta();
        meta.setLore(lines);
        it.setItemMeta(meta);
        return it;
    }
    public static List<String> formatAttributes(Map<String, Double> attrs) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Double> e : attrs.entrySet()) list.add("§7"+e.getKey()+": §a"+String.format("%.2f", e.getValue()));
        return list;
    }
    @SuppressWarnings("deprecation")
    public static Map<String, Double> parseAttributes(List<String> lore) {
        Map<String, Double> map = new HashMap<>();
        if (lore == null) return map;
        for (String s : lore) {
            String stripped = ChatColor.stripColor(s);
            int i = stripped.indexOf(":");
            if (i <= 0) continue;
            String k = stripped.substring(0, i).trim();
            // Skip non-attribute keys if necessary, or just parse everything that looks like "Key: Value"
            // We should filter out "Melting Point" etc if they are not considered "Attributes" for the result map.
            // But the user might want them? 
            // Usually "Melting Point" is a property, "Attack" is an attribute.
            // ForgingService.finalizeForge uses this map to sum up "Attributes".
            // If "Melting Point" is in the map, it will be summed up.
            // In MaterialService.updateItemLore, Melting/Extensibility are added separately before the "Attributes:" section?
            // No, look at MaterialService:
            // lore.add("§7熔点: §f" + ...);
            // lore.add("§7延展性: §f" + ...);
            // lore.add("§7属性:");
            // lore.add("  §7" + e.getKey() + ": §a" + e.getValue());
            
            // So real attributes are indented? "  §7Key: §aValue"
            // stripColor will remove spaces? No.
            // "  Key: Value"
            
            // If we blindly parse all "Key: Value", we get Melting Point too.
            // We should probably filter based on context or indentation?
            // Or just parsing everything is fine, as long as the Result Item *should* have them?
            // But the result item is a new item.
            // We sum up attributes. Do we sum up Melting Point? Probably not.
            
            // Let's look at `MaterialService.updateItemLore` again.
            // It explicitly adds "Attributes:" header.
            // Maybe we should only parse lines *after* "Attributes:" or lines that are indented?
            // Or maybe just parse lines that don't match known system keys?
            
            if (k.equals("熔点") || k.equals("延展性") || k.equals("稳定度") || k.equals("材料星缀") || k.equals("裂痕值")) continue;
            
            String vStr = stripped.substring(i+1).trim();
            try { 
                map.put(k, Double.parseDouble(vStr)); 
            } catch (Exception ignored) {}
        }
        return map;
    }
    
    @SuppressWarnings("deprecation")
    public static double getValue(ItemStack it, String key, double def) {
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasLore()) return def;
        List<String> lore = it.getItemMeta().getLore();
        for (String s : lore) {
            String stripped = ChatColor.stripColor(s);
            if (stripped.contains(key + ":")) { // contains to handle indentation or prefixes
                 // But we want to be sure it's the key.
                 // "  Key: Value" -> split by ":" -> "  Key", " Value" -> trim -> "Key", "Value"
                 int idx = stripped.indexOf(":");
                 if (idx > 0) {
                     String k = stripped.substring(0, idx).trim();
                     if (k.equals(key)) {
                         try {
                             return Double.parseDouble(stripped.substring(idx+1).trim());
                         } catch (Exception ignored) {}
                     }
                 }
            }
        }
        return def;
    }
}
