package io.github.hielkemaps.racecommand.race.player.types;

import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.player.RacePlayer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.UUID;

public class DefaultRacePlayer extends RacePlayer {
    public DefaultRacePlayer(Race race, UUID uuid) {
        super(race, uuid);
    }

    @Override
    public void onLeaveRace() {

    }

    @Override
    public void onDropItem(PlayerDropItemEvent e) {

    }

    @Override
    public void onPlayerSwitchHandItem(PlayerSwapHandItemsEvent e) {

    }

    @Override
    public void onInventoryInteract(InventoryClickEvent e) {

    }

    @Override
    public void onPlayerRightClick(PlayerInteractEvent e) {

    }

    @Override
    public void onJoin(PlayerJoinEvent e) {

    }
}
