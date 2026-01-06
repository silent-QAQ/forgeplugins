package com.forge.command;

import com.forge.ForgePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GemsCommand implements CommandExecutor {
    private final ForgePlugin plugin;
    public GemsCommand(ForgePlugin plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        plugin.gems().openGui((Player) sender);
        return true;
    }
}
