package io.github.hielkemaps.racecommand.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.player.RacePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Kick {

    public static void register(List<Argument<?>> arguments){

        //race kick <player>
        arguments.add(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
            Player p = (Player) info.sender();
            List<String> names = new ArrayList<>();
            Race race = RaceManager.getRace(p.getUniqueId());
            if (race == null) return new String[0];

            List<RacePlayer> players = race.getPlayers();
            for (RacePlayer racePlayer : players) {
                UUID uuid = racePlayer.getUniqueId();
                if (uuid.equals(p.getUniqueId())) continue;
                names.add(Bukkit.getOfflinePlayer(uuid).getName());
            }

            return names.toArray(new String[0]);
        })));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    String playerName = (String) args.get(0);

                    //noinspection deprecation
                    UUID toKick = Bukkit.getOfflinePlayer(playerName).getUniqueId();

                    if (p.getUniqueId().equals(toKick)) {
                        p.sendMessage(Main.PREFIX + "You can't kick yourself");
                        return;
                    }
                    Race race = RaceManager.getRace(p.getUniqueId());
                    race.kickPlayer(toKick);
                }).register();
    }
}
