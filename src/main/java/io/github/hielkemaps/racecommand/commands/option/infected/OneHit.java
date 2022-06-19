package io.github.hielkemaps.racecommand.commands.option.infected;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import org.bukkit.ChatColor;

import java.util.List;

public class OneHit {
    public static void register(List<Argument<?>> arguments) {

        arguments.add(new LiteralArgument("oneHit"));
        arguments.add(new BooleanArgument("value"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    boolean value = (Boolean) args[0];

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race instanceof InfectedRace) {
                        InfectedRace iRace = (InfectedRace) race;

                        if (iRace.setOneHit(value)) {
                            if (value)
                                race.sendMessage(Main.PREFIX + "One hit enabled. Zombies now infect villagers in one hit");
                            else
                                race.sendMessage(Main.PREFIX + "One hit disabled. Villagers now require multiple hits to infect");
                        } else {
                            p.sendMessage(Main.PREFIX + ChatColor.RED + "Nothing changed. One hit was already set to " + value);
                        }
                    }
                }).register();
    }
}
