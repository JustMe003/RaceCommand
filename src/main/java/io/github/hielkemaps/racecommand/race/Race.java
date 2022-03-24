package io.github.hielkemaps.racecommand.race;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.Util;
import io.github.hielkemaps.racecommand.race.player.RacePlayer;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Race {

    private final UUID id = UUID.randomUUID();
    private final String name;
    protected final UUID owner;
    protected final List<RacePlayer> players = new ArrayList<>();
    private boolean isStarting = false;
    private boolean hasStarted = false;
    private final Set<UUID> InvitedPlayers = new HashSet<>();
    private int place = 1;

    //options
    private boolean isPublic = false;
    private int countDown = 5;
    private boolean broadcast = false;
    private boolean ghostPlayers = false;

    //tasks
    private BukkitTask countDownTask;
    private BukkitTask countDownStopTask;
    private BukkitTask playingTask;

    public Race(UUID owner, String name) {

        this.owner = owner;
        this.name = name;

        players.add(onPlayerJoin(owner));

        PlayerWrapper pw = PlayerManager.getPlayer(owner);
        pw.setInRace(true);
    }

    public void start() {
        place = 1;
        isStarting = true;

        sendMessage(Main.PREFIX + "Starting race...");

        //Tp players to start
        for (RacePlayer racePlayer : players) {
            racePlayer.reset();

            Player player = racePlayer.getPlayer();
            if (player != null) {
                player.performCommand("restart");
                onPlayerStart(racePlayer);
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
        AtomicInteger seconds = new AtomicInteger(countDown);

        return Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            for (RacePlayer racePlayer : players) {
                Player player = racePlayer.getPlayer();
                if (player == null) continue;

                //Always invisible during countdown
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, true, false, false));

                StringBuilder sb = new StringBuilder();

                if (seconds.get() == 1) sb.append(ChatColor.RED);
                else if (seconds.get() == 2) sb.append(ChatColor.GOLD);
                else if (seconds.get() == 3) sb.append(ChatColor.YELLOW);
                else if (seconds.get() > 3) sb.append(ChatColor.GREEN);

                sb.append(ChatColor.BOLD).append(seconds);

                if (seconds.get() <= 10) {
                    player.sendTitle(" ", sb.toString(), 2, 18, 2);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1);
                } else {
                    player.sendTitle(ChatColor.YELLOW + "Race starting in", sb.toString(), 0, 30, 0);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.1f, 1f);
                }
            }
            seconds.getAndDecrement();
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

            sendMessage(Main.PREFIX + "Race has started");
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
            sendMessage(Main.PREFIX + "Race has ended");
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
        if (m != null) {
            Objective timeObjective = m.getMainScoreboard().getObjective("time");
            if (timeObjective != null) {
                Score score = timeObjective.getScore(player.getName());
                if (score.isScoreSet()) {
                    time = score.getScore();
                }
            }
        }

        onPlayerFinish(racePlayer, place, time);
        racePlayer.setFinished(true, place, time);

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
            //Otherwise update invited
            for (UUID uuid : InvitedPlayers) {
                PlayerManager.getPlayer(uuid).updateRequirements();
            }
            //Update start/stop requirement for owner only
            PlayerManager.getPlayer(owner).updateRequirements();
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

        sendMessageToRaceMembers(Main.PREFIX + ChatColor.GREEN + "+ " + ChatColor.RESET + ChatColor.GRAY + addedPlayer.getName());

        PlayerWrapper pw = PlayerManager.getPlayer(uuid);
        pw.setInRace(true);

        //If joined during countdown, tp to start
        if (isStarting) {
            addedPlayer.performCommand("restart");
        }

        PlayerManager.getPlayer(owner).updateRequirements();
        players.add(onPlayerJoin(uuid));
    }

    public boolean hasPlayer(UUID uuid) {
        return getRacePlayer(uuid) != null;
    }

    public void leavePlayer(UUID uuid) {
        removePlayer(uuid);
    }

    public void kickPlayer(UUID uuid) {
        removePlayer(uuid);

        OfflinePlayer kickedPlayer = Bukkit.getOfflinePlayer(uuid);
        if (kickedPlayer.isOnline()) {
            kickedPlayer.getPlayer().sendMessage(Main.PREFIX + "You have been kicked from the race");
        }
    }

    private void removePlayer(UUID uuid) {
        RacePlayer racePlayer = getRacePlayer(uuid);
        onPlayerLeave(racePlayer);
        racePlayer.onLeaveRace();

        players.remove(racePlayer);

        sendMessageToRaceMembers(Main.PREFIX + ChatColor.RED + "- " + ChatColor.RESET + ChatColor.GRAY + racePlayer.getName());

        PlayerWrapper pw = PlayerManager.getPlayer(uuid);
        pw.setInRace(false);
        PlayerManager.getPlayer(owner).updateRequirements();
    }

    public void disband() {
        cancelTasks();

        //get race off public visibility
        setIsPublic(false);

        //Clear outgoing invites
        getInvitedPlayers().forEach(uuid -> {
            PlayerWrapper wPlayer = PlayerManager.getPlayer(uuid);
            wPlayer.removeInvite(owner);
        });

        for (RacePlayer racePlayer : players) {
            racePlayer.onLeaveRace();
            PlayerWrapper pw = PlayerManager.getPlayer(racePlayer.getUniqueId());
            pw.setInRace(false);

            Player player = racePlayer.getPlayer();
            if (player == null) continue; //we can do nothing with offline players

            if (!racePlayer.getUniqueId().equals(owner))
                player.sendMessage(Main.PREFIX + "The race has been disbanded");
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

        isPublic = value;

        if (value) {
            RaceManager.publicRaces.add(owner);
        } else {
            RaceManager.publicRaces.remove(owner);
        }

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

    public boolean hasStarted() {
        return hasStarted;
    }

    public void invitePlayer(UUID invited) {
        InvitedPlayers.add(invited);
        PlayerManager.getPlayer(invited).addInvite(owner);
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

    public void sendMessage(String message) {

        if (broadcast) {
            Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
        } else {
            sendMessageToRaceMembers(message);
        }
    }

    public void sendMessageToRaceMembers(String message) {
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
        sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.RESET + "" + ChatColor.BOLD + " Results " + ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "           ");
        players.sort(Comparator.comparing(RacePlayer::getPlace));

        for (RacePlayer player : players) {
            sendMessage(player.toString());
        }
        sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "                                    ");
    }

    /**
     * @param excludedPlayer player in race who's time won't be picked
     * @return time objective score for all players in race, -1 if not found
     */
    public int getCurrentObjective(UUID excludedPlayer, String name) {
        int time = -1;

        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return time;

        Scoreboard s = sm.getMainScoreboard();
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

        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm != null) {
            Scoreboard s = sm.getMainScoreboard();

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
        sendMessage(Main.PREFIX + ChatColor.GREEN + player.getName() + " finished " + Util.ordinal(place) + " place!" + ChatColor.WHITE + " (" + Util.getTimeString(time) + ")");
    }

    public void onCountdownFinish() {
        players.stream().map(RacePlayer::getPlayer).filter(Objects::nonNull).forEach(player -> {
            player.sendTitle(" ", ChatColor.BOLD + "GO", 2, 18, 2);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 2);
        });
    }

    protected abstract void onPlayerStart(RacePlayer racePlayer);

    protected abstract void onRaceStop();

    public abstract void onTick();

    public abstract void onPlayerDamagedByPlayer(EntityDamageByEntityEvent e, RacePlayer target, RacePlayer attacker);

    public abstract void onPlayerQuit(PlayerQuitEvent e, RacePlayer racePlayer);

    public abstract void onPlayerHeal(EntityRegainHealthEvent e, RacePlayer racePlayer);

    public abstract void onPlayerJoin(PlayerJoinEvent e, RacePlayer racePlayer);

    public abstract RacePlayer onPlayerJoin(UUID player);

    public abstract void onPlayerMove(PlayerMoveEvent e, RacePlayer racePlayer);

    public abstract void onCancelTasks();

    public abstract void onPlayerRespawn(PlayerRespawnEvent e, RacePlayer racePlayer);

    protected abstract void onPlayerLeave(RacePlayer racePlayer);
}
