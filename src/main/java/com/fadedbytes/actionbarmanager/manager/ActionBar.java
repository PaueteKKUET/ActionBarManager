package com.fadedbytes.actionbarmanager.manager;

import com.fadedbytes.actionbarmanager.utils.ActionBarSupplier;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 *     The ActionBar object stores information about the action bar. It can be created with a {@link ActionBarSupplier} in order to create dynamic
 *     displays, but can be created with a {@link String}, for static text.
 * </p>
 * <p>
 *     An ActionBar <b>needs</b> an {@link ActionBarPriority} level, in order to decide which ActionBar should be displayed, if multiple ActionBars are available for the same player.
 * </p>
 * <p>
 *     You can use the same ActionBar object for multiple players, if you want to display the same information. You can use the {@link org.bukkit.entity.Player} parameter
 *     of the {@link ActionBarSupplier supplier} to adjust the display for a specific player.
 * </p>
 * <p>
 *     The ActionBar needs a {@link NamespacedKey}. This is used to identify the ActionBar.
 * <p>
 *     Check out the {@link #create(ActionBarSupplier, ActionBarPriority, NamespacedKey)}  create()} method for more information.
 * </p>
 */
public class ActionBar {

    private final ActionBarSupplier SUPPLIER;   // The supplier of the string to display
    private final ActionBarPriority PRIORITY;   // The priority of the ActionBar
    private final NamespacedKey     KEY;        // The namespaced key of the ActionBar

    /**
     * Creates a new ActionBar, specifying the {@link ActionBarSupplier supplier} and the {@link ActionBarPriority priority}, besides the {@link NamespacedKey key}.
     * @param supplier the supplier of the text. It will be executed every time the ActionBar is displayed.
     * @param priority the priority of the ActionBar. It will be used to decide which ActionBar should be displayed, if multiple ActionBars are available for the same player.
     * @param key the namespaced key of the ActionBar.
     */
    private ActionBar( ActionBarSupplier supplier, ActionBarPriority priority, NamespacedKey key ) {

        if ( supplier == null ) {
            throw new IllegalArgumentException( "Supplier cannot be null!" );
        }

        if ( key == null ) {
            throw new IllegalArgumentException( "NamespacedKey cannot be null!" );
        }

        this.SUPPLIER   = supplier;
        this.PRIORITY   = priority;
        this.KEY        = key;
    }

    /**
     * Default <b>dynamic</b> ActionBar creator. You can define the {@link ActionBarSupplier text supplier} and the {@link ActionBarPriority priority} of the ActionBar.
     * @param supplier the supplier of the text. It will be executed every time the ActionBar is displayed.
     * @param priority the priority of the ActionBar. It will be used to decide which ActionBar should be displayed, if multiple ActionBars are available for the same player.
     * @return the ActionBar object.
     */
    public static ActionBar create( ActionBarSupplier supplier, ActionBarPriority priority, NamespacedKey key ) {
        return new ActionBar( supplier, priority, key );
    }

    /**
     * Create a new <b>dynamic</b> ActionBar object with {@link ActionBarPriority#NORMAL NORMAL} priority and a {@link ActionBarSupplier text supplier}.
     * @param supplier the supplier of the text. It will be executed every time the ActionBar is displayed.
     * @return the ActionBar object.
     */
    public static ActionBar create( ActionBarSupplier supplier, NamespacedKey key ) {
        return new ActionBar( supplier, ActionBarPriority.NORMAL, key );
    }

    /**
     * Default <b>static</b> ActionBar creator. You can define the {@link String static text} and the {@link ActionBarPriority priority} of the ActionBar.
     * @param text the text to display.
     * @param priority the priority of the ActionBar. It will be used to decide which ActionBar should be displayed, if multiple ActionBars are available for the same player.
     * @return the ActionBar object.
     */
    public static ActionBar create( String text, ActionBarPriority priority, NamespacedKey key ) {
        return new ActionBar( (p) -> text, priority, key );
    }

    /**
     * Create a new <b>static</b> ActionBar object with {@link ActionBarPriority#NORMAL NORMAL} priority and a {@link String static text}.
     * @param text the text to display.
     * @return the ActionBar object.
     */
    public static ActionBar create( String text, NamespacedKey key ) {
        return new ActionBar( (p) -> text, ActionBarPriority.NORMAL, key );
    }

    /**
     * Get the current text of the ActionBar.
     * @return the text to display currently. It may change in the future, if the supplier is dynamic.
     */
    @Nullable String getText(Player player) {
        return SUPPLIER.get(player);
    }

    /**
     * Get the priority of the ActionBar.
     * @return the priority of the ActionBar.
     */
    ActionBarPriority getPriority() {
        return PRIORITY;
    }

    /**
     * @return the namespaced key of the ActionBar.
     */
    public NamespacedKey getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public String toString() {
        return  KEY + " {" +
                "CURRENT_TEXT=" + SUPPLIER.get(null) +
                ", PRIORITY=" + PRIORITY +
                '}';
    }
}
