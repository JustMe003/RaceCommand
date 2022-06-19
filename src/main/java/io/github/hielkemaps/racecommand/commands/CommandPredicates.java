package io.github.hielkemaps.racecommand.commands;

import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

public class CommandPredicates {

    public static Predicate<CommandSender> playerInRace = sender -> PlayerManager.getPlayer(((Player) sender).getUniqueId()).isInRace();

    public static Predicate<CommandSender> playerHasJoinableRaces = sender -> PlayerManager.getPlayer(((Player) sender).getUniqueId()).hasJoinableRace();

    public static Predicate<CommandSender> playerInInfectedRace = sender -> PlayerManager.getPlayer(((Player) sender).getUniqueId()).isInInfectedRace();

    public static Predicate<CommandSender> playerIsRaceOwner = sender -> {
        Player player = (Player) sender;
        if (player == null) return false;

        Race race = RaceManager.getRace(player.getUniqueId());
        if (race == null) return false;

        return race.isOwner(player.getUniqueId());
    };

    public static Predicate<CommandSender> raceHasStarted = sender -> {
        Player player = (Player) sender;
        if (player == null) return false;

        Race race = RaceManager.getRace(player.getUniqueId());
        if (race == null) return false;

        return race.hasStarted();
    };

    public static Predicate<CommandSender> raceIsStarting = sender -> {
        Player player = (Player) sender;
        if (player == null) return false;

        Race race = RaceManager.getRace(player.getUniqueId());
        if (race == null) return false;

        return race.isStarting();
    };

    public static Predicate<CommandSender> playerToKick = sender -> {
        Player player = (Player) sender;
        if (player == null) return false;

        Race race = RaceManager.getRace(player.getUniqueId());
        if (race == null) return false;

        return race.getPlayers().size() > 1;
    };
}
