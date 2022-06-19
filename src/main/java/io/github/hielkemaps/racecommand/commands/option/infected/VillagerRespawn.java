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

public class VillagerRespawn {

    public static void register(List<Argument<?>> arguments) {

        arguments.add(new LiteralArgument("villagerRespawn"));
        arguments.add(new BooleanArgument("value"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    boolean value = (Boolean) args[0];

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race instanceof InfectedRace) {
                        InfectedRace iRace = (InfectedRace) race;

                        if (iRace.setVillagerRespawn(value)) {
                            if (value)
                                race.sendMessage(Main.PREFIX + "Enabled villager respawn");
                            else
                                race.sendMessage(Main.PREFIX + "Disabled villager respawn. Death will infect villagers");
                        } else {
                            p.sendMessage(Main.PREFIX + ChatColor.RED + "Nothing changed. Villager respawn was already set to " + value);
                        }
                    }
                }).register();
    }
}
