package com.forge.service;

import com.forge.ForgePlugin;
import com.forge.store.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
// import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class RecipeService {
    private final YamlStore store;
    private final YamlConfiguration cfg;
    private final Map<Player, String> openRecipe = new HashMap<>();
    private final Map<Player, Map<String, Double>> attrPercent = new HashMap<>();
    private final Map<Player, String> selectedStyle = new HashMap<>();
    public RecipeService(YamlStore store) { this.store = store; cfg = store.load("recipes.yml"); }
    public void createOrOpen(Player p, String name) {
        cfg.set("recipes."+name+".name", name);
        store.save("recipes.yml", cfg);
        openRecipe.put(p, name);
        openEditGui(p, name);
    }
    public String getOpenRecipe(Player p) {
        return openRecipe.get(p);
    }
    public void openEditGui(Player p, String name) {
        ForgePlugin.get().sessions().backupInventory(p);
        p.getInventory().clear();
        
        java.util.Map<String, String> params = new HashMap<>();
        params.put("{name}", name);
        GuiConfigService.GuiLayout layout = ForgePlugin.get().guiConfig().createGui("recipe_edit", params);
        Inventory inv = layout.inventory;
        
        // Row 2: Weapon Part Templates (Slots T)
        java.util.List<Integer> tSlots = layout.getSlots('T');
        java.util.List<String> tpls = cfg.getStringList("recipes."+name+".templates");
        
        for (int i = 0; i < Math.min(tpls.size(), tSlots.size()); i++) {
             String tName = tpls.get(i);
             Material icon = Material.PAPER;
             String iconStr = ForgePlugin.get().templates().getConfig().getString("templates."+tName+".icon");
             try { if(iconStr!=null) icon = Material.valueOf(iconStr); } catch(Exception ignored){}
             inv.setItem(tSlots.get(i), item(icon, "§e"+tName)); 
        }

        // Row 4-5: Weapon Styles (Slots M)
        java.util.List<Integer> mSlots = layout.getSlots('M');
        java.util.List<String> styles = cfg.getStringList("recipes."+name+".styles");
        int page = ForgePlugin.get().sessions().getPage(p, "recipe_styles");
        int start = page * mSlots.size();
        
        for (int i = 0; i < Math.min(styles.size() - start, mSlots.size()); i++) {
            inv.setItem(mSlots.get(i), item(Material.PAPER, styles.get(start + i)));
        }
        
        ForgePlugin.get().guiConfig().applyPlayerOverlay(p, layout);
        p.openInventory(inv);
    }
    
    public YamlConfiguration getConfig() { return cfg; }

    @SuppressWarnings("deprecation")
    public void saveStyle(Player p, ItemStack item) {
        String r = openRecipe.get(p);
        if (r == null) return;
        // Save style item? 
        // "Weapon Material Management: Click 'Add Material' ... put material in hotbar ... save".
        // This implies saving the item itself as a "Style".
        // Let's serialize it? Or just name?
        // "Clear display of all available weapon styles".
        // I will assume we save the ItemStack in a separate "styles.yml" or under recipe.
        // For simplicity, let's just save the name and use the item type as icon?
        // Or better: Serialize the itemstack to Base64 or just store properties.
        // Since I don't have ItemSerializer handy, I'll store Material and Name.
        
        String styleName = item.hasItemMeta() ? com.forge.util.Texts.getDisplayName(item.getItemMeta()) : item.getType().name();
        // Store style definition: Icon, ModelData?
        cfg.set("recipes."+r+".style_items."+styleName+".type", item.getType().name());
        if (item.hasItemMeta()) {
            if (item.getItemMeta().hasCustomModelData()) {
                cfg.set("recipes."+r+".style_items."+styleName+".cmd", item.getItemMeta().getCustomModelData());
            }
        }
        
        java.util.List<String> styles = cfg.getStringList("recipes."+r+".styles");
        if (!styles.contains(styleName)) {
            styles.add(styleName);
            cfg.set("recipes."+r+".styles", styles);
        }
        store.save("recipes.yml", cfg);
    }
    public void setAttrPercent(Player p, String attr, double percent) {
        attrPercent.computeIfAbsent(p, k -> new HashMap<>()).put(attr, percent);
        String r = openRecipe.get(p);
        if (r != null) {
            cfg.set("recipes."+r+".attrPercent."+attr, percent);
            store.save("recipes.yml", cfg);
        }
    }
    public void openComposeGui(Player p) {
        String r = openRecipe.get(p);
        Inventory inv = Bukkit.createInventory(p, 54, ForgePlugin.get().getConfig().getString("gui.titles.compose_select", "武器材质选择"));
        java.util.List<String> styles = cfg.getStringList("recipes."+r+".styles");
        java.util.Set<String> learned = ForgePlugin.get().playerDao().getStyles(p.getUniqueId());
        int page = ForgePlugin.get().sessions().getPage(p, "styles");
        int start = page * 45;
        int i = 0;
        for (int idx = start; idx < Math.min(styles.size(), start + 45); idx++) {
            String s = styles.get(idx);
            if (!learned.contains(s)) continue;
            ItemStack it = item(Material.PAPER, s);
            inv.setItem(i++, it);
        }
        inv.setItem(49, item(com.forge.util.MaterialCompat.anvil(), "§a确认材质"));
        inv.setItem(52, item(com.forge.util.MaterialCompat.rocket(), "上一页"));
        inv.setItem(53, item(com.forge.util.MaterialCompat.rocket(), "下一页"));
        p.openInventory(inv);
    }
    public void saveHotbar(Player p) {
        String r = openRecipe.get(p);
        if (r == null) return;
        java.util.List<String> styles = new java.util.ArrayList<>(cfg.getStringList("recipes."+r+".styles"));
        for (int i = 0; i < 9; i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (it == null || it.getType() == Material.AIR) continue;
            String name = it.hasItemMeta() ? com.forge.util.Texts.getDisplayName(it.getItemMeta()) : it.getType().name();
            if (!styles.contains(name)) styles.add(name);
        }
        cfg.set("recipes."+r+".styles", styles);
        store.save("recipes.yml", cfg);
    }
    public void selectStyle(Player p, String style) { selectedStyle.put(p, style); }

    public void saveConfig() {
        store.save("recipes.yml", cfg);
    }
    public void compose(Player p) {
        String r = openRecipe.get(p);
        if (r == null) return;
        
        // 1. Check if player has all required parts?
        // User said: "Must collect all weapon parts required by recipe".
        // How do we know what parts are required?
        // "recipes.<name>.templates" list defines required templates.
        java.util.List<String> requiredTemplates = cfg.getStringList("recipes."+r+".templates");
        
        // Player should have these parts in inventory? Or stored in a specific way?
        // "Player inputs /dt hc ... Grid shows learned weapon materials ... select one ... system sums up attributes of all weapon parts".
        // This implies the parts are already forged and in player's inventory?
        // Or maybe stored in "collected parts"?
        // "Collect all weapon parts" implies they are items.
        
        // Let's search player's inventory for items matching the templates?
        // Item matches template if it has "Template: <name>" in NBT or Lore?
        // Or maybe simple name match?
        // Let's assume LoreUtil puts "Part: <TemplateName>" or similar.
        // Actually ForgingService produces an item based on "Offhand Item" (Template Part).
        // Let's assume the forged item name or lore identifies the template.
        
        // For simplicity: Check player inventory for items named after the templates.
        
        // Let's implement logic:
        // Iterate required templates.
        // Find matching item in inventory.
        // If missing, fail.
        
        // But first, let's fix ForgingService to tag the item.
        
        // Back to Compose logic:
        java.util.Map<String, Double> total = new java.util.HashMap<>();
        
        // Sum attributes
        // We need "Attribute Percent" config for this recipe.
        // "Left click template in Recipe Edit to modify attribute percent".
        // We need to store this percent.
        // cfg.getDouble("recipes."+r+".percents."+templateName, 100.0);
        
        for (String tpl : requiredTemplates) {
            // Find item
            ItemStack part = null;
            for (ItemStack it : p.getInventory().getContents()) {
                 if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasLore()) continue;
                 java.util.List<String> lore = it.getItemMeta().getLore();
                 if (lore.contains("§8Template: " + tpl)) {
                     part = it;
                     break;
                 }
            }
            
            if (part == null) {
                p.sendMessage("§c缺少部件: " + tpl);
                return;
            }
            
            double percent = cfg.getDouble("recipes."+r+".percents."+tpl, 100.0) / 100.0;
            java.util.Map<String, Double> attrs = com.forge.util.LoreUtil.parseAttributes(com.forge.util.Texts.getLore(part.getItemMeta()));
            for (java.util.Map.Entry<String, Double> e : attrs.entrySet()) {
                total.put(e.getKey(), total.getOrDefault(e.getKey(), 0.0) + e.getValue() * percent);
            }
        }
        
        // Create Result Item based on Selected Style
        String style = selectedStyle.get(p);
        if (style == null) {
             p.sendMessage("§c请先选择一种样式！");
             return;
        }
        
        Material type = Material.DIAMOND_SWORD;
        int cmd = 0;
        String typeStr = cfg.getString("recipes."+r+".style_items."+style+".type");
        if (typeStr != null) type = Material.valueOf(typeStr);
        cmd = cfg.getInt("recipes."+r+".style_items."+style+".cmd", 0);
        
        ItemStack result = new ItemStack(type);
        ItemMeta meta = result.getItemMeta();
        com.forge.util.Texts.setDisplayName(meta, style);
        // "Recipe Name" is `r`.
        // Let's use Recipe Name + Style Name?
        // User said: "/dt wq create <Weapon Name>". So `r` is the Weapon Name.
        com.forge.util.Texts.setDisplayName(meta, "§6" + r); 
        
        if (cmd != 0) meta.setCustomModelData(cmd);
        
        com.forge.util.Texts.setLore(meta, com.forge.util.LoreUtil.formatAttributes(total));
        result.setItemMeta(meta);
        ForgePlugin.get().adapters().applyAttributes(result, total);
        
        // Consume parts? "Collect all parts". Usually implies consumption.
        // Let's consume them.
        for (String tpl : requiredTemplates) {
            for (ItemStack it : p.getInventory().getContents()) {
                 if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasLore()) continue;
                 if (it.getItemMeta().getLore().contains("§8Template: " + tpl)) {
                     it.setAmount(it.getAmount() - 1);
                     break;
                 }
            }
        }
        
        p.getInventory().addItem(result);
        p.sendMessage(ForgePlugin.get().msg().msg("saved"));
        p.closeInventory();
    }
    private ItemStack item(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        com.forge.util.Texts.setDisplayName(meta, name);
        it.setItemMeta(meta);
        return it;
    }
}
