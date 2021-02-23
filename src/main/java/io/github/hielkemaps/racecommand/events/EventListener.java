package io.github.hielkemaps.racecommand.events;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.RacePlayer;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

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

        CommandAPI.updateRequirements(player);

        //Remove inRace tag if player is not in current active race
        boolean removeTag = true;
        Race race = RaceManager.getRace(uuid);
        if (race != null) {

            if (race.hasStarted()) {
                removeTag = false;

                //if player rejoins in active race, we must sync times with the other players
                race.syncTime(player);
            }

            //If joined during countdown, tp to start
            if (race.isStarting()) {
                player.performCommand("restart");
            }
            race.onPlayerJoin(e, race.getRacePlayer(uuid));
        }
        if (removeTag) {
            player.removeScoreboardTag("inRace");

            PlayerWrapper wPlayer = PlayerManager.getPlayer(uuid);
            wPlayer.setInRace(false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {

        //Leave race when race hasn't started
        //Otherwise you could easily cheat because you don't get tped when the race starts
        UUID player = e.getPlayer().getUniqueId();
        Race race = RaceManager.getRace(player);
        if (race != null) {
            race.onPlayerQuit(e, race.getRacePlayer(e.getPlayer().getUniqueId()));

            //if after leaving there are 1 or no players left in the race, we disband it
            if (race.getOnlinePlayerCount() <= 2) {
                RaceManager.disbandRace(race.getOwner());
            }

            if (!race.isOwner(player)) {
                if (!race.hasStarted()) {
                    race.leavePlayer(player); //player leaves the race if it hasn't started yet
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageByEntityEvent e) {

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
}
