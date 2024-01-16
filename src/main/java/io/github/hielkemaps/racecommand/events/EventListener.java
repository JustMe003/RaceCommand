package io.github.hielkemaps.racecommand.events;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.abilities.Ability;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.RacePlayer;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventListener implements Listener {

    @EventHandler
    public void entityRegainHealthEvent(EntityRegainHealthEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof Player) {

            Race race = RaceManager.getRace(entity.getUniqueId());
            if (race != null) {
                if (race.hasStarted()) {
                    RacePlayer racePlayer = race.getRacePlayer(entity.getUniqueId());
                    race.onPlayerHeal(e, racePlayer);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();

        Race race = RaceManager.getRace(player);
        if (race != null) {
            if (race.hasStarted()) {
                RacePlayer racePlayer = race.getRacePlayer(player.getUniqueId());
                race.onPlayerRespawn(e, racePlayer);
            }
        }
    }

    public static List<UUID> playersJoinEvent = new ArrayList<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerWrapper wPlayer = PlayerManager.getPlayer(uuid);

        //If waiting to join event, join race
        if (playersJoinEvent.contains(uuid)) {
            playersJoinEvent.remove(uuid);

            // If player already in race
            Race currentRace = RaceManager.getRace(uuid);
            if (currentRace != null) {
                if (currentRace.isEvent()) return; // If already in event race, we don't have to add the player again
                RaceManager.DisbandOrLeaveRace(uuid);  //Disband current race if owner, or leave it
            }

            for (Race race : RaceManager.getPublicRaces()) {
                if (race.isEvent() && !race.hasStarted()) {
                    race.addPlayer(uuid);
                }
            }
        }

        wPlayer.onPlayerJoin();

        //Remove inRace tag if player is not in current active race
        Race race = RaceManager.getRace(player);
        if (race != null) {

            //if player rejoins in active race, we must sync times with the other players
            if (race.hasStarted()) {
                race.syncTime(player);
            }

            //If joined during countdown, tp to start
            if (race.isStarting()) {
                player.performCommand("restart");
            }

            race.onPlayerJoin(e, race.getRacePlayer(uuid));
        } else {

            //if player is NOT in race, but thinks it is, we need to change it
            if (wPlayer.isInRace()) wPlayer.setInRace(false);

            //always remove tag just to be sure
            player.removeScoreboardTag("inRace");

            //If not in race and there is an event race, we invite the player
            for (Race pr : RaceManager.getPublicRaces()) {
                if (pr.isEvent() && !pr.hasStarted()) {
                    pr.invitePlayer(uuid);
                }
            }
        }

        CommandAPI.updateRequirements(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {

        //Leave or disband race when race hasn't started
        //Otherwise you could easily cheat because you won't get teleported when the race starts
        Player player = e.getPlayer();
        Race race = RaceManager.getRace(player);
        if (race == null) return;

        //if after leaving there are no players left in the race, we disband it
        if (race.getOnlinePlayerCount() <= 1) {
            if (race.hasStarted() || !race.isEvent()) {
                RaceManager.disbandRace(race);
            }
        }

        //if the race has not started yet and player is not owner
        if (!race.hasStarted() && !race.isOwner(player.getUniqueId())) {
            race.removePlayer(player); //player leaves the race if it hasn't started yet
        }

        race.onPlayerQuit(e, race.getRacePlayer(e.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        //If player damages another player
        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            e.setCancelled(true);
            handlePlayerDamage(e);
        }

        //If arrow damages player
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Arrow) {
            handleArrowDamage(e);
        }
    }

    private void handleArrowDamage(EntityDamageByEntityEvent e) {
        Arrow arrow = (Arrow) e.getDamager();

        if (!arrow.getScoreboardTags().contains("raceplugin")) return;

        e.setCancelled(true);
        Player player = (Player) e.getEntity();

        Race race = RaceManager.getRace(player);
        if (race == null || !race.getTypeString().equals("§2Infected")) return;

        RacePlayer racePlayer = race.getRacePlayer(player.getUniqueId());

        //Infected players can not be damaged by arrows
        if (racePlayer.isInfected()) return;

        for (String scoreboardTag : arrow.getScoreboardTags()) {
            if (scoreboardTag.startsWith("race_")) {
                String id = scoreboardTag.substring(5);
                if (id.equals(race.getId().toString())) {
                    e.setDamage(1);
                    e.setCancelled(false); //allow damage
                    return;
                }
            }
        }
    }

    private void handlePlayerDamage(EntityDamageByEntityEvent e) {
        Player player = (Player) e.getEntity();
        UUID attacker = e.getDamager().getUniqueId();

        Race playerRace = RaceManager.getRace(player);
        if (playerRace == null) return;

        //If both players are in the same race
        // and race has started
        if (playerRace.hasPlayer(attacker) && playerRace.hasStarted()) {
            RacePlayer racePlayer = playerRace.getRacePlayer(player.getUniqueId());
            RacePlayer raceAttacker = playerRace.getRacePlayer(attacker);

            //if both players are ingame
            if (!racePlayer.isFinished() && !raceAttacker.isFinished()) {
                playerRace.onPlayerDamagedByPlayer(e, racePlayer, raceAttacker);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        PlayerWrapper player = PlayerManager.getPlayer(e.getPlayer().getUniqueId());

        if (player.isInRace()) {
            Race race = RaceManager.getRace(e.getPlayer());
            if (race == null) return;

            //Freeze players in last 10 seconds of countdown
            if (race.isStarting() && race.getCountDown() < 10) {
                Location to = e.getFrom();
                to.setPitch(e.getTo().getPitch());
                to.setYaw(e.getTo().getYaw());
                e.setTo(to);
                return;
            }

            race.onPlayerMove(e, race.getRacePlayer(e.getPlayer().getUniqueId()));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        //on right click
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            PlayerWrapper player = PlayerManager.getPlayer(e.getPlayer().getUniqueId());

            List<Ability> abilities = player.getAbilities();
            for (Ability ability : abilities) {
                if (ability.getItem().equals(e.getItem())) {
                    ability.activate();
                }
            }
        }

    }

    @EventHandler
    public void onPlayerSwitchHandItem(PlayerSwapHandItemsEvent e) {
        PlayerWrapper player = PlayerManager.getPlayer(e.getPlayer().getUniqueId());

        List<Ability> abilities = player.getAbilities();
        for (Ability ability : abilities) {
            if (ability.getItem().equals(e.getOffHandItem())) {
                e.setCancelled(true);
                return;
            }
        }

    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        PlayerWrapper player = PlayerManager.getPlayer(e.getPlayer().getUniqueId());

        List<Ability> abilities = player.getAbilities();
        for (Ability ability : abilities) {
            if (ability.getItem().equals(e.getItemDrop().getItemStack())) {
                e.setCancelled(true);
                return;
            }
        }

    }

    @EventHandler
    public void onInventoryInteract(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            PlayerWrapper player = PlayerManager.getPlayer(e.getWhoClicked().getUniqueId());

            List<Ability> abilities = player.getAbilities();
            for (Ability ability : abilities) {
                if (e.getCurrentItem() != null && e.getCurrentItem().equals(ability.getItem())) {
                    e.setCancelled(true);
                    return;
                }
            }

        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (e.getEntity() instanceof Arrow arrow) {

            if (arrow.getScoreboardTags().contains("raceplugin")) {
                if (e.getHitBlock() != null) {
                    arrow.remove();
                }
            }
        }
    }
}
