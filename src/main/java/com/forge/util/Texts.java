package com.forge.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Texts {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private Texts() {}

    @SuppressWarnings("deprecation")
    public static String getDisplayName(ItemMeta meta) {
        if (meta == null) return "";
        try {
            Component c = meta.displayName();
            return c == null ? "" : PLAIN.serialize(c);
        } catch (Throwable t) {
            try {
                return meta.hasDisplayName() ? meta.getDisplayName() : "";
            } catch (Throwable ignored) { return ""; }
        }
    }

    @SuppressWarnings("deprecation")
    public static void setDisplayName(ItemMeta meta, String name) {
        if (meta == null) return;
        try {
            meta.displayName(LEGACY.deserialize(name));
        } catch (Throwable t) {
            try { meta.setDisplayName(name); } catch (Throwable ignored) {}
        }
    }

    @SuppressWarnings("deprecation")
    public static List<String> getLore(ItemMeta meta) {
        if (meta == null) return Collections.emptyList();
        try {
            List<Component> comps = meta.lore();
            if (comps == null) return Collections.emptyList();
            List<String> out = new ArrayList<>(comps.size());
            for (Component c : comps) out.add(LEGACY.serialize(c));
            return out;
        } catch (Throwable t) {
            try {
                List<String> lore = meta.getLore();
                return lore == null ? Collections.emptyList() : new ArrayList<>(lore);
            } catch (Throwable ignored) { return Collections.emptyList(); }
        }
    }

    @SuppressWarnings("deprecation")
    public static void setLore(ItemMeta meta, List<String> lore) {
        if (meta == null) return;
        if (lore == null) lore = Collections.emptyList();
        try {
            List<Component> comps = new ArrayList<>(lore.size());
            for (String s : lore) comps.add(LEGACY.deserialize(s));
            meta.lore(comps);
        } catch (Throwable t) {
            try { meta.setLore(lore); } catch (Throwable ignored) {}
        }
    }

    public static String stripColor(String s) {
        if (s == null) return "";
        try {
            return PLAIN.serialize(LEGACY.deserialize(s));
        } catch (Throwable t) {
            return s.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
        }
    }

    @SuppressWarnings("deprecation")
    public static Inventory createInventory(Player holder, int size, String title) {
        try {
            return Bukkit.createInventory(holder, size, Component.text(title));
        } catch (Throwable t) {
            try { return Bukkit.createInventory(holder, size, title); } catch (Throwable ignored) {}
            return Bukkit.createInventory(holder, size);
        }
    }
}
