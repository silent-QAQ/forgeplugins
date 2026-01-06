package com.forge.service;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import com.forge.model.ForgeSession;

public class SessionService {
    private final Map<Player, ItemStack> offhand = new HashMap<>();
    private final Map<Player, String> editingMaterial = new HashMap<>();
    private final Map<Player, String> awaitingKey = new HashMap<>();
    public void saveOffhand(Player p, ItemStack it) { offhand.put(p, it == null ? null : it.clone()); }
    public ItemStack getOffhand(Player p) { return offhand.get(p); }
    public void beginMaterialEdit(Player p, String materialName) { editingMaterial.put(p, materialName); }
    public String currentMaterial(Player p) { return editingMaterial.getOrDefault(p, "PAPER"); }
    public void awaitKey(Player p, String key) { awaitingKey.put(p, key); }
    public String awaitingKey(Player p) { return awaitingKey.get(p); }
    public void clearAwait(Player p) { awaitingKey.remove(p); }
    private final Map<Player, ForgeSession> forge = new HashMap<>();
    public ForgeSession startForge(Player p, String template) { 
        ForgeSession s = new ForgeSession(p, template);
        forge.put(p, s); 
        return s;
    }
    public ForgeSession forge(Player p) { return forge.get(p); }
    public void endForge(Player p) { forge.remove(p); }
    private final Map<Player, Map<String, Integer>> pages = new HashMap<>();
    public int getPage(Player p, String key) { return pages.getOrDefault(p, new HashMap<>()).getOrDefault(key, 0); }
    public void setPage(Player p, String key, int page) { pages.computeIfAbsent(p, k -> new HashMap<>()).put(key, Math.max(0, page)); }

    private final Map<Player, ItemStack[]> invBackup = new HashMap<>();
    public void backupInventory(Player p) {
        invBackup.put(p, p.getInventory().getContents());
        p.getInventory().clear();
    }
    public ItemStack[] getBackup(Player p) {
        return invBackup.get(p);
    }
    public void updateBackpackBackup(Player p) {
        if (!invBackup.containsKey(p)) return;
        ItemStack[] current = p.getInventory().getContents();
        ItemStack[] saved = invBackup.get(p);
        for (int i = 9; i < current.length && i < saved.length; i++) {
            saved[i] = current[i];
        }
    }
    public void consumeOneFromBackup(Player p, ItemStack ref) {
        if (!invBackup.containsKey(p) || ref == null) return;
        ItemStack[] saved = invBackup.get(p);
        for (int i = 0; i < saved.length; i++) {
            ItemStack s = saved[i];
            if (s != null && s.isSimilar(ref)) {
                int amt = s.getAmount();
                if (amt > 1) { s.setAmount(amt - 1); saved[i] = s; }
                else { saved[i] = null; }
                break;
            }
        }
    }
    public void updateInventoryBackup(Player p) {
        if (!invBackup.containsKey(p)) return;
        invBackup.put(p, p.getInventory().getContents());
    }
    public void restoreInventory(Player p) {
        if (invBackup.containsKey(p)) {
            p.getInventory().setContents(invBackup.remove(p));
        }
    }
    public void restoreAll() {
        for (Map.Entry<Player, ItemStack[]> entry : invBackup.entrySet()) {
            Player p = entry.getKey();
            if (p != null && p.isOnline()) {
                p.getInventory().setContents(entry.getValue());
            }
        }
        invBackup.clear();
    }
    public boolean hasBackup(Player p) { return invBackup.containsKey(p); }

    private final Map<Player, String> editingTemplate = new HashMap<>();
    public void beginTemplateEdit(Player p, String name) { editingTemplate.put(p, name); }
    public String currentTemplate(Player p) { return editingTemplate.get(p); }
    public void endTemplateEdit(Player p) { editingTemplate.remove(p); }

    private final Map<Player, String> awaitingMaterial = new HashMap<>();
    public void awaitMaterial(Player p, String templateName) { awaitingMaterial.put(p, templateName); }
    public String awaitingMaterial(Player p) { return awaitingMaterial.get(p); }
    public void clearAwaitMaterial(Player p) { awaitingMaterial.remove(p); }
}
