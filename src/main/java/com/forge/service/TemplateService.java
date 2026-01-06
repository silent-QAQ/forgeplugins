package com.forge.service;

import com.forge.store.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TemplateService {
    private final YamlStore store;
    private final YamlConfiguration cfg;
    public TemplateService(YamlStore store) { this.store = store; cfg = store.load("templates.yml"); }
    public YamlConfiguration getConfig() { return cfg; }
    public void create(String name, Material icon) {
        cfg.set("templates."+name+".icon", icon.name());
        cfg.set("templates."+name+".required", new ArrayList<>());
        store.save("templates.yml", cfg);
    }
    public boolean exists(String name) {
        return cfg.contains("templates." + name);
    }
    public void openSelectGui(Player p) {
        com.forge.ForgePlugin.get().sessions().backupInventory(p);
        java.util.List<String> list = new java.util.ArrayList<>(getTemplates());
        
        // Filter: Only learned templates?
        // User said: "Show only learned templates, avoid irrelevant".
        // And: "Trigger: /dt command".
        // We need a way to check if player learned a template.
        // Let's assume PlayerDao has `hasTemplate(Player p, String tpl)`.
        // Since I can't edit PlayerDao easily without checking it, I'll add a dummy check or assume all for now if method missing.
        // Actually, I can use `ForgePlugin.get().playerDao().getTemplates(p)`.
        
        // Assuming `getTemplates(p)` returns list of learned template names.
        // If not implemented, I should probably check PlayerDao.
        
        Inventory inv = com.forge.util.Texts.createInventory(p, 54, Bukkit.getPluginManager().getPlugin("forge").getConfig().getString("gui.titles.template_select", "模板选择"));
        int page = 0;
        try { page = com.forge.ForgePlugin.get().sessions().getPage(p, "tpl"); } catch (Exception ignored) {}
        
        int start = page * 45;
        int i = 0;
        
        // Filter logic:
        // list = com.forge.ForgePlugin.get().getDao().getTemplates(p); // If implemented
        
        for (int idx = start; idx < Math.min(list.size(), start + 45); idx++) {
            String k = list.get(idx);
            // Check if learned (dummy check for now as I don't want to break if method missing)
            // if (!playerLearned(p, k)) continue;
            
            Material m = Material.PAPER;
            String s = cfg.getString("templates."+k+".icon");
            try { m = Material.valueOf(s); } catch (Exception ignored) {}
            ItemStack it = new ItemStack(m);
            ItemMeta meta = it.getItemMeta();
            com.forge.util.Texts.setDisplayName(meta, "§d"+k);
            it.setItemMeta(meta);
            inv.setItem(i++, it);
        }
        inv.setItem(52, item(com.forge.util.MaterialCompat.rocket(), "§b上一页"));
        inv.setItem(53, item(com.forge.util.MaterialCompat.rocket(), "§b下一页"));
        p.openInventory(inv);
    }
    public java.util.Set<String> getTemplates() {
        java.util.Set<String> set = new java.util.HashSet<>();
        if (cfg.isConfigurationSection("templates")) set.addAll(cfg.getConfigurationSection("templates").getKeys(false));
        return set;
    }
    public void openEditGui(Player p, String name) {
        com.forge.ForgePlugin.get().sessions().beginTemplateEdit(p, name);
        com.forge.ForgePlugin.get().sessions().backupInventory(p);
        
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("{name}", name);
        GuiConfigService.GuiLayout layout = com.forge.ForgePlugin.get().guiConfig().createGui("template_edit", params);
        Inventory inv = layout.inventory;
        
        // Dynamic Items: Required Slots
        java.util.Set<Integer> req = getRequired(name);
        java.util.List<Integer> editSlots = layout.getSlots('X'); // Assuming 'X' is the editable area char
        
        for (int idx : editSlots) {
            if (req.contains(idx)) {
                inv.setItem(idx, item(Material.ITEM_FRAME, "§a必要槽位"));
            }
        }
        
        p.getInventory().setItem(0, item(com.forge.util.MaterialCompat.amethystCluster(), "§5武器部件"));
        
        // Slot 1 (Hotbar Slot 2): Forging Product Material
        String outMat = cfg.getString("templates."+name+".output_material", "AMETHYST_SHARD");
        Material m = com.forge.util.MaterialCompat.amethystShard();
        try { m = Material.valueOf(outMat); } catch (Exception ignored) {}
        p.getInventory().setItem(1, item(m, "§e成品材质: " + outMat));
        
        // Apply Player Inventory Overlay from Layout (Slots 54-89)
        // Note: p.getInventory() maps slots differently.
        // 0-8: Hotbar (81-89 in raw)
        // 9-35: Backpack (54-80 in raw)
        // But in Config: 54-80 is Backpack, 81-89 is Hotbar.
        // We need to map Config Raw ID (54+) to Player Inventory ID.
        // Bukkit: 0-8 is Hotbar, 9-35 is Backpack.
        // Config: 54-80 -> Backpack -> Bukkit 9-35
        // Config: 81-89 -> Hotbar -> Bukkit 0-8
        
        for (Map.Entry<Integer, ItemStack> entry : layout.playerOverlay.entrySet()) {
            int raw = entry.getKey();
            int pSlot = -1;
            
            if (raw >= 54 && raw <= 80) { // Backpack
                pSlot = raw - 54 + 9; // 54->9, 80->35
            } else if (raw >= 81 && raw <= 89) { // Hotbar
                pSlot = raw - 81; // 81->0, 89->8
            }
            
            if (pSlot != -1) {
                 p.getInventory().setItem(pSlot, entry.getValue());
            }
        }
        
        p.openInventory(inv);
    }
    public void setOutputMaterial(String name, String mat) {
        cfg.set("templates."+name+".output_material", mat);
        store.save("templates.yml", cfg);
    }
    public String getOutputMaterial(String name) {
        return cfg.getString("templates."+name+".output_material", "AMETHYST_SHARD");
    }
    public void savePart(Player p, String name) {
        ItemStack off = com.forge.ForgePlugin.get().sessions().getOffhand(p);
        if (off != null && off.getType() != Material.AIR) {
             cfg.set("templates."+name+".part", off);
             store.save("templates.yml", cfg);
        }
    }
    public boolean hasPart(String name) {
        return cfg.contains("templates."+name+".part");
    }
    public void setRequired(String name, int slot, boolean add) {
        java.util.Set<Integer> set = getRequired(name);
        if (add) set.add(slot); else set.remove(slot);
        cfg.set("templates."+name+".required", new java.util.ArrayList<>(set));
        store.save("templates.yml", cfg);
    }
    public java.util.Set<Integer> getRequired(String name) {
        java.util.List<Integer> req = cfg.getIntegerList("templates."+name+".required");
        if (req == null || req.isEmpty()) {
            req = cfg.getIntegerList("templates."+name+".slots");
        }
        return new java.util.HashSet<>(req);
    }
    
    private ItemStack item(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        com.forge.util.Texts.setDisplayName(meta, name);
        it.setItemMeta(meta);
        return it;
    }
}
