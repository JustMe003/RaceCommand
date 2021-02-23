package io.github.hielkemaps.racecommand.race.types;

import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.Util;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RacePlayer;
import org.bukkit.ChatColor;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class NormalRace extends Race {

    public NormalRace(UUID uniqueId, String name) {
        super(uniqueId,name);
    }

    @Override
    protected void onPlayerStart(RacePlayer player) {

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
    public void onPlayerDamagedByPlayer(EntityDamageByEntityEvent e, RacePlayer player, RacePlayer attacker) {
        e.setCancelled(true);
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent e, RacePlayer player) {

    }

    @Override
    public void onPlayerHeal(EntityRegainHealthEvent e, RacePlayer player) {

    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent e, RacePlayer player) {

    }

    @Override
    public void onRacePlayerJoin(RacePlayer player) {

    }

    @Override
    public void onPlayerMove(PlayerMoveEvent e, RacePlayer player) {

    }

    @Override
    public void onCancelTasks() {

    }

    @Override
    public void onPlayerRespawn(PlayerRespawnEvent e, RacePlayer racePlayer) {

    }
}
