package io.github.hielkemaps.racecommand.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;

import java.util.List;

public class Stop {

    public static void register(List<Argument<?>> arguments){
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race == null) return;
                    race.stop();
                }).register();
    }
}
