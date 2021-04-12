package io.github.hielkemaps.racecommand.abilities;

import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RacePlayer;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class GlowingAbility extends Ability{

    public GlowingAbility(InfectedRace race, UUID uuid, int slot) {
        super(race, uuid, 60, 200, item(), slot);
    }

    private static ItemStack item() {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName("Show villagers");
        item.setItemMeta(itemMeta);
        return item;
    }

    @Override
    void onActiveTick() {

    }

    @Override
    void onActivate() {
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
    void onDeactivate() {
    }
}
