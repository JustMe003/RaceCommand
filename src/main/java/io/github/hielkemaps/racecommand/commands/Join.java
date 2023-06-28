package io.github.hielkemaps.racecommand.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class Join {

    public static void register(List<Argument<?>> arguments){

        //race join <player>
        arguments.add(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> PlayerManager.getPlayer(((Player) info.sender()).getUniqueId()).getJoinableRaces())));
        new CommandAPICommand("race").withArguments(arguments).executesPlayer((p, args) -> {
            String playerName = (String) args.get(0);
            //noinspection deprecation
            UUID uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();

            Race race = RaceManager.getRace(uuid);
            if (race == null) {
                throw CommandAPI.failWithString("Race not found");
            }

            //If joining own race
            if (race.hasPlayer(p.getUniqueId())) {
                p.sendMessage(Main.PREFIX + "You already joined " + playerName + "'s race");
                return;
            }

            PlayerWrapper wPlayer = PlayerManager.getPlayer(p.getUniqueId());

            if (race.isPublic() || race.hasInvited(p)) {

                if (race.hasStarted()) {
                    p.sendMessage(Main.PREFIX + "Can't join race: This race has already started");
                    return;
                }

                //If player in existing race
                if (wPlayer.isInRace()) {
                    Race raceToLeave = RaceManager.getRace(p.getUniqueId());

                    if (raceToLeave != null) {
                        UUID raceOwner = raceToLeave.getOwner();

                        if (raceOwner.equals(p.getUniqueId())) {
                            RaceManager.disbandRace(p.getUniqueId());
                            p.sendMessage(Main.PREFIX + "You have disbanded the race");
                        } else {
                            raceToLeave.leavePlayer(p.getUniqueId());
                            p.sendMessage(Main.PREFIX + "You have left " + raceToLeave.getName() + "'s race");
                        }
                    }
                }

                //Join race
                if (wPlayer.acceptInvite(uuid)) {
                    p.sendMessage(Main.PREFIX + "You joined " + playerName + "'s race!");
                } else {
                    throw CommandAPI.failWithString("Could not join race");
                }
            }
        }).register();
    }
}
