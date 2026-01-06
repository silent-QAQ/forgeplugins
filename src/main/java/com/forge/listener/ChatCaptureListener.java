package com.forge.listener;

import com.forge.ForgePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatCaptureListener implements Listener {
    private final ForgePlugin plugin;
    public ChatCaptureListener(ForgePlugin plugin) { this.plugin = plugin; }
    @EventHandler
    @SuppressWarnings("deprecation")
    public void onChat(AsyncPlayerChatEvent e) {
        String key = plugin.sessions().awaitingKey(e.getPlayer());
        if (key == null) return;
        e.setCancelled(true);
        if ("__STAR_TAG__".equals(key)) {
            String tag = e.getMessage();
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.materials().setStarTag(e.getPlayer(), tag);
                plugin.sessions().clearAwait(e.getPlayer());
                e.getPlayer().sendMessage(plugin.msg().msg("saved"));
                plugin.materials().openEditGui(e.getPlayer());
            });
            return;
        }
        if ("稳定度".equals(key)) {
            // Check for x/x format
            String input = e.getMessage();
            if (input.contains("/")) {
                // Validate both parts
                try {
                    String[] parts = input.split("/");
                    if (parts.length == 2) {
                        Double.parseDouble(parts[0].trim());
                        Double.parseDouble(parts[1].trim());
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.materials().setStability(e.getPlayer(), input);
                            plugin.sessions().clearAwait(e.getPlayer());
                            e.getPlayer().sendMessage(plugin.msg().msg("saved"));
                            plugin.materials().openEditGui(e.getPlayer());
                        });
                        return;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        try {
            double v = Double.parseDouble(e.getMessage());
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if ("稳定度".equals(key)) {
                    // Single value -> x/x
                    plugin.materials().setStability(e.getPlayer(), String.format("%.0f", v) + "/" + String.format("%.0f", v));
                } else {
                    plugin.materials().setValue(e.getPlayer(), key, v);
                }
                plugin.sessions().clearAwait(e.getPlayer());
                e.getPlayer().sendMessage(plugin.msg().msg("saved"));
                plugin.materials().openEditGui(e.getPlayer());
            });
        } catch (NumberFormatException ex) {
            e.getPlayer().sendMessage("§c请输入有效的数字!");
        }
    }
}
