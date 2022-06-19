package io.github.hielkemaps.racecommand.skins;

import org.bukkit.entity.Player;

public class SkinManager {

    public static boolean classPresent = true;

    public static boolean changeSkin(Player player, String skin) {
        if(classPresent){
            SkinAPI.changeSkin(player,skin);
            return true;
        }
        return false;
    }
}
