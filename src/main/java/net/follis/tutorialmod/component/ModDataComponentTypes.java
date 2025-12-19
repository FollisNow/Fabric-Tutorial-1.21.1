package net.follis.tutorialmod.component;

import com.mojang.serialization.Codec;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.block.entity.custom.AmethystBeeHiveBlockEntity;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.List;
import java.util.function.UnaryOperator;

public class ModDataComponentTypes {
    public static final ComponentType<BlockPos> COORDINATES_BLOCK =
            register("coordinates_block", builder -> builder.codec(BlockPos.CODEC));
    public static final ComponentType<Vec3i> COORDINATES_CHUNK =
            register("coordinates_chunk", builder -> builder.codec(Vec3i.CODEC));
    public static final ComponentType<String> BIOME =
            register("biome", builder -> builder.codec(Codec.STRING));
    public static final ComponentType<Integer> ENTITY_COUNT0 =
            register("entity_count0", builder -> builder.codec(Codec.INT));
    public static final ComponentType<Integer> ENTITY_COUNT1 =
            register("entity_count1", builder -> builder.codec(Codec.INT));





    public static final ComponentType<List<AmethystBeeHiveBlockEntity.AmethystBeeData>> AMETHYST_BEES =
            register("bees", (builder) -> builder.codec(AmethystBeeHiveBlockEntity.AmethystBeeData.LIST_CODEC)
                    .packetCodec(AmethystBeeHiveBlockEntity.AmethystBeeData.PACKET_CODEC.collect(PacketCodecs.toList())).cache());

    public static final ComponentType<List<AbstractEntityJarItem.BugData>> BUGS =
            register("bugs", builder -> builder.codec(AbstractEntityJarItem.BugData.LIST_CODEC)
                    .packetCodec(AbstractEntityJarItem.BugData.PACKET_CODEC.collect(PacketCodecs.toList())).cache());

    public static final ComponentType<Integer> BUG_VARIANT =
            register("bug_variant", builder -> builder.codec(Codec.INT));
    public static final ComponentType<String> ENTITY_TYPE =
            register("entity_type", builder -> builder.codec(Codec.STRING));


    public static final ComponentType<Integer> ENTITY_ID_CODEC =
            register("golden_needle_target", builder -> builder.codec(Codec.INT));
    public static final ComponentType<Integer> GOLDEN_NEEDLE_STACKS_CODEC =
            register("golden_needle_stacks", builder -> builder.codec(Codec.INT));

    public static final ComponentType<Integer> POISON_TRACKING =
            register("poison_tracking", builder -> builder.codec(Codec.INT));


    private static <T>ComponentType<T> register(String name, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(TutorialMod.MOD_ID, name),
                builderOperator.apply(ComponentType.builder()).build());
    }

    public static void registerDataComponentTypes() {
        TutorialMod.LOGGER.info("Registering Data Component Types for " + TutorialMod.MOD_ID);
    }
}
