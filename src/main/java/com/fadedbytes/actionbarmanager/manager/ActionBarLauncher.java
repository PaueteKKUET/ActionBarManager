package com.fadedbytes.actionbarmanager.manager;

import com.fadedbytes.actionbarmanager.utils.PlayerConditionSupplier;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 *     The ActionBarLauncher class is used to launch action bars with some conditions.
 * </p>
 * <p>
 *     The ActionBarLauncher must be launched after creating it. The {@link #launch() launch()} method must be called to do that.
 * </p>
 * <p>
 *     You can set some conditions in order to decide if the action bar should be shown or not:
 *     <ul>
 *         <li>
 *             <b>TIME</b>: the time (in ticks) the ActionBar should be shown in the player's screen. If the player is not online, the time is ignored. Eternal launches are not affected by this condition.
 *         </li>
 *         <li>
 *             <b>TTL</b>: the total time (in ticks) the Actionbar will be in the queue for each player. This value is decremented every cycle, even if the action bar is not shown. If the TTL reaches 0, the launch is discarded for that player. An eternal action bar is not affected by this condition.
 *         </li>
 *         <li>
 *             A {@link #CONDITION_SUPPLIER General condition} will be checked each time the action bar called to shown. If the condition is not met, the action bar will be skipped, but the players <b>will not</b> lose TTL. Please, use {@link #TRUE_GENERAL_SUPPLIER} if the actionbar should not check any general condition.
 *         </li>
 *         <li>
 *             A {@link #PLAYER_CONDITION_SUPPLIER Player condition} will be checked each time the action bar called to shown for a specific player. If the condition is not met, the action bar will be skipped for that specific player, they <b>will</b> lose TTL (but will not lose TIME). Please, use {@link #TRUE_PLAYER_SUPPLIER} if the actionbar should not check any player-specific condition.
 *         </li>
 *     </ul>
 * </p>
 * <p>
 *     Launchers have some methods that can be called before launching in order to modify the action bar behaviour:
 *     <ul>
 *         <li>
 *             {@link #setPlayerList(List) SetPlayerList()} <b>should</b> be called before launching to set the list of players to send the action bar to. If not set, the action bar will finalize immediately after the launching.
 *         </li>
 *         <li>
 *             {@link #makeEternal() MakeEternal()} <b>can</b> be called before launching to set if the ActionBar will expire or not. If not set, the action bar will expire for an specific player after the defined ticks.
 *         </li>
 *         <li>
 *             {@link #launch() Launch()} <b>must</b> be called in order to launch the action bar.
 *         </li>
 *     </ul>
 * </p>
 * <p>
 *     Once launched, the launcher can not be modified again. You should not store any reference to the ActionBarLauncher object after the launch. Memory leaks are possible if you do.
 * </p>
 * <br/>
 * <p>
 *     Use the {@link #of(ActionBar, Supplier, PlayerConditionSupplier, int, int)  specific method} in order to create the launcher
 * </p>
 */
public class ActionBarLauncher {

    /**
     * The {@link Supplier supplier} that should be used if the launcher should never be skipped
     */
    public static final Supplier<Boolean> TRUE_GENERAL_SUPPLIER = () -> true;
    /**
     * The {@link PlayerConditionSupplier player condition supplier} that should be used if the launcher has not got any player-specific filter.
     */
    public static final PlayerConditionSupplier TRUE_PLAYER_SUPPLIER = (p) -> true;


    private static final int DEFAULT_TICKS = 100;                       // The default ticks the action bar will be shown.
                                                                        // The default TTL is 5 times the default ticks.


    private final ActionBar ACTIONBAR;                                  // The action bar to show.
    private final Supplier<Boolean> CONDITION_SUPPLIER;                 // The general condition.
    private final PlayerConditionSupplier PLAYER_CONDITION_SUPPLIER;    // The player-specific condition.
    private final int TIME;                                             // The time (in ticks) the action bar should be shown for each player.
    private final int TTL;                                              // The total time (in ticks) the action bar can stay in the queue.
    private final HashMap<OfflinePlayer, Integer> TIME_LEFT_PER_PLAYER; // The time left for each player.
    private final HashMap<OfflinePlayer, Integer> TTL_PER_PLAYER;       // The TTL left for each player.
    private boolean eternal;                                            // If the action bar is eternal or not.
    private boolean launched;                                           // If the action bar has been launched or not.

    /**
     * Creates a new ActionBarLauncher.
     * @param actionBar The action bar to show.
     * @param conditionSupplier The general condition.
     * @param playerConditionSupplier The player-specific condition.
     * @param time The time (in ticks) the action bar should be shown for each player.
     * @param ttl The total time (in ticks) the action bar can stay in the queue.
     */
    private ActionBarLauncher(@NotNull ActionBar actionBar, @NotNull Supplier<Boolean> conditionSupplier, @NotNull PlayerConditionSupplier playerConditionSupplier, int time, int ttl) {
        if (time <= 0 || ttl <= 0) {
            throw new IllegalArgumentException("Time and TTL must be greater than 0");
        }

        if (time < ttl) {
            throw new IllegalArgumentException("Time must be greater than TTL");
        }

        this.ACTIONBAR                  = actionBar;
        this.CONDITION_SUPPLIER         = conditionSupplier;
        this.PLAYER_CONDITION_SUPPLIER  = playerConditionSupplier;

        this.TIME_LEFT_PER_PLAYER       = new HashMap<>();
        this.TTL_PER_PLAYER             = new HashMap<>();

        this.TIME           = time;
        this.TTL            = ttl;

        this.eternal        = false;
        this.launched       = false;

    }

    /**
     * Creates a new ActionBarLauncher without specifying neither the time nor the TTL. The default time is {@link #DEFAULT_TICKS} ticks and the default TTL is 5 times the default time.
     * @param actionBar The action bar to show.
     * @param conditionSupplier The general condition.
     * @param playerConditionSupplier The player-specific condition.
     */
    private ActionBarLauncher(@NotNull ActionBar actionBar, @NotNull Supplier<Boolean> conditionSupplier, @NotNull PlayerConditionSupplier playerConditionSupplier) {
        this(actionBar, conditionSupplier, playerConditionSupplier, DEFAULT_TICKS, DEFAULT_TICKS * 5);
    }

    /**
     * Generates a new ActionBarLauncher, using the specified action bar, general condition and player-specific condition, besides the time and TTL.
     * @param actionBar The action bar to show.
     * @param conditionSupplier The general condition.
     * @param playerConditionSupplier The player-specific condition.
     * @param time The time (in ticks) the action bar should be shown for each player.
     * @param ttl The total time (in ticks) the action bar can stay in the queue.
     * @return The generated ActionBarLauncher, for chaining.
     */
    public static ActionBarLauncher of(@NotNull ActionBar actionBar, @NotNull Supplier<Boolean> conditionSupplier, @NotNull PlayerConditionSupplier playerConditionSupplier, int time, int ttl) {
        return new ActionBarLauncher(actionBar, conditionSupplier, playerConditionSupplier, time, ttl);
    }

    /**
     * Generates a new ActionBarLauncher, using the specified action bar, general condition and player-specific condition. The default time is {@link #DEFAULT_TICKS} ticks and the default TTL is 5 times the default time.
     * @param actionBar The action bar to show.
     * @param conditionSupplier The general condition.
     * @param playerConditionSupplier The player-specific condition.
     * @return The generated ActionBarLauncher, for chaining.
     */
    public static ActionBarLauncher of(@NotNull ActionBar actionBar, @NotNull Supplier<Boolean> conditionSupplier, @NotNull PlayerConditionSupplier playerConditionSupplier) {
        return new ActionBarLauncher(actionBar, conditionSupplier, playerConditionSupplier);
    }

    /**
     * Generates a new ActionBarLauncher, using the specified action bar and providing a general condition to be executed. The default time is {@link #DEFAULT_TICKS} ticks and the default TTL is 5 times the default time.
     * @param actionBar The action bar to show.
     * @param conditionSupplier The general condition.
     * @return The generated ActionBarLauncher, for chaining.
     */
    public static ActionBarLauncher of(@NotNull ActionBar actionBar, @NotNull Supplier<Boolean> conditionSupplier) {
        return new ActionBarLauncher(actionBar, conditionSupplier, TRUE_PLAYER_SUPPLIER);
    }

    /**
     * Generates a new ActionBarLauncher, without any condition to be executed. The default time is {@link #DEFAULT_TICKS} ticks and the default TTL is 5 times the default time.
     * @param actionBar The action bar to show.
     * @return The generated ActionBarLauncher, for chaining.
     */
    public static ActionBarLauncher of(@NotNull ActionBar actionBar) {
        return new ActionBarLauncher(actionBar, TRUE_GENERAL_SUPPLIER, TRUE_PLAYER_SUPPLIER);
    }

    /**
     * Makes the action bar eternal. This means that the action bar will be shown until the server is restarted.
     * @return The same ActionBarLauncher, for chaining.
     */
    public ActionBarLauncher makeEternal() {
        if (launched) {
            throw new IllegalStateException("The ActionBarLauncher is already launched");
        }

        this.eternal = true;

        return this;
    }

    /**
     * Sets the players to whom the launcher will try to show the action bar. Note that this method should be called before launching. Otherwise, the launcher will finalize right after the launch.
     * @param players The players to whom the launcher will try to show the action bar.
     * @return The same ActionBarLauncher, for chaining.
     */
    public ActionBarLauncher setPlayerList(List<OfflinePlayer> players) {
        if (launched) {
            throw new IllegalStateException("The ActionBarLauncher is already launched");
        }
        players.forEach(player -> {
            TIME_LEFT_PER_PLAYER.put(player, TIME);
            TTL_PER_PLAYER.put(player, TTL);
        });
        return this;
    }

    /**
     * @return if the action bar is eternal or not.
     */
    public boolean isEternal() {
        return eternal;
    }

    /**
     * Launches the action bar.
     */
    public void launch() {
        if (launched) {
            throw new IllegalStateException("The ActionBarLauncher is already launched");
        }

        this.launched = true;
        Manager.addLauncher(this);
    }

    /**
     * @return if the launcher can be used right now.
     */
    boolean canUse() {
        return CONDITION_SUPPLIER.get();
    }

    /**
     * @param offlinePlayer The player to check.
     * @return if the launcher can be used right now for the specified player.
     */
    boolean canUseOnPlayer(OfflinePlayer offlinePlayer) {
        if (offlinePlayer instanceof Player player) {
            return PLAYER_CONDITION_SUPPLIER.canExecuteFor(player);
        }
        return false;
    }

    /**
     * Registers a use of the launcher for the specified player. This implies a reduction of the TTL. If the use was successful, the TIME will be reduced too.
     * Note that the reduction amount is determined by the {@link com.fadedbytes.actionbarmanager.ActionBarManager#LOOP_DELAY LOOP_DELAY} field.
     * @param player The player who used the launcher.
     * @param used If the launcher was used successfully.
     */
    void registerUse(OfflinePlayer player, boolean used) {
        if (this.isEternal()) return;

        TTL_PER_PLAYER.put(player, TTL_PER_PLAYER.get(player) - com.fadedbytes.actionbarmanager.ActionBarManager.LOOP_DELAY);
        if (used) {
            TIME_LEFT_PER_PLAYER.put(player, TIME_LEFT_PER_PLAYER.get(player) - com.fadedbytes.actionbarmanager.ActionBarManager.LOOP_DELAY);
        }
    }

    /**
     * @return a list of players who can use the launcher.
     */
    List<OfflinePlayer> getPlayers() {
        return TTL_PER_PLAYER
                .keySet()
                .stream()
                .filter(player -> TTL_PER_PLAYER.get(player) > 0)
                .collect(Collectors.toList());
    }

    /**
     * Generates the current text to be shown in the action bar.
     * @param player the player to get the message for.
     * @return the current text of the action bar for the specified player.
     */
    String getMessage(Player player) {
        return this.ACTIONBAR.getText(player);
    }

    /**
     * @return the level of priority of the action bar, as an integer.
     */
    int getPriority() {
        return this.ACTIONBAR.getPriority().getPriority();
    }

    /**
     * Determines if the launcher is still valid for the specified player.
     * @param player The player to check.
     * @return if the launcher is still valid for the specified player.
     */
    boolean canBeCleanedForPlayer(OfflinePlayer player) {
       if (this.isEternal()) return false;

       return TTL_PER_PLAYER.get(player) <= 0 || TIME_LEFT_PER_PLAYER.get(player) <= 0;
    }

    boolean hasKey(NamespacedKey key) {
        return this.ACTIONBAR.getKey().equals(key);
    }

}
