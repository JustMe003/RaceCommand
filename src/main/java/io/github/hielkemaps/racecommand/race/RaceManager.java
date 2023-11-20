package io.github.hielkemaps.racecommand.race;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RaceManager {

    public static List<Race> races = new ArrayList<>();

    public static void addRace(Race race) {
        races.add(race);
    }

    public static Race getRace(String name) {
        return races.stream()
                .filter(race -> race.getName().equals(name))
                .findFirst().orElse(null);
    }

    public static List<Race> getPublicRaces() {
        return races.stream()
                .filter(Race::isPublic)
                .collect(Collectors.toList());
    }

    public static Race getRace(Player player) {
        return getRace(player.getUniqueId());
    }

    public static Race getRace(UUID player) {
        for (Race r : races) {
            if (r.hasPlayer(player)) {
                return r;
            }
        }
        return null;
    }

    public static void disbandRace(Race race) {
        race.disband();
        races.remove(race);
    }

    public static boolean hasJoinablePublicRace(UUID uuid) {
        for (Race race : races) {
            if (race.isPublic() && !race.hasPlayer(uuid)) {
                return true;
            }
        }
        return false;
    }

    public static void stopEvent() {
        for (Race race : getPublicRaces()) {
            if (race.isEvent()) {
                disbandRace(race);
            }
        }
    }

    public static Race getEvent() {
        for (Race race : getPublicRaces()) {
            if (race.isEvent()) {
                return race;
            }
        }
        return null;
    }
}
