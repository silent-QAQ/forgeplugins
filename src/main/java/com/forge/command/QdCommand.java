package com.forge.command;

import com.forge.ForgePlugin;
// import com.forge.service.*;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class QdCommand implements CommandExecutor {
    private final ForgePlugin plugin;
    public QdCommand(ForgePlugin plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.msg().msg("only_player")); return true; }
        Player p = (Player) sender;
        
        // Handle Template Material Setting (Slot 45/Hotbar 2 override)
        String awaitingMat = plugin.sessions().awaitingMaterial(p);
        if (awaitingMat != null) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand != null && hand.getType() != Material.AIR) {
                plugin.templates().setOutputMaterial(awaitingMat, hand.getType().name());
                sender.sendMessage("§a成功设置成品材质为: " + hand.getType().name());
            } else {
                // Allow resetting to default? Or error?
                // Let's assume error.
                sender.sendMessage("§c手中没有物品！");
                return true;
            }
            plugin.sessions().clearAwaitMaterial(p);
            plugin.templates().openEditGui(p, awaitingMat);
            return true;
        }

        // Handle Template Part saving
        String editingTpl = plugin.sessions().currentTemplate(p);
        if (editingTpl != null) {
            ItemStack off = p.getInventory().getItemInOffHand();
            plugin.sessions().saveOffhand(p, off);
            sender.sendMessage(plugin.msg().msg("qd_saved"));
            return true;
        }

        // Handle Recipe Styles saving
        String openRecipe = plugin.recipes().getOpenRecipe(p);
        if (openRecipe != null) {
            // Check hotbar for new styles
            // "Click 'Add Material' pearl -> return to backpack -> put material in hotbar -> /qd save".
            // So we iterate hotbar.
            boolean saved = false;
            for (int i = 0; i < 9; i++) {
                ItemStack it = p.getInventory().getItem(i);
                if (it != null && it.getType() != Material.AIR) {
                    plugin.recipes().saveStyle(p, it);
                    saved = true;
                }
            }
            if (saved) {
                sender.sendMessage(plugin.msg().msg("qd_saved"));
                // Re-open GUI?
                plugin.recipes().openEditGui(p, openRecipe);
            } else {
                sender.sendMessage("§c请将武器材质放入快捷栏！");
            }
            return true;
        }

        plugin.recipes().saveHotbar(p);
        sender.sendMessage(plugin.msg().msg("qd_saved"));
        return true;
    }
}
