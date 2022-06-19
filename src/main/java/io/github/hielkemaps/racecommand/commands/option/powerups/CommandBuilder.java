package io.github.hielkemaps.racecommand.commands.option.powerups;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;

import java.util.ArrayList;
import java.util.List;

public class CommandBuilder {

    public static void register(List<Argument<?>> arguments) {

        arguments.add(new LiteralArgument("powerups"));

        //race option infected powerups enabled <true/false>
        Enabled.register(new ArrayList<>(arguments));
    }
}
