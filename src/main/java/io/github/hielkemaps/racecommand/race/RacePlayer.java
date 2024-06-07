package io.github.hielkemaps.racecommand.race;

import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.Util;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RacePlayer implements Comparable<RacePlayer> {

    private final Race race;
    private final UUID uuid;
    private final String name;

    private boolean isInfected = false;
    private boolean isSkeleton = false;

    private boolean finished = false;
    private int place = Integer.MAX_VALUE;
    private int time;
    private int wager;
    private int additionalWager;

    public RacePlayer(Race race, UUID uuid) {
        this.race = race;
        this.uuid = uuid;
        name = Bukkit.getOfflinePlayer(uuid).getName();
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished, int place, int time) {
        this.finished = finished;
        this.place = place;
        this.time = time;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public int getTime() {
        return time;
    }

    public int getWager() {
        return wager;
    }

    public int getAdditionalWager() {
        return additionalWager;
    }

    public int getTotalWager() {
        return wager + additionalWager;
    }

    public boolean setNewWager(int wager) {
        if (wager < race.getMinimumWager()) {
            return false;
        }
        int diff = wager - this.wager;
        race.increasePrizePool(diff);
        this.wager = wager;
        return true;
    }

    public boolean increaseAdditionalWager(int inc) {
        if (additionalWager + inc < 0) {
            return false;
        }
        race.increasePrizePool(inc);
        additionalWager += inc;
        return true;
    }

    public boolean hasWager() {
        return wager + additionalWager > 0;
    }

    @Override
    public int compareTo(RacePlayer o) {
        return Integer.compare(this.place, o.place);
    }

    public TextComponent getResult() {
        TextColor color = NamedTextColor.DARK_GRAY;

        if (place == 1) color = NamedTextColor.GOLD;
        else if (place == 2) color = NamedTextColor.GRAY;
        else if (place == 3) color = TextColor.color(164, 102, 40);

        if (!finished) {
            return Component.text(name, NamedTextColor.WHITE)
                    .append(Component.text(" - DNF", NamedTextColor.DARK_GRAY));
        }

        return Component.empty()
                .append(Component.text(Util.ordinal(place) + ": ", color, TextDecoration.BOLD))
                .append(Component.text(name, NamedTextColor.WHITE))
                .append(Component.text(" - " + Util.getTimeString(time), NamedTextColor.GRAY));
    }

    public String getName() {
        return name;
    }

    public boolean isOwner() {
        return race.getOwner() != null && race.getOwner().equals(uuid);
    }

    public boolean isInfected() {
        return isInfected;
    }

    public void setInfected(boolean value) {
        if (isInfected == value) return;

        PlayerWrapper player = PlayerManager.getPlayer(uuid);
        if (value) {

            //Only add abilities if race has started, and player is NOT first infected (because freeze countdown handles that)
            InfectedRace infectedRace = (InfectedRace) race;
            if (infectedRace.hasStarted() && !infectedRace.getFirstInfected().getUniqueId().equals(uuid)) {
                player.addAbilities();
            }

            player.disguiseAs(DisguiseType.ZOMBIE_VILLAGER);
            player.setMaxHealth(20);
        } else {
            isSkeleton = false;
            player.removeAbilities();
            player.disguiseAs(DisguiseType.VILLAGER);
        }
        isInfected = value;
    }

    public boolean isOnline() {
        return Bukkit.getOfflinePlayer(uuid).isOnline();
    }

    public Race getRace() {
        return race;
    }

    public void setSkeleton(boolean value) {
        if (isSkeleton == value) return;
        isSkeleton = value;

        if (isOnline()) {
            PlayerWrapper p = getWrapper();
            if (isSkeleton) {
                p.disguiseAs(DisguiseType.SKELETON);
                p.hideAbilities();
                p.skeletonTimer();
                return;
            }

            p.disguiseAs(DisguiseType.ZOMBIE_VILLAGER);
            p.showAbilities();
        }
    }

    public int getPlace() {
        return place;
    }

    public boolean isSkeleton() {
        return isSkeleton;
    }

    public PlayerWrapper getWrapper() {
        return PlayerManager.getPlayer(uuid);
    }

    public void refundAllWagers() {
        if (wager + additionalWager > 0) {
            givePointsSilently(wager + additionalWager);
            race.increasePrizePool(-(wager + additionalWager)); // remove from prizepool

            Component message = Main.PREFIX
                    .append(Component.text("You got refunded ", NamedTextColor.GRAY))
                    .append(Component.text(wager + additionalWager, NamedTextColor.YELLOW))
                    .append(Component.text(" Parcoins", NamedTextColor.GRAY));

            getPlayer().sendMessage(message);
            wager = 0;
            additionalWager = 0;
        }
    }

    public void givePointsSilently(int points) {
        PlayerPoints.getInstance().getAPI().give(uuid, points);
    }

    public void takePointsSilently(int points) {
        PlayerPoints.getInstance().getAPI().take(this.uuid, points);
    }

    public void givePoints(int points) {
        givePointsSilently(points);

        Component message = Main.PREFIX
                .append(Component.text("You won ", NamedTextColor.GRAY))
                .append(Component.text(points, TextColor.color(255, 255, 254), TextDecoration.BOLD)) //Rainbow-colored
                .append(Component.text(" Parcoins!", NamedTextColor.GRAY));

        getPlayer().sendMessage(message);
    }

    public void takePoints(int points) {
        takePointsSilently(points);

        Component message = Main.PREFIX
                .append(Component.text("You payed ", NamedTextColor.GRAY))
                .append(Component.text(points, NamedTextColor.YELLOW))
                .append(Component.text(" Parcoins", NamedTextColor.GRAY));

        getPlayer().sendMessage(message);
    }
}