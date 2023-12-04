package io.github.hielkemaps.racecommand;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.github.hielkemaps.racecommand.events.EventListener;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import io.github.hielkemaps.racecommand.race.types.NormalRace;
import io.github.hielkemaps.racecommand.race.types.PvpRace;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Main extends JavaPlugin implements PluginMessageListener {

    private static Plugin instance;

    public static String startFunction;

    public Main() {
        instance = this;
    }

    public static Plugin getInstance() {
        return instance;
    }

    public static Component PREFIX = Component.empty().append(Component.text("[RACE] ", NamedTextColor.YELLOW, TextDecoration.BOLD));

    @Override
    public void onEnable() {

        saveDefaultConfig();
        FileConfiguration config = getConfig();
        startFunction = config.getString("start-function");

        //Register commands
        new Commands();

        //Register EventListener
        getServer().getPluginManager().registerEvents(new EventListener(), this);

        getServer().getMessenger().registerIncomingPluginChannel(this, "hielkemaps:main", this);
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] bytes) {
        getLogger().info("Messaged received!");

        if (!channel.equalsIgnoreCase("hielkemaps:main")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String subChannel = in.readUTF();
        getLogger().info("Channel: " + subChannel);

        String action = in.readUTF();
        String args = in.readUTF();

        // do things with the data
        getLogger().info("Action: " + action);
        getLogger().info("Args: " + args);

        if (action.equals("createRace")) {
            RaceManager.stopEvent();

            String[] info = args.split("_");
            int minutes = Integer.parseInt(info[0]);
            String type = info[1];
            int first = Integer.parseInt(info[2]);
            int second = Integer.parseInt(info[3]);
            int third = Integer.parseInt(info[4]);
            int fourth = Integer.parseInt(info[5]);

            Race race;
            if (type.equals("pvp")) {
                race = new PvpRace(getInstance().getServer().getConsoleSender());
            } else if (type.equals("infected")) {
                race = new InfectedRace(getInstance().getServer().getConsoleSender());
            } else {
                race = new NormalRace(getInstance().getServer().getConsoleSender());
            }
            race.setPrizes(first, second, third, fourth);
            race.setCountDown(minutes * 60);

            race.start();
            race.setBroadcast(true);

            RaceManager.addRace(race);
        }

        if (action.equals("joinRace")) {
            UUID playerUUID = UUID.fromString(args);
            Player joiningPlayer = Bukkit.getPlayer(playerUUID);

            //If player is already in server, add them
            if (joiningPlayer != null) {
                Race event = RaceManager.getEvent();
                if (event == null) {
                    joiningPlayer.sendMessage(Main.PREFIX.append(Component.text("Event not found")));
                    return;
                }

                if(event.hasStarted()){
                    joiningPlayer.sendMessage(Main.PREFIX.append(Component.text("Event already started! :(")));
                    return;
                }

                if (event.hasPlayer(playerUUID)) {
                    joiningPlayer.sendMessage(Main.PREFIX.append(Component.text("You already joined this race")));
                    return;
                }

                event.addPlayer(playerUUID);
            } else {
                //Else we add them to the list and add them when they join
                EventListener.playersJoinEvent.add(playerUUID);
            }
        }
    }
}