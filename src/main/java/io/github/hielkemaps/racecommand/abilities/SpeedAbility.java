package io.github.hielkemaps.racecommand.abilities;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class SpeedAbility extends Ability {

    public SpeedAbility(UUID uuid, int slot) {
        super(uuid, 120, 80, item(), slot);
    }

    private static ItemStack item() {
        ItemStack item = new ItemStack(Material.SUGAR);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName("Speed boost");
        item.setItemMeta(itemMeta);
        return item;
    }

    @Override
    public void onActivate() {
        Player player = getPlayer();
        if (player != null) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 2, true, true, true));
        }
    }

    @Override
    public void onDeactivate() {

    }
}
