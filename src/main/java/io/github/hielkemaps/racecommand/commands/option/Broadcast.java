package io.github.hielkemaps.racecommand.commands.option;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import org.bukkit.ChatColor;

import java.util.List;

public class Broadcast {
    public static void register(List<Argument<?>> arguments) {

        arguments.add(new LiteralArgument("broadcast").withPermission(CommandPermission.OP));
        arguments.add(new BooleanArgument("value"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    boolean value = (boolean) args.get(0);

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race == null) return;

                    if (race.setBroadcast(value)) {
                        if (value) {
                            p.sendMessage(Main.PREFIX + "Enabled broadcasting");
                        } else {
                            p.sendMessage(Main.PREFIX + "Disabled broadcasting");
                        }

                    } else {
                        p.sendMessage(Main.PREFIX + ChatColor.RED + "Nothing changed. Broadcast was already set to " + value);
                    }
                }).register();
    }
}
