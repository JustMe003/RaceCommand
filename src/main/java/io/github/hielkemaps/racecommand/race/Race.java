package io.github.hielkemaps.racecommand.race;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.hielkeapi.HielkeAPI;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.Util;
import io.github.hielkemaps.racecommand.util.TimeConverter;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Race {

    private final UUID id = UUID.randomUUID();
    private final String name;
    protected UUID owner = null;
    protected final List<RacePlayer> players = new ArrayList<>();
    private boolean isStarting = false;
    private boolean hasStarted = false;
    private final Set<UUID> InvitedPlayers = new HashSet<>();
    protected String type;
    protected String formattedType;
    private int place = 1;

    //options
    private boolean isPublic = false;
    private int countDown = 5;
    private boolean broadcast = false;
    private boolean ghostPlayers = false;

    private boolean isEvent = false;

    //tasks
    private BukkitTask countDownTask;
    private BukkitTask countDownStopTask;
    private BukkitTask playingTask;
    private final int[] prizes = {0, 0, 0, 0};
    private AtomicInteger currentCountdown = new AtomicInteger(100000);

    public Race(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            this.name = sender.getName();
            this.owner = player.getUniqueId();

            RacePlayer racePlayer = new RacePlayer(this, owner);
            players.add(racePlayer);
            onRacePlayerJoin(racePlayer);

            PlayerWrapper pw = PlayerManager.getPlayer(owner);
            pw.setInRace(true);

            Component msg = Main.PREFIX
                    .append(Component.text("Created race! Invite players with "))
                    .append(Component.text("/race invite", NamedTextColor.WHITE).clickEvent(ClickEvent.suggestCommand("/race invite ")).hoverEvent(Component.text("Click to suggest!")));
            player.sendMessage(msg);
            return;
        }

        //Races created by console or command block
        this.isEvent = true;
        this.name = "Event";
        setIsPublic(true);

        // Invite all players in server
        Bukkit.getOnlinePlayers().forEach(player -> invitePlayer(player.getUniqueId()));
    }

    public void start() {
        place = 1;
        isStarting = true;

        sendMessage(Main.PREFIX.append(Component.text("Starting race...")));

        //Teleport to start
        if (countDown < 10) {
            for (RacePlayer player : players) {
                if (player.getPlayer() != null) {
                    player.getPlayer().performCommand("restart");
                }
            }
        }

        //Countdown task
        countDownTask = getCountDownTask();

        //Stop countdown task
        countDownStopTask = getStopTask();

        playingTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::tick, countDown * 20L, 1);
        updateRequirements();
    }

    private BukkitTask getCountDownTask() {
        currentCountdown = new AtomicInteger(countDown);

        return Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            for (RacePlayer racePlayer : players) {
                Player player = racePlayer.getPlayer();
                if (player == null) continue;

                TextColor color = NamedTextColor.GREEN;
                if (currentCountdown.get() == 1) color = NamedTextColor.RED;
                else if (currentCountdown.get() == 2) color = NamedTextColor.GOLD;
                else if (currentCountdown.get() == 3) color = NamedTextColor.YELLOW;

                //Teleport to start
                if (currentCountdown.get() == 10) {
                    player.performCommand("restart");
                }

                if (currentCountdown.get() <= 10) {
                    //Invisible last 10 seconds
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, true, false, false));

                    Times times = Times.times(Duration.ofMillis(100), Duration.ofMillis(900), Duration.ofMillis(100));
                    player.showTitle(Title.title(Component.empty(), Component.text(currentCountdown.toString(), color, TextDecoration.BOLD), times));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1);
                } else {
                    Times times = Times.times(Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(0));
                    player.showTitle(Title.title(Component.text("Race starting in", NamedTextColor.YELLOW), Component.text(TimeConverter.convertSecondsToTimeString(currentCountdown.get()), color, TextDecoration.BOLD), times));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.1f, 1f);
                }
            }
            currentCountdown.getAndDecrement();
        }, 0, 20);
    }

    private BukkitTask getStopTask() {
        return Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            countDownTask.cancel();

            for (RacePlayer racePlayer : getOnlinePlayers()) {
                Player player = racePlayer.getPlayer();
                player.addScoreboardTag("inRace");
                executeStartFunction(player);

                isStarting = false;
                hasStarted = true;
            }
            onCountdownFinish();

            sendMessage(Main.PREFIX.append(Component.text("Race has started")));
        }, 20L * countDown);
    }

    /**
     * Runs every tick if the game has started
     */
    private void tick() {
        onTick();

        //stop race if everyone has finished
        if (players.stream().allMatch(RacePlayer::isFinished)) {
            stop();
            sendMessage(Main.PREFIX.append(Component.text("Race has ended")));
        }

        for (RacePlayer racePlayer : players) {
            if (racePlayer.isFinished()) continue;

            Player player = racePlayer.getPlayer();
            if (player == null) continue;

            //No spectators in race
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.ADVENTURE);
            }

            if (ghostPlayers) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 50, 0, true, false, false));
            }

            Team team = PlayerManager.getPlayer(racePlayer.getUniqueId()).getTeam();
            if (team.getName().equals("finished")) {
                setIsFinished(racePlayer);
            }
        }
    }

    /**
     * Called when a player has finished
     *
     * @param racePlayer the player that finished
     */
    private void setIsFinished(RacePlayer racePlayer) {

        Player player = racePlayer.getPlayer();

        //Make player visible
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        //Get finish time
        int time = -1;
        ScoreboardManager m = Bukkit.getScoreboardManager();
        Objective timeObjective = m.getMainScoreboard().getObjective("time");
        if (timeObjective != null) {
            Score score = timeObjective.getScore(player.getName());
            if (score.isScoreSet()) {
                time = score.getScore();
            }
        }

        onPlayerFinish(racePlayer, place, time);
        racePlayer.setFinished(true, place, time);
        HielkeAPI.onPlayerFinishedRace(player, type, place, time, isEvent);

        place++;
    }

    /**
     * Update command requirements
     */
    private void updateRequirements() {
        //If race is public, update everyone
        if (isPublic) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                CommandAPI.updateRequirements(onlinePlayer);
            }
        } else {
            //Otherwise, update invited
            for (UUID uuid : InvitedPlayers) {
                PlayerManager.getPlayer(uuid).updateRequirements();
            }
            //Update start/stop requirement for owner only
            if (owner != null) {
                PlayerManager.getPlayer(owner).updateRequirements();
            }
        }
    }

    public void stop() {
        for (RacePlayer racePlayer : players) {
            Player player = racePlayer.getPlayer();
            if (player != null) player.removeScoreboardTag("inRace");
        }

        onRaceStop();
        cancelTasks();
        isStarting = false;
        hasStarted = false;
        updateRequirements();
    }

    public void setCountDown(int value) {
        countDown = value;
    }

    public void addPlayer(UUID uuid) {
        Player addedPlayer = Bukkit.getPlayer(uuid);
        if (addedPlayer == null) return;

        Component joinMsg = Main.PREFIX
                .append(Component.text("+ ", NamedTextColor.GREEN))
                .append(Component.text(addedPlayer.getName(), NamedTextColor.GRAY));
        sendMessageToRaceMembers(joinMsg);
        RacePlayer newPlayer = new RacePlayer(this, uuid);
        players.add(newPlayer);

        PlayerWrapper pw = PlayerManager.getPlayer(uuid);
        pw.setInRace(true);

        //If joined during countdown, tp to start
        if (isStarting) {
            addedPlayer.performCommand("restart");
        }

        if (owner != null) {
            PlayerManager.getPlayer(owner).updateRequirements();
        }
        onRacePlayerJoin(newPlayer);

        String text = isEvent ? "You joined the race!" : "You joined " + name + "'s race!";
        pw.sendMessage(Main.PREFIX.append(Component.text(text)));
    }

    public boolean hasPlayer(UUID uuid) {
        return getRacePlayer(uuid) != null;
    }

    public void leavePlayer(Player player) {
        removePlayer(player.getUniqueId());

        String text = isEvent ? "You have left the race" : "You have left " + name + "'s race";
        player.sendMessage(Main.PREFIX.append(Component.text(text)));
    }

    public void kickPlayer(UUID uuid) {
        removePlayer(uuid);

        OfflinePlayer kickedPlayer = Bukkit.getOfflinePlayer(uuid);
        if (kickedPlayer.getPlayer() != null) {
            kickedPlayer.getPlayer().sendMessage(Main.PREFIX.append(Component.text("You have been kicked from the race")));
        }
    }

    private void removePlayer(UUID uuid) {
        RacePlayer racePlayer = getRacePlayer(uuid);
        onPlayerLeave(racePlayer);
        players.remove(racePlayer);

        Component leaveMessage = Main.PREFIX
                .append(Component.text("- ", NamedTextColor.RED))
                .append(Component.text(racePlayer.getName(), NamedTextColor.GRAY));
        sendMessageToRaceMembers(leaveMessage);

        PlayerWrapper pw = PlayerManager.getPlayer(uuid);
        pw.setInRace(false);
        if (owner != null) {
            PlayerManager.getPlayer(owner).updateRequirements();
        }
    }

    protected void disband() {
        cancelTasks();

        //get race off public visibility
        setIsPublic(false);

        //Clear outgoing invites
        getInvitedPlayers().forEach(uuid -> {
            PlayerWrapper wPlayer = PlayerManager.getPlayer(uuid);
            wPlayer.removeInvite(name);
        });

        for (RacePlayer racePlayer : players) {
            PlayerWrapper pw = PlayerManager.getPlayer(racePlayer.getUniqueId());
            pw.setInRace(false);

            Player player = racePlayer.getPlayer();
            if (player == null) continue; //we can do nothing with offline players

            if (racePlayer.getUniqueId().equals(owner)) {
                player.sendMessage(Main.PREFIX.append(Component.text("You have disbanded the race")));
            } else {
                player.sendMessage(Main.PREFIX.append(Component.text("The race has been disbanded")));
            }
        }
    }

    private void cancelTasks() {
        if (countDownTask != null) countDownTask.cancel();
        if (countDownStopTask != null) countDownStopTask.cancel();
        if (playingTask != null) playingTask.cancel();
        onCancelTasks();
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean setIsPublic(boolean value) {
        if (value == isPublic) return false;
        this.isPublic = value;

        updateRequirements();
        return true;
    }

    public boolean setBroadcast(boolean value) {
        if (value == broadcast) return false;

        broadcast = value;
        return true;
    }

    public boolean isStarting() {
        return isStarting;
    }

    public int getCountDown() {
        return currentCountdown.get();
    }

    public boolean hasStarted() {
        return hasStarted;
    }

    public void invitePlayer(UUID invited) {
        InvitedPlayers.add(invited);
        PlayerManager.getPlayer(invited).receiveInvite(this);
    }

    public void removeInvited(UUID invited) {
        InvitedPlayers.remove(invited);
    }

    public boolean hasInvited(Player p) {
        return InvitedPlayers.contains(p.getUniqueId());
    }

    public Set<UUID> getInvitedPlayers() {
        return InvitedPlayers;
    }

    public void sendMessage(Component message) {
        if (broadcast) {
            Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
        } else {
            sendMessageToRaceMembers(message);
        }
    }

    public void sendMessageToRaceMembers(Component message) {
        for (RacePlayer racePlayer : players) {
            Player p = racePlayer.getPlayer();
            if (p != null) p.sendMessage(message);
        }
    }

    public boolean setGhostPlayers(boolean value) {
        if (value == ghostPlayers) return false;

        ghostPlayers = value;
        return true;
    }

    public void printResults() {
        sendMessage(Component.empty()
                .append(Component.text("            ", NamedTextColor.GOLD, TextDecoration.STRIKETHROUGH))
                .append(Component.text(" Results ", NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text("            ", NamedTextColor.GOLD, TextDecoration.STRIKETHROUGH)));

        players.sort(Comparator.comparing(RacePlayer::getPlace));

        for (RacePlayer player : players) {
            sendMessage(player.getResult());
        }
        sendMessage(Component.text("                                     ", NamedTextColor.GOLD, TextDecoration.STRIKETHROUGH));
    }

    /**
     * @param excludedPlayer player in race whose time won't be picked
     * @return time objective score for all players in race, -1 if not found
     */
    public int getCurrentObjective(UUID excludedPlayer, String name) {
        int time = -1;

        Scoreboard s = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective timeObj = s.getObjective(name);
        if (timeObj == null) return time;

        for (RacePlayer racePlayer : players) {
            if (racePlayer.getUniqueId().equals(excludedPlayer)) continue;

            //if player is online
            Player player = racePlayer.getPlayer();
            if (player != null) {
                time = timeObj.getScore(player.getName()).getScore(); //we found our time!
                break;
            }
        }

        return time;
    }

    public int getOnlinePlayerCount() {
        int count = 0;
        for (RacePlayer player : players) {
            if (player.getPlayer() != null) count++;
        }
        return count;
    }

    public List<RacePlayer> getOnlinePlayers() {
        List<RacePlayer> result = new ArrayList<>();

        for (RacePlayer player : players) {
            if (player.isOnline()) {
                result.add(player);
            }
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }

    /**
     * Syncs a player's time and time_tick values with the rest of the race
     *
     * @param player player which scoreboard values will be updated
     */
    public void syncTime(Player player) {
        UUID uuid = player.getUniqueId();

        int timeToSet = getCurrentObjective(uuid, "time");
        int ticksToSet = getCurrentObjective(uuid, "time_tick");

        // should never happen
        if (timeToSet == -1 || ticksToSet == -1) {
            Bukkit.getLogger().warning("[Race] OnPlayerJoin objective result is -1, something is wrong!");
            return;
        }

        Scoreboard s = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective timeObj = s.getObjective("time");
        if (timeObj != null) {
            Score score = timeObj.getScore(player.getName());
            score.setScore(timeToSet);
        }

        Objective tickObj = s.getObjective("time_tick");
        if (tickObj != null) {
            Score score = tickObj.getScore(player.getName());
            score.setScore(ticksToSet);
        }
    }

    public void executeStartFunction(Player player) {
        if (Main.startFunction != null && !Main.startFunction.equals("")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + player.getName() + " at @s run function " + Main.startFunction);
        }
    }

    public RacePlayer getRacePlayer(UUID uniqueId) {
        for (RacePlayer player : players) {
            if (player.getUniqueId().equals(uniqueId)) return player;
        }
        return null;
    }

    public List<RacePlayer> getPlayers() {
        return players;
    }

    public boolean isOwner(UUID uniqueId) {
        return uniqueId.equals(owner);
    }

    public void onPlayerFinish(RacePlayer player, int place, int time) {
        //Let players know
        Component finishMsg = Main.PREFIX
                .append(Component.text(player.getName() + " finished " + Util.ordinal(place) + " place!", NamedTextColor.GREEN))
                .append(Component.text(" (" + Util.getTimeString(time) + ")", NamedTextColor.WHITE));
        sendMessage(finishMsg);

        if (isEvent) {
            if (place == 1) player.givePoints(prizes[0]);
            if (place == 2) player.givePoints(prizes[1]);
            if (place == 3) player.givePoints(prizes[2]);
            if (place >= 4) player.givePoints(prizes[3]);
        }
    }

    public void onCountdownFinish() {
        players.stream().map(RacePlayer::getPlayer).filter(Objects::nonNull).forEach(player -> {

            // Show "GO" title
            Times times = Times.times(Duration.ofMillis(100), Duration.ofMillis(900), Duration.ofMillis(100));
            player.showTitle(Title.title(Component.text(""), Component.text("GO", NamedTextColor.WHITE, TextDecoration.BOLD), times));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 2);
        });
    }

    protected abstract void onRaceStop();

    public abstract void onTick();

    public abstract void onPlayerDamagedByPlayer(EntityDamageByEntityEvent e, RacePlayer target, RacePlayer attacker);

    public abstract void onPlayerQuit(PlayerQuitEvent e, RacePlayer racePlayer);

    public abstract void onPlayerHeal(EntityRegainHealthEvent e, RacePlayer racePlayer);

    public abstract void onPlayerJoin(PlayerJoinEvent e, RacePlayer racePlayer);

    public abstract void onRacePlayerJoin(RacePlayer player);

    public abstract void onPlayerMove(PlayerMoveEvent e, RacePlayer racePlayer);

    public abstract void onCancelTasks();

    public abstract void onPlayerRespawn(PlayerRespawnEvent e, RacePlayer racePlayer);

    protected abstract void onPlayerLeave(RacePlayer racePlayer);

    public boolean isEvent() {
        return isEvent;
    }

    public String getTypeString() {
        return formattedType;
    }

    public void setPrizes(int first, int second, int third, int fourth) {
        prizes[0] = first;
        prizes[1] = second;
        prizes[2] = third;
        prizes[3] = fourth;
    }
}
