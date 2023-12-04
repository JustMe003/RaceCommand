package io.github.hielkemaps.racecommand.abilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class LeapAbility extends Ability{


    public LeapAbility(UUID uuid, int slot) {
        super(uuid, 15, 1000, item(), slot);
    }

    private static ItemStack item() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.displayName(Component.text("Leap")
                .style(Style.style(TextColor.color(177, 255, 140), TextDecoration.BOLD)).decoration(TextDecoration.ITALIC,false)
        );
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
    void onAdd() {

    }

    @Override
    public void onRemove() {

    }
}
