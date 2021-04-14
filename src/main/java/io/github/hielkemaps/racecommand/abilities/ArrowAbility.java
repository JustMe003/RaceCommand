package io.github.hielkemaps.racecommand.abilities;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class ArrowAbility extends Ability {

    public ArrowAbility(UUID uuid, int slot) {
        super(uuid, 20, 500, item(), slot);
    }

    private static ItemStack item() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName("Shoot infected");
        item.setItemMeta(itemMeta);
        return item;
    }

    @Override
    void onActiveTick() {
        if (getRace() != null) {
            Player p = getPlayer();
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_DISPENSER_LAUNCH, 0.5f, 1.0f);
            p.getWorld().spawn(p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(1.2D)), Arrow.class, entity -> {
                entity.setInvulnerable(true);
                entity.addScoreboardTag("raceplugin");
                entity.addScoreboardTag("race_" + getRace().getId().toString());
                entity.setPersistent(false);
                entity.setVelocity(p.getEyeLocation().getDirection().multiply(2.0D));
                entity.setKnockbackStrength(2);
                entity.setDamage(0);
            });
        }
    }

    @Override
    void onActivate() {

    }

    @Override
    void onAdd() {

    }

    @Override
    void onRemove() {
    }
}
