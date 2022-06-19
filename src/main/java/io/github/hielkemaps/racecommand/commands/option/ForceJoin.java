package io.github.hielkemaps.racecommand.commands.option;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public class ForceJoin {

    public static void register(List<Argument<?>> arguments) {

        arguments.add(new PlayerArgument("players"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    @SuppressWarnings("unchecked")
                    Collection<Player> players = (Collection<Player>) args[0];
                    boolean onePlayerJoins = players.size() == 1;

                    Race newRace = RaceManager.getRace(p.getUniqueId());
                    if (newRace == null) return;

                    for (Player player : players) {

                        // You can't join your own race
                        if (player.getUniqueId().equals(p.getUniqueId())) continue;

                        PlayerWrapper wPlayer = PlayerManager.getPlayer(player.getUniqueId());

                        //leave old race
                        if (wPlayer.isInRace()) {
                            Race race = RaceManager.getRace(player.getUniqueId());
                            if (race == null) continue;

                            //If player is race owner, disband race
                            //Otherwise leave race
                            if (race.isOwner(player.getUniqueId())) {
                                RaceManager.disbandRace(player.getUniqueId());
                                player.sendMessage(Main.PREFIX + "You have disbanded the race");
                            } else {
                                //If already in the same race, do nothing
                                if (newRace.getOwner().equals(race.getOwner())) {
                                    if (onePlayerJoins)
                                        p.sendMessage(Main.PREFIX + player.getName() + " is already in the race");
                                    continue;
                                }
                                race.leavePlayer(player.getUniqueId());
                                player.sendMessage(Main.PREFIX + "You have left the race");
                            }
                        }

                        newRace.addPlayer(player.getUniqueId());

                        if (newRace.isStarting()) {
                            player.performCommand("restart");
                        }

                        //allow new player even if race has started
                        if (newRace.hasStarted()) {
                            player.performCommand("restart");

                            //+2 ticks
                            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), () -> {
                                newRace.executeStartFunction(player); //run start function 2 ticks later so restart was successful
                                player.addScoreboardTag("inRace");

                                //+2 ticks
                                Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), () -> {
                                    newRace.syncTime(player);
                                    player.teleport(p);
                                }, 2L);
                            }, 2L);
                        }

                        player.sendMessage(Main.PREFIX + "You joined " + p.getName() + "'s race!");
                    }
                }).register();
    }
}
