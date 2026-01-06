package com.forge.util;

import com.forge.ForgePlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class Sounds {
    private Sounds() {}

    private static final Map<String, String> DEFAULTS = new HashMap<>();
    static {
        DEFAULTS.put("start_forge", "BLOCK_ANVIL_USE");
        DEFAULTS.put("confirm_cure", "BLOCK_ANVIL_USE");
        DEFAULTS.put("pointer_move", "BLOCK_BELL_USE");
        DEFAULTS.put("pointer_on_material", "BLOCK_AMETHYST_BLOCK_HIT");
        DEFAULTS.put("comb_success", "BLOCK_ENCHANTMENT_TABLE_USE");
        DEFAULTS.put("comb_error", "BLOCK_NOTE_BLOCK_BASS");
        DEFAULTS.put("place_material", "ITEM_ARMOR_EQUIP_CHAIN");
    }

    public static void play(ForgePlugin plugin, Player p, String key) {
        if (p == null) return;
        String path = "sounds." + key + ".name";
        String sName = plugin.getConfig().getString(path, DEFAULTS.getOrDefault(key, ""));
        double vol = plugin.getConfig().getDouble("sounds." + key + ".volume", 1.0);
        double pit = plugin.getConfig().getDouble("sounds." + key + ".pitch", 1.0);
        if (sName == null || sName.isEmpty()) return;
        Sound snd;
        try { snd = Sound.valueOf(sName); } catch (Exception e) { return; }
        p.playSound(p.getLocation(), snd, (float) vol, (float) pit);
    }
}
