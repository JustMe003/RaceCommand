package io.github.hielkemaps.racecommand.race;

import dev.jorel.commandapi.CommandAPI;
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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
    private int totalPrizePool = 0;

    //options
    private boolean isPublic = false;
    private int countDown = 5;
    private boolean broadcast = false;
    private boolean ghostPlayers = false;
    private int minimumWager = 0;

    private boolean isEvent = false;

    //tasks
    private BukkitTask countDownTask;
    private BukkitTask countDownStopTask;
    private BukkitTask playingTask;
    private final int[] prizes = {0, 0, 0, 0};
    private AtomicInteger currentCountdown = new AtomicInteger(100000);

    public Race(CommandSender sender) {
        if (sender instanceof Player player) {
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
    }

    public void startCountdown(long delay) {
        place = 1;
        isStarting = true;

        //Teleport to start
        if (countDown < 10) {
            for (RacePlayer player : players) {
                if (player.getPlayer() != null) {
                    player.getPlayer().performCommand("restart");
                }
            }
        }

        //Start countdown
        currentCountdown = new AtomicInteger(countDown);
        countDownTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::countdown, 0, 20);

        //Start race task
        countDownStopTask = Bukkit.getScheduler().runTaskLater(Main.getInstance(), this::start, 20L * countDown);

        updateRequirements();
    }

    private void countdown() {
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
    }

    private void start() {
        countDownTask.cancel();
        isStarting = false;

        if (players.size() < 2) {
            sendMessage(Main.PREFIX.append(Component.text("Not enough players to start!")));
            if (isEvent) {
                RaceManager.disbandRace(this); //Disband if event
            }
            return;
        }

        // Recalculate prize pool, just in case something went wrong in adding / removing parcoins
        int actualPrizePool = 0;
        for (RacePlayer racePlayer : players) {
            actualPrizePool += racePlayer.getTotalWager();
        }

        // When setting the prizes of the race, add the prizes to the already existing prizes
        // In case the race is an event and has already loaded some prizes
        // Rather than overriding the event prizes, we should add the prize pool to the existing prizes
        if (actualPrizePool > 0) {
            if (players.size() == 2) {
                // 1st: 100%
                setPrizes(getFirstPrize() + actualPrizePool, getSecondPrize(), getThirdPrize(), getFourthPlusPrize());
            } else if (players.size() == 3) {
                // 1st: 70%, 2nd: 30%
                int firstPrize = (int) Math.round(actualPrizePool * 0.7);
                setPrizes(getFirstPrize() + firstPrize, getSecondPrize() + actualPrizePool - firstPrize, getThirdPrize(), getFourthPlusPrize());
            } else {
                // 1st: 60%, 2nd: 25%, 3rd: 15%
                int firstPrize = (int) Math.round(actualPrizePool * 0.6);
                int secondPrize = (int) Math.round(actualPrizePool * 0.25);
                setPrizes(getFirstPrize() + firstPrize, getSecondPrize() + secondPrize, getThirdPrize() + actualPrizePool - firstPrize - secondPrize, getFourthPlusPrize());
            }
        }

        hasStarted = true;
        onRaceStart();

        for (RacePlayer racePlayer : getOnlinePlayers()) {
            Player player = racePlayer.getPlayer();
            player.addScoreboardTag("inRace");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + player.getName() + " at @s run function time:start");

            // Show "GO" title
            Times times = Times.times(Duration.ofMillis(100), Duration.ofMillis(900), Duration.ofMillis(100));
            player.showTitle(Title.title(Component.text(""), Component.text("GO", NamedTextColor.WHITE, TextDecoration.BOLD), times));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 2);
        }

        //Start playing task
        playingTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::tick, 0, 1);
        sendMessage(Main.PREFIX.append(Component.text("Race has started")));
    }

    /**
     * Runs every tick if the game has started
     */
    private void tick() {
        onTick();

        //stop race if everyone has finished
        if (players.stream().allMatch(RacePlayer::isFinished)) {
            stop();
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

            if(player.getScoreboardTags().contains("finished")){
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
        //HielkeAPI.onPlayerFinishedRace(player, type, place, time, isEvent);

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

        // Disband race if event, otherwise it will stay there and invite joining players
        if (isEvent) RaceManager.disbandRace(this);
        else sendMessage(Main.PREFIX.append(Component.text("Race has ended")));
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
        if (minimumWager > 0) {
            newPlayer.setNewWager(minimumWager);
        }

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

    public void kickPlayer(UUID uuid) {
        removePlayerSilent(uuid);

        OfflinePlayer kickedPlayer = Bukkit.getOfflinePlayer(uuid);
        if (kickedPlayer.getPlayer() != null) {
            kickedPlayer.getPlayer().sendMessage(Main.PREFIX.append(Component.text("You have been kicked from the race")));
        }
    }

    public void removePlayer(Player player) {
        removePlayerSilent(player.getUniqueId());

        String text = isEvent ? "You have left the race" : "You have left " + name + "'s race";
        player.sendMessage(Main.PREFIX.append(Component.text(text)));
    }

    public void removePlayerSilent(UUID uuid) {
        RacePlayer racePlayer = getRacePlayer(uuid);
        onPlayerLeave(racePlayer);
        racePlayer.refundAllWagers();
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
            racePlayer.refundAllWagers();
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

    public boolean setMinimumWager(int wage) {
        if (wage == minimumWager) return false;

        minimumWager = wage;
        return true;
    }

    public int getMinimumWager() {
        return minimumWager;
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


        Bukkit.getLogger().info("---------------- Race Results ----------------");
        for (RacePlayer player : players) {
            final String plain = PlainTextComponentSerializer.plainText().serialize(player.getResult());
            Bukkit.getLogger().info(plain);
        }
        Bukkit.getLogger().info("----------------------------------------------");
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

    public RacePlayer getRacePlayer(UUID uniqueId) {
        for (RacePlayer player : players) {
            if (player.getUniqueId().equals(uniqueId)) return player;
        }
        return null;
    }

    public List<RacePlayer> getPlayers() {
        return players;
    }

    public int getTotalPrizePool() {
        return totalPrizePool;
    }

    public int increasePrizePool(int addedPrizes) {
        totalPrizePool += addedPrizes;
        return totalPrizePool;
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

        if (isEvent || totalPrizePool > 0) {
            if (place == 1 && prizes[0] > 0) player.givePoints(getFirstPrize());
            if (place == 2 && prizes[1] > 0) player.givePoints(getSecondPrize());
            if (place == 3 && prizes[2] > 0) player.givePoints(getThirdPrize());
            if (place >= 4 && prizes[3] > 0) player.givePoints(getFourthPlusPrize());
        }
    }

    public abstract void onRaceStart();

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

    public int getFirstPrize() {
        return prizes[0];
    }

    public int getSecondPrize() {
        return prizes[1];
    }

    public int getThirdPrize() {
        return prizes[2];
    }

    public int getFourthPlusPrize() {
        return prizes[3];
    }
}
