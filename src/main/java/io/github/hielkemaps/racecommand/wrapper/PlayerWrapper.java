package io.github.hielkemaps.racecommand.wrapper;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.abilities.*;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import io.github.hielkemaps.racecommand.skins.SkinManager;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class PlayerWrapper {

    private final UUID uuid;
    private boolean inRace = false;
    private final Set<UUID> raceInvites = new HashSet<>();
    private boolean hasChangedSkin = false;
    private double maxHealth = 20;

    public PlayerWrapper(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isInRace() {
        return inRace;
    }

    public void setInRace(boolean value) {
        inRace = value;

        if (!value) {
            if(isOnline()) getPlayer().removeScoreboardTag("inRace");
            resetSkin();
            setMaxHealth(20);
            setHealth(20);
        }

        updateRequirements();
    }

    public boolean hasJoinableRace() {
        return !raceInvites.isEmpty() || RaceManager.hasJoinablePublicRace(uuid);
    }

    public String[] getJoinableRaces() {
        List<String> joinable = new ArrayList<>();

        //Add invites
        for (UUID uuid : raceInvites) {
            Race race = RaceManager.getRace(uuid);
            if (race != null && !race.hasStarted()) {
                joinable.add(race.getName());
            }
        }
        //Add open races
        for (UUID uuid : RaceManager.publicRaces) {

            //Exclude when player is owner
            if (!uuid.equals(this.uuid)) {

                Race race = RaceManager.getRace(uuid);
                if (race != null && !race.hasStarted()) {
                    joinable.add(race.getName());
                }
            }

        }
        return joinable.toArray(new String[0]);
    }

    public void addInvite(UUID sender) {
        raceInvites.add(sender);
        updateRequirements();
    }

    public boolean acceptInvite(UUID sender) {
        Race race = RaceManager.getRace(sender);
        if (race == null) return false;

        //Join race
        race.addPlayer(uuid);
        race.removeInvited(uuid);

        //Update requirements
        raceInvites.remove(sender);
        updateRequirements();
        return true;
    }

    public void removeInvite(UUID from) {
        raceInvites.remove(from);
    }

    public void updateRequirements() {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        CommandAPI.updateRequirements(player);
    }

    public Team getTeam() {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return null;

        ScoreboardManager manager = Bukkit.getServer().getScoreboardManager();
        if (manager == null) return null;

        Scoreboard scoreboard = manager.getMainScoreboard();

        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(p.getName())) return team;
        }
        return null;
    }

    public void resetSkin() {
        if (isOnline() && hasChangedSkin) {

            SkinManager.changeSkin(getPlayer(), getPlayer().getName());
            hasChangedSkin = false;

            //Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "forward console skin clear " + getPlayer().getName());
        }
    }

    public boolean isInInfectedRace() {
        if (isInRace()) {
            Race race = RaceManager.getRace(uuid);
            return (race instanceof InfectedRace);
        }
        return false;
    }

    public void setMaxHealth(double value) {
        maxHealth = value;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        attribute.setBaseValue(value);
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public boolean isOnline() {
        return Bukkit.getPlayer(uuid) != null;
    }

    public void sendMessage(String msg) {
        if (isOnline()) {
            getPlayer().sendMessage(msg);
        }
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public void setHealth(int value) {
        if (isOnline()) {
            getPlayer().setHealth(value);
        }
    }

    public void changeSkin(String skin) {
        if(isOnline()){
            SkinManager.changeSkin(getPlayer(),skin);
            hasChangedSkin = true;
        }
    }
}
