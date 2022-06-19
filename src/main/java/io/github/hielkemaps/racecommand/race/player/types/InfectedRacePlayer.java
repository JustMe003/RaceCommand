package io.github.hielkemaps.racecommand.race.player.types;

import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.powerups.*;
import io.github.hielkemaps.racecommand.race.player.RacePlayer;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class InfectedRacePlayer extends RacePlayer {

    private boolean isInfected = false;
    private boolean isSkeleton = false;
    private final int skinId;

    public InfectedRacePlayer(InfectedRace race, UUID uuid) {
        super(race, uuid);

        //get random int from 1 to 15
        skinId = ThreadLocalRandom.current().nextInt(1, 16);

        getWrapper().changeSkin(getVillagerSkin());
    }

    private void endSkeleton() {
        Player player = getPlayer();
        player.sendTitle(org.bukkit.ChatColor.GREEN + "" + org.bukkit.ChatColor.BOLD + "GO!", "", 0, 60, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 1F, 1.3F);
        bukkitTask.cancel();

        setSkeleton(false);
    }

    public String getVillagerSkin() {
        return "villager" + skinId;
    }

    public String getZombieSkin() {
        return "villager" + skinId + "zombie";
    }

    public void skeletonTimer() {
        Player infected = getPlayer();
        PlayerWrapper p = getWrapper();
        infected.setHealth(2);
        p.setMaxHealth(2);
        infected.sendTitle(" ", org.bukkit.ChatColor.RED + "" + org.bukkit.ChatColor.BOLD + "Healing...", 10, 60, 10);
        infected.playSound(infected.getLocation(), Sound.ENTITY_SKELETON_HURT, 0.5F, 1.0F);

        //heal infected slowly
        bukkitTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            Player player = getPlayer();

            if (player != null) {
                if (player.getHealth() < 20) {
                    double health = player.getHealth() + 2;
                    if (health >= 20) health = 20;

                    p.setMaxHealth(health);
                    player.setHealth(health);

                    if (health == 20) {
                        endSkeleton();
                    } else {
                        player.sendTitle(" ", org.bukkit.ChatColor.RED + "" + org.bukkit.ChatColor.BOLD + "Healing...", 0, 60, 10);
                        player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_HURT, 0.5F, 1.0F);
                    }
                } else {
                    endSkeleton();
                }
            }
        }, 30, 30);
    }

    public boolean isInfected() {
        return isInfected;
    }

    public void setInfected(boolean value) {
        if (isInfected == value) return;

        PlayerWrapper player = getWrapper();
        if (value) {

            //Only add abilities if race has started, and player is NOT first infected (because freeze countdown handles that)
            InfectedRace race = (InfectedRace) getRace();
            if (race.doPowerUps() && race.hasStarted() && !race.getFirstInfected().getUniqueId().equals(getUniqueId())) {
                addPowerUps();
            }

            player.changeSkin(getZombieSkin());
            player.setMaxHealth(20);
        } else {
            isSkeleton = false;
            removePowerUps();
            player.changeSkin(getVillagerSkin());
        }
        isInfected = value;
    }

    public void setSkeleton(boolean value) {
        if (isSkeleton == value) return;

        if (isOnline()) {
            PlayerWrapper p = getWrapper();
            if (value) {
                p.changeSkin("skeleton");
                hidePowerUps();
                skeletonTimer();
            } else if (isInfected) {
                p.changeSkin(getZombieSkin());
                showPowerUps();
            }
        }
        isSkeleton = value;
    }

    public boolean isSkeleton() {
        return isSkeleton;
    }

    @Override
    public void registerAbilities(UUID uuid) {
        powerups.add(new SpeedPowerUp(uuid, 0));
        powerups.add(new LeapPowerUp(uuid, 1));
        powerups.add(new BlindPowerUp(uuid, 2));
        powerups.add(new GlowingPowerUp(uuid, 3));
        powerups.add(new ArrowPowerUp(uuid, 4));
    }
}
