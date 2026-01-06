package com.forge.model;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

// Model class for Forging Session
public class ForgeSession {
    public final Player player;
    public final String templateName;
    public int pointerIndex = 0;
    public int taskId = -1;
    public int cureTaskId = -1;
    public boolean delaying = false;
    public final Map<Integer, ItemStack> placed = new HashMap<>();
    public final Map<Integer, ItemStack> originalPlaced = new HashMap<>();
    public final Map<Integer, ItemStack> cureMark = new HashMap<>();
    public final Map<Integer, Boolean> required = new HashMap<>();
    public final Map<Integer, Boolean> cured = new HashMap<>();
    public final Map<Integer, Integer> cureQuality = new HashMap<>();
    public int bestPoint = -1;
    public int bestPointSlot = 4; // Default best point slot
    public int arrowPos = 0;
    public int successCount = 0;
    public int errorCount = 0;
    public final Map<Integer, Integer> successPerSlot = new HashMap<>();
    public final Map<Integer, Integer> errorPerSlot = new HashMap<>();
    public final Map<Integer, Integer> distPerSlot = new HashMap<>();
    public boolean awaitingCure = false;
    public int tickCounter = 0;
    public int arrowTick = 0; // Tick counter for arrow movement
    public int forgingLevel = 0; // Cached level
    
    // Combing Game State
    public com.forge.game.Minigame currentGame = null;
    public int combingGoal = 0;
    public int combingCurrent = 0;
    
    public ForgeSession(Player player, String templateName) {
        this.player = player;
        this.templateName = templateName;
    }
    
    // Result GUI State
    public ItemStack resultItem;
    public Map<String, Double> finalAttributes;
    public int resultPage = 0;
    
    // Grid Slots for Forging (Ordered List of 'X' slots)
    public List<Integer> gridSlots = new ArrayList<>();
    
    // Cached Game Overlay for restoration
    public Map<Integer, ItemStack> gameOverlay = new HashMap<>();
}
