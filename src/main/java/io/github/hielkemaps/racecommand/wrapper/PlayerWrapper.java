package io.github.hielkemaps.racecommand.wrapper;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.abilities.*;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
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

    private final List<Ability> abilities = new ArrayList<>();

    private BukkitTask skeletonTimer;

    public PlayerWrapper(UUID uuid) {
        this.uuid = uuid;

        abilities.add(new SpeedAbility(uuid, 0));
        abilities.add(new LeapAbility(uuid, 1));
        abilities.add(new BlindAbility(uuid, 2));
        abilities.add(new GlowingAbility(uuid, 3));
        abilities.add(new ArrowAbility(uuid, 4));
    }

    public boolean isInRace() {
        return inRace;
    }

    public void setInRace(boolean value) {
        inRace = value;

        if (!value) {
            getPlayer().removeScoreboardTag("inRace");
            resetSkin();
            removeAbilities();
            setMaxHealth(20);
            setHealth(20);
            cancelTasks();
        }

        updateRequirements();
    }

    private void cancelTasks() {
        if (skeletonTimer != null) skeletonTimer.cancel();
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
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "forward console skin clear " + getPlayer().getName());
            hasChangedSkin = false;
        }
    }

    public void changeSkin(String name) {
        if (isOnline()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "forward console skin set " + getPlayer().getName() + " " + name);
            hasChangedSkin = true;
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


    public void skeletonTimer() {
        Player infected = getPlayer();
        infected.setHealth(2);
        setMaxHealth(2);
        infected.sendTitle("", org.bukkit.ChatColor.RED + "" + org.bukkit.ChatColor.BOLD + "Healing...", 10, 60, 10);
        infected.playSound(infected.getLocation(), Sound.ENTITY_SKELETON_HURT, 0.5F, 1.0F);

        //heal infected slowly
        skeletonTimer = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            Player player = getPlayer();

            if (player != null) {
                if (player.getHealth() < 20) {
                    double health = player.getHealth() + 2;
                    if (health >= 20) health = 20;

                    setMaxHealth(health);
                    player.setHealth(health);

                    if (health == 20) {
                        endSkeleton(player);
                    } else {
                        player.sendTitle("", org.bukkit.ChatColor.RED + "" + org.bukkit.ChatColor.BOLD + "Healing...", 0, 60, 10);
                        player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_HURT, 0.5F, 1.0F);
                    }
                } else {
                    endSkeleton(player);
                }
            }
        }, 30, 30);
    }

    private void endSkeleton(Player player) {
        player.sendTitle(org.bukkit.ChatColor.GREEN + "" + org.bukkit.ChatColor.BOLD + "GO!", "", 0, 60, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 1F, 1.3F);
        skeletonTimer.cancel();

        Race race = RaceManager.getRace(uuid);
        if (race != null) {
            race.getRacePlayer(uuid).setSkeleton(false);
        }
    }

    public void addAbilities() {
        abilities.forEach(Ability::add);
    }

    public void removeAbilities() {
        abilities.forEach(Ability::remove);
    }

    public void onPlayerJoin() {
		if (!inRace) {
            removeAbilities();
        }

        for (Ability ability : abilities) {
            ability.onPlayerJoin();
        }
    }

    public void setHealth(int value) {
        if (isOnline()) {
            getPlayer().setHealth(value);
        }
    }

    public List<Ability> getAbilities() {
        return abilities;
    }

    public void hideAbilities() {
        abilities.forEach(Ability::hide);
    }

    public void showAbilities() {
        abilities.forEach(Ability::show);
    }
}
