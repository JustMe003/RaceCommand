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
                    .append(Component.text(" - DNF" + Util.getTimeString(time), NamedTextColor.DARK_GRAY));
        }

        return Component.text(Util.ordinal(place) + ": ", color, TextDecoration.BOLD)
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

        if (isOnline()) {
            PlayerWrapper p = PlayerManager.getPlayer(uuid);
            if (value) {
                p.disguiseAs(DisguiseType.SKELETON);
                p.hideAbilities();
                p.skeletonTimer();
            } else if (isInfected) {
                p.disguiseAs(DisguiseType.ZOMBIE_VILLAGER);
                p.showAbilities();
            }
        }
        isSkeleton = value;
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

    public void givePoints(int points) {
        PlayerPoints.getInstance().getAPI().give(uuid, points);

        Component message = Main.PREFIX
                .append(Component.text("You won ", NamedTextColor.GRAY))
                .append(Component.text(points, NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" Parcoins!", NamedTextColor.GRAY));

        getPlayer().sendMessage(message);
    }
}