package io.github.hielkemaps.racecommand.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import io.github.hielkemaps.racecommand.Util;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.player.RacePlayer;
import io.github.hielkemaps.racecommand.race.player.types.InfectedRacePlayer;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import org.bukkit.ChatColor;

import java.util.List;

public class Info {

    public static void register(List<Argument<?>> arguments){

        //race info
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race == null) return;

                    String ownerName = race.getName();

                    p.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.RESET + "" + ChatColor.BOLD + " " + ownerName + "'s race " + ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "           ");
                    p.sendMessage("Visibility: " + (race.isPublic() ? ChatColor.GREEN + "public" : ChatColor.RED + "private"));
                    p.sendMessage("Players:");

                    for (RacePlayer racePlayer : race.getPlayers()) {
                        String name = racePlayer.getName();

                        StringBuilder str = new StringBuilder();
                        str.append(ChatColor.GRAY).append("-");

                        //Gold name if finished
                        if (racePlayer.isFinished()) {
                            str.append(ChatColor.GOLD);
                        }

                        str.append(name);

                        //Display time if finished
                        if (racePlayer.isFinished()) {
                            str.append(ChatColor.WHITE).append(" (");
                            str.append(Util.getTimeString(racePlayer.getTime()));
                            str.append(")");
                        }

                        if (racePlayer.isOwner()) {
                            str.append(ChatColor.GREEN).append(" [Owner]");
                        }

                        if (race instanceof InfectedRace) {
                            InfectedRacePlayer infectedRacePlayer = (InfectedRacePlayer) racePlayer;
                            if (infectedRacePlayer.isInfected()) {
                                str.append(ChatColor.GREEN).append(" [Infected]");
                            }

                            if (infectedRacePlayer.isSkeleton()) {
                                str.append(ChatColor.WHITE).append(" [Skeleton]");
                            }
                        }


                        p.sendMessage(str.toString());
                    }
                    p.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + ownerName.replaceAll(".", "  ") + "                                ");
                }).register();
    }
}
