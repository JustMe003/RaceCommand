package io.github.hielkemaps.racecommand.powerups;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class LeapPowerUp extends PowerUp {

    public float power = 1;

    public LeapPowerUp(UUID uuid, int slot) {
        super(uuid, 15, 500, item(), slot);
    }

    private static ItemStack item() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "Leap");
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
            player.setVelocity(player.getEyeLocation().getDirection().multiply(power));
        }
    }

    @Override
    void onAdd() {

    }

    @Override
    public void onRemove() {

    }
}
