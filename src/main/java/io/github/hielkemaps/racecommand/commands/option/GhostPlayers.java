package io.github.hielkemaps.racecommand.commands.option;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.RaceManager;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Objects;

import static io.github.hielkemaps.racecommand.commands.CommandPredicates.playerInInfectedRace;

public class GhostPlayers {

    public static void register(List<Argument<?>> arguments){

        arguments.add(new LiteralArgument("ghostPlayers").withRequirement(playerInInfectedRace.negate())); //not for infected gamemode
        arguments.add(new BooleanArgument("value"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    boolean value = (boolean) args[0];
                    if (Objects.requireNonNull(RaceManager.getRace(p.getUniqueId())).setGhostPlayers(value)) {

                        if (value) {
                            p.sendMessage(Main.PREFIX + "Players in race will now be see-through");
                        } else {
                            p.sendMessage(Main.PREFIX + "Players in race will no longer be see-through");
                        }
                    } else {
                        p.sendMessage(Main.PREFIX + ChatColor.RED + "Nothing changed. ghostPlayers was already set to " + value);
                    }
                }).register();
    }
}
