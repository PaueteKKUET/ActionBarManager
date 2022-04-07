package com.fadedbytes.actionbarmanager;

import com.fadedbytes.actionbarmanager.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ActionBarManager extends JavaPlugin {

    public static final int LOOP_DELAY = 2;
    private static JavaPlugin PLUGIN;


    @Override
    public void onEnable() {
        PLUGIN = this;
        getLogger().info("ActionBarManager enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ActionBarManager disabled!");
    }

    public static JavaPlugin getPlugin() {
        return PLUGIN;
    }

    private static void init() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(
                getPlugin(),
                Manager::loop,
                0,
                LOOP_DELAY
        );
    }
}