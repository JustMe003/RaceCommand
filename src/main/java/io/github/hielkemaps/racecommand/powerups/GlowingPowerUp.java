package io.github.hielkemaps.racecommand.powerups;

import io.github.hielkemaps.racecommand.race.player.RacePlayer;
import io.github.hielkemaps.racecommand.race.player.types.InfectedRacePlayer;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class GlowingPowerUp extends PowerUp {

    public GlowingPowerUp(UUID uuid, int slot) {
        super(uuid, 60, 200, item(), slot);
    }

    private static ItemStack item() {
        ItemStack item = new ItemStack(Material.BELL);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "Make villagers glow");
        item.setItemMeta(itemMeta);
        return item;
    }

    @Override
    void onActiveTick() {

    }

    @Override
    void onActivate() {

        InfectedRace race = (InfectedRace) getRace();
        if(race == null) return;

        Player abilityPlayer = getPlayer();
        abilityPlayer.playSound(abilityPlayer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.5f,2f);

        //make villagers glow for 3 sec
        for(RacePlayer p : race.getPlayers()){
            InfectedRacePlayer player = (InfectedRacePlayer) p;

            if(!player.isInfected() && player.isOnline()){
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
