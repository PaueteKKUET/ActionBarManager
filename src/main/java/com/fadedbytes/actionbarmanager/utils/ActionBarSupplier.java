package com.fadedbytes.actionbarmanager.utils;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 *     The ActionBarSupplier interface is used to supply the ActionBar text. This text may be static or dynamic.
 * </p>
 * <p>
 *     The player is passed to the supplier so it can be used to determine the text. For example, you can get the player name and display it in the ActionBar.
 * </p>
 * <p>
 *     The function must be prepared to receive a null player.
 * </p>
 */
@FunctionalInterface
public interface ActionBarSupplier {
    /**
     * Gets the current ActionBar text.
     * @param player the player that may be used to determine the text..
     * @return the current ActionBar text.
     */
    String get( @Nullable Player player);
}
