package io.github.hielkemaps.racecommand.commands.option;

import dev.jorel.commandapi.arguments.Argument;

import java.util.ArrayList;
import java.util.List;

public class CommandBuilder {

    public static void register(List<Argument<?>> arguments){

        //race option visibility <public/private>
        Visibility.register(new ArrayList<>(arguments));

        //race option ghostplayers <true/false>
        GhostPlayers.register(new ArrayList<>(arguments));

        //race option broadcast
        Broadcast.register(new ArrayList<>(arguments));

        //race option infected
        io.github.hielkemaps.racecommand.commands.option.infected.CommandBuilder.register(new ArrayList<>(arguments));

        //race option powerups
        io.github.hielkemaps.racecommand.commands.option.powerups.CommandBuilder.register(new ArrayList<>(arguments));
    }
}
