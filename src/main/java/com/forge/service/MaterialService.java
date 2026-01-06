package com.forge.service;

import com.forge.ForgePlugin;
import com.forge.store.YamlStore;
// import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MaterialService {
    private final YamlStore store;
    private final YamlConfiguration cfg;
    // private final FileConfiguration main;
    private final SessionService sessions;
    private final AttributeService attributes;
    public MaterialService(YamlStore store, FileConfiguration main, SessionService sessions, AttributeService attributes) {
        this.store = store;
        // this.main = main;
        this.sessions = sessions;
        this.attributes = attributes;
        cfg = store.load("materials.yml");
    }
    
    public void openEditGui(Player p) {
        // Open edit for item in hand
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            p.sendMessage("§c请手持要编辑的材料！");
            return;
        }
        openEditGui(p, hand);
    }

    
    public void openEditGui(Player p, ItemStack hand) {
        if (sessions != null) {
            // Store reference to "editing material" - actually we edit the item in hand.
            // But we need to know we are in edit mode.
            sessions.beginMaterialEdit(p, "HAND"); 
            sessions.backupInventory(p);
        }
        
        GuiConfigService.GuiLayout layout = ForgePlugin.get().guiConfig().createGui("material_edit", null);
        Inventory inv = layout.inventory;
        
        // Slot M (Material Display) - Slot 4
        int mSlot = layout.getFirstSlot('M');
        if (mSlot != -1) {
             ItemStack display = hand.clone();
             ItemMeta dm = display.getItemMeta();
             if (dm != null) com.forge.util.Texts.setDisplayName(dm, "§e当前编辑物品");
             display.setItemMeta(dm);
             inv.setItem(mSlot, display);
        }
        
        // Slot A (Attributes) - Slots 9-44
        java.util.List<Integer> aSlots = layout.getSlots('A');
        
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        items.add(withLore(item(Material.LAVA_BUCKET, "§e熔点"), java.util.Arrays.asList("§f当前: " + com.forge.util.LoreUtil.getValue(hand, "熔点", 240))));
        items.add(withLore(item(Material.NETHER_STAR, "§e材料星缀"), java.util.Arrays.asList("§f当前: " + getStarTag(hand))));
        items.add(withLore(item(Material.IRON_BARS, "§e延展性"), java.util.Arrays.asList("§f当前: " + com.forge.util.LoreUtil.getValue(hand, "延展性", 0))));
        items.add(withLore(item(Material.OBSIDIAN, "§e稳定度"), java.util.Arrays.asList("§f当前: " + getStabilityString(hand))));
        
        java.util.Map<String, Double> attrs = com.forge.util.LoreUtil.parseAttributes(hand.hasItemMeta() && hand.getItemMeta().hasLore() ? com.forge.util.Texts.getLore(hand.getItemMeta()) : null);
        
        // Show all configured attributes, with current values from item
        java.util.Map<String, String> all = attributes.all();
        for (String k : all.keySet()) {
             Material icon = Material.PAPER;
             String iconName = all.get(k);
             if (iconName != null) {
                 try { icon = Material.valueOf(iconName); } catch (Exception ignored) {}
             }
             double val = attrs.getOrDefault(k, 0.0);
             items.add(withLore(item(icon, "§b" + k), java.util.Arrays.asList("§f数值: " + val)));
        }
        
        for (int i = 0; i < Math.min(items.size(), aSlots.size()); i++) {
             inv.setItem(aSlots.get(i), items.get(i));
        }
        
        ForgePlugin.get().guiConfig().applyPlayerOverlay(p, layout);
        p.openInventory(inv);
    }

    public void setStability(Player p, String val) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return;
        ItemMeta meta = hand.getItemMeta();
        java.util.List<String> lore = meta.hasLore() ? com.forge.util.Texts.getLore(meta) : new java.util.ArrayList<>();
        lore.removeIf(s -> com.forge.util.Texts.stripColor(s).startsWith("稳定度:"));
        if (lore.isEmpty()) lore.add("§e§l[锻造材料]");
        lore.add(1, "§7稳定度: §f" + val);
        com.forge.util.Texts.setLore(meta, lore);
        hand.setItemMeta(meta);
        p.getInventory().setItemInMainHand(hand);
    }
    public void setValue(Player p, String key, double val) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return;
        
        ItemMeta meta = hand.getItemMeta();
        java.util.List<String> lore = meta.hasLore() ? com.forge.util.Texts.getLore(meta) : new java.util.ArrayList<>();
        
        // We need to update or add the key
        // Special keys: 熔点, 延展性, 稳定度
        // Normal keys: Attributes
        
        // Strategy: 
        // 1. Remove existing line with this key
        // 2. Add new line
        
        // Remove existing
        lore.removeIf(s -> com.forge.util.Texts.stripColor(s).startsWith(key + ":") || com.forge.util.Texts.stripColor(s).trim().startsWith(key + ":"));
        
        // Add new
        if (key.equals("熔点") || key.equals("延展性") || key.equals("稳定度")) {
            // Add at top or specific section?
            // "Material Service" updateItemLore used to add them at top.
            // Let's just add to list. Order might be messy if we just append.
            // But preserving order is hard without parsing whole structure.
            // Let's just append for now, or insert at top if empty?
            if (lore.isEmpty()) lore.add("§e§l[锻造材料]");
            if (key.equals("稳定度")) {
                lore.add(1, "§7稳定度: §f" + String.format("%.0f", val) + "/" + String.format("%.0f", val));
            } else {
                lore.add(1, "§7" + key + ": §f" + String.format("%.0f", val)); // Insert after header
            }
        } else {
            // Attribute
            // Check if "Attributes:" header exists
            boolean hasHeader = false;
            for (String s : lore) if (s.contains("属性:")) hasHeader = true;
            if (!hasHeader) lore.add("§7属性:");
            
            lore.add("  §7" + key + ": §a" + val);
        }
        
        com.forge.util.Texts.setLore(meta, lore);
        hand.setItemMeta(meta);
        p.getInventory().setItemInMainHand(hand); // Update hand
    }
    public void setStarTag(Player p, String tag) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return;
        ItemMeta meta = hand.getItemMeta();
        java.util.List<String> lore = meta.hasLore() ? com.forge.util.Texts.getLore(meta) : new java.util.ArrayList<>();
        lore.removeIf(s -> com.forge.util.Texts.stripColor(s).startsWith("材料星缀:"));
        if (lore.isEmpty()) lore.add("§e§l[锻造材料]");
        lore.add(1, "§7材料星缀: §f" + tag);
        com.forge.util.Texts.setLore(meta, lore);
        hand.setItemMeta(meta);
        p.getInventory().setItemInMainHand(hand);
    }
    
    // Repel/Resonance still use config as they are relations
    public void setRepel(String a, String b, double v) {
        cfg.set("repel."+a+"."+b, v);
        store.save("materials.yml", cfg);
    }
    public void setResonance(String a, String b, double v) {
        cfg.set("resonance."+a+"."+b, v);
        store.save("materials.yml", cfg);
    }
    public double getRepel(String a, String b) { return cfg.getDouble("repel."+a+"."+b, 0); }
    public double getResonance(String a, String b) { return cfg.getDouble("resonance."+a+"."+b, 0); }
    public java.util.List<String> listRepel(String tag) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (cfg.isConfigurationSection("repel")) {
            for (String a : cfg.getConfigurationSection("repel").getKeys(false)) {
                if (a.equals(tag) && cfg.isConfigurationSection("repel."+a)) {
                    for (String b : cfg.getConfigurationSection("repel."+a).getKeys(false)) {
                        double v = cfg.getDouble("repel."+a+"."+b, 0);
                        list.add(a+"&"+b+"-"+v);
                    }
                } else if (cfg.isConfigurationSection("repel."+a) && cfg.getConfigurationSection("repel."+a).getKeys(false).contains(tag)) {
                    double v = cfg.getDouble("repel."+a+"."+tag, 0);
                    list.add(a+"&"+tag+"-"+v);
                }
            }
        }
        return list;
    }
    public java.util.List<String> listResonance(String tag) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (cfg.isConfigurationSection("resonance")) {
            for (String a : cfg.getConfigurationSection("resonance").getKeys(false)) {
                if (a.equals(tag) && cfg.isConfigurationSection("resonance."+a)) {
                    for (String b : cfg.getConfigurationSection("resonance."+a).getKeys(false)) {
                        double v = cfg.getDouble("resonance."+a+"."+b, 0);
                        list.add(a+"&"+b+"-"+v);
                    }
                } else if (cfg.isConfigurationSection("resonance."+a) && cfg.getConfigurationSection("resonance."+a).getKeys(false).contains(tag)) {
                    double v = cfg.getDouble("resonance."+a+"."+tag, 0);
                    list.add(a+"&"+tag+"-"+v);
                }
            }
        }
        return list;
    }
    
    private ItemStack item(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        com.forge.util.Texts.setDisplayName(meta, name);
        it.setItemMeta(meta);
        return it;
    }
    private ItemStack withLore(ItemStack it, java.util.List<String> lore) {
        ItemMeta meta = it.getItemMeta();
        com.forge.util.Texts.setLore(meta, lore);
        it.setItemMeta(meta);
        return it;
    }

    private String getStabilityString(ItemStack it) {
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasLore()) return "5";
        for (String s : com.forge.util.Texts.getLore(it.getItemMeta())) {
            String stripped = com.forge.util.Texts.stripColor(s);
            if (stripped.startsWith("稳定度:")) {
                return stripped.substring(stripped.indexOf(":")+1).trim();
            }
        }
        return "5";
    }

    private String getStarTag(ItemStack it) {
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasLore()) return "0";
        for (String s : com.forge.util.Texts.getLore(it.getItemMeta())) {
            String stripped = com.forge.util.Texts.stripColor(s);
            if (stripped.startsWith("材料星缀:")) {
                String v = stripped.substring(stripped.indexOf(":")+1).trim();
                return v.isEmpty() ? "0" : v;
            }
        }
        return "0";
    }
}
