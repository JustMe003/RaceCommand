package io.github.hielkemaps.racecommand.abilities;

import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.player.RacePlayer;
import io.github.hielkemaps.racecommand.race.player.types.InfectedRacePlayer;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.UUID;

public abstract class Ability {

    HashMap<Integer, ItemStack> queuedItems = new HashMap<>();

    final ItemStack item;
    final UUID uuid;
    final int duration;
    final int delay;
    final int slot;

    BukkitTask activateTask = null;
    BukkitTask deactivateTask = null;
    BukkitTask tickTask = null;

    private boolean canRun = true;
    private boolean isHidden = false;

    public Ability(UUID uuid, int duration, int delay, ItemStack item, int slot) {
        this.uuid = uuid;
        this.duration = duration;
        this.delay = delay;
        this.slot = slot;
        this.item = item;
    }

    private void updateInventory(ItemStack item) {

        //if player is offline or hidden, we queue the item
        if (getPlayer() == null || isHidden) {
            queuedItems.put(slot, item);
        } else {
            getPlayer().getInventory().setItem(slot, item); //put in players inventory
        }
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
        if (getRace() != null) {
            int infectedPlayers = getInfectedPlayerCount();
            return infectedPlayers == 1 ? duration : (long) (duration * infectedPlayers * 0.5);
        }
        return duration;
    }

    private int getInfectedPlayerCount() {
        int count = 0;
        for (RacePlayer p : getRace().getPlayers()) {
            InfectedRacePlayer player = (InfectedRacePlayer) p;
            if (player.isInfected()) count++;
        }
        return count;
    }

    public InfectedRace getRace() {
        return (InfectedRace) RaceManager.getRace(uuid);
    }

    public void deActivate() {
        onRemove();

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

    abstract void onAdd();

    abstract void onRemove();

    Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public ItemStack getItem() {
        return item;
    }

    public void add() {
        onAdd();
        canRun = true;
        updateInventory(item);
    }

    public void remove() {
        onRemove();
        updateInventory(new ItemStack(Material.AIR));

        if (activateTask != null) activateTask.cancel();
        if (deactivateTask != null) deactivateTask.cancel();
        if (tickTask != null) tickTask.cancel();

        queuedItems.clear();
    }

    public void hide() {
        updateInventory(new ItemStack(Material.AIR));
        isHidden = true;
    }

    public void show() {
        isHidden = false;

        if (activateTask == null) {
            updateInventory(item);
        } else if (activateTask.isCancelled()) {
            updateInventory(item);
        }
    }

    public void onPlayerJoin() {
        queuedItems.forEach((integer, itemStack) -> getPlayer().getInventory().setItem(integer, itemStack));
        queuedItems.clear();
    }
}
