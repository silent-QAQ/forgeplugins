package com.forge.service;

import com.forge.ForgePlugin;
import com.forge.model.ForgeSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ForgingService {
    private final ForgePlugin plugin;
    public ForgingService(ForgePlugin plugin) { this.plugin = plugin; }
    
    public void openForging(Player p, String template) {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("{template}", template);
        // Load Placement Phase GUI
        GuiConfigService.GuiLayout layout = plugin.guiConfig().createGui("forging_placement", params);
        Inventory inv = layout.inventory;
        
        // Mark required slots
        java.util.Set<Integer> req = plugin.templates().getRequired(template);
        java.util.List<Integer> slots = layout.getSlots('X');
        
        for (int idx : slots) {
             if (req.contains(idx)) {
                 inv.setItem(idx, glass("§a必要槽位"));
             }
        }
        
        p.openInventory(inv);
        
        // Backup full inventory
        plugin.sessions().backupInventory(p);
        
        // Restore full inventory visually (so player sees their items in placement phase)
        ItemStack[] backup = plugin.sessions().getBackup(p);
        if (backup != null) {
            p.getInventory().setContents(backup);
        }
        
        // Apply Placement Phase Overlay (Buttons etc)
        plugin.guiConfig().applyPlayerOverlay(p, layout);
        
        ForgeSession s = plugin.sessions().startForge(p, template);
        if (s != null) {
             s.forgingLevel = plugin.playerDao().getForgingLevel(p.getUniqueId());
             s.gridSlots = new ArrayList<>(slots); // Store ordered 'X' slots
             java.util.Collections.sort(s.gridSlots); // Ensure order (0-53)
        }
    }
    public void startPointer(Player p) {
        ForgeSession s = plugin.sessions().forge(p);
        if (s == null || s.gridSlots.isEmpty()) return;
        
        // Validate materials placed
        java.util.Set<Integer> req = plugin.templates().getRequired(s.templateName);
        for (int idx : req) {
            if (!s.placed.containsKey(idx)) {
                p.sendMessage(plugin.msg().msg("missing_material"));
                return;
            }
        }
        
        // Switch to Game Phase
        GuiConfigService.GuiLayout gameLayout = plugin.guiConfig().createGui("forging_game", null);
        // Note: Top Inventory stays same (chest), only Player Inventory changes.
        // Apply Game Phase Overlay (Combing area, buttons)
        plugin.guiConfig().applyPlayerOverlay(p, gameLayout);
        com.forge.util.Sounds.play(plugin, p, "start_forge");
        
        // Setup Combing Game
        setupCombingGame(p, s);
        
        Inventory inv = p.getOpenInventory().getTopInventory();
        
        // Initialize Pointer at Start of Grid
        s.pointerIndex = s.gridSlots.get(0);
        s.tickCounter = 0;
        
        // Force visual update for start in Top Inventory
        updateSlotVisual(inv, s.pointerIndex, s); // Draw material if exists (underneath)
        inv.setItem(s.pointerIndex, glass("§a锻造进程")); // Overlay pointer
        
        s.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    if (p.getOpenInventory() == null) {
                        Bukkit.getScheduler().cancelTask(s.taskId);
                        s.taskId = -1;
                        return;
                    }
    
                    if (s.awaitingCure) {
                        return;
                    }
                    
                    // --- Arrow Animation in Hotbar ---
                    // Only animate if on a material that needs curing
                    if (s.placed.containsKey(s.pointerIndex) && !s.cured.getOrDefault(s.pointerIndex, false)) {
                        ItemStack it = s.placed.get(s.pointerIndex);
                        double melt = com.forge.util.LoreUtil.getValue(it, "熔点", 240);
                        int arrowInterval = (int) (melt / 9.0);
                        if (arrowInterval < 1) arrowInterval = 1;
                        
                        s.arrowTick++;
                        if (s.arrowTick >= arrowInterval) {
                            // Clear OLD position first
                            // In Hotbar Layout, X=85(4), F=89(8).
                            // But arrow moves 0-8.
                            // We should clear the old position if it's not a functional button we want to keep visible?
                            // Actually, layout has "UUUUXUUUF".
                            // If arrow overwrites 'U' (Air), clearing it means setting Air.
                            // If arrow overwrites 'X' (4) or 'F' (8), we must restore them.
                            
                            restoreHotbarSlot(p, s.arrowPos);
                            p.getInventory().setItem(s.bestPointSlot, item(org.bukkit.Material.DIAMOND_BLOCK, "§b最佳固化点"));
    
                            s.arrowTick = 0;
                            s.arrowPos += 1; // Always move right (one direction)
                            if (s.arrowPos > 8) { 
                                 s.arrowPos = 0; // Loop back to start
                            }
                            
                            // Draw Arrow at NEW position
                            p.getInventory().setItem(s.arrowPos, item(Material.ARROW, "§a固化指针"));
                        } else {
                            // Ensure arrow persists on current position (prevent flickering)
                             p.getInventory().setItem(s.arrowPos, item(Material.ARROW, "§a固化指针"));
                             p.getInventory().setItem(s.bestPointSlot, item(org.bukkit.Material.DIAMOND_BLOCK, "§b最佳固化点"));
                        }
                    } else {
                        // Not on material: Ensure hotbar is clean
                        // Restore all hotbar slots to layout state
                        GuiConfigService.GuiLayout layout = plugin.guiConfig().getLayout("forging_game");
                        for (int i = 0; i < 9; i++) {
                             // Map hotbar 0-8 to raw 81-89
                             int raw = 81 + i;
                             ItemStack item = layout.playerOverlay.get(raw);
                             if (item != null) p.getInventory().setItem(i, item);
                             else p.getInventory().setItem(i, new ItemStack(Material.AIR));
                        }
                        
                        // Force Ensure Confirm Button (Slot 8 / Raw 89) and Best Point (Slot 4 / Raw 85) exist
                        // This acts as a fallback if overlay is missing them
                        if (p.getInventory().getItem(8) == null || p.getInventory().getItem(8).getType() == Material.AIR) {
                             // 89 is 'F'
                             if (layout.playerOverlay.containsKey(89)) {
                                 p.getInventory().setItem(8, layout.playerOverlay.get(89));
                             }
                        }
                        
                        s.arrowPos = 0; // Reset arrow pos
                        s.arrowTick = 0;
                    }
                    
                    // --- Top Inventory Pointer & Logic ---
                    s.tickCounter++;
                    int speed = 100; // Base speed for blank slot
                    
                    if (s.placed.containsKey(s.pointerIndex)) {
                         ItemStack it = s.placed.get(s.pointerIndex);
                         speed = (int) com.forge.util.LoreUtil.getValue(it, "熔点", 240);
                         if (speed < 1) speed = 1;
                    } else {
                         // Blank Slot: Fixed 20 ticks (1s)
                         speed = 20; 
                    }
                    
                    if (s.tickCounter < speed) return;
                    s.tickCounter = 0;
                    s.arrowPos = 0; // Reset arrow for new slot
                    s.arrowTick = 0;
                    
                    // Restore Hotbar buttons before moving to next slot (ensure clean state)
                    GuiConfigService.GuiLayout layout = plugin.guiConfig().getLayout("forging_game");
                    for (int i = 0; i < 9; i++) {
                         int raw = 81 + i;
                         ItemStack item = layout.playerOverlay.get(raw);
                         if (item != null) p.getInventory().setItem(i, item);
                         else p.getInventory().setItem(i, new ItemStack(Material.AIR));
                    }
                    
                    // Move pointer
                    int prev = s.pointerIndex;
                    int next = nextIndex(s, s.pointerIndex);
                    
                    // Check if we reached the end
                    if (next == -1) { 
                        Bukkit.getScheduler().cancelTask(s.taskId);
                        s.taskId = -1;
                        finalizeForge(p);
                        return;
                    }
                    
                    s.pointerIndex = next;
                    com.forge.util.Sounds.play(plugin, p, "pointer_move");
    
                    // Visual update
                    updateSlotVisual(inv, prev, s);
                    inv.setItem(s.pointerIndex, glass("§a锻造进程")); 
                    
                    if (s.placed.containsKey(s.pointerIndex) && !s.cured.getOrDefault(s.pointerIndex, false)) {
                         com.forge.util.Sounds.play(plugin, p, "pointer_on_material");
                         setupCombingGame(p, s);
                         // Don't set arrow manually here, loop will handle it.
                         // But we need to ensure Arrow starts at 0 immediately?
                         // Loop runs every tick. arrowTick=0.
                         // First tick of new slot -> loop logic sees placed contains key -> starts arrow logic.
                         // It will place arrow at 0.
                    } else {
                        clearCombingGame(p);
                    }
                    
                    // Check previous slot failure
                    if (s.placed.containsKey(prev) && !s.cured.getOrDefault(prev, false)) {
                        ItemStack origPrev = s.originalPlaced.get(prev);
                        int combPrev = s.successPerSlot.getOrDefault(prev, 0);
                        int errPrev = s.errorPerSlot.getOrDefault(prev, 0);
                        String myStarPrev = getStarTag(origPrev);
                        double cracksPrev = 0;
                        double resoPrev = 0;
                        for (int dd = -1; dd <= 1; dd++) {
                            for (int cc = -1; cc <= 1; cc++) {
                                if (dd == 0 && cc == 0) continue;
                                int nn = prev + dd*9 + cc;
                                String nbStarPrev = s.placed.containsKey(nn) ? getStarTag(s.placed.get(nn)) : getStarTag(s.originalPlaced.get(nn));
                                if (nbStarPrev == null) nbStarPrev = "0";
                                cracksPrev += plugin.materials().getRepel(myStarPrev, nbStarPrev);
                                resoPrev += plugin.materials().getResonance(myStarPrev, nbStarPrev);
                            }
                        }
                        s.placed.put(prev, buildCuredPane(origPrev, combPrev, errPrev, 99, cracksPrev, resoPrev, myStarPrev));
                        s.cured.put(prev, true);
                        s.distPerSlot.put(prev, 99); // Max distance penalty
                        updateSlotVisual(inv, prev, s);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Task error: " + e.getMessage());
                    e.printStackTrace();
                    Bukkit.getScheduler().cancelTask(s.taskId);
                    s.taskId = -1;
                }
            }
        }, 0L, 1L);
    }

    private void restoreHotbarSlot(Player p, int slot) {
        GuiConfigService.GuiLayout layout = plugin.guiConfig().getLayout("forging_game");
        int raw = 81 + slot;
        ItemStack item = layout.playerOverlay.get(raw);
        if (item != null) p.getInventory().setItem(slot, item);
        else p.getInventory().setItem(slot, new ItemStack(Material.AIR));
        
        // Ensure Confirm Button (Hotbar Slot 8 / Raw 89)
        if (p.getInventory().getItem(8) == null || p.getInventory().getItem(8).getType() == Material.AIR) {
            if (layout.playerOverlay.containsKey(89)) {
                p.getInventory().setItem(8, layout.playerOverlay.get(89));
            }
        }
        
        // Force Ensure Best Point (Slot 4) if missing (because AIR in overlay?)
        // Config now sets it to Pane, so overlay should have it.
        // But let's force it if slot 4 is AIR.
        ItemStack bp = p.getInventory().getItem(4);
        if (slot == 4 && (bp == null || bp.getType() == Material.AIR)) {
             if (layout.playerOverlay.containsKey(85)) { // 81+4=85
                 p.getInventory().setItem(4, layout.playerOverlay.get(85));
             }
        }
    }
    
    private void setupCombingGame(Player p, ForgeSession s) {
        // Fill inventory with Blank first (except Slot 4 and 8, and skip Hotbar 0-8 range for safety)
        // Backpack slots: 9-35.
        // We only want to touch 9-35 for combing game.
        // But now we use layout 'C' for combing area.
        GuiConfigService.GuiLayout layout = plugin.guiConfig().getLayout("forging_game");
        java.util.List<Integer> cSlots = layout.getSlots('C');
        
        // Map raw slots (54-80) to player inventory slots (9-35)
        java.util.List<Integer> pSlots = new ArrayList<>();
        for (int raw : cSlots) {
            if (raw >= 54 && raw <= 80) {
                pSlots.add(raw - 54 + 9);
            }
        }
        
        // Fill with Blank
        for (int slot : pSlots) {
             p.getInventory().setItem(slot, blank("§7空白"));
        }
        
        ItemStack it = s.placed.get(s.pointerIndex);
        int complexity = (int) com.forge.util.LoreUtil.getValue(it, "延展性", 0);

        double[] st = getStabilityMinMax(it);
        int leftRange = rangeFromStability(st[0]);
        int rightRange = rangeFromStability(st[1]);
        int offset = new java.util.Random().nextInt(leftRange + rightRange + 1) - leftRange;
        s.bestPointSlot = Math.max(0, Math.min(8, 4 + offset));
        p.getInventory().setItem(s.bestPointSlot, item(org.bukkit.Material.DIAMOND_BLOCK, "§b最佳固化点"));
        
        // Randomly place 'complexity' number of Purple Panes in 'C' area
        // BUG FIX: Collections.shuffle was shuffling the list of slots, but we were iterating 0..complexity.
        // If list is shuffled, taking first N is random. This is correct.
        // But maybe complexity is 0? Or pSlots empty?
        
        if (complexity > 0 && !pSlots.isEmpty()) {
            java.util.Collections.shuffle(pSlots);
            for (int i = 0; i < complexity && i < pSlots.size(); i++) {
                p.getInventory().setItem(pSlots.get(i), purple("§5未梳理材料纹路"));
            }
        }
        
        // Reset Combing Progress
        s.combingGoal = complexity;
        s.combingCurrent = 0;
    }

    private int rangeFromStability(double stability) {
        int r = 4 - (int) Math.floor(stability / 25.0);
        if (r < 1) r = 1;
        return r;
    }
    private double[] getStabilityMinMax(ItemStack it) {
        double defMin = plugin.getConfig().getDouble("materials.defaults.stability_min", 5.0);
        double defMax = plugin.getConfig().getDouble("materials.defaults.stability_max", 5.0);
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasLore()) return new double[]{defMin, defMax};
        for (String s : com.forge.util.Texts.getLore(it.getItemMeta())) {
            String stripped = com.forge.util.Texts.stripColor(s);
            if (stripped.startsWith("稳定度:")) {
                String v = stripped.substring(stripped.indexOf(":")+1).trim();
                if (v.contains("/")) {
                    String[] parts = v.split("/");
                    try {
                        double min = Double.parseDouble(parts[0].trim());
                        double max = (parts.length > 1) ? Double.parseDouble(parts[1].trim()) : min;
                        return new double[]{min, max};
                    } catch (Exception ignored) {}
                } else {
                    try {
                        double x = Double.parseDouble(v);
                        return new double[]{x, x};
                    } catch (Exception ignored) {}
                }
            }
        }
        return new double[]{defMin, defMax};
    }

    private void clearCombingGame(Player p) {
        // Clear 'C' area to blank
        GuiConfigService.GuiLayout layout = plugin.guiConfig().getLayout("forging_game");
        java.util.List<Integer> cSlots = layout.getSlots('C');
        for (int raw : cSlots) {
            if (raw >= 54 && raw <= 80) {
                 p.getInventory().setItem(raw - 54 + 9, blank("§7空白"));
            }
        }
        
        ForgeSession s = plugin.sessions().forge(p);
        if (s != null) {
            s.combingGoal = 0;
            s.combingCurrent = 0;
        }
    }

    public void restoreSlotVisual(Player p, int slot) {
        com.forge.model.ForgeSession s = plugin.sessions().forge(p);
        if (s == null) return;
        Inventory inv = p.getOpenInventory().getTopInventory();
        updateSlotVisual(inv, slot, s);
    }

    // Helper for visual update
    private void updateSlotVisual(Inventory inv, int slot, ForgeSession s) {
        if (s.placed.containsKey(slot)) {
            inv.setItem(slot, s.placed.get(slot));
        } else if (s.cureMark.containsKey(slot)) {
            inv.setItem(slot, s.cureMark.get(slot));
        } else if (plugin.templates().getRequired(s.templateName).contains(slot)) {
            inv.setItem(slot, glass("§a必要槽位"));
        } else {
            inv.setItem(slot, new ItemStack(Material.AIR));
        }
    }

    public void finalizeForge(Player p) {
        com.forge.model.ForgeSession s = plugin.sessions().forge(p);
        if (s == null) return;
        java.util.Set<Integer> req = plugin.templates().getRequired(s.templateName);
        for (int idx : req) {
            // Must be cured AND must be Perfect(0) or Correct(1-2)
            // General(3+) is considered failure for required slots
            if (!s.cured.getOrDefault(idx, false)) { 
                p.sendMessage(plugin.msg().msg("forging_failed_req")); // "Necessary slot failed"
                p.closeInventory(); 
                plugin.sessions().endForge(p); 
                return; 
            }
            int dist = s.distPerSlot.getOrDefault(idx, 99);
            if (dist > 2) {
                 p.sendMessage("§c锻造失败！必要槽位未达到正确固化标准！");
                 p.closeInventory();
                 plugin.sessions().endForge(p);
                 return;
            }
        }
        
        // Calculate success based on Repel (still used?)
        // User didn't mention Repel in new formula, but "Repel" logic was: success += -Repel.
        // Let's keep it as a base check for "Fail/Success" of the forging process itself?
        // Or should we remove it?
        // The prompt says: "Material cured attributes = Base * (1 + (Success-Error)*10%) * (1.2 - 0.2*Dist)"
        // It doesn't mention Repel/Resonance explicitly for the *calculation formula*, but maybe they still apply?
        // "Success" in formula refers to "Combing Success".
        
        // Let's assume Repel still affects overall success rate (whether it breaks).
        double successRate = 100.0;
        // 读取各材料的裂痕值，降低成功率（1裂痕值降低1成功率）
        double crackTotal = 0;
        for (java.util.Map.Entry<Integer, org.bukkit.inventory.ItemStack> e : s.placed.entrySet()) {
            crackTotal += getCrackValue(e.getValue());
        }
        successRate -= crackTotal;
        if (successRate < 0) successRate = 0; if (successRate > 100) successRate = 100;
        boolean pass = new java.util.Random().nextDouble() * 100 <= successRate;
        
        java.util.Map<String, Double> total = new java.util.HashMap<>();
        double meltSum = 0;
        double extSum = 0;
        int matCount = s.placed.size();
        
        for (java.util.Map.Entry<Integer, org.bukkit.inventory.ItemStack> e : s.placed.entrySet()) {
            java.util.Map<String, Double> base = new java.util.HashMap<>();
            if (e.getValue().hasItemMeta() && e.getValue().getItemMeta().hasLore()) {
                base = com.forge.util.LoreUtil.parseAttributes(com.forge.util.Texts.getLore(e.getValue().getItemMeta()));
            }
            
            // 已在固化阶段写入属性值，此处无需重复应用梳理/距离/共鸣
            // 累加熔点与延展性（从玻璃板读取，支持默认值继承）
            meltSum += com.forge.util.LoreUtil.getValue(e.getValue(), "熔点", 240);
            extSum += com.forge.util.LoreUtil.getValue(e.getValue(), "延展性", 0);

            for (java.util.Map.Entry<String, Double> be : base.entrySet()) {
                total.put(be.getKey(), total.getOrDefault(be.getKey(), 0.0) + be.getValue());
            }
        }
        
        // Filter to registered attributes only
        java.util.Map<String, String> registered = plugin.attributes().all();
        java.util.Map<String, Double> filtered = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, Double> eAttr : total.entrySet()) {
            if (registered.containsKey(eAttr.getKey())) {
                filtered.put(eAttr.getKey(), eAttr.getValue());
            }
        }
        // 失败时缩放为配置比例（默认10%）
        double scale = pass ? 1.0 : (plugin.getConfig().getDouble("forging.fail_attr_percent", 10.0) / 100.0);
        if (scale != 1.0) {
            java.util.Map<String, Double> scaled = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, Double> eAttr : filtered.entrySet()) {
                scaled.put(eAttr.getKey(), eAttr.getValue() * scale);
            }
            filtered = scaled;
        }
        
        // Create result item
        org.bukkit.inventory.ItemStack resultItem;
        
        // Use Template Output Material if configured
        String tplMatName = plugin.templates().getOutputMaterial(s.templateName);
        Material tplMat = Material.PAPER;
        try { tplMat = Material.valueOf(tplMatName); } catch (Exception ignored) {}
        
        // If player has a base item in offhand, we might want to preserve it, but user said "Forging Product Material" overrides.
        // But if offhand has item meta (name, lore), we might want to keep it?
        // Usually forging creates a new item.
        // Let's create a NEW item of the configured type.
        resultItem = new org.bukkit.inventory.ItemStack(tplMat);
        
        java.util.List<String> lore = new java.util.ArrayList<>();
        // 熔点为总和；延展性为平均数*2（四舍五入）
        double avgExt = matCount > 0 ? extSum / matCount : 0;
        long finalExt = Math.round(avgExt * 2.0);
        lore.add("§7熔点: §f" + String.format("%.0f", meltSum));
        lore.add("§7延展性: §f" + finalExt);
        // 成品材料星缀：相同数量最多的值
        java.util.Map<String, Integer> resCounts = new java.util.HashMap<>();
        for (java.util.Map.Entry<Integer, org.bukkit.inventory.ItemStack> e2 : s.placed.entrySet()) {
            String rk = getStarTag(e2.getValue());
            resCounts.put(rk, resCounts.getOrDefault(rk, 0) + 1);
        }
        String inheritRes = "0";
        int maxCount = -1;
        for (java.util.Map.Entry<String, Integer> rc : resCounts.entrySet()) {
            if (rc.getValue() > maxCount) { maxCount = rc.getValue(); inheritRes = rc.getKey(); }
        }
        lore.add("§7材料星缀: §f" + inheritRes);
        lore.add("§7裂痕值: §f" + String.format("%.0f", crackTotal));
        lore.addAll(com.forge.util.LoreUtil.formatAttributes(filtered));
        lore.add("§8Template: " + s.templateName); 
        resultItem = com.forge.util.LoreUtil.lore(resultItem, lore);
        
        // Name? "Forged [Template Name]"?
        // Or keep default name.
        // Let's set a default name.
        // User said: "Template Name is actually the Finished Product Name".
        // So if template is "DragonSlayer", result should be named "DragonSlayer".
        resultItem = com.forge.util.LoreUtil.name(resultItem, "§f" + s.templateName.replace("的模板", ""));
        
        plugin.adapters().applyAttributes(resultItem, filtered);
        
        // Store result in session and open Result GUI
        s.resultItem = resultItem;
        s.finalAttributes = filtered;
        s.resultPage = 0;
        
        // Restore player inventory first (so bottom is correct)
        plugin.sessions().restoreInventory(p);
        
        openResultGui(p);
    }
    
    public void openResultGui(Player p) {
        ForgeSession s = plugin.sessions().forge(p);
        if (s == null || s.resultItem == null) return;
        
        GuiConfigService.GuiLayout layout = plugin.guiConfig().createGui("result", null);
        Inventory inv = layout.inventory;
        // Do not modify player inventory (backpack/hotbar) in result page; keep player's normal inventory
        
        // Slot 'R' (Result Item)
        int rSlot = layout.getFirstSlot('R');
        if (rSlot != -1) inv.setItem(rSlot, s.resultItem);
        
        // Slots 'A' (Attributes)
        java.util.List<Integer> aSlots = layout.getSlots('A');
        
        List<Map.Entry<String, Double>> attrs = new ArrayList<>(s.finalAttributes.entrySet());
        int perPage = aSlots.size();
        if (perPage <= 0) perPage = 1;
        int maxPages = (int) Math.ceil(attrs.size() / (double) perPage);
        if (maxPages <= 0) maxPages = 1;
        if (s.resultPage < 0) s.resultPage = 0;
        if (s.resultPage >= maxPages) s.resultPage = maxPages - 1;
        int start = s.resultPage * perPage;
        int end = Math.min(start + perPage, attrs.size());
        
        for (int i = start; i < end; i++) {
            Map.Entry<String, Double> entry = attrs.get(i);
            int slotIndex = i - start;
            if (slotIndex < aSlots.size()) {
                int slot = aSlots.get(slotIndex);
                inv.setItem(slot, item(Material.PAPER, "§b" + entry.getKey(), "§f数值: " + String.format("%.2f", entry.getValue())));
            }
        }
        
        // Navigation Buttons remain as configured in top inventory
        
        p.openInventory(inv);
    }
    
    private ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        com.forge.util.Texts.setDisplayName(meta, name);
        if (lore.length > 0) com.forge.util.Texts.setLore(meta, Arrays.asList(lore));
        it.setItemMeta(meta);
        return it;
    }

    private String nameOf(org.bukkit.inventory.ItemStack it) {
        if (it == null) return "PAPER";
        // Strip colors to match config keys
        if (it.hasItemMeta() && it.getItemMeta() != null) {
             return com.forge.util.Texts.stripColor(com.forge.util.Texts.getDisplayName(it.getItemMeta()));
        }
        return it.getType().name();
    }
    // Removed setupCureInventory and confirmCure
    
    public void combMaterial(Player p, int slot) {
        ForgeSession s = plugin.sessions().forge(p);
        if (s == null) return;
        
        ItemStack it = p.getInventory().getItem(slot);
        if (it == null || !it.hasItemMeta()) return;
        
        // Correct Combing: Clicking Purple Pane ("未梳理")
        if (com.forge.util.Texts.getDisplayName(it.getItemMeta()).contains("未梳理")) {
            p.getInventory().setItem(slot, glass("§a已梳理"));
            s.successPerSlot.put(s.pointerIndex, s.successPerSlot.getOrDefault(s.pointerIndex, 0) + 1);
            com.forge.util.Sounds.play(plugin, p, "comb_success");
        } 
        // Error Combing: Clicking Blank Pane ("空白")
        else if (com.forge.util.Texts.getDisplayName(it.getItemMeta()).contains("空白")) {
            p.getInventory().setItem(slot, item(com.forge.util.MaterialCompat.blackPane(), "§c错误梳理")); 
            // Actually, prompt didn't specify visual for error, but we need to track it.
            s.errorPerSlot.put(s.pointerIndex, s.errorPerSlot.getOrDefault(s.pointerIndex, 0) + 1);
            com.forge.util.Sounds.play(plugin, p, "comb_error");
        }
    }

    public void confirmCure(Player p) {
        ForgeSession s = plugin.sessions().forge(p);
        if (s == null) return;
        
        // Must be on a material
        if (!s.placed.containsKey(s.pointerIndex)) return;
        
        // Removed strict combing check as requested
        // "Quantity is only used for attribute limit breakthrough"
        // TODO: Record combing ratio for finalizeForge
        
        int actualDist = Math.abs(s.arrowPos - s.bestPointSlot);
        
        int combed = s.successPerSlot.getOrDefault(s.pointerIndex, 0);
        int errors = s.errorPerSlot.getOrDefault(s.pointerIndex, 0);
        ItemStack orig = s.originalPlaced.get(s.pointerIndex);
        String myStar = getStarTag(orig);
        double cracks = 0;
        double resoSum = 0;
        for (int d = -1; d <= 1; d++) {
            for (int c = -1; c <= 1; c++) {
                if (d == 0 && c == 0) continue;
                int n = s.pointerIndex + d*9 + c;
                String nbStar = s.placed.containsKey(n) ? getStarTag(s.placed.get(n)) : getStarTag(s.originalPlaced.get(n));
                if (nbStar == null) nbStar = "0";
                cracks += plugin.materials().getRepel(myStar, nbStar);
                resoSum += plugin.materials().getResonance(myStar, nbStar);
            }
        }
        ItemStack res = buildCuredPane(orig, combed, errors, actualDist, cracks, resoSum, myStar);
        com.forge.util.Sounds.play(plugin, p, "confirm_cure");
        s.placed.put(s.pointerIndex, res);
        s.cured.put(s.pointerIndex, true);
        s.distPerSlot.put(s.pointerIndex, actualDist);
        
        // Clear combing game (reset to blank)
        clearCombingGame(p);
        
        s.tickCounter = 0; 
    }

    private ItemStack item(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        com.forge.util.Texts.setDisplayName(meta, name);
        it.setItemMeta(meta);
        return it;
    }
    private ItemStack glass(String name) {
        ItemStack it = new ItemStack(com.forge.util.MaterialCompat.greenPane());
        ItemMeta meta = it.getItemMeta();
        com.forge.util.Texts.setDisplayName(meta, name);
        it.setItemMeta(meta);
        return it;
    }
    private ItemStack purple(String name) {
        return item(com.forge.util.MaterialCompat.purplePane(), name);
    }
    private ItemStack blank(String name) {
        return item(com.forge.util.MaterialCompat.lightGrayPane(), name);
    }
    private double getCrackValue(ItemStack it) {
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasLore()) return 0;
        for (String s : com.forge.util.Texts.getLore(it.getItemMeta())) {
            String stripped = com.forge.util.Texts.stripColor(s);
            if (stripped.startsWith("裂痕值:")) {
                try { return Double.parseDouble(stripped.substring(stripped.indexOf(":")+1).trim()); } catch (Exception ignored) {}
            }
        }
        return 0;
    }
    private ItemStack buildCuredPane(ItemStack original, int combed, int errors, int dist, double cracks, double resoSum, String starTag) {
        double bonus = plugin.getConfig().getDouble("forging.comb_success_bonus", 1.0);
        double penalty = plugin.getConfig().getDouble("forging.comb_error_penalty", 1.0);
        double distMult = 1.2 - 0.2 * dist;
        if (distMult < 0) distMult = 0;
        double combMult = 1.0 + (combed * bonus - errors * penalty) * 0.1;
        if (combMult < 0) combMult = 0;

        java.util.Map<String, Double> base = new java.util.HashMap<>();
        if (original != null && original.hasItemMeta() && original.getItemMeta().hasLore()) {
            base = com.forge.util.LoreUtil.parseAttributes(com.forge.util.Texts.getLore(original.getItemMeta()));
        }
        java.util.Map<String, Double> cured = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, Double> e : base.entrySet()) {
            double resoMult = 1 + (resoSum / 100.0);
            cured.put(e.getKey(), e.getValue() * combMult * distMult * resoMult);
        }

        ItemStack pane;
        if (dist == 0) pane = item(com.forge.util.MaterialCompat.yellowPane(), "§6完美固化");
        else if (dist <= 2) pane = item(com.forge.util.MaterialCompat.greenPane(), "§a正确固化");
        else pane = item(com.forge.util.MaterialCompat.blackPane(), "§8一般固化");

        java.util.List<String> lore = new java.util.ArrayList<>();
        String matName = (original != null) ? nameOf(original) : "PAPER";
        lore.add("§7材料: §f" + matName);
        lore.add("§7材料星缀: §f" + (starTag == null ? "0" : starTag));
        lore.add("§7裂痕值: §f" + String.format("%.0f", cracks));
        // 继承熔点与延展性
        double origMelt = com.forge.util.LoreUtil.getValue(original, "熔点", 240);
        double origExt = com.forge.util.LoreUtil.getValue(original, "延展性", 0);
        lore.add("§7熔点: §f" + String.format("%.0f", origMelt));
        lore.add("§7延展性: §f" + String.format("%.0f", origExt));
        // 材料星缀与裂痕由上方字段表示，不再复制旧共鸣词条
        lore.addAll(com.forge.util.LoreUtil.formatAttributes(cured));
        pane = com.forge.util.LoreUtil.lore(pane, lore);
        return pane;
    }
    private String getStarTag(ItemStack it) {
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasLore()) return "0";
        for (String s : com.forge.util.Texts.getLore(it.getItemMeta())) {
            String stripped = com.forge.util.Texts.stripColor(s);
            if (stripped.startsWith("材料星缀:")) {
                String v = stripped.substring(stripped.indexOf(":")+1).trim();
                return v.isEmpty() ? "0" : v;
            }
        }
        return "0";
    }
    private int nextIndex(ForgeSession s, int idx) {
        if (s.gridSlots == null) return idx + 1; // Fallback
        int i = s.gridSlots.indexOf(idx);
        if (i == -1 || i + 1 >= s.gridSlots.size()) return -1;
        return s.gridSlots.get(i + 1);
    }
}
