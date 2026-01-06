package com.forge.service;

import com.forge.ForgePlugin;
// import com.forge.util.MaterialCompat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class GemService {
    @SuppressWarnings("unused")
    private final ForgePlugin plugin;
    private final String GUI_TITLE = "§8宝石镶嵌";
    
    // Layout 9x6 = 54 slots
    // HBBBBBBBH (0-8)
    // HHHHHHHHH (9-17)
    // HSHSHSHSH (18-26)
    // HHHHXHHHH (27-35)
    // HSHSHSHSH (36-44)
    // HHHHHHHHH (45-53)
    
    private final int SLOT_EQUIP = 31;
    private final int[] SLOTS_GEM = {19, 21, 23, 25, 37, 39, 41, 43};
    private final int[] SLOTS_MAT = {1, 2, 3, 4, 5, 6, 7};
    
    public GemService(ForgePlugin plugin) {
        this.plugin = plugin;
    }
    
    @SuppressWarnings("deprecation")
    public void openGui(Player p) {
        Inventory inv = Bukkit.createInventory(p, 54, GUI_TITLE);
        setupBackground(inv);
        p.openInventory(inv);
    }
    
    private void setupBackground(Inventory inv) {
        // Fill H (Background)
        ItemStack bg = item(com.forge.util.MaterialCompat.lightGrayPane(), "§e中间放入装备后可强化", 
            "§a请将物品放在中间", "§a请将保护石放在信标处");
        
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }
        
        // Clear B (Material Slots) -> Actually prompt says "No Material needed, used to put things"
        // So we leave them AIR? Or specific slot type?
        // "Material slot does not need material" usually means it's empty (AIR) for player to place items.
        for (int slot : SLOTS_MAT) {
            inv.setItem(slot, new ItemStack(Material.AIR));
        }
        
        // Setup X (Equipment Slot) - Default Icon
        inv.setItem(SLOT_EQUIP, item(Material.NETHER_STAR, "§c此处放入强化物品"));
        
        // Setup S (Gem Slots) - Default Icon
        updateGemSlots(inv, null);
    }
    
    @SuppressWarnings("deprecation")
    public void updateGemSlots(Inventory inv, ItemStack equip) {
        // Reset S slots first
        ItemStack defGem = item(Material.BARRIER, "§e拿着宝石点击该槽位可强化", 
            "§a请将物品放在中间", "§a请将强化材料放在上方空位");
            
        for (int slot : SLOTS_GEM) {
            inv.setItem(slot, defGem);
        }
        
        if (equip == null || equip.getType() == Material.AIR || !equip.hasItemMeta() || !equip.getItemMeta().hasLore()) {
            return;
        }
        
        List<String> lore = equip.getItemMeta().getLore();
        int slotIdx = 0;
        
        for (String line : lore) {
            if (slotIdx >= SLOTS_GEM.length) break;
            
            if (line.contains("§7[§c攻击宝石槽位§7]")) {
                inv.setItem(SLOTS_GEM[slotIdx], item(Material.RED_SHULKER_BOX, "§f可镶嵌§c红宝石", "§7手持宝石左键点击"));
                slotIdx++;
            } else if (line.contains("§7[§a防御宝石槽位§7]")) {
                inv.setItem(SLOTS_GEM[slotIdx], item(Material.GREEN_SHULKER_BOX, "§f可镶嵌§b绿宝石", "§7手持宝石左键点击"));
                slotIdx++;
            } else if (line.contains("§7[§d特殊宝石槽位§7]")) {
                inv.setItem(SLOTS_GEM[slotIdx], item(Material.MAGENTA_SHULKER_BOX, "§f可镶嵌§d特殊宝石", "§7手持宝石左键点击"));
                slotIdx++;
            }
            // What if the slot is already filled? 
            // The prompt only gives config for "Socket Slots".
            // Presumably filled slots have different lore or we don't handle them here yet.
        }
    }
    
    // Helper to create item
    @SuppressWarnings("deprecation")
    private ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        it.setItemMeta(meta);
        return it;
    }
    
    public boolean isGemGui(String title) {
        return title.equals(GUI_TITLE);
    }
    
    public int getEquipSlot() { return SLOT_EQUIP; }
}
