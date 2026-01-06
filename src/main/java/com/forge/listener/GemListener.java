package com.forge.listener;

import com.forge.ForgePlugin;
import com.forge.service.GemService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GemListener implements Listener {
    private final ForgePlugin plugin;
    
    public GemListener(ForgePlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    @SuppressWarnings("deprecation")
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        GemService gems = plugin.gems();
        
        if (gems.isGemGui(title)) {
            e.setCancelled(true); // Default cancel
            
            int slot = e.getRawSlot();
            Inventory inv = e.getView().getTopInventory();
            
            // Allow clicking in Player Inventory
            if (e.getClickedInventory() == e.getWhoClicked().getInventory()) {
                e.setCancelled(false);
                return;
            }
            
            // Allow interactions with Material Slots (B) and Equip Slot (X)
            // B slots: 1-7
            if (slot >= 1 && slot <= 7) {
                e.setCancelled(false);
                return;
            }
            
            // Equip Slot (X): 31
            if (slot == gems.getEquipSlot()) {
                e.setCancelled(false);
                // Schedule update
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack item = inv.getItem(gems.getEquipSlot());
                    gems.updateGemSlots(inv, item);
                });
                return;
            }
            
            // Gem Slots (S)
            // If clicked with a gem, handle logic (TODO)
            // For now, just prevent taking the icon.
        }
    }
    
    @EventHandler
    @SuppressWarnings("deprecation")
    public void onDrag(InventoryDragEvent e) {
        if (plugin.gems().isGemGui(e.getView().getTitle())) {
            // Only allow dragging into allowed slots?
            // Simplest is to cancel drags involving top inventory to prevent messing up icons
            for (int slot : e.getRawSlots()) {
                if (slot < 54) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
}
