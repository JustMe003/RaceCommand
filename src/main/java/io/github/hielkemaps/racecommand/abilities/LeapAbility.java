package io.github.hielkemaps.racecommand.abilities;

import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class LeapAbility extends Ability{


    public LeapAbility(InfectedRace race, UUID uuid, int slot) {
        super(race, uuid, 15, 200, item(), slot);
    }

    private static ItemStack item() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName("Leap");
        item.setItemMeta(itemMeta);
        return item;
    }

    @Override
    void onActiveTick() {
        Player player = getPlayer();
        if (player != null) {
            player.getWorld().spawnParticle(Particle.WHITE_ASH, player.getEyeLocation(),5, 0.2, 0.5, 0.2);
        }
    }

    @Override
    public void onActivate() {
        Player player = getPlayer();
        if (player != null) {
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.5f,1f);
            player.setVelocity(player.getEyeLocation().getDirection());
        }
    }

    @Override
    public void onDeactivate() {

    }
}
