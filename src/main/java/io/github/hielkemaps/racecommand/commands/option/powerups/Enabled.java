package io.github.hielkemaps.racecommand.commands.option.powerups;

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

public class Enabled {
    public static void register(List<Argument<?>> arguments) {

        arguments.add(new LiteralArgument("enabled"));
        arguments.add(new BooleanArgument("value"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    boolean value = (Boolean) args[0];

                    Race race = RaceManager.getRace(p.getUniqueId());

                    if (race.setDoPowerUps(value)) {
                        if (value)
                            race.sendMessage(Main.PREFIX + "Enabled powerups");
                        else
                            race.sendMessage(Main.PREFIX + "Disabled powerups");
                    } else {
                        p.sendMessage(Main.PREFIX + ChatColor.RED + "Nothing changed. Powerups were already set to " + value);
                    }
                }).register();
    }
}
