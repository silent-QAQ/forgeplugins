package com.forge.game;

import com.forge.model.ForgeSession;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CombingGame implements Minigame {
    private int goal;
    private int current;
    private final int ANVIL_SLOT = 35; // Modified slot

    @Override
    public void start(Player p, ForgeSession s) {
        // Clear area 9-35 except 35
        for (int i = 9; i < 36; i++) {
            if (i != ANVIL_SLOT) p.getInventory().setItem(i, blank("§7空白"));
        }
        
        // Calculate complexity
        // We need plugin instance to get materials, pass it or use static access?
        // For simplicity, let's assume complexity is passed or retrieved from session if stored.
        // But session doesn't store material props.
        // We will assume complexity is calculated outside or injected.
        // Refactoring to keep it simple: We will let ForgingService handle logic for now, 
        // OR we make Minigame abstract class with plugin reference.
        // Let's stick to Interface and pass complexity via constructor or setup.
    }
    
    // Helper methods
    @SuppressWarnings("deprecation")
    private ItemStack blank(String name) {
        ItemStack it = new ItemStack(com.forge.util.MaterialCompat.lightGrayPane());
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        it.setItemMeta(meta);
        return it;
    }

    @Override
    public void onClick(Player p, ForgeSession s, int slot) {
        // Handled in service for now to access plugin resources
    }

    @Override
    public boolean isCompleted() {
        return current >= goal;
    }

    @Override
    public void clear(Player p) {
        for (int i = 9; i < 36; i++) {
            if (i != ANVIL_SLOT) p.getInventory().setItem(i, blank("§7空白"));
        }
    }
}
