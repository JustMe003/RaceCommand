package io.github.hielkemaps.racecommand.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;

import java.util.List;

public class Leave {

    public static void register(List<Argument<?>> arguments){

        //race leave
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race != null) {
                        race.leavePlayer(p.getUniqueId());
                        p.sendMessage(Main.PREFIX + "You have left the race");
                    }
                }).register();
    }
}
