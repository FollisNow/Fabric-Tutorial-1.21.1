package net.follis.goldenscarabmod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.follis.goldenscarabmod.entity.custom.GoldenSwarmEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.List;

/**
 * Debug commands for testing the boss without waiting for the random planner or for its phases. They act on the
 * nearest boss (a clone is ignored) within 64 blocks and need permission level 2 (operators, command blocks).
 *
 *   /goldenswarm force <shape|burst|translocate|clone|arm|tentacles>   make that attack the next one, as soon as it can start
 *   /goldenswarm sethealth <1-100>                           set the boss' health to that percentage
 *   /goldenswarm status                                      phase, health, planned attack, cooldown
 *
 * Call GoldenSwarmCommands.register() once from your mod's onInitialize().
 */
public final class GoldenSwarmCommands {

    // the order is the pattern ids 1..6 of GoldenSwarmEntity (SHAPE_THROW, BURST, TRANSLOCATE, CLONE, ARM_SLAM, TENTACLES)
    private static final List<String> ATTACKS = List.of("shape", "burst", "translocate", "clone", "arm", "tentacles");

    private GoldenSwarmCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("goldenswarm")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("force")
                                .then(CommandManager.argument("attack", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(ATTACKS, builder))
                                        .executes(context -> force(context.getSource(),
                                                StringArgumentType.getString(context, "attack")))))
                        .then(CommandManager.literal("sethealth")
                                .then(CommandManager.argument("percent", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> setHealth(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "percent")))))
                        .then(CommandManager.literal("status")
                                .executes(context -> status(context.getSource())))));
    }

    // ------------------------------------------------------------------------------------ commands

    private static int force(ServerCommandSource source, String name) {
        int index = ATTACKS.indexOf(name.toLowerCase());
        if (index < 0) {
            source.sendError(Text.literal("Unknown attack '" + name + "'. Choose one of: " + String.join(", ", ATTACKS)));
            return 0;
        }
        GoldenSwarmEntity boss = nearestBoss(source);
        if (boss == null) {
            source.sendError(Text.literal("No Golden Swarm within 64 blocks."));
            return 0;
        }
        boss.debugForcePattern(index + 1);
        source.sendFeedback(() -> Text.literal("Next attack: " + ATTACKS.get(index)
                + ". It starts as soon as the boss has a target in that attack's range."), true);
        return 1;
    }

    private static int setHealth(ServerCommandSource source, int percent) {
        GoldenSwarmEntity boss = nearestBoss(source);
        if (boss == null) {
            source.sendError(Text.literal("No Golden Swarm within 64 blocks."));
            return 0;
        }
        boss.setHealth(boss.getMaxHealth() * percent / 100f);
        source.sendFeedback(() -> Text.literal("Boss health set to " + percent + "% (phase " + boss.getPhase() + ")."), true);
        return 1;
    }

    private static int status(ServerCommandSource source) {
        GoldenSwarmEntity boss = nearestBoss(source);
        if (boss == null) {
            source.sendError(Text.literal("No Golden Swarm within 64 blocks."));
            return 0;
        }
        int planned = boss.debugPlannedPattern();
        String plannedName = planned >= 1 && planned <= ATTACKS.size() ? ATTACKS.get(planned - 1) : "none yet";
        int readyIn = boss.debugTicksUntilReady();
        String message = String.format("Phase %d | health %.0f/%.0f | next attack: %s | ready in %.1fs | clone: %s | airborne: %s | target: %s",
                boss.getPhase(), boss.getHealth(), boss.getMaxHealth(), plannedName, readyIn / 20.0,
                boss.hasClone() ? "yes" : "no", boss.isAirborne() ? "yes" : "no",
                boss.getTarget() != null ? boss.getTarget().getName().getString() : "none");
        source.sendFeedback(() -> Text.literal(message), false);
        return 1;
    }

    // ------------------------------------------------------------------------------------ helpers

    /** The living boss (not a clone) closest to whoever ran the command, within 64 blocks. */
    private static GoldenSwarmEntity nearestBoss(ServerCommandSource source) {
        Vec3d here = source.getPosition();
        Box area = new Box(here, here).expand(64.0);
        GoldenSwarmEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (GoldenSwarmEntity candidate : source.getWorld().getEntitiesByClass(GoldenSwarmEntity.class, area,
                e -> e.isAlive() && !e.isClone())) {
            double d = candidate.squaredDistanceTo(here);
            if (d < bestDistance) {
                bestDistance = d;
                best = candidate;
            }
        }
        return best;
    }
}