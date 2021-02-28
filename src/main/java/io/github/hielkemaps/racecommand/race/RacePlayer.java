package io.github.hielkemaps.racecommand.race;

import io.github.hielkemaps.racecommand.Util;
import io.github.hielkemaps.racecommand.abilities.Ability;
import io.github.hielkemaps.racecommand.abilities.SpeedAbility;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RacePlayer implements Comparable<RacePlayer> {

    private final Race race;
    private final UUID uuid;
    private final String name;

    private final List<Ability> abilities = new ArrayList<>();
    private boolean isInfected = false;
    private boolean isSkeleton = false;
    private final int skinId;

    private boolean finished = false;
    private int place;
    private int time;


    public RacePlayer(Race race, UUID uuid) {
        this.race = race;
        this.uuid = uuid;
        name = Bukkit.getOfflinePlayer(uuid).getName();

        //get random int from 1 to 15
        skinId = ThreadLocalRandom.current().nextInt(1, 16);
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

        if (place == 1) s.append(ChatColor.GOLD);
        else if (place == 2) s.append(ChatColor.GRAY);
        else if (place == 3) s.append(ChatColor.of("#a46628"));
        else s.append(ChatColor.DARK_GRAY);

        s.append(ChatColor.BOLD);

        s.append(Util.ordinal(place)).append(": ").append(ChatColor.RESET).append(name).append(ChatColor.DARK_GRAY).append(" - ").append(ChatColor.GRAY).append(Util.getTimeString(time));
        return s.toString();
    }

    public String getName() {
        return name;
    }

    public boolean isOwner() {
        return race.getOwner().equals(uuid);
    }

    public boolean isInfected() {
        return isInfected;
    }

    public void setInfected(boolean value) {
        if (isInfected == value) return;

        if (isOnline()) {

            PlayerWrapper player = PlayerManager.getPlayer(uuid);
            if (value) {
                addAbility(new SpeedAbility(uuid, 0)); //speed ability for infected
                player.changeSkin(getZombieSkin());
                player.setMaxHealth(20);
                getPlayer().setHealth(20);
            } else {
                isSkeleton = false;
                player.changeSkin(getVillagerSkin());
            }
            isInfected = value;
        }
    }

    public String getVillagerSkin() {
        return "villager" + skinId;
    }

    public String getZombieSkin() {
        return "villager" + skinId + "zombie";
    }

    public boolean isOnline() {
        return Bukkit.getPlayer(uuid) != null;
    }

    public Race getRace() {
        return race;
    }

    public void setSkeleton(boolean value) {
        if (isSkeleton == value) return;

        if (isOnline()) {
            PlayerWrapper p = PlayerManager.getPlayer(uuid);
            if (value) {
                p.changeSkin("skeleton");
                p.skeletonTimer();
            } else {
                p.changeSkin(getZombieSkin());
            }
            isSkeleton = value;
        }
    }

    public boolean isSkeleton() {
        return isSkeleton;
    }

    public PlayerWrapper getWrapper() {
        return PlayerManager.getPlayer(uuid);
    }

    public void addAbility(Ability ability) {
        abilities.add(ability);
    }

    public List<Ability> getAbilities() {
        return abilities;
    }
}