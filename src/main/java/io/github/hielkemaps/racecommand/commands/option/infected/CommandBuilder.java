package io.github.hielkemaps.racecommand.commands.option.infected;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;

import java.util.ArrayList;
import java.util.List;

import static io.github.hielkemaps.racecommand.commands.CommandPredicates.playerInInfectedRace;

public class CommandBuilder {

    public static void register(List<Argument<?>> arguments) {

        arguments.add(new LiteralArgument("infected").withRequirement(playerInInfectedRace));

        //race option infected setFirstInfected <player>
        SetFirstInfected.register(new ArrayList<>(arguments));

        //race option infected starDelay <seconds>
        StartDelay.register(new ArrayList<>(arguments));

        //race option infected villagerRespawn <true/false>
        VillagerRespawn.register(new ArrayList<>(arguments));

        //race option infected oneHit <true/false>
        OneHit.register(new ArrayList<>(arguments));
    }
}
