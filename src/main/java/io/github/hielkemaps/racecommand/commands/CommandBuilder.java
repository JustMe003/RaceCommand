package io.github.hielkemaps.racecommand.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.skins.SkinManager;

import java.util.ArrayList;
import java.util.List;

import static io.github.hielkemaps.racecommand.commands.CommandPredicates.*;

public  class CommandBuilder {

    public static void register() {
        List<Argument<?>> arguments = new ArrayList<>();

        //CREATE NORMAL
        arguments.add(new LiteralArgument("create").withRequirement(playerInRace.negate()));
        Create.register(arguments);

        //START
        arguments.clear();
        arguments.add(new LiteralArgument("start").withRequirement(playerInRace.and(playerIsRaceOwner).and(raceHasStarted.negate().and(raceIsStarting.negate()))));
        Start.register(arguments);

        //STOP
        arguments.clear();
        arguments.add(new LiteralArgument("stop").withRequirement(playerInRace.and(playerIsRaceOwner).and(raceHasStarted.or(raceIsStarting))));
        Stop.register(arguments);

        //INVITE
        arguments.clear();
        arguments.add(new LiteralArgument("invite").withRequirement(playerInRace.and(playerIsRaceOwner)));
        Invite.register(arguments);

        //INVITEOP
        arguments.clear();
        arguments.add(new LiteralArgument("inviteOP").withRequirement(playerInRace.and(playerIsRaceOwner)).withPermission(CommandPermission.OP));
        InviteOP.register(arguments);

        //JOIN
        arguments.clear();
        arguments.add(new LiteralArgument("join").withRequirement(playerHasJoinableRaces));
        Join.register(arguments);

        //LEAVE
        arguments.clear();
        arguments.add(new LiteralArgument("leave").withRequirement(playerInRace.and(playerIsRaceOwner.negate())));
        Leave.register(arguments);

        //DISBAND
        arguments.clear();
        arguments.add(new LiteralArgument("disband").withRequirement(playerInRace.and(playerIsRaceOwner)));
        Disband.register(arguments);

        //INFO
        arguments.clear();
        arguments.add(new LiteralArgument("info").withRequirement(playerInRace));
        Info.register(arguments);

        //KICK
        arguments.clear();
        arguments.add(new LiteralArgument("kick").withRequirement(playerInRace.and(playerIsRaceOwner).and(playerToKick)));
        Kick.register(arguments);

        //FORCEJOIN
        arguments.clear();
        arguments.add(new LiteralArgument("forcejoin").withRequirement(playerInRace.and(playerIsRaceOwner)).withPermission(CommandPermission.OP));
        ForceJoin.register(arguments);

        //OPTION
        arguments.clear();
        arguments.add(new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        io.github.hielkemaps.racecommand.commands.option.CommandBuilder.register(arguments);

        //SKIN TEST
        arguments.clear();
        arguments.add(new LiteralArgument("setskin").withPermission(CommandPermission.OP));
        arguments.add(new StringArgument("name"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    SkinManager.changeSkin(p, (String) args.get(0));
                    p.sendMessage(Main.PREFIX + "Set skin to " + args.get(0));
                }).register();
    }
}
