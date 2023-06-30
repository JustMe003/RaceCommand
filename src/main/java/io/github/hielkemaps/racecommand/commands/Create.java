package io.github.hielkemaps.racecommand.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.RaceMode;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import io.github.hielkemaps.racecommand.race.types.NormalRace;
import io.github.hielkemaps.racecommand.race.types.PvpRace;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class Create {

    public static void register(List<Argument<?>> arguments){

        //race create
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    createRace(RaceMode.normal, p);
                }).register();

        //race create <mode>
        arguments.add(new MultiLiteralArgument(RaceMode.getNames(RaceMode.class)));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    RaceMode mode = RaceMode.valueOf((String) args.get(0));
                    createRace(mode, p);
                }).register();
    }

    private static void createRace(RaceMode mode, Player p) {
        Race race;
        switch (mode) {
            case pvp:
                race = new PvpRace(p.getUniqueId(), p.getName());
                break;
            case infected:
                race = new InfectedRace(p.getUniqueId(), p.getName());
                break;
            default:
                race = new NormalRace(p.getUniqueId(), p.getName());
        }
        RaceManager.addRace(race);

        TextComponent msg = new TextComponent(Main.PREFIX + "Created race! Invite players with ");
        TextComponent click = new TextComponent(ChatColor.WHITE + "/race invite");
        click.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/race invite "));
        msg.addExtra(click);

        p.spigot().sendMessage(msg);
    }
}
