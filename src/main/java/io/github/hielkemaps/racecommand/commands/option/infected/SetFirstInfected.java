package io.github.hielkemaps.racecommand.commands.option.infected;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.player.types.InfectedRacePlayer;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SetFirstInfected {
    public static void register(List<Argument<?>> arguments) {

        arguments.add(new LiteralArgument("setFirstInfected"));
        arguments.add(new PlayerArgument("player").replaceSuggestions(info -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            List<String> names = new ArrayList<>();

            Race race = RaceManager.getRace(((Player) info.sender()).getUniqueId());
            if (race == null) return new String[0];

            //Only show players that are in your race
            for (Player p : players) {
                if (race.hasPlayer(p.getUniqueId())) {
                    names.add(p.getName());
                }
            }
            return names.toArray(new String[0]);
        }));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    Player player = (Player) args[0];
                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race instanceof InfectedRace) {
                        InfectedRacePlayer racePlayer = (InfectedRacePlayer) race.getRacePlayer(player.getUniqueId());
                        if (racePlayer != null) {
                            ((InfectedRace) race).setFirstInfected(racePlayer);
                            p.sendMessage(Main.PREFIX + player.getName() + " will be the first infected");
                        } else {
                            CommandAPI.fail("Could not find player " + player.getName() + ".");
                            ((InfectedRace) race).setRandomFirstInfected();
                        }
                    }
                }).register();
    }
}
