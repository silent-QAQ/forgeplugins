package com.forge.listener;

import com.forge.ForgePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {
    private final ForgePlugin plugin;

    public GuiListener(ForgePlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (plugin.sessions().hasBackup(e.getPlayer())) {
            plugin.sessions().restoreInventory(e.getPlayer());
        }
        com.forge.model.ForgeSession s = plugin.sessions().forge(e.getPlayer());
        if (s != null && s.taskId != -1) {
             org.bukkit.Bukkit.getScheduler().cancelTask(s.taskId);
        }
        plugin.sessions().endForge(e.getPlayer());
    }
    
    @EventHandler
    @SuppressWarnings("deprecation")
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            Player p = (Player) e.getPlayer();
            String tForge = plugin.getConfig().getString("gui.titles.forging", "锻造");
            String tResult = plugin.getConfig().getString("gui.titles.result", "锻造成品");
            
            // Check if we need to restore inventory (for 'U' cleared slots)
            // But wait, 'U' slots are overwritten in player inventory.
            // If we close the GUI, we must restore the original items if they were backed up.
            // The existing restoreInventory(p) logic handles this because we did a full backup.
            // So closing ANY custom GUI that messed with player inventory should trigger restore.
            // Currently restoreInventory is called at the end of this method.
            
            if (e.getView().getTitle().contains(tResult)) {
                // If closing result GUI, ensure we give the item if it's still there
                com.forge.model.ForgeSession s = plugin.sessions().forge(p);
                if (s != null && s.resultItem != null) {
                    java.util.List<Integer> rSlots = null;
                    com.forge.service.GuiConfigService.GuiLayout lay = plugin.guiConfig().getLayout("result");
                    if (lay != null) rSlots = lay.getSlots('R');
                    boolean given = false;
                    if (rSlots != null && !rSlots.isEmpty()) {
                        for (int slot : rSlots) {
                            ItemStack item = e.getInventory().getItem(slot);
                            if (item != null && item.getType() != Material.AIR) {
                                p.getInventory().addItem(item);
                                given = true;
                            }
                        }
                    }
                    if (!given) {
                        p.getInventory().addItem(s.resultItem);
                    }
                    p.sendMessage("§a您收回了锻造成品！");
                    plugin.sessions().endForge(p);
                }
                // No return here, let it fall through to restoreInventory
            }

            if (e.getView().getTitle().contains(tForge)) {
                 // Ensure task is cancelled if running
                 com.forge.model.ForgeSession s = plugin.sessions().forge(p);
                 if (s != null && s.taskId != -1) {
                      org.bukkit.Bukkit.getScheduler().cancelTask(s.taskId);
                 }
                 plugin.sessions().endForge(p);
            }
            
            if (plugin.sessions().hasBackup(p)) {
                plugin.sessions().restoreInventory(p);
            }
        }
    }
    @EventHandler
    @SuppressWarnings("deprecation")
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        String tSel = plugin.getConfig().getString("gui.titles.template_select", "模板选择");
        String tEdit = plugin.getConfig().getString("gui.titles.template_edit", "模板编辑");
        String tMat = plugin.getConfig().getString("gui.titles.material_edit", "材料属性编辑");
        String tForge = plugin.getConfig().getString("gui.titles.forging", "锻造");
        String tResult = plugin.getConfig().getString("gui.titles.result", "锻造成品");
        String tStyle = plugin.getConfig().getString("gui.titles.compose_select", "武器材质选择");
        
        if (title.contains(tResult)) {
            e.setCancelled(true);
            
            // Allow clicking in Player Inventory (Bottom)
            if (e.getClickedInventory() == p.getInventory()) {
                e.setCancelled(false);
                return;
            }
            
            int slot = e.getRawSlot();
            char func = plugin.guiConfig().getFunctionChar("result", slot);
            
            if (func == 'R') {
                e.setCancelled(false);
                return;
            }
            com.forge.model.ForgeSession sR = plugin.sessions().forge(p);
            if (sR != null && sR.resultItem != null && e.getClickedInventory() != p.getInventory()) {
                ItemStack ci = e.getCurrentItem();
                if (ci != null && ci.getType() != Material.AIR) {
                    String dn1 = ci.hasItemMeta() ? com.forge.util.Texts.getDisplayName(ci.getItemMeta()) : "";
                    String dn2 = sR.resultItem.hasItemMeta() ? com.forge.util.Texts.getDisplayName(sR.resultItem.getItemMeta()) : "";
                    if (ci.getType() == sR.resultItem.getType() && dn1.equals(dn2)) {
                        e.setCancelled(false);
                        return;
                    }
                }
            }
            
            if (func == 'P') {
                com.forge.model.ForgeSession s = plugin.sessions().forge(p);
                if (s != null) {
                     s.resultPage--;
                     if (s.resultPage < 0) s.resultPage = 0;
                     plugin.forging().openResultGui(p);
                }
            } else if (func == 'N') {
                com.forge.model.ForgeSession s = plugin.sessions().forge(p);
                if (s != null) {
                     s.resultPage++;
                     // clamp will be applied in openResultGui
                     plugin.forging().openResultGui(p);
                }
            }
            return;
        }

        if (title.contains(tSel)) {
            e.setCancelled(true);
            ItemStack it = e.getCurrentItem();
            if (it == null) return;
            if (it.getType() == com.forge.util.MaterialCompat.rocket() && it.getItemMeta() != null) {
            String n = com.forge.util.Texts.getDisplayName(it.getItemMeta());
                int page = plugin.sessions().getPage(p, "tpl");
                if (n.contains("上一页")) plugin.sessions().setPage(p, "tpl", Math.max(0, page - 1));
                else if (n.contains("下一页")) plugin.sessions().setPage(p, "tpl", page + 1);
                plugin.templates().openSelectGui(p);
                return;
            }
            String tpl = it.getItemMeta() != null ? com.forge.util.Texts.getDisplayName(it.getItemMeta()).replace("§d", "") : "";
            if (tpl.isEmpty()) return;
            plugin.forging().openForging(p, tpl);
            return;
        }
        
        // Recipe Edit GUI
        if (title.contains("武器配方编辑")) { // Using hardcoded title part to match config title "武器配方编辑 {name}"
            e.setCancelled(true);
            
            // Allow interactions in Player Inventory (Bottom)
            if (e.getClickedInventory() == p.getInventory()) {
                e.setCancelled(false);
                return;
            }
            
            int slot = e.getRawSlot();
            char func = plugin.guiConfig().getFunctionChar("recipe_edit", slot);
            ItemStack cursor = p.getItemOnCursor();
            ItemStack current = e.getCurrentItem();
            
            // Buttons logic
            if (func == 'S') { // Save
                p.closeInventory();
                p.sendMessage(plugin.msg().msg("saved"));
                return;
            }
            if (func == 'P') { // Prev Page
                int page = plugin.sessions().getPage(p, "recipe_styles");
                plugin.sessions().setPage(p, "recipe_styles", Math.max(0, page - 1));
                String name = title.substring(title.lastIndexOf(' ') + 1);
                plugin.recipes().openEditGui(p, name);
                return;
            }
            if (func == 'N') { // Next Page
                int page = plugin.sessions().getPage(p, "recipe_styles");
                plugin.sessions().setPage(p, "recipe_styles", page + 1);
                String name = title.substring(title.lastIndexOf(' ') + 1);
                plugin.recipes().openEditGui(p, name);
                return;
            }
            
            // Handle 'T' (Templates) and 'M' (Materials) slots
            // Logic:
            // 1. If Cursor has Item -> Place Copy into Slot (Cursor stays same)
            // 2. If Cursor Empty & Slot has Item -> Copy Slot Item to Cursor
            
            if (func == 'T' || func == 'M') {
                p.sendMessage("§c武器配方功能已移除");
                return;
            }
            
            return;
        }
        
        if (title.contains(tEdit)) {
            e.setCancelled(true);
            ItemStack it = e.getCurrentItem();
            
            int slot = e.getRawSlot();
            char func = plugin.guiConfig().getFunctionChar("template_edit", slot);
            
            // Handle Save Button (Slot 89 -> Player Hotbar Slot 8)
            // But we are using Player Inventory click check.
            if (e.getClickedInventory() == p.getInventory() && e.getSlot() == 8) {
                p.closeInventory();
                p.sendMessage(plugin.msg().msg("saved"));
                return;
            }
            
            // Handle Output Material Change (Slot 1 - Hotbar 2)
            // Raw Slot check is tricky, relying on Clicked Inventory is safer.
            if (e.getClickedInventory() == p.getInventory() && e.getSlot() == 1) {
                 String name = title.substring(title.lastIndexOf(' ') + 1);
                 
                 // User request: Close inventory, wait for /qd
                 p.closeInventory();
                 
                 // Restore inventory from backup so they can pick items
                 if (plugin.sessions().hasBackup(p)) {
                     plugin.sessions().restoreInventory(p);
                 }
                 
                 // Mark session state: "Awaiting Material Selection for Template: name"
                 // We can use SessionService to store this state.
                 // "awaitingKey" is used for attributes (Double).
                 // Let's reuse it or add a new one. "awaitingMaterial".
                 plugin.sessions().awaitMaterial(p, name);
                 
                 p.sendMessage("§e请将想要设置的材质拿在手中，然后输入 §a/qd §e完成设置！");
                 return;
            }
            
            if (func == 'X') { // Editable Area
                if (e.getRawSlot() >= 54) return;
                
                String name = title.substring(title.lastIndexOf(' ') + 1);
                boolean now = it != null && it.getType() == Material.ITEM_FRAME;
                plugin.templates().setRequired(name, slot, !now);
                ItemStack mark;
                if (!now) {
                    mark = new ItemStack(Material.ITEM_FRAME);
                    org.bukkit.inventory.meta.ItemMeta meta = mark.getItemMeta();
                    meta.setDisplayName("§a必要槽位");
                    mark.setItemMeta(meta);
                } else {
                    mark = new ItemStack(com.forge.util.MaterialCompat.lightGrayPane());
                    org.bukkit.inventory.meta.ItemMeta meta = mark.getItemMeta();
                    meta.setDisplayName("§7必要槽位未选");
                    mark.setItemMeta(meta);
                }
                e.getInventory().setItem(slot, mark);
                return;
            }
        }
        if (title.contains(tMat)) {
            e.setCancelled(true);
            ItemStack it = e.getCurrentItem();
            
            // Allow clicking in player inventory (bottom) if needed, but usually we block it here.
            // Config layout applies overlay, so we might need to block.
            
            int slot = e.getRawSlot();
            char func = plugin.guiConfig().getFunctionChar("material_edit", slot);
            
            if (func == 'S') { // Save
                p.closeInventory();
                p.sendMessage(plugin.msg().msg("saved"));
                return;
            }
            
            if (func == 'P') { // Prev Page (for attributes if we had pagination, currently attrs fit in 36 slots)
                // Implement pagination logic later if needed
                return;
            }
            if (func == 'N') { // Next Page
                return;
            }
            
            if (func == 'A' || slot == 0 || slot == 1 || slot == 2 || (it != null && it.getType() == Material.PAPER)) {
                // Edit Attribute
                String name = it == null || it.getItemMeta() == null ? "" : com.forge.util.Texts.getDisplayName(it.getItemMeta());
                if (name.contains("材料星缀")) { 
                    plugin.sessions().awaitKey(p, "__STAR_TAG__");
                    p.closeInventory();
                    p.sendMessage("§e请输入新的星缀:");
                    return; 
                }
                if (name.contains("熔点")) plugin.sessions().awaitKey(p, "熔点");
                else if (name.contains("延展性")) plugin.sessions().awaitKey(p, "延展性");
                else if (name.contains("稳定度")) plugin.sessions().awaitKey(p, "稳定度");
                else plugin.sessions().awaitKey(p, name.replace("§b", "").replace("§e", "")); // Strip colors
                
                if (plugin.sessions().awaitingKey(p) != null) {
                     p.closeInventory();
                     p.sendMessage("§e请输入新的数值:");
                }
                return;
            }
        }
        if (title.contains(tForge)) {
            e.setCancelled(true); // Cancel all clicks by default in Forging GUI to prevent messing up
            
            com.forge.model.ForgeSession session = plugin.sessions().forge(p);
            if (session == null) return;
            
            String layoutKey = (session.taskId != -1) ? "forging_game" : "forging_placement";
            
            ItemStack it = e.getCurrentItem();
            
            // Handle Player Inventory Clicks
            if (e.getClickedInventory() == p.getInventory()) {
                
                // Interactive Phase (Combing Game)
            if (session.taskId != -1) {
                 e.setCancelled(true); // Lock inventory
                 // Block shift/number/double clicks to avoid bypass
                 if (e.isShiftClick() || e.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY || e.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) {
                     return;
                 }
                 
                 // Check if clicking Confirm Cure (F) or Combing (C)
            // But we should check layout char logic ideally, or fallback to item meta/type checks
            // Let's use layout function char if possible, mapping player slot to raw config slot is hard in listener.
            // Easier to check Item Meta/Type as defined in Config Items.
                
                if (it != null && it.getType() == Material.ANVIL && it.getItemMeta() != null && getDisplayName(it.getItemMeta()).contains("确认固化")) {
                    plugin.forging().confirmCure(p);
                    return;
                }
                
                if (it != null && it.getType() == com.forge.util.MaterialCompat.purplePane() && it.getItemMeta() != null && getDisplayName(it.getItemMeta()).contains("未梳理")) {
                    plugin.forging().combMaterial(p, e.getSlot());
                    return;
                }
                 
                 // Clicking on Blank counts as error combing (do not allow pickup)
                 if (it != null && it.getType() == com.forge.util.MaterialCompat.lightGrayPane() && it.getItemMeta() != null && getDisplayName(it.getItemMeta()).contains("空白")) {
                     plugin.forging().combMaterial(p, e.getSlot());
                     return;
                 }
                 return;
            }

       // Normal Phase (Placement)
       // Allow clicking inside backpack part (9-35)
       if (e.getSlot() >= 9) {
           if (e.isShiftClick()) { e.setCancelled(true); return; }
           if (e.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY || e.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) { e.setCancelled(true); return; }
           e.setCancelled(false);
           return;
       }
       
       // Hotbar buttons (Start Forge - 'K')
       if (it != null && it.getType() == Material.ANVIL && it.getItemMeta() != null && getDisplayName(it.getItemMeta()).contains("开始锻造")) {
           if (p.getItemOnCursor() != null && p.getItemOnCursor().getType() != Material.AIR) {
               p.sendMessage("§c请先放下光标上的物品！");
               return;
           }
           // 在应用游戏覆盖之前，仅保存玩家背包区（9-35），避免将快捷栏覆盖写入备份
           plugin.sessions().updateBackpackBackup(p);
           plugin.forging().startPointer(p);
           return;
       }
       return;
   }
   
   // Handle Top Inventory (Forging Table)
            if (e.getClickedInventory() != p.getInventory()) {
                if (e.getClickedInventory() == null) return; // Prevent outside click crash
                
                // If in Interactive Phase (taskId != -1), prevent modifying materials
                if (session.taskId != -1) {
                    // Allow clicking specific interactive elements if needed, but mostly automated
                    return; 
                }
                
                // Material Placement Phase
                int slot = e.getRawSlot();
                if (slot >= 54) return; // Should not happen given getClickedInventory check
                
                // Only allow interaction with 'X' slots
                char func = plugin.guiConfig().getFunctionChar(layoutKey, slot);
                if (func != 'X') return;
                
                ItemStack cursor = e.getCursor();
                boolean hasCursor = cursor != null && cursor.getType() != Material.AIR;
                
                if (hasCursor && cursor != null) {
                    // Place Item (Allow in any slot)
                    ItemStack sourceRef = cursor.clone();
                    ItemStack toPlace = cursor.clone();
                    toPlace.setAmount(1);
                    
                    session.placed.put(slot, toPlace);
                    session.originalPlaced.put(slot, toPlace.clone());
                    e.getInventory().setItem(slot, toPlace);
                    com.forge.util.Sounds.play(plugin, p, "place_material");
                    
                    // Consume 1 from cursor
                    if (cursor.getAmount() > 1) {
                        cursor.setAmount(cursor.getAmount() - 1);
                        p.setItemOnCursor(cursor);
                    } else {
                        p.setItemOnCursor(new ItemStack(Material.AIR));
                    }
                    // Persist consumption into backup by matching original stack, without copying overlay
                    plugin.sessions().consumeOneFromBackup(p, sourceRef);
                } else {
                    // Pick up Item (Only if it was placed by player)
                    if (session.placed.containsKey(slot)) {
                         ItemStack placed = session.placed.get(slot);
                         session.placed.remove(slot);
                         session.originalPlaced.remove(slot);
                         session.cureMark.remove(slot);
                         e.getInventory().setItem(slot, new ItemStack(Material.AIR)); // Clear slot
                         
                         // Put onto cursor
                         p.setItemOnCursor(placed);
                         
                         // Restore visual if it was a required slot
                         plugin.forging().restoreSlotVisual(p, slot);
                    }
                    return;
                }
            }
        }
        if (title.contains(tStyle)) {
            e.setCancelled(true);
            p.sendMessage("§c武器材质选择功能已移除");
            return;
        }
    }

    // 武器配方相关方法已移除
    
    private String getDisplayName(org.bukkit.inventory.meta.ItemMeta meta) {
        return com.forge.util.Texts.getDisplayName(meta);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = p.getOpenInventory() != null ? p.getOpenInventory().getTitle() : "";
        String tForge = plugin.getConfig().getString("gui.titles.forging", "锻造");
        if (title.contains(tForge)) {
            // Check if dragging only affects player backpack (slots 9-35 of player inventory)
            boolean safe = true;
            int topSize = e.getView().getTopInventory().getSize(); // Usually 54
            for (int rawSlot : e.getRawSlots()) {
                if (rawSlot < topSize) { safe = false; break; } // Dragging in top inventory
                int playerSlot = rawSlot - topSize;
                if (playerSlot < 9) { safe = false; break; } // Dragging in hotbar
            }
            if (safe) {
                e.setCancelled(false);
            } else {
                e.setCancelled(true);
            }
        }
    }
}
