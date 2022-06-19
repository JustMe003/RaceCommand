package io.github.hielkemaps.racecommand.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;

import java.util.List;

public class Start {

    public static void register(List<Argument<?>> arguments){

        //race start
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race == null) return;

                    race.start();
                }).register();

        //race start <countdown>
        arguments.add(new IntegerArgument("countdown", 0, 1000));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    int value = (int) args[0];

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race == null) return;

                    race.setCountDown(value);
                    race.start();
                }).register();
    }
}
