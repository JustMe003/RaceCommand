package io.github.hielkemaps.racecommand.race.types;

import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.player.RacePlayer;
import io.github.hielkemaps.racecommand.race.player.types.InfectedRacePlayer;
import io.github.hielkemaps.racecommand.race.player.types.NormalRacePlayer;
import io.github.hielkemaps.racecommand.race.player.types.PvpRacePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class PvpRace extends Race {

    public PvpRace(UUID owner, String name) {
        super(owner, name);
    }

    @Override
    protected void onPlayerStart(RacePlayer racePlayer) {

    }

    @Override
    protected void onRaceStop() {
        //show results if any, otherwise show stop message
        if (players.stream().noneMatch(RacePlayer::isFinished)) sendMessage(Main.PREFIX + "stopped race");
        else printResults();
    }

    @Override
    public void onTick() {

    }

    @Override
    public void onPlayerDamagedByPlayer(EntityDamageByEntityEvent e, RacePlayer target, RacePlayer attacker) {
        e.setCancelled(false);
    }

    @Override
    public void onPlayerDamagedByEntity(EntityDamageByEntityEvent e, RacePlayer player) {

        //Arrow detection
        if (e.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) e.getDamager();

            if (arrow.getScoreboardTags().contains("raceplugin")) {
                Race race = RaceManager.getRace(player.getUniqueId());

                for (String scoreboardTag : arrow.getScoreboardTags()) {
                    if (scoreboardTag.startsWith("race_")) {
                        String id = scoreboardTag.substring(5);
                        if (id.equals(race.getId().toString())) {
                            e.setCancelled(false); //allow damage
                            e.setDamage(1);

                            Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getInstance(), () -> {
                                ((LivingEntity) e.getEntity()).setNoDamageTicks(0); // after 100ms set the no damage ticks to 0 so arrows can hurt again
                            }, 2L);

                            return;
                        }
                    }
                }
                e.setCancelled(true);
            }
        }
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent e, RacePlayer racePlayer) {
    }

    @Override
    public void onPlayerHeal(EntityRegainHealthEvent e, RacePlayer racePlayer) {
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent e, RacePlayer racePlayer) {
    }

    @Override
    public RacePlayer onPlayerJoin(UUID uuid) {
        return new PvpRacePlayer(this, uuid);
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent e, RacePlayer racePlayer) {
    }

    @Override
    public void onCancelTasks() {
    }

    @Override
    public void onPlayerRespawn(PlayerRespawnEvent e, RacePlayer racePlayer) {

    }

    @Override
    protected void onPlayerLeave(RacePlayer racePlayer) {

    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent e, RacePlayer racePlayer) {

    }
}
