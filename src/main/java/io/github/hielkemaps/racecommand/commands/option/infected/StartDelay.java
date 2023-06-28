package io.github.hielkemaps.racecommand.commands.option.infected;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;

import java.util.List;

public class StartDelay {
    public static void register(List<Argument<?>> arguments) {

        arguments.add(new LiteralArgument("startDelay"));
        arguments.add(new IntegerArgument("seconds", 5));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    int delay = (Integer) args.get(0);
                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race instanceof InfectedRace) {
                        ((InfectedRace) race).setStartDelay(delay);
                        race.sendMessage(Main.PREFIX + "Start delay set to " + delay + " seconds");
                    }
                }).register();
    }
}
