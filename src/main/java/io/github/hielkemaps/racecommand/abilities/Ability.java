package io.github.hielkemaps.racecommand.abilities;

import io.github.hielkemaps.racecommand.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public abstract class Ability {

    final ItemStack item;
    final UUID uuid;
    final int duration;
    final int delay;
    final int slot;

    private boolean canRun = true;

    public Ability(UUID uuid, int duration, int delay, ItemStack item, int slot) {
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
            setEmptyItem(true);
            canRun = false;
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), this::deActivate, duration);
        }
    }

    public void deActivate() {
        onDeactivate();
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            canRun = true;
            setEmptyItem(false);
        }, delay);
    }

    abstract void onActivate();

    abstract void onDeactivate();

    Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public ItemStack getItem() {
        return item;
    }

    private void setEmptyItem(boolean value) {
        if (value) {
            updateInventory(new ItemStack(Material.AIR));
        } else {
            updateInventory(item);
        }
    }
}
