package io.github.hielkemaps.racecommand.abilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class SpeedAbility extends Ability {

    public SpeedAbility(UUID uuid, int slot) {
        super(uuid, 200, 500, item(), slot);
    }

    private static ItemStack item() {
        ItemStack item = new ItemStack(Material.SUGAR);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.displayName(Component.text("Speed")
                .style(Style.style(TextColor.color(125, 233, 245), TextDecoration.BOLD)).decoration(TextDecoration.ITALIC,false)
        );
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
