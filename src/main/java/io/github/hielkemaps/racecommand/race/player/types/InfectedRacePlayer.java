package io.github.hielkemaps.racecommand.race.player.types;

import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.abilities.*;
import io.github.hielkemaps.racecommand.race.player.RacePlayer;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class InfectedRacePlayer extends RacePlayer {

    private boolean isInfected = false;
    private boolean isSkeleton = false;
    private final int skinId;

    private final List<Ability> abilities = new ArrayList<>();
    private BukkitTask skeletonTimer;

    public InfectedRacePlayer(InfectedRace race, UUID uuid) {
        super(race, uuid);

        //get random int from 1 to 15
        skinId = ThreadLocalRandom.current().nextInt(1, 16);

        PlayerManager.getPlayer(uuid).changeSkin(getVillagerSkin());

        abilities.add(new SpeedAbility(uuid, 0));
        abilities.add(new LeapAbility(uuid, 1));
        abilities.add(new BlindAbility(uuid, 2));
        abilities.add(new GlowingAbility(uuid, 3));
        abilities.add(new ArrowAbility(uuid, 4));
    }

    private void cancelTasks() {
        if (skeletonTimer != null) skeletonTimer.cancel();
    }

    private void endSkeleton() {
        Player player = getPlayer();
        player.sendTitle(org.bukkit.ChatColor.GREEN + "" + org.bukkit.ChatColor.BOLD + "GO!", "", 0, 60, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 1F, 1.3F);
        skeletonTimer.cancel();

        setSkeleton(false);
    }

    public String getVillagerSkin() {
        return "villager" + skinId;
    }

    public String getZombieSkin() {
        return "villager" + skinId + "zombie";
    }

    public void addAbilities() {
        abilities.forEach(Ability::add);
    }

    public void removeAbilities() {
        abilities.forEach(Ability::remove);
    }

    public void skeletonTimer() {
        Player infected = getPlayer();
        PlayerWrapper p = getWrapper();
        infected.setHealth(2);
        p.setMaxHealth(2);
        infected.sendTitle("", org.bukkit.ChatColor.RED + "" + org.bukkit.ChatColor.BOLD + "Healing...", 10, 60, 10);
        infected.playSound(infected.getLocation(), Sound.ENTITY_SKELETON_HURT, 0.5F, 1.0F);

        //heal infected slowly
        skeletonTimer = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
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
                        player.sendTitle("", org.bukkit.ChatColor.RED + "" + org.bukkit.ChatColor.BOLD + "Healing...", 0, 60, 10);
                        player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_HURT, 0.5F, 1.0F);
                    }
                } else {
                    endSkeleton();
                }
            }
        }, 30, 30);
    }

    public void hideAbilities() {
        abilities.forEach(Ability::hide);
    }

    public void showAbilities() {
        abilities.forEach(Ability::show);
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
            if (race.hasStarted() && !race.getFirstInfected().getUniqueId().equals(getUniqueId())) {
                addAbilities();
            }

            player.changeSkin(getZombieSkin());
            player.setMaxHealth(20);
        } else {
            isSkeleton = false;
            removeAbilities();
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
                hideAbilities();
                skeletonTimer();
            } else if (isInfected) {
                p.changeSkin(getZombieSkin());
                showAbilities();
            }
        }
        isSkeleton = value;
    }

    public boolean isSkeleton() {
        return isSkeleton;
    }

    @Override
    public void onLeaveRace() {
        removeAbilities();
        cancelTasks();
    }

    @Override
    public void onDropItem(PlayerDropItemEvent e) {
        for (Ability ability : abilities) {
            if (ability.getItem().equals(e.getItemDrop().getItemStack())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public void onPlayerSwitchHandItem(PlayerSwapHandItemsEvent e) {
        for (Ability ability : abilities) {
            if (ability.getItem().equals(e.getOffHandItem())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public void onInventoryInteract(InventoryClickEvent e) {
        for (Ability ability : abilities) {
            if (e.getCurrentItem() != null && e.getCurrentItem().equals(ability.getItem())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public void onPlayerRightClick(PlayerInteractEvent e) {
        for (Ability ability : abilities) {
            if (ability.getItem().equals(e.getItem())) {
                ability.activate();
            }
        }
    }

    @Override
    public void onJoin(PlayerJoinEvent e) {
        abilities.forEach(Ability::onPlayerJoin);
    }
}
