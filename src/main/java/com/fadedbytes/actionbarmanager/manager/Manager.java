package com.fadedbytes.actionbarmanager.manager;

import com.google.common.collect.Lists;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 *     The Manager class stores {@link ActionBarLauncher action bar launchers}.
 *     Each {@value com.fadedbytes.actionbarmanager.ActionBarManager#LOOP_DELAY} ticks,
 *     the {@link #loop()} method is called. This method will iterate through all the launchers,
 *     in order to select the action bar which should be displayed for each player.
 * </p>
 * <p>
 *     There should be only one instance of this class, and should not be accessed by your own code.
 * </p>
 * <p>
 *     You can remove a specific actionbar launcher by calling {@link #removeActionBar(NamespacedKey)} with the same key used to create that ActionBar.
 * </p>
 */
public final class Manager {

    private static final Manager MANAGER;           // The only instance of this class.
    private static final byte CLEAN_CYCLES = 100;   // The number of iterations needed before the buffers are cleaned.

    private static int CURRENT_CYCLES;

    static {
        MANAGER         = new Manager();            // Initialize the only instance of this class.
        CURRENT_CYCLES  = 0;                        // Initialize the current cycles counter.
    }

    private final List<ActionBarLauncher> ACTIVE_ACTIONBARS;                    // The action bar launchers which still having some players to display to.
    private final HashMap<Player, List<ActionBarLauncher>> PLAYERS_TO_UPDATE;   // The players which have any action bar to display, and the launchers which should be displayed for them.

    /**
     * Creates the manager instance. Should not be called more than once.
     */
    private Manager() {
        ACTIVE_ACTIONBARS = new ArrayList<>();
        PLAYERS_TO_UPDATE = new HashMap<>();
    }

    /**
     * Registers a new action bar launcher.
     * @param launcher the launcher to register.
     */
    static void addLauncher(ActionBarLauncher launcher) {
        MANAGER.ACTIVE_ACTIONBARS.add(launcher);
    }

    /**
     * Loops through all the launchers, and selects the action bar which should be displayed for each player.
     * Then, it will send the action bar to the player.
     * <br>
     * <b>NEVER CALL THIS METHOD</b>, as it is intended to be called by the plugin itself.
     */
    public static void loop() {
        CURRENT_CYCLES++;

        checkActionBars();
        showActionBars();

        if (CURRENT_CYCLES >= CLEAN_CYCLES) {
            clean();
            CURRENT_CYCLES = 0;
        }
    }

    /**
     * Matches the action bar launchers with the players who need these action bars.
     */
    private synchronized static void checkActionBars() {
        for (ActionBarLauncher actionBarLauncher : MANAGER.ACTIVE_ACTIONBARS) {
            if (!actionBarLauncher.canUse()) continue;

            for (OfflinePlayer player : actionBarLauncher.getPlayers()) {
                if (!actionBarLauncher.canUseOnPlayer(player) && player instanceof Player) {
                    actionBarLauncher.registerUse(player, false);
                    continue;
                }

                if (player instanceof Player validPlayer) {
                    if (MANAGER.PLAYERS_TO_UPDATE.containsKey(validPlayer)) {
                        MANAGER.PLAYERS_TO_UPDATE.get(validPlayer).add(actionBarLauncher);
                    } else {
                        MANAGER.PLAYERS_TO_UPDATE.put(validPlayer, new ArrayList<>(Lists.newArrayList(actionBarLauncher)));
                    }
                    actionBarLauncher.registerUse(player, true);
                }
            }
        }
    }

    /**
     * Shows the best action bar to each player.
     */
    private synchronized static void showActionBars() {
        for (Player player : MANAGER.PLAYERS_TO_UPDATE.keySet()) {
            List<ActionBarLauncher> actionBars = MANAGER.PLAYERS_TO_UPDATE.get(player);
            if (actionBars.size() < 1) return;
            actionBars.sort(Comparator.comparingInt(ActionBarLauncher::getPriority));
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBars.get(0).getMessage(player)));
        }
    }

    /**
     * Cleans the {@link #ACTIVE_ACTIONBARS} and {@link #PLAYERS_TO_UPDATE} lists of unused objects.
     */
    private synchronized static void clean() {


        // CLEAN PLAYERS WITHOUT ACTION BARS
        // Iterates over all the players, and removes them if they don't have any action bar to display.
        List<Player> removablePlayers = new ArrayList<>();
        for (Player player : MANAGER.PLAYERS_TO_UPDATE.keySet()) {
            List<ActionBarLauncher> launchers = MANAGER.PLAYERS_TO_UPDATE.get(player);
            List<ActionBarLauncher> removableLaunchers = new ArrayList<>();

            for (ActionBarLauncher launcher : launchers) {
                if (launcher.canBeCleanedForPlayer(player)) {
                    removableLaunchers.add(launcher);
                }
            }

            launchers.removeAll(removableLaunchers);
            if (launchers.size() < 1) {
                removablePlayers.add(player);
            }
        }
        for (Player player : removablePlayers) {
            MANAGER.PLAYERS_TO_UPDATE.remove(player);
        }

        // CLEAN ACTION BARS WITHOUT PLAYERS AVAILABLE
        // Iterates over all the launchers. For each, iterates over all the players. The launcher will be removed if any player has a reference to it.
        List<ActionBarLauncher> removableLaunchers = new ArrayList<>();
        for (ActionBarLauncher launcher : MANAGER.ACTIVE_ACTIONBARS) {
            if (launcher.isEternal()) continue;

            AtomicBoolean canBeRemoved = new AtomicBoolean(true);
            MANAGER.PLAYERS_TO_UPDATE
                    .keySet()
                    .forEach((player) -> {
                        if (MANAGER.PLAYERS_TO_UPDATE.get(player).contains(launcher)) {
                            canBeRemoved.set(false);
                        }
                    });
            if (canBeRemoved.get()) {
                removableLaunchers.add(launcher);
            }
        }
        MANAGER.ACTIVE_ACTIONBARS.removeAll(removableLaunchers);

    }

    /**
     * Removes from the queue all the launchers whose action bar key matches the given key.
     * @param key the key of the action bar to remove.
     */
    public static void removeActionBar(NamespacedKey key) {
        MANAGER.ACTIVE_ACTIONBARS.removeIf(actionBarLauncher -> actionBarLauncher.hasKey(key));
        MANAGER.PLAYERS_TO_UPDATE.keySet().forEach(player -> MANAGER.PLAYERS_TO_UPDATE.get(player).removeIf(actionBarLauncher -> actionBarLauncher.hasKey(key)));
    }

}
