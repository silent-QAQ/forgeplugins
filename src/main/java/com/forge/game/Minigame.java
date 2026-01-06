package com.forge.game;

import com.forge.model.ForgeSession;
import org.bukkit.entity.Player;

public interface Minigame {
    void start(Player p, ForgeSession s);
    void onClick(Player p, ForgeSession s, int slot);
    boolean isCompleted();
    void clear(Player p);
}
