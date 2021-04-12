package io.github.hielkemaps.racecommand.abilities;

import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.RacePlayer;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public abstract class Ability {

    final ItemStack item;
    final InfectedRace race;
    final UUID uuid;
    final int duration;
    final int delay;
    final int slot;

    BukkitTask activateTask = null;
    BukkitTask deactivateTask = null;
    BukkitTask tickTask = null;

    private boolean canRun = true;

    public Ability(InfectedRace race, UUID uuid, int duration, int delay, ItemStack item, int slot) {
        this.race = race;
        this.uuid = uuid;
        this.duration = duration;
        this.delay = delay;
        this.slot = slot;
        this.item = item;
        updateInventory(item);
    }

    private void updateInventory(ItemStack item) {
        getPlayer().getInventory().setItem(slot, item); //put in players inventory
    }

    public void activate() {
        if (canRun) {
            onActivate();
            getPlayer().getInventory().clear(slot);
            canRun = false;
            deactivateTask = Bukkit.getScheduler().runTaskLater(Main.getInstance(), this::deActivate, getDuration());
            tickTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::onActiveTick, 0, 1);
        }
    }

    // more coolDown the more infected there are
    private long getDuration() {
        int infectedPlayers = getInfectedPlayerCount();

        return infectedPlayers == 1 ? duration : (long) (duration * infectedPlayers * 0.5);
    }

    private int getInfectedPlayerCount() {
        int count = 0;
        for (RacePlayer player : race.getPlayers()) {
            if (player.isInfected()) count++;
        }
        return count;
    }

    public void deActivate() {
        onDeactivate();

        if (tickTask != null) {
            tickTask.cancel();
        }

        activateTask = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            canRun = true;
            updateInventory(item);
        }, delay);
    }

    abstract void onActiveTick();

    abstract void onActivate();

    abstract void onDeactivate();

    Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public ItemStack getItem() {
        return item;
    }

    public void removeAbility() {
        getPlayer().getInventory().clear(slot);

        if (activateTask != null) activateTask.cancel();
        if (deactivateTask != null) deactivateTask.cancel();
        if (tickTask != null) tickTask.cancel();
    }
}
