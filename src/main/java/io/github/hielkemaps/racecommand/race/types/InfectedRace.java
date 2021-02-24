package io.github.hielkemaps.racecommand.race.types;

import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.Util;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RacePlayer;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.*;
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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class InfectedRace extends Race {


    private RacePlayer firstInfected = null;
    private int infectedDelay = 30;

    private BukkitTask freezeTimer;
    private BukkitTask stopFreezeTask;


    public InfectedRace(UUID owner, String name) {
        super(owner, name);
    }

    @Override
    protected void onPlayerStart(RacePlayer racePlayer) {

    }


    @Override
    public void onRaceStop() {

        //reset skins 3 sec after stop
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            for (RacePlayer p : getPlayers()) {
                if (p.isInfected()) {
                    p.setInfected(false);
                }

                PlayerWrapper player = p.getWrapper();
                player.setMaxHealth(20);
            }
        }, 60);

        sendMessage(Main.PREFIX + "stopped race");
    }

    public void setFirstInfected(RacePlayer player) {
        firstInfected = player;
    }

    @Override
    public void onCountdownFinish() {

        //set first infected
        if (firstInfected == null || !firstInfected.isOnline()) {
            firstInfected = Util.getRandomItem(getOnlinePlayers());
        }
        firstInfected.setInfected(true);
        sendMessage(Main.PREFIX + ChatColor.DARK_GREEN + firstInfected.getName() + ChatColor.RESET +
                ChatColor.GREEN + " is the first infected! They will be set free in " + ChatColor.DARK_GREEN +
                ChatColor.BOLD + infectedDelay + ChatColor.RESET + ChatColor.GREEN + " seconds!");

        freezeInfected();
    }

    @Override
    public void onTick() {
        for (RacePlayer player : getPlayers()) {
            if (player.isInfected()) {
                player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20, 1, true, false, false));
            }
        }
    }

    private void freezeInfected() {

        AtomicInteger seconds = new AtomicInteger(infectedDelay);


        //Countdown
        freezeTimer = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {

            Player player = firstInfected.getPlayer();

            //if player has left
            if (player == null) {

                sendMessage(Main.PREFIX + ChatColor.DARK_GREEN + firstInfected.getName() + ChatColor.RESET + ChatColor.GREEN + " has left!");

                firstInfected = Util.getRandomItem(getOnlinePlayers()); //pick new random infected
                firstInfected.setInfected(true);
                player = firstInfected.getPlayer();

                sendMessage(Main.PREFIX + ChatColor.DARK_GREEN + firstInfected.getName() + ChatColor.RESET + ChatColor.GREEN + " is the new infected player!");
                firstInfected.getPlayer().performCommand("restart"); //take new player back to start
            }

            StringBuilder sb = new StringBuilder();
            if (seconds.get() == 1) sb.append(ChatColor.RED);
            else if (seconds.get() == 2) sb.append(ChatColor.GOLD);
            else if (seconds.get() == 3) sb.append(ChatColor.YELLOW);
            else if (seconds.get() > 3) sb.append(ChatColor.GREEN);
            sb.append(ChatColor.BOLD).append(seconds.toString());

            if (seconds.get() <= 10) {
                player.sendTitle(" ", sb.toString(), 2, 18, 2);
                player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1, 1);
            } else {
                player.sendTitle(ChatColor.DARK_GREEN + "You will be released in ", sb.toString(), 0, 30, 0);
                player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.05f, 1f);
            }

            seconds.getAndDecrement();
        }, 0, 20);

        //Stop countdown
        stopFreezeTask = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            freezeTimer.cancel();

            Player player = firstInfected.getPlayer();
            sendMessage(Main.PREFIX + ChatColor.DARK_GREEN + player.getName() + ChatColor.RESET + ChatColor.GREEN + " has been unleashed!");
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 1f);
        }, 20L * infectedDelay);
    }

    @Override
    public void onPlayerFinish(RacePlayer player, int place, int time) {
        if (!player.isInfected()) {
            sendMessage(Main.PREFIX + ChatColor.DARK_GREEN + player.getName() + ChatColor.RESET + ChatColor.GREEN + " escaped! Villagers win!");
            stop();
        }
    }

    @Override
    public void onPlayerDamagedByPlayer(EntityDamageByEntityEvent e, RacePlayer target, RacePlayer attacker) {

        //if freeze countdown is active or one player is skeleton
        if (!freezeTimer.isCancelled() || attacker.isSkeleton() || target.isSkeleton()) {
            e.setCancelled(true); //disable pvp
            return;
        }

        //if infected hits player - 2 damage
        if (attacker.isInfected() && !target.isInfected()) {
            e.setDamage(0);

            PlayerWrapper wPlayer = target.getWrapper();

            double health = wPlayer.getMaxHealth() - 1.5;
            wPlayer.setMaxHealth(health);

            //spawn damage particle
            Player bukkitPlayer = target.getPlayer();
            Location loc = bukkitPlayer.getLocation();
            bukkitPlayer.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc.getX(), loc.getY() + 1, loc.getZ(), 2, 0.1, 0.1, 0.1, 0.2);

            //if player dies by the damage, turn infected
            if (health <= 0) {
                Player mPlayer = target.getPlayer();

                mPlayer.sendTitle(" ", ChatColor.GREEN + "" + ChatColor.BOLD + "You have been infected!", 10, 60, 10);
                target.getRace().sendMessage(Main.PREFIX + target.getName() + " got infected!");
                mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0F, 1.0F);

                target.setInfected(true);
                onPlayerInfected();
            } else {
                target.getPlayer().setHealth(health);
            }
        } else if (!attacker.isInfected() && target.isInfected()) {
            e.setDamage(0);

            double health = target.getPlayer().getHealth() - 6;
            Player infected = target.getPlayer();
            infected.playSound(infected.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_HURT, 1.0F, 1.0F);

            //if infected dies, cancel
            if (health <= 0) {
                onInfectedDied(target);
                e.setCancelled(true);
            } else {
                target.getPlayer().setHealth(health);
            }
        } else {
            e.setCancelled(true);
        }
    }

    private void onInfectedDied(RacePlayer player) {
        Player infected = player.getPlayer();
        infected.playSound(infected.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_DEATH, 1.0F, 1.0F);

        //Skeleton skin
        player.setSkeleton(true);
    }

    private void onPlayerInfected() {

        //if everyone is infected
        if (getPlayers().stream().allMatch(RacePlayer::isInfected)) {

            for (RacePlayer p : getPlayers()) {
                p.getPlayer().sendTitle(" ", ChatColor.BOLD + "" + ChatColor.DARK_GREEN + "Infected won!", 10, 60, 10);
                p.getPlayer().playSound(p.getPlayer().getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 100f, 0.5f);
            }
            sendMessage(Main.PREFIX + ChatColor.GREEN + "Infected won the game!");
            stop();
        }
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent e, RacePlayer racePlayer) {
        if (racePlayer.isInfected()) {
            racePlayer.setInfected(false);
        }
    }

    @Override
    public void onPlayerHeal(EntityRegainHealthEvent e, RacePlayer racePlayer) {
        if (racePlayer.isSkeleton()) {
            e.setCancelled(true); //disable healing for skeletons
        }
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent e, RacePlayer racePlayer) {
        PlayerWrapper wPlayer = racePlayer.getWrapper();
        if (racePlayer.isInfected()) {
            wPlayer.changeSkin("zombie_villager");
        } else {
            wPlayer.changeSkin("villager");
        }
    }

    @Override
    public void onRacePlayerJoin(RacePlayer player) {
        player.getWrapper().changeSkin("villager");
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent e, RacePlayer racePlayer) {

        boolean freezePlayer = false;

        //freeze first infected player
        if (racePlayer.equals(firstInfected)) {
            if (freezeTimer != null && !freezeTimer.isCancelled()) {
                freezePlayer = true;
            }
        }

        if (racePlayer.isSkeleton()) freezePlayer = true;

        if (freezePlayer) {
            Location to = e.getFrom();
            to.setPitch(e.getTo().getPitch());
            to.setYaw(e.getTo().getYaw());
            e.setTo(to);
        }
    }

    @Override
    public void onCancelTasks() {
        if (freezeTimer != null) freezeTimer.cancel();
        if (stopFreezeTask != null) stopFreezeTask.cancel();
    }

    @Override
    public void onPlayerRespawn(PlayerRespawnEvent e, RacePlayer racePlayer) {
        if (!racePlayer.isInfected()) {
            PlayerWrapper wPlayer = racePlayer.getWrapper();
            wPlayer.setMaxHealth(wPlayer.getMaxHealth());
        }
    }

    @Override
    protected void onPlayerRemoved(UUID uuid) {
        if (uuid.equals(firstInfected.getUniqueId())) {
            if (!hasStarted()) {
                firstInfected = null;
                sendMessage(Main.PREFIX + "First infected has left! Setting to random player...");
            }
        }
    }

    public void setInfectedDelay(int infectedDelay) {
        this.infectedDelay = infectedDelay;
    }
}
