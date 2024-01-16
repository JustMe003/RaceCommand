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

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
        if (!channel.equalsIgnoreCase("hielkemaps:main")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String subChannel = in.readUTF();
        String action = in.readUTF();
        String args = in.readUTF();

        // do things with the data
        if (action.equals("createRace")) {
            createEventRace(args);
        }

        if (action.equals("joinRace")) {
            joinEventRace(args);
        }

        if (action.equals("deleteRace")) {
            RaceManager.stopEvent();
        }
    }

    private void joinEventRace(String args) {
        UUID playerUUID = UUID.fromString(args);
        Player joiningPlayer = Bukkit.getPlayer(playerUUID);

        //If player has yet to join the event
        if (joiningPlayer == null) {
            EventListener.playersJoinEvent.add(playerUUID);    // Add them to the list and add them when they join
            return;
        }

        // If player already in race
        Race currentRace = RaceManager.getRace(playerUUID);
        if (currentRace != null) {
            if (currentRace.isEvent()) {
                joiningPlayer.sendMessage(Main.PREFIX.append(Component.text("You already joined this race")));
                return;
            }
            RaceManager.DisbandOrLeaveRace(playerUUID);  //Disband current race if owner, or leave it
        }

        Race event = RaceManager.getEvent();
        if (event == null) {
            joiningPlayer.sendMessage(Main.PREFIX.append(Component.text("Event not found")));
            return;
        }

        if (event.hasStarted()) {
            joiningPlayer.sendMessage(Main.PREFIX.append(Component.text("Event already started! :(")));
            return;
        }
        event.addPlayer(playerUUID);
    }

    private void createEventRace(String args) {
        RaceManager.stopEvent();

        String[] info = args.split("_");
        LocalTime startTime = LocalTime.parse(info[0]);
        LocalTime currentTime = LocalTime.now(ZoneOffset.UTC);
        
        long timeToStart = ChronoUnit.NANOS.between(currentTime, startTime);
        int seconds = (int) TimeUnit.NANOSECONDS.toSeconds(timeToStart);
        long remainingNanos = timeToStart - TimeUnit.SECONDS.toNanos(seconds);

        //Don't create race created for the past, don't.
        if (timeToStart < 0) {
            return;
        }

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

        race.setCountDown(seconds);
        race.startCountdown(remainingNanos);

        race.setBroadcast(true);
        RaceManager.addRace(race);

        // Invite all players in server
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (EventListener.playersJoinEvent.contains(player.getUniqueId())) continue;
            race.invitePlayer(player.getUniqueId());
        }
    }
}