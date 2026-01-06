package com.forge.service;

import com.forge.ForgePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuiConfigService {
    private final ForgePlugin plugin;

    public GuiConfigService(ForgePlugin plugin) {
        this.plugin = plugin;
    }

    public static class GuiLayout {
        public final Inventory inventory;
        public final Map<Character, List<Integer>> slots; // Character -> Slot Indices
        public final Map<Integer, ItemStack> playerOverlay; // Slot -> Item (for 54+)

        public GuiLayout(Inventory inventory, Map<Character, List<Integer>> slots, Map<Integer, ItemStack> playerOverlay) {
            this.inventory = inventory;
            this.slots = slots;
            this.playerOverlay = playerOverlay;
        }
        
        public GuiLayout(Inventory inventory, Map<Character, List<Integer>> slots) {
            this(inventory, slots, new HashMap<>());
        }
        
        public List<Integer> getSlots(char c) {
            return slots.getOrDefault(c, Collections.emptyList());
        }
        
        public int getFirstSlot(char c) {
            List<Integer> list = slots.get(c);
            return (list != null && !list.isEmpty()) ? list.get(0) : -1;
        }
    }

    private final Map<String, GuiLayout> layoutCache = new HashMap<>();

    public GuiLayout getLayout(String key) {
        if (layoutCache.containsKey(key)) return layoutCache.get(key);
        
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("gui.layouts." + key);
        if (section == null) {
            section = plugin.getConfig().getConfigurationSection("gui." + key);
            if (section == null) {
                section = plugin.getConfig().getConfigurationSection(key);
            }
        }
        if (section == null) {
            if ("result".equals(key)) {
                Map<Character, List<Integer>> slots = new HashMap<>();
                slots.put('R', java.util.Arrays.asList(13));
                List<Integer> a = new java.util.ArrayList<>();
                for (int i = 18; i <= 35; i++) a.add(i);
                slots.put('A', a);
                slots.put('P', java.util.Arrays.asList(45));
                slots.put('N', java.util.Arrays.asList(53));
                GuiLayout layout = new GuiLayout(null, slots);
                layoutCache.put(key, layout);
                return layout;
            }
            return null;
        }
        
        // int size = section.getInt("size", 54);
        List<String> layoutLines = section.getStringList("layout");
        Map<Character, List<Integer>> slotMap = new HashMap<>();
        
        int slot = 0;
        for (String line : layoutLines) {
            for (char c : line.toCharArray()) {
                if (slot >= 90) break;
                slotMap.computeIfAbsent(c, k -> new ArrayList<>()).add(slot);
                slot++;
            }
        }
        
        GuiLayout layout = new GuiLayout(null, slotMap); // Inventory is null for cached layout structure
        layoutCache.put(key, layout);
        return layout;
    }

    @SuppressWarnings("deprecation")
    public GuiLayout createGui(String key, Map<String, String> titlePlaceholders) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("gui.layouts." + key);
        if (section == null) {
            section = plugin.getConfig().getConfigurationSection("gui." + key);
            if (section == null) {
                section = plugin.getConfig().getConfigurationSection(key);
            }
        }
        if (section == null) {
            plugin.getLogger().warning("GUI Layout not found: " + key);
            if ("result".equals(key)) {
                Inventory inv = Bukkit.createInventory(null, 54, plugin.getConfig().getString("gui.titles.result", "锻造成品"));
                Map<Character, List<Integer>> slots = new HashMap<>();
                // Result slot at row 1, center (slot 13)
                slots.put('R', java.util.Arrays.asList(13));
                // Attributes rows (rows 2-3): slots 18-26 and 27-35
                List<Integer> a = new java.util.ArrayList<>();
                for (int i = 18; i <= 35; i++) a.add(i);
                slots.put('A', a);
                // Navigation buttons: P=45, N=53
                slots.put('P', java.util.Arrays.asList(45));
                slots.put('N', java.util.Arrays.asList(53));
                return new GuiLayout(inv, slots, new HashMap<>());
            }
            return new GuiLayout(Bukkit.createInventory(null, 54, "Error: Missing Config"), new HashMap<>());
        }

        String title = section.getString("title", "GUI");
        if (titlePlaceholders != null) {
            for (Map.Entry<String, String> entry : titlePlaceholders.entrySet()) {
                title = title.replace(entry.getKey(), entry.getValue());
            }
        }
        
        int size = section.getInt("size", 54);
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        List<String> layoutLines = section.getStringList("layout");
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        
        Map<Character, List<Integer>> slotMap = new HashMap<>();
        Map<Integer, ItemStack> overlay = new HashMap<>();
        
        // Standard Slot ID iteration: 0-size + Player Inventory
        // Layout definition maps visual grid to slot IDs
        int slot = 0;
        for (String line : layoutLines) {
            for (char c : line.toCharArray()) {
                // If we exceed 89 (last slot of hotbar), stop.
                // 0-53: Top, 54-89: Bottom
                if (slot >= 90) break;
                
                // Record slot mapping
                slotMap.computeIfAbsent(c, k -> new ArrayList<>()).add(slot);
                
                // If it's an 'O' (Original), we don't overwrite it with GUI items.
                // If it's 'U' (Empty), we overwrite with AIR.
                if (c == 'U') {
                    ItemStack air = new ItemStack(Material.AIR);
                    if (slot < size) {
                        inv.setItem(slot, air);
                    } else {
                        overlay.put(slot, air);
                    }
                }
                // If it's not 'O' and not 'U', we check items section.
                else if (c != 'O') {
                    ItemStack item = null;
                    if (itemsSection != null && itemsSection.contains(String.valueOf(c))) {
                         item = createItem(itemsSection.getConfigurationSection(String.valueOf(c)));
                    }
                    
                    if (item != null) {
                        if (slot < size) {
                            inv.setItem(slot, item);
                        } else {
                            // Slot is in player inventory.
                            overlay.put(slot, item);
                        }
                    }
                } else if (c == 'O') {
                    // For 'O', we explicitly do NOT put anything into the overlay map.
                    // This ensures applyPlayerOverlay() skips these slots, leaving original items intact.
                }
                
                slot++;
            }
        }
        
        return new GuiLayout(inv, slotMap, overlay);
    }
    
    public void applyPlayerOverlay(Player p, GuiLayout layout) {
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
    }
    
    public char getFunctionChar(String key, int slot) {
        GuiLayout layout = getLayout(key);
        if (layout == null) return ' ';
        for (Map.Entry<Character, List<Integer>> entry : layout.slots.entrySet()) {
            if (entry.getValue().contains(slot)) return entry.getKey();
        }
        return ' ';
    }
    
    @SuppressWarnings("deprecation")
    private ItemStack createItem(ConfigurationSection section) {
        if (section == null) return null;
        String matName = section.getString("material", "AIR");
        Material mat = Material.getMaterial(matName);
        if (mat == null || mat == Material.AIR) return null;
        
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (section.contains("name")) {
                meta.setDisplayName(section.getString("name").replace("&", "§"));
            }
            if (section.contains("lore")) {
                List<String> lore = section.getStringList("lore");
                List<String> colored = new ArrayList<>();
                for (String l : lore) colored.add(l.replace("&", "§"));
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
