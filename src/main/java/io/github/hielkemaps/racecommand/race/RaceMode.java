package io.github.hielkemaps.racecommand.race;

import java.util.Arrays;

public enum RaceMode {
    normal, pvp, infected;

    public static String[] getNames(Class<? extends Enum<?>> e) {
        return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }
}
