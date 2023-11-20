package io.github.hielkemaps.racecommand.wrapper;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.abilities.*;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.*;

public class PlayerWrapper {

    private final UUID uuid;
    private boolean inRace = false;
    private final Set<String> raceInvites = new HashSet<>();
    private boolean isDisguised = false;
    private double maxHealth = 20;

    private final List<Ability> abilities = new ArrayList<>();

    private BukkitTask skeletonTimer;

    public PlayerWrapper(UUID uuid) {
        this.uuid = uuid;

        abilities.add(new SpeedAbility(uuid, 0));
        abilities.add(new LeapAbility(uuid, 1));
        abilities.add(new BlindAbility(uuid, 2));
        abilities.add(new GlowingAbility(uuid, 3));
        abilities.add(new ArrowAbility(uuid, 4));
    }

    public boolean isInRace() {
        return inRace;
    }

    public void setInRace(boolean value) {
        inRace = value;

        if (!value) {
            if (isOnline()) getPlayer().removeScoreboardTag("inRace");
            disableDisguise();
            removeAbilities();
            setMaxHealth(20);
            setHealth(20);
            cancelTasks();
        }

        updateRequirements();
    }

    private void cancelTasks() {
        if (skeletonTimer != null) skeletonTimer.cancel();
    }

    public boolean hasJoinableRace() {
        return !raceInvites.isEmpty() || RaceManager.hasJoinablePublicRace(uuid);
    }

    public String[] getJoinableRaces() {
        List<String> joinable = new ArrayList<>();

        //Add invites
        for (String name : raceInvites) {
            Race race = RaceManager.getRace(name);
            if (race != null && !race.hasStarted()) {
                joinable.add(race.getName());
            }
        }
        //Add open races
        for (Race race : RaceManager.getPublicRaces()) {
            if (race.hasPlayer(uuid)) continue;

            joinable.add(race.getName());
        }
        return joinable.toArray(new String[0]);
    }

    public void receiveInvite(Race race) {
        // Update data
        raceInvites.add(race.getName());
        updateRequirements();


        String joinText = race.isEvent() ? "[Join]" : "Accept";
        String inviteText = race.isEvent() ? "You have been invited to an event race! " : race.getName() + " wants to race! ";

        Component msg = Main.PREFIX
                .append(Component.text(inviteText))
                .append(Component.text(joinText, NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/race join " + race.getName())));

        // Send the message to the player
        getPlayer().sendMessage(msg);
    }

    public boolean acceptInvite(String name) {
        Race race = RaceManager.getRace(name);
        if (race == null) return false;

        //Join race
        race.addPlayer(uuid);
        race.removeInvited(uuid);

        //Update requirements
        raceInvites.remove(name);
        updateRequirements();
        return true;
    }

    public void removeInvite(String from) {
        raceInvites.remove(from);
    }

    public void updateRequirements() {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        CommandAPI.updateRequirements(player);
    }

    public Team getTeam() {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return null;

        Scoreboard scoreboard = Bukkit.getServer().getScoreboardManager().getMainScoreboard();

        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(p.getName())) return team;
        }
        return null;
    }

    public void disableDisguise() {
        if (isOnline() && isDisguised) {
            DisguiseAPI.getDisguise(getPlayer()).stopDisguise();
            isDisguised = false;
        }
    }

    public void disguiseAs(DisguiseType type) {
        if (isOnline()) {
            MobDisguise mobDisguise = new MobDisguise(type);
            mobDisguise.setEntity(getPlayer());
            mobDisguise.setKeepDisguiseOnPlayerDeath(true);
            mobDisguise.setHearSelfDisguise(true);
            mobDisguise.setViewSelfDisguise(true);
            mobDisguise.getWatcher().setCustomName(getPlayer().getName());
            mobDisguise.getWatcher().setCustomNameVisible(true);
            mobDisguise.startDisguise();
            isDisguised = true;
        }
    }

    public boolean isInInfectedRace() {
        if (!isInRace()) return false;

        Race race = RaceManager.getRace(getPlayer().getName());
        return (race instanceof InfectedRace);
    }

    public void setMaxHealth(double value) {
        maxHealth = value;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(value);
        }
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public boolean isOnline() {
        return Bukkit.getPlayer(uuid) != null;
    }

    public void sendMessage(Component msg) {
        if (isOnline()) {
            getPlayer().sendMessage(msg);
        }
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }


    public void skeletonTimer() {
        Player infected = getPlayer();
        infected.setHealth(2);
        setMaxHealth(2);
        infected.showTitle(Title.title(Component.empty(), Component.text("Healing...", NamedTextColor.RED, TextDecoration.BOLD),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))));
        infected.playSound(infected.getLocation(), Sound.ENTITY_SKELETON_HURT, 0.5F, 1.0F);

        //heal infected slowly
        skeletonTimer = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            Player player = getPlayer();

            if (player != null) {
                if (player.getHealth() < 20) {
                    double health = player.getHealth() + 2;
                    if (health >= 20) health = 20;

                    setMaxHealth(health);
                    player.setHealth(health);

                    if (health == 20) {
                        endSkeleton(player);
                    } else {
                        player.showTitle(Title.title(Component.empty(), Component.text("Healing...", NamedTextColor.RED, TextDecoration.BOLD),
                                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500))));
                        player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_HURT, 0.5F, 1.0F);
                    }
                } else {
                    endSkeleton(player);
                }
            }
        }, 30, 30);
    }

    private void endSkeleton(Player player) {
        player.showTitle(Title.title(Component.empty(), Component.text("GO", NamedTextColor.GREEN, TextDecoration.BOLD),
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(1000))));

        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 1F, 1.3F);
        skeletonTimer.cancel();

        Race race = RaceManager.getRace(getPlayer().getName());
        if (race != null) {
            race.getRacePlayer(uuid).setSkeleton(false);
        }
    }

    public void addAbilities() {
        abilities.forEach(Ability::add);
    }

    public void removeAbilities() {
        abilities.forEach(Ability::remove);
    }

    public void onPlayerJoin() {
        if (!inRace) {
            removeAbilities();
        }

        for (Ability ability : abilities) {
            ability.onPlayerJoin();
        }
    }

    public void setHealth(int value) {
        if (isOnline()) {
            getPlayer().setHealth(value);
        }
    }

    public List<Ability> getAbilities() {
        return abilities;
    }

    public void hideAbilities() {
        abilities.forEach(Ability::hide);
    }

    public void showAbilities() {
        abilities.forEach(Ability::show);
    }
}
