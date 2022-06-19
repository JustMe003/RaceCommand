package io.github.hielkemaps.racecommand;

import io.github.hielkemaps.racecommand.commands.CommandBuilder;
import io.github.hielkemaps.racecommand.events.EventListener;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.skins.SkinAPI;
import io.github.hielkemaps.racecommand.skins.SkinManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Plugin instance;

    public static String startFunction;

    public Main() {
        instance = this;
    }

    public static Plugin getInstance() {
        return instance;
    }

    public static String PREFIX = ChatColor.YELLOW + "" + ChatColor.BOLD + "[RACE] " + ChatColor.RESET + "" + ChatColor.GRAY;

    @Override
    public void onEnable() {

        try {
            SkinAPI.init();
        } catch (NoClassDefFoundError ignored) {
            SkinManager.classPresent = false;
            Bukkit.getLogger().warning("[RaceCommand] SkinsRestorer API not found, changing skins won't work!");
        }

        saveDefaultConfig();
        FileConfiguration config = getConfig();
        startFunction = config.getString("start-function");

        //Register commands
        CommandBuilder.register();

        //Register EventListener
        getServer().getPluginManager().registerEvents(new EventListener(), this);
    }


    @Override
    public void onDisable() {
        RaceManager.getRaces().forEach(
                (uuid, race) -> race.disband()
        );
    }
}