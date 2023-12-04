package io.github.hielkemaps.racecommand.abilities;

import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RacePlayer;
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

public class GlowingAbility extends Ability{

    public GlowingAbility(UUID uuid, int slot) {
        super(uuid, 60, 200, item(), slot);
    }

    private static ItemStack item() {
        ItemStack item = new ItemStack(Material.BELL);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.displayName(Component.text("Make Villagers Glow")
                .style(Style.style(TextColor.color(255, 247, 184), TextDecoration.BOLD)).decoration(TextDecoration.ITALIC,false)
        );
        item.setItemMeta(itemMeta);
        return item;
    }

    @Override
    void onActiveTick() {

    }

    @Override
    void onActivate() {

        Race race = getRace();
        if(race == null) return;

        Player abilityPlayer = getPlayer();
        abilityPlayer.playSound(abilityPlayer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.5f,2f);

        //make villagers glow for 3 sec
        for(RacePlayer player : race.getPlayers()){
            if(!player.isInfected()){
                player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 1, true, true, true));
            }
        }
    }

    @Override
    void onAdd() {

    }

    @Override
    void onRemove() {
    }
}
