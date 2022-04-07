package com.fadedbytes.actionbarmanager.utils;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlayerConditionSupplier {
    boolean canExecuteFor(Player player);
}
