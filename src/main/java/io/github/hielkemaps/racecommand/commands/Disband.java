package io.github.hielkemaps.racecommand.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.RaceManager;

import java.util.List;

public class Disband {

    public static void register(List<Argument<?>> arguments){

        //race disband
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    RaceManager.disbandRace(p.getUniqueId());
                    p.sendMessage(Main.PREFIX + "You have disbanded the race");
                }).register();
    }
}
