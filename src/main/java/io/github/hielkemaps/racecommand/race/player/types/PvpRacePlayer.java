package io.github.hielkemaps.racecommand.race.player.types;

import io.github.hielkemaps.racecommand.powerups.*;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.player.RacePlayer;

import java.util.UUID;

public class PvpRacePlayer extends RacePlayer {
    public PvpRacePlayer(Race race, UUID uuid) {
        super(race, uuid);
    }

    @Override
    public void registerAbilities(UUID uuid) {
        powerups.add(new SpeedPowerUp(uuid, 0));
        powerups.add(new LeapPowerUp(uuid, 1));
        powerups.add(new ArrowPowerUp(uuid, 2));
    }
}
