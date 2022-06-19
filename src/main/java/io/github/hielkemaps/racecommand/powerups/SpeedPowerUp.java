package io.github.hielkemaps.racecommand.powerups;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class SpeedPowerUp extends PowerUp {

    public SpeedPowerUp(UUID uuid, int slot) {
        super(uuid, 200, 500, item(), slot);
    }

    private static ItemStack item() {
        ItemStack item = new ItemStack(Material.SUGAR);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "Speed Boost");
        item.setItemMeta(itemMeta);
        return item;
    }

    @Override
    void onActiveTick() {

    }

    @Override
    public void onActivate() {
        Player player = getPlayer();
        if (player != null) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.5f,1f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, true, true, true));
        }
    }

    @Override
    void onAdd() {

    }

    @Override
    public void onRemove() {

    }
}
