package net.follis.goldenscarabmod.world.gen;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.world.structure.TempleCoverPiece;
import net.follis.goldenscarabmod.world.structure.TempleShellPiece;
import net.follis.goldenscarabmod.world.structure.TempleStructure;
import net.follis.goldenscarabmod.world.structure.TempleRoomPiece;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

public final class ModStructures {
    public static final StructureType<TempleStructure> TEMPLE = Registry.register(
            Registries.STRUCTURE_TYPE, Identifier.of(GoldenScarabMod.MOD_ID, "temple"), () -> TempleStructure.CODEC);

    public static final StructurePieceType TEMPLE_ROOM_PIECE = Registry.register(
            Registries.STRUCTURE_PIECE, Identifier.of(GoldenScarabMod.MOD_ID, "temple_room"),
            (StructurePieceType.ManagerAware) TempleRoomPiece::new);

    public static final StructurePieceType TEMPLE_COVER_PIECE = Registry.register(
            Registries.STRUCTURE_PIECE, Identifier.of(GoldenScarabMod.MOD_ID, "temple_cover"),
            (StructurePieceType.ManagerAware) TempleCoverPiece::new);

    public static final StructurePieceType TEMPLE_SHELL_PIECE = Registry.register(
            Registries.STRUCTURE_PIECE, Identifier.of(GoldenScarabMod.MOD_ID, "temple_shell"),
            (StructurePieceType) (context, nbt) -> new TempleShellPiece(nbt));

    public static void registerStructures() {
        GoldenScarabMod.LOGGER.info("Registering Structures for " + GoldenScarabMod.MOD_ID);
    }
}

