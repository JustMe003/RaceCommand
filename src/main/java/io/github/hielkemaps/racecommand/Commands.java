package io.github.hielkemaps.racecommand;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.RaceMode;
import io.github.hielkemaps.racecommand.race.RacePlayer;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import io.github.hielkemaps.racecommand.race.types.NormalRace;
import io.github.hielkemaps.racecommand.race.types.PvpRace;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Predicate;

public class Commands {

    public Commands() {

        //CREATE NORMAL
        new CommandAPICommand("race")
                .withArguments(new LiteralArgument("create").withRequirement(playerInRace.negate()))
                .executesPlayer((p, args) -> {
                    createRace(RaceMode.normal, p);
                }).register();

        //CREATE
        List<Argument<?>> arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("create").withRequirement(playerInRace.negate()));
        arguments.add(new MultiLiteralArgument("type", RaceMode.getNames(RaceMode.class)));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((sender, args) -> {
                    RaceMode mode = RaceMode.valueOf((String) args.get(0));
                    createRace(mode, sender);
                })
                .executes((sender, args) -> {
                    RaceMode mode = RaceMode.valueOf((String) args.get(0));
                    createRace(mode, sender);
                    Bukkit.getLogger().info("Creating race!");
                })
                .register();

        //START
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("start").withRequirement(playerInRace.and(playerisOP.or(playerIsRaceOwner).and(raceHasStarted.negate().and(raceIsStarting.negate())))));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    Race race = RaceManager.getRace(p);
                    if (race == null) return;

                    race.startCountdown(0);
                }).register();

        //START COUNTDOWN
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("start").withRequirement(playerInRace.and(playerisOP.or(playerIsRaceOwner).and(raceHasStarted.negate().and(raceIsStarting.negate())))));
        arguments.add(new IntegerArgument("countdown", 3, 1000));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    int value = (int) args.get("countdown");

                    Race race = RaceManager.getRace(p);
                    if (race == null) return;

                    race.setCountDown(value);
                    race.startCountdown(0);
                }).register();

        //STOP
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("stop").withRequirement(playerInRace.and(playerisOP.or(playerIsRaceOwner).and(raceHasStarted.or(raceIsStarting)))));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    Race race = RaceManager.getRace(p);
                    if (race == null) return;
                    race.stop();
                }).register();

        //INVITE
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("invite").withRequirement(playerInRace.and(playerisOP.or(playerIsRaceOwner))));
        arguments.add(new PlayerArgument("player").withRequirement(sender -> !sender.isOp()).replaceSuggestions(ArgumentSuggestions.strings(info -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            List<String> names = new ArrayList<>();

            Race race = RaceManager.getRace(((Player) info.sender()));
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
                    Player invited = (Player) args.get(0);

                    //If invite yourself
                    if (invited.getUniqueId().equals(p.getUniqueId())) {
                        p.sendMessage(Main.PREFIX.append(Component.text("You can't invite yourself")));
                        return;
                    }

                    Race race = RaceManager.getRace(p);
                    if (race == null) return;

                    //If player is already in race
                    if (race.hasPlayer(invited.getUniqueId())) {
                        p.sendMessage(Main.PREFIX.append(Component.text("That player is already in your race")));
                        return;
                    }

                    //If already invited
                    if (race.hasInvited(invited)) {
                        p.sendMessage(Main.PREFIX.append(Component.text("You have already invited " + invited.getName())));
                        return;
                    }

                    race.invitePlayer(invited.getUniqueId());
                    p.sendMessage(Main.PREFIX.append(Component.text("Invited player " + invited.getName())));
                }).register();


        //INVITE ALL - OP ONLY
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("invite").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new EntitySelectorArgument.ManyPlayers("players").withPermission(CommandPermission.OP).replaceSuggestions(ArgumentSuggestions.strings(info -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            List<String> names = new ArrayList<>();

            Race race = RaceManager.getRace(((Player) info.sender()));
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
                    Collection<Player> invitedPlayers = (Collection<Player>) args.get(0);
                    boolean onePlayerInvited = invitedPlayers.size() == 1;

                    for (Player invited : invitedPlayers) {

                        //If invite yourself
                        if (invited.getUniqueId().equals(p.getUniqueId())) {
                            if (onePlayerInvited) {
                                p.sendMessage(Main.PREFIX.append(Component.text("You can't invite yourself")));
                            }
                            continue;
                        }

                        Race race = RaceManager.getRace(p);
                        if (race == null) return;

                        //If player is already in your race
                        if (race.hasPlayer(invited.getUniqueId())) {
                            if (onePlayerInvited) {
                                p.sendMessage(Main.PREFIX.append(Component.text("That player is already in your race")));
                            }
                            continue;
                        }

                        //OPs invitation will always go through, even if already invited
                        race.invitePlayer(invited.getUniqueId());
                        p.sendMessage(Main.PREFIX.append(Component.text("Invited player " + invited.getName())));
                    }
                }).register();

        //JOIN
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("join").withRequirement(playerHasJoinableRaces));
        arguments.add(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings((info) -> PlayerManager.getPlayer(((Player) info.sender()).getUniqueId()).getJoinableRaces())));
        new CommandAPICommand("race").withArguments(arguments)
                .withOptionalArguments(new BooleanArgument("pay_wager")).withUsage("/race join [player]")
                .executesPlayer((p, args) -> {
            String raceName = (String) args.get(0);
            Race race = RaceManager.getRace(raceName);
            if (race == null) {
                throw CommandAPI.failWithString("Race not found");
            }

            //If joining own races
            if (race.hasPlayer(p.getUniqueId())) {
                p.sendMessage(Main.PREFIX.append(Component.text("You already joined this race")));
                return;
            }

            PlayerWrapper wPlayer = PlayerManager.getPlayer(p.getUniqueId());

            if (race.isPublic() || race.hasInvited(p)) {

                if (race.hasStarted()) {
                    p.sendMessage(Main.PREFIX.append(Component.text("Can't join race: This race has already started")));
                    return;
                }

                int minWager = race.getMinimumWager();
                Boolean payWager = (Boolean) args.get("pay_wager");
                if (minWager > 0) {
                    // Has minimum wager

                    if (minWager > wPlayer.getPlayerPoints() && !wPlayer.isInRace()) {
                        // Player does not have enough points
                        p.sendMessage(Component.text("This race requires you to pay a minimum wager of " + minWager + " parcoins, but you don't have enough parcoins. Please ask the race creator to lower the entry cost or get more parcoins yourself", NamedTextColor.YELLOW));
                        return;
                    } else if (wPlayer.isInRace()) {
                        // Maybe player has wager in current race, that could be enough to pay the minimum wager for this race
                        Race raceWithPlayer = RaceManager.getRace(p.getUniqueId());

                        if (raceWithPlayer == null || raceWithPlayer.hasStarted()) {
                            // no race or race has started means not enough parcoins to join
                            p.sendMessage(Component.text("This race requires you to pay a minimum wager of " + minWager + " parcoins, but you don't have enough parcoins. Please ask the race creator to lower the entry cost or get more parcoins yourself", NamedTextColor.YELLOW));
                            return;
                        }

                        RacePlayer racePlayer = raceWithPlayer.getRacePlayer(p.getUniqueId());

                        if (racePlayer == null) {
                            // Invalid state, but still has not enough parcoins to join
                            p.sendMessage(Component.text("This race requires you to pay a minimum wager of " + minWager + " parcoins, but you don't have enough parcoins. Please ask the race creator to lower the entry cost or get more parcoins yourself", NamedTextColor.YELLOW));
                            return;
                        }

                        if (!racePlayer.hasWager() || racePlayer.getTotalWager() < minWager) {
                            // Either has no wager in current race, so not enough parcoins
                            // Or the total wager in current race is not enough to pay the minimum wager for this race
                            p.sendMessage(Component.text("This race requires you to pay a minimum wager of " + minWager + " parcoins, but you don't have enough parcoins. Please ask the race creator to lower the entry cost or get more parcoins yourself", NamedTextColor.YELLOW));
                            return;
                        }
                    }

                    if (payWager == null || !payWager) {
                        // pay_wager was not set or was false
                        // Player can pay minimum wager
                        Component msg = Main.PREFIX
                                .append(Component.text("This race requires you to pay a minimum wager of " + race.getMinimumWager() + " parcoins to join. Are you sure you want to join? "))
                                .append(Component.text("[Yes]", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/race join " + raceName + " " + true)));
                        p.sendMessage(msg);
                        return;
                    }
                }

                //If player in existing race, leave
                if (wPlayer.isInRace()) {
                    Race raceToLeave = RaceManager.getRace(p);

                    if (raceToLeave != null) {
                        UUID raceOwner = raceToLeave.getOwner();

                        if (raceOwner.equals(p.getUniqueId())) {
                            RaceManager.disbandRace(raceToLeave);
                        } else {
                            raceToLeave.removePlayer(p);
                        }
                    }
                }

                //Join race
                if (!wPlayer.acceptInvite(raceName)) {
                    throw CommandAPI.failWithString("Could not join race");
                } else {
                    if (payWager != null && payWager) {
                        // pay_wager was true
                        wPlayer.takePlayerPoints(minWager);

                        Component message = Main.PREFIX
                                .append(Component.text("You payed ", NamedTextColor.GRAY))
                                .append(Component.text(minWager, NamedTextColor.YELLOW))
                                .append(Component.text(" Parcoins", NamedTextColor.GRAY));

                        p.sendMessage(message);
                    }
                }
            }
        }).register();

        //DISBAND
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("disband").withRequirement(playerInRace.and(playerisOP.or(playerIsRaceOwner))));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    RaceManager.disbandRace(RaceManager.getRace(p));
                }).register();

        //LEAVE
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("leave").withRequirement(playerInRace.and(playerisOP.or((playerIsRaceOwner.negate())))));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    Race race = RaceManager.getRace(p);
                    if (race != null) {
                        race.removePlayer(p);
                    }
                }).register();

        //INFO
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("info").withRequirement(playerInRace));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    Race race = RaceManager.getRace(p);
                    if (race == null) return;

                    String ownerName = race.getName();

                    if (race.isEvent()) {
                        p.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "             " + ChatColor.RESET + "" + ChatColor.BOLD + " Event race " + ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "             ");
                    } else {
                        p.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.RESET + "" + ChatColor.BOLD + " " + ownerName + "'s race " + ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "           ");
                    }
                    p.sendMessage("Visibility: " + (race.isPublic() ? ChatColor.GREEN + "Public" : ChatColor.RED + "Private"));
                    p.sendMessage("Type: " + race.getTypeString());
                    if (race.getMinimumWager() > 0) {
                        p.sendMessage("Minimum wager: " + ChatColor.GRAY + race.getMinimumWager());
                    }
                    if (race.getTotalPrizePool() > 0) {
                        p.sendMessage("Prize pool: " + ChatColor.GOLD + race.getTotalPrizePool());
                    }
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

                        if (racePlayer.isInfected()) {
                            str.append(ChatColor.GREEN).append(" [Infected]");
                        }

                        if (racePlayer.isSkeleton()) {
                            str.append(ChatColor.WHITE).append(" [Skeleton]");
                        }

                        p.sendMessage(str.toString());
                    }
                    p.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + ownerName.replaceAll(".", "  ") + "                                ");
                }).register();

        //KICK
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("kick").withRequirement(playerInRace.and(playerIsRaceOwner).and(playerToKick)));
        arguments.add(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings((info) ->
        {
            Player p = (Player) info.sender();
            List<String> names = new ArrayList<>();
            Race race = RaceManager.getRace(p);
            if (race == null) return new String[0];

            List<RacePlayer> players = race.getPlayers();
            for (RacePlayer racePlayer : players) {
                UUID uuid = racePlayer.getUniqueId();
                if (uuid.equals(p.getUniqueId())) continue;
                names.add(Bukkit.getOfflinePlayer(uuid).getName());
            }

            return names.toArray(new String[0]);
        })));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    String playerName = (String) args.get(0);

                    UUID toKick = Bukkit.getOfflinePlayer(playerName).getUniqueId();

                    if (p.getUniqueId().equals(toKick)) {
                        p.sendMessage(Main.PREFIX.append(Component.text("You can't kick yourself")));
                        return;
                    }
                    Race race = RaceManager.getRace(p);
                    race.kickPlayer(toKick);
                }).register();

        //Option visibility
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new LiteralArgument("visibility"));
        arguments.add(new MultiLiteralArgument("public", "private"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    String s = (String) args.get(0);
                    if (Objects.requireNonNull(RaceManager.getRace(p)).setIsPublic(s.equals("public"))) {
                        p.sendMessage(Main.PREFIX.append(Component.text("Set race visibility to " + s)));
                    } else {
                        p.sendMessage(Main.PREFIX.append(Component.text("Nothing changed. Race visibility was already " + s, NamedTextColor.RED)));
                    }
                }).register();

        //Option ghost players
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new LiteralArgument("ghostPlayers").withRequirement(playerInInfectedRace.negate())); //not for infected gamemode
        arguments.add(new BooleanArgument("value"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    boolean value = (boolean) args.get(0);
                    if (RaceManager.getRace(p).setGhostPlayers(value)) {

                        if (value) {
                            p.sendMessage(Main.PREFIX.append(Component.text("Players in race will now be see-through")));
                        } else {
                            p.sendMessage(Main.PREFIX.append(Component.text("Players in race will no longer be see-through")));
                        }
                    } else {
                        p.sendMessage(Main.PREFIX.append(Component.text("Nothing changed. ghostPlayers was already set to " + value, NamedTextColor.RED)));
                    }
                }).register();


        //Option infected player
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new LiteralArgument("firstInfected").withRequirement(playerInInfectedRace));
        arguments.add(new EntitySelectorArgument.OnePlayer("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            List<String> names = new ArrayList<>();

            Race race = RaceManager.getRace(((Player) info.sender()));
            if (race == null) return new String[0];

            //Only show players that are in your race
            for (Player p : players) {
                if (race.hasPlayer(p.getUniqueId())) {
                    names.add(p.getName());
                }
            }
            return names.toArray(new String[0]);
        })));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    Player player = (Player) args.get(0);
                    Race race = RaceManager.getRace(p);
                    if (race instanceof InfectedRace) {
                        RacePlayer racePlayer = race.getRacePlayer(player.getUniqueId());
                        if (racePlayer != null) {
                            ((InfectedRace) race).setFirstInfected(racePlayer);
                            p.sendMessage(Main.PREFIX.append(Component.text(player.getName() + " will be the first infected")));
                        } else {
                            throw CommandAPI.failWithString("Could not find player " + player.getName());
                        }
                    }
                }).register();

        //Option infected player
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new LiteralArgument("infectedDelay").withRequirement(playerInInfectedRace));
        arguments.add(new IntegerArgument("seconds", 5));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    int delay = (Integer) args.get(0);
                    Race race = RaceManager.getRace(p);
                    if (race instanceof InfectedRace) {
                        ((InfectedRace) race).setInfectedDelay(delay);
                        race.sendMessage(Main.PREFIX.append(Component.text("Infected delay set to " + delay + " seconds")));
                    }
                }).register();

        //Option broadcast - OP ONLY
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new LiteralArgument("broadcast").withPermission(CommandPermission.OP));
        arguments.add(new BooleanArgument("value"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    boolean value = (boolean) args.get(0);

                    Race race = RaceManager.getRace(p);
                    if (race == null) return;

                    if (race.setBroadcast(value)) {
                        if (value) {
                            p.sendMessage(Main.PREFIX.append(Component.text("Enabled broadcasting")));
                        } else {
                            p.sendMessage(Main.PREFIX.append(Component.text("Disabled broadcasting")));
                        }

                    } else {
                        p.sendMessage(Main.PREFIX.append(Component.text("Nothing changed. Broadcast was already set to " + value, NamedTextColor.RED)));
                    }
                }).register();

        //Option set minimum wager
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new LiteralArgument("wager").withRequirement(playerInRace.and(Predicate.not(playerInInfectedRace)))); // Don't allow wagers in infected races (Idk how I would hand out the parcoins xD)
        arguments.add(new IntegerArgument("minimum", 10));
        new CommandAPICommand("race")
                .withArguments((arguments))
                .executesPlayer((p, args) -> {
                    try {
                        int wager = (int) args.get(0);

                        Race race = RaceManager.getRace(p);
                        if (race == null) return;

                        // Only allow wager to be set when no other player are in the race
                        if (race.getPlayers().size() != 1) {
                            p.sendMessage(Main.PREFIX.append(Component.text("You can only set the minimum wage if no other players have joined the race", NamedTextColor.RED)));
                            return;
                        }

                        int oldWager = race.getMinimumWager();

                        PlayerWrapper wPlayer = PlayerManager.getPlayer(p.getUniqueId());

                        if (wager > wPlayer.getPlayerPoints() + oldWager) {
                            p.sendMessage(Main.PREFIX.append(Component.text("You must be able to pay the minimum wager yourself!", NamedTextColor.RED)));
                            return;
                        }

                        if (race.setMinimumWager(wager)) {
                            if (oldWager > wager) {
                                // Don't want the rainbow text flashing on the screen, just for giving some of the parcoins back :)
                                race.getPlayers().get(0).givePointsSilently(oldWager - wager);
                                p.sendMessage(Main.PREFIX.append(Component.text("Added ", NamedTextColor.GRAY))
                                        .append(Component.text(oldWager - wager, NamedTextColor.YELLOW))
                                        .append(Component.text(" parcoins", NamedTextColor.GRAY)));
                            } else {
                                race.getPlayers().get(0).takePoints(wager - oldWager);
                            }
                            p.sendMessage(Main.PREFIX.append(Component.text("Minimum wager set to " + wager)));
                        } else {
                            // no change, no need to remove / add points
                            p.sendMessage(Main.PREFIX.append(Component.text("Minimum wager was already set to " + wager, NamedTextColor.RED)));
                        }

                        race.getPlayers().get(0).setNewWager(wager);
                    } catch(NullPointerException e) {
                        p.sendMessage(Main.PREFIX.append(Component.text("You must enter a number higher than 10 as the minimum wager", NamedTextColor.RED)));
                    }
                }).register();

        //wager increase
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("wager").withRequirement(playerInRace));
        arguments.add(new LiteralArgument("increase"));
        arguments.add(new IntegerArgument("amount", 1));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                   Integer wager = (Integer) args.get(0);

                   if (wager == null) return;

                   Race race = RaceManager.getRace(p);

                   if (race == null) return;

                   RacePlayer player = race.getRacePlayer(p.getUniqueId());
                   if (player.getWrapper().getPlayerPoints() < wager) {
                       p.sendMessage(Component.text("You don't have enough parcoins!", NamedTextColor.RED));
                   } else {
                       player.increaseAdditionalWager(wager);
                       player.takePointsSilently(wager);
                       Component msg = Component.text("Increased wager with ", NamedTextColor.GRAY)
                                            .append(Component.text(wager, NamedTextColor.GOLD))
                                            .append(Component.text("!", NamedTextColor.GRAY));

                       p.sendMessage(msg);
                   }
                }).register();

        //wager decrease
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("wager").withRequirement(playerInRace));
        arguments.add(new LiteralArgument("decrease"));
        arguments.add(new IntegerArgument("amount", 1));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    Integer wager = (Integer) args.get(0);

                    if (wager == null) return;

                    Race race = RaceManager.getRace(p);

                    if (race == null) return;

                    RacePlayer player = race.getRacePlayer(p.getUniqueId());
                    if (player.getAdditionalWager() < wager) {
                        p.sendMessage(Component.text("You can at most remove " + player.getAdditionalWager() + " from your wagers!", NamedTextColor.RED));
                    } else {
                        player.increaseAdditionalWager(-wager);
                        player.givePointsSilently(wager);
                        Component msg = Component.text("Decreased wager with ", NamedTextColor.GRAY)
                                .append(Component.text(wager, NamedTextColor.GOLD))
                                .append(Component.text("!", NamedTextColor.GRAY));

                        p.sendMessage(msg);
                    }
                }).register();

        //Force join - OP ONLY
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("forcejoin").withRequirement(playerInRace.and(playerisOP.or(playerIsRaceOwner))).withPermission(CommandPermission.OP));
        arguments.add(new EntitySelectorArgument.ManyEntities("players"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    @SuppressWarnings("unchecked")
                    Collection<Player> players = (Collection<Player>) args.get(0);
                    boolean onePlayerJoins = players.size() == 1;

                    Race newRace = RaceManager.getRace(p);
                    if (newRace == null) return;

                    for (Player player : players) {

                        // You can't join your own race
                        if (player.getUniqueId().equals(p.getUniqueId())) continue;

                        PlayerWrapper wPlayer = PlayerManager.getPlayer(player.getUniqueId());

                        //leave old race
                        if (wPlayer.isInRace()) {
                            Race race = RaceManager.getRace(player);
                            if (race == null) continue;

                            //If player is race owner, disband race
                            //Otherwise leave race
                            if (race.isOwner(player.getUniqueId())) {
                                RaceManager.disbandRace(race);
                            } else {
                                //If already in the same race, do nothing
                                if (newRace.getOwner().equals(race.getOwner())) {
                                    if (onePlayerJoins) {
                                        p.sendMessage(Main.PREFIX.append(Component.text(player.getName() + " is already in the race")));
                                    }
                                    continue;
                                }
                                race.removePlayer(player);
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

                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + player.getName() + " at @s run function time:start");
                                player.addScoreboardTag("inRace");

                                //+2 ticks
                                Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), () -> {
                                    newRace.syncTime(player);
                                    player.teleport(p);
                                }, 2L);
                            }, 2L);
                        }
                    }
                }).register();
    }

    Predicate<CommandSender> playerInRace = sender -> {
        if (!(sender instanceof Player)) return false;

        return PlayerManager.getPlayer(((Player) sender).getUniqueId()).isInRace();
    };

    Predicate<CommandSender> playerHasJoinableRaces = sender -> {
        if (!(sender instanceof Player)) return true;

        return PlayerManager.getPlayer(((Player) sender).getUniqueId()).hasJoinableRace();
    };

    Predicate<CommandSender> playerInInfectedRace = sender -> {
        if (!(sender instanceof Player)) return true;

        return PlayerManager.getPlayer(((Player) sender).getUniqueId()).isInInfectedRace();
    };

    Predicate<CommandSender> playerisOP = sender -> {
        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;
        return player.isOp();
    };

    Predicate<CommandSender> playerIsRaceOwner = sender -> {
        if (!(sender instanceof Player)) return false;

        Player player = (Player) sender;
        Race race = RaceManager.getRace(player);
        if (race == null) return false;

        return race.isOwner(player.getUniqueId());
    };

    Predicate<CommandSender> raceHasStarted = sender -> {
        if (!(sender instanceof Player)) return false;

        Player player = (Player) sender;
        Race race = RaceManager.getRace(player);
        if (race == null) return false;

        return race.hasStarted();
    };

    Predicate<CommandSender> raceIsStarting = sender -> {
        if (!(sender instanceof Player)) return false;

        Player player = (Player) sender;
        Race race = RaceManager.getRace(player);
        if (race == null) return false;

        return race.isStarting();
    };

    Predicate<CommandSender> playerToKick = sender -> {
        if (!(sender instanceof Player)) return false;

        Player player = (Player) sender;
        Race race = RaceManager.getRace(player);
        if (race == null) return false;

        return race.getPlayers().size() > 1;
    };

    public void createRace(RaceMode mode, CommandSender sender) {
        Race race;
        switch (mode) {
            case pvp:
                race = new PvpRace(sender);
                break;
            case infected:
                race = new InfectedRace(sender);
                break;
            default:
                race = new NormalRace(sender);
        }
        RaceManager.addRace(race);
    }
}
