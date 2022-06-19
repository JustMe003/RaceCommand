package io.github.hielkemaps.racecommand.commands.option;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.RaceManager;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Objects;

public class Visibility {

    public static void register(List<Argument<?>> arguments){

        arguments.add(new LiteralArgument("visibility"));
        arguments.add(new MultiLiteralArgument("public", "private"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    String s = (String) args[0];
                    if (Objects.requireNonNull(RaceManager.getRace(p.getUniqueId())).setIsPublic(s.equals("public"))) {
                        p.sendMessage(Main.PREFIX + "Set race visibility to " + s);
                    } else {
                        p.sendMessage(Main.PREFIX + ChatColor.RED + "Nothing changed. Race visibility was already " + s);
                    }
                }).register();
    }
}
