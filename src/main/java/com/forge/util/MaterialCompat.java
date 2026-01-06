package com.forge.util;

import org.bukkit.Material;

public final class MaterialCompat {
    public static Material lightGrayPane() { return safe("LIGHT_GRAY_STAINED_GLASS_PANE", Material.GLASS_PANE); }
    public static Material amethystShard() {
        try { return Material.valueOf("AMETHYST_SHARD"); }
        catch (IllegalArgumentException e) { return Material.valueOf("QUARTZ"); }
    }
    public static Material greenPane() { return safe("GREEN_STAINED_GLASS_PANE", Material.GLASS_PANE); }
    public static Material blackPane() { return safe("BLACK_STAINED_GLASS_PANE", Material.GLASS_PANE); }
    public static Material purplePane() { return safe("PURPLE_STAINED_GLASS_PANE", Material.GLASS_PANE); }
    public static Material yellowPane() { return safe("YELLOW_STAINED_GLASS_PANE", Material.GLASS_PANE); }
    public static Material amethystCluster() { return safe("AMETHYST_CLUSTER", Material.EMERALD); }
    public static Material rocket() { return safe("FIREWORK_ROCKET", Material.FIREWORK_ROCKET); }
    public static Material enderPearl() { return safe("ENDER_PEARL", Material.ENDER_PEARL); }
    public static Material arrow() { return safe("ARROW", Material.ARROW); }
    public static Material anvil() { return safe("ANVIL", Material.ANVIL); }
    private static Material safe(String name, Material fallback) {
        try { return Material.valueOf(name); } catch (Exception ignored) { return fallback; }
    }
}
