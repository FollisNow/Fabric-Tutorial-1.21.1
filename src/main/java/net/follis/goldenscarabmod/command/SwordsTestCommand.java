package net.follis.goldenscarabmod.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.follis.goldenscarabmod.entity.custom.FlyingSwordEntity;
import net.follis.goldenscarabmod.pattern.SwordFlock;
import net.follis.goldenscarabmod.pattern.SwordPatterns;
import net.follis.goldenscarabmod.pattern.SwordShield;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public final class SwordsTestCommand {
    private SwordsTestCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("swords")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("orbit").executes(ctx -> attack(ctx.getSource(), 0)))
                        .then(CommandManager.literal("ground").executes(ctx -> attack(ctx.getSource(), 1)))
                        .then(CommandManager.literal("wall").executes(ctx -> attack(ctx.getSource(), 2)))
                        .then(CommandManager.literal("flock").executes(ctx -> flock(ctx.getSource(), true)))
                        .then(CommandManager.literal("flockstop").executes(ctx -> flock(ctx.getSource(), false)))
                        .then(CommandManager.literal("shield").executes(ctx -> shield(ctx.getSource(), true)))
                        .then(CommandManager.literal("shieldstop").executes(ctx -> shield(ctx.getSource(), false)))
                        .then(CommandManager.literal("black").executes(ctx -> wanderer(ctx.getSource(), FlyingSwordEntity.Kind.BLACK)))
                        .then(CommandManager.literal("red").executes(ctx -> wanderer(ctx.getSource(), FlyingSwordEntity.Kind.RED)))));
    }

    private static int attack(ServerCommandSource source, int which) {
        if (!(source.getEntity() instanceof LivingEntity caster)) return 0;
        ServerWorld world = source.getWorld();
        Vec3d look = caster.getRotationVec(1.0F);
        Predicate<LivingEntity> targets = e -> e != caster;   // everything but you

        int duration = switch (which) {
            case 0 -> SwordPatterns.orbitingSwords(world, caster, targets);
            case 1 -> SwordPatterns.groundStabs(world, caster, look, targets);
            default -> SwordPatterns.swordWall(world, caster, look, targets);
        };
        source.sendFeedback(() -> Text.literal("The attack lasts " + duration + " ticks"), false);
        return 1;
    }
    /** You play the boss: the shield goes up around you for 20 seconds. */
    private static int shield(ServerCommandSource source, boolean start) {
        if (!(source.getEntity() instanceof LivingEntity caster)) return 0;
        if (start) {
            int duration = SwordShield.start(source.getWorld(), caster, 400, p -> true);
            source.sendFeedback(() -> Text.literal("Shield up for " + duration + " ticks"), false);
        } else {
            SwordShield.stop(caster);
            source.sendFeedback(() -> Text.literal("Shield down"), false);
        }
        return 1;
    }
    /** You play the boss: the arena is a circle of 14 blocks around where you stand. */
    private static int flock(ServerCommandSource source, boolean start) {
        if (!(source.getEntity() instanceof LivingEntity caster)) return 0;
        if (start) {
            SwordFlock.start(source.getWorld(), caster, caster.getPos(), 14.0, e -> e != caster);
            source.sendFeedback(() -> Text.literal("Flock started around you"), false);
        } else {
            SwordFlock.stop(caster);
            source.sendFeedback(() -> Text.literal("Flock stopped"), false);
        }
        return 1;
    }

    private static int wanderer(ServerCommandSource source, FlyingSwordEntity.Kind kind) {
        if (!(source.getEntity() instanceof LivingEntity caster)) return 0;
        SwordFlock.addWanderer(caster, kind);
        return 1;
    }
}