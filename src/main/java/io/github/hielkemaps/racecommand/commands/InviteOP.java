package io.github.hielkemaps.racecommand.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class InviteOP {

    public static void register(List<Argument<?>> arguments){

        //race inviteOP @a - OP only
        arguments.add(new EntitySelectorArgument<Collection<Player>>("players", EntitySelector.MANY_PLAYERS)
                .withPermission(CommandPermission.OP)
                .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                    List<String> names = new ArrayList<>();

                    Race race = RaceManager.getRace(((Player) info.sender()).getUniqueId());
                    if (race == null) return new String[0];

                    //Don't show players that are already in your race
                    for (Player p : players) {
                        if (race.hasPlayer(p.getUniqueId())) continue;
                        names.add(p.getName());
                    }
                    return names.toArray(new String[0]);
                })));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    @SuppressWarnings("unchecked")
                    Collection<Player> invitedPlayers = (Collection<Player>) args[0];
                    boolean onePlayerInvited = invitedPlayers.size() == 1;

                    for (Player invited : invitedPlayers) {

                        //If invite yourself
                        if (invited.getUniqueId().equals(p.getUniqueId())) {
                            if (onePlayerInvited) p.sendMessage(Main.PREFIX + "You can't invite yourself");
                            continue;
                        }

                        Race race = RaceManager.getRace(p.getUniqueId());
                        if (race == null) return;

                        //If player is already in your race
                        if (race.hasPlayer(invited.getUniqueId())) {
                            if (onePlayerInvited) p.sendMessage(Main.PREFIX + "That player is already in your race");
                            continue;
                        }

                        //OPs invitation will always go through, even if already invited

                        race.invitePlayer(invited.getUniqueId());
                        TextComponent msg = new TextComponent(Main.PREFIX + p.getName() + " wants to race! ");
                        TextComponent accept = new TextComponent(ChatColor.GREEN + "[Accept]");
                        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/race join " + p.getName()));
                        msg.addExtra(accept);

                        Objects.requireNonNull(Bukkit.getPlayer(invited.getUniqueId())).spigot().sendMessage(msg);
                        p.sendMessage(Main.PREFIX + "Invited player " + invited.getName());
                    }
                }).register();
    }
}
