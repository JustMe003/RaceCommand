package io.github.hielkemaps.racecommand.events;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.player.RacePlayer;
import io.github.hielkemaps.racecommand.race.player.types.InfectedRacePlayer;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

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

        Race race = RaceManager.getRace(player.getUniqueId());
        if (race != null) {
            if (race.hasStarted()) {
                RacePlayer racePlayer = race.getRacePlayer(player.getUniqueId());
                race.onPlayerRespawn(e, racePlayer);
            }
        }
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerWrapper wPlayer = PlayerManager.getPlayer(uuid);

        //Remove inRace tag if player is not in current active race
        Race race = RaceManager.getRace(uuid);
        if (race != null) {

            //if player rejoins in active race, we must sync times with the other players
            if (race.hasStarted()) {
                race.syncTime(player);
            }

            //If joined during countdown, tp to start
            if (race.isStarting()) {
                player.performCommand("restart");
            }

            RacePlayer p = race.getRacePlayer(uuid);
            race.onPlayerJoin(e, p);
            p.onJoin(e);
        } else {

            //if player is NOT in race, but thinks it is, we need to change it
            if (wPlayer.isInRace()) wPlayer.setInRace(false);

            //always remove tag just to be sure
            player.removeScoreboardTag("inRace");
        }

        Bukkit.getLogger().info("Player in race: " + wPlayer.isInRace());

        //if not in race but changed skin, we need to reset it
        if(!wPlayer.isInRace()){
            wPlayer.resetSkin();
        }

        CommandAPI.updateRequirements(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {

        //Leave or disband race when race hasn't started
        //Otherwise you could easily cheat because you won't get tped when the race starts
        UUID player = e.getPlayer().getUniqueId();
        Race race = RaceManager.getRace(player);
        if (race != null) {

            //if after leaving there are 1 or no players left in the race, we disband it
            if (race.getOnlinePlayerCount() <= 2) {
                RaceManager.disbandRace(race.getOwner());
            }

            //if the race has not started yet and player is not owner
            if (!race.hasStarted() && !race.isOwner(player)) {
                race.leavePlayer(player); //player leaves the race if it hasn't started yet
            }

            race.onPlayerQuit(e, race.getRacePlayer(e.getPlayer().getUniqueId()));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e){
        Player player = e.getEntity();

        Race race = RaceManager.getRace(player.getUniqueId());
        if (race != null) {
            if (race.hasStarted()) {
                RacePlayer racePlayer = race.getRacePlayer(player.getUniqueId());
                race.onPlayerDeath(e, racePlayer);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {

        //If player damages another player
        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            UUID player = e.getEntity().getUniqueId();
            UUID attacker = e.getDamager().getUniqueId();

            Race playerRace = RaceManager.getRace(player);
            if (playerRace != null) {

                //If both players are in the same race
                // and race has started
                if (playerRace.hasPlayer(attacker) && playerRace.hasStarted()) {
                    RacePlayer racePlayer = playerRace.getRacePlayer(player);
                    RacePlayer raceAttacker = playerRace.getRacePlayer(attacker);

                    //if both players are ingame
                    if (!racePlayer.isFinished() && !raceAttacker.isFinished()) {
                        playerRace.onPlayerDamagedByPlayer(e, racePlayer, raceAttacker);
                        return;
                    }
                }
            }
            e.setCancelled(true); //disable pvp
        }

        if(e.getEntity() instanceof Player && !(e.getDamager() instanceof Player)){
            UUID player = e.getEntity().getUniqueId();
            Race playerRace = RaceManager.getRace(player);
            if (playerRace != null) {
                playerRace.onPlayerDamagedByEntity(e,playerRace.getRacePlayer(player));
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        PlayerWrapper player = PlayerManager.getPlayer(e.getPlayer().getUniqueId());

        if (player.isInRace()) {

            Race race = RaceManager.getRace(e.getPlayer().getUniqueId());
            if (race == null) return;

            //Freeze players when starting race
            if (race.isStarting()) {
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
            UUID uuid = e.getPlayer().getUniqueId();
            Race race = RaceManager.getRace(uuid);
            if(race != null){
                race.getRacePlayer(uuid).onPlayerRightClick(e);
            }
        }
    }

    @EventHandler
    public void onPlayerSwitchHandItem(PlayerSwapHandItemsEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        Race race = RaceManager.getRace(uuid);
        if(race != null){
            race.getRacePlayer(uuid).onPlayerSwitchHandItem(e);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        Race race = RaceManager.getRace(uuid);
        if(race != null){
            race.getRacePlayer(uuid).onDropItem(e);
        }
    }

    @EventHandler
    public void onInventoryInteract(InventoryClickEvent e) {
        UUID uuid = e.getWhoClicked().getUniqueId();
        Race race = RaceManager.getRace(uuid);
        if(race != null){
            race.getRacePlayer(uuid).onInventoryInteract(e);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (e.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) e.getEntity();

            if (arrow.getScoreboardTags().contains("raceplugin")) {
                if (e.getHitBlock() != null) {
                    arrow.remove();
                }
            }
        }

        if (e.getHitBlock() != null && e.getHitBlock().getType() == Material.CHORUS_FLOWER) {
            BlockData data = e.getHitBlock().getBlockData();
            e.getEntity().remove();
            e.getHitBlock().setType(Material.AIR);

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                e.getHitBlock().setType(Material.CHORUS_FLOWER);
                e.getHitBlock().setBlockData(data);
            });
        }
    }
}
