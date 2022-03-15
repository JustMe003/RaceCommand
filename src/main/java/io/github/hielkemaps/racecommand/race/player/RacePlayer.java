package io.github.hielkemaps.racecommand.race.player;

import io.github.hielkemaps.racecommand.Util;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.UUID;

public abstract class RacePlayer implements Comparable<RacePlayer> {

    private final Race race;
    private final UUID uuid;
    private final String name;

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

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();

        if (place == 1)
            s.append(ChatColor.GOLD);
        else if (place == 2)
            s.append(ChatColor.GRAY);
        else if (place == 3)
            s.append(ChatColor.of("#a46628"));
        else
            s.append(ChatColor.DARK_GRAY);

        s.append(ChatColor.BOLD);

        if (finished) {
            s.append(Util.ordinal(place)).append(": ").append(ChatColor.RESET).append(name).append(ChatColor.DARK_GRAY)
                    .append(" - ").append(ChatColor.GRAY).append(Util.getTimeString(time));
        } else {
            s.append(ChatColor.RESET).append(name).append(ChatColor.DARK_GRAY).append(" - ").append(ChatColor.GRAY)
                    .append("DNF");
        }
        return s.toString();
    }

    public String getName() {
        return name;
    }

    public boolean isOwner() {
        return race.getOwner().equals(uuid);
    }

    public boolean isOnline() {
        return Bukkit.getOfflinePlayer(uuid).isOnline();
    }

    public Race getRace() {
        return race;
    }

    public int getPlace() {
        return place;
    }

    public PlayerWrapper getWrapper() {
        return PlayerManager.getPlayer(uuid);
    }

    public abstract void onLeaveRace();

    public abstract void onDropItem(PlayerDropItemEvent e);

    public abstract void onPlayerSwitchHandItem(PlayerSwapHandItemsEvent e);

    public abstract void onInventoryInteract(InventoryClickEvent e);

    public abstract void onPlayerRightClick(PlayerInteractEvent e);

    public abstract void onJoin(PlayerJoinEvent e);
}