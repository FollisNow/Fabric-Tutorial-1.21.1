package net.follis.goldenscarabmod.world.structure;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.follis.goldenscarabmod.GoldenScarabMod;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TempleCommand {
    private static volatile boolean clearOverrides = false;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (clearOverrides) {
                clearOverrides = false;
                TempleStructure.debugSize = null;
                TempleStructure.debugShell = true;
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("temple")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("room")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .then(CommandManager.argument("turns", IntegerArgumentType.integer(0, 3))
                                                .executes(ctx -> placeRoom(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name"), IntegerArgumentType.getInteger(ctx, "turns"))))))
                        .then(CommandManager.argument("size", IntegerArgumentType.integer(6, 12))
                                .suggests((ctx, b) -> CommandSource.suggestMatching(List.of("6", "8", "10", "12"), b))
                                .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "size"), true))
                                .then(CommandManager.literal("shell")
                                        .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "size"), true)))
                                .then(CommandManager.literal("noshell")
                                        .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "size"), false))))));


    }

    private static int run(ServerCommandSource source, int size, boolean shell) {
        if (size % 2 != 0) {
            source.sendError(Text.literal("Size must be 6, 8, 10 or 12"));
            return 0;
        }
        TempleStructure.debugSize = size;
        TempleStructure.debugShell = shell;
        clearOverrides = true;   // cleared at the end of this tick, after the queued /place has run

        BlockPos p = BlockPos.ofFloored(source.getPosition());
        source.getServer().getCommandManager().executeWithPrefix(source,
                "place structure " + GoldenScarabMod.MOD_ID + ":temple " + p.getX() + " " + p.getY() + " " + p.getZ());
        return 1;
    }
    private static int placeRoom(ServerCommandSource src, String name, int turns) {
        Identifier tid = Identifier.of(GoldenScarabMod.MOD_ID, "temple/" + name);
        RoomDef def = TempleRooms.find(tid);
        Optional<StructureTemplate> template = src.getWorld().getStructureTemplateManager().getTemplate(tid);
        if (def == null || template.isEmpty()) { src.sendError(Text.literal("Unknown room " + name)); return 0; }

        BlockPos corner = BlockPos.ofFloored(src.getPosition());
        TempleLayout.Placed p = new TempleLayout.Placed(def, turns, def.shape(turns), 0, 0, 0);
        BlockPos origin = TempleStructure.roomOrigin(corner, p);
        StructurePlacementData data = new StructurePlacementData()
                .setRotation(TempleStructure.rotationFor(turns)).setMirror(BlockMirror.NONE)
                .addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS);
        template.get().place(src.getWorld(), origin, origin, data, src.getWorld().getRandom(), Block.NOTIFY_LISTENERS);

        String doors = def.shape(turns).ports().stream().map(q -> q.side().name()).collect(Collectors.joining(" "));
        src.sendFeedback(() -> Text.literal(name + " turns=" + turns + " expected doors: " + doors), false);
        return 1;
    }
}
