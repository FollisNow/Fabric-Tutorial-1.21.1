package net.follis.tutorialmod.item.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.entity.custom.CocoonMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CaddisflyCocoonItem extends Item {

    public CaddisflyCocoonItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        CocoonData data = stack.get(ModDataComponentTypes.COCOON);
        if (data != null) {
            for (CocoonMaterial material : data.segments()) {
                tooltip.add(Text.literal(material.asString())
                        .withColor(material.getColor()));
            }
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    public static CaddisflyCocoonItem.CocoonData randomCocoonData() {
        CocoonMaterial[] materials = CocoonMaterial.values();
        Random random = Random.create();
        int segmentCount = 4;

        CocoonData.Builder builder = CocoonData.builder();
        for (int i = 0; i < segmentCount; i++) {
            builder.addMaterial(materials[random.nextInt(materials.length)]);
        }
        return builder.build();
    }

    public record CocoonData(List<CocoonMaterial> segments) {
        public static final Codec<CocoonData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(CocoonMaterial.CODEC.listOf().fieldOf("segments").forGetter(CocoonData::segments))
                        .apply(instance, CocoonData::new));

        public static final PacketCodec<ByteBuf, CocoonData> PACKET_CODEC;
        static {
            PACKET_CODEC = PacketCodec.tuple(
                    CocoonMaterial.PACKET_CODEC.collect(PacketCodecs.toList()),
                    CocoonData::segments,
                    CocoonData::new
            );
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * Convenience factory for building CocoonData straight from an entity's segment
         * list (e.g. LarvaeEntity#getCocoonSegments()), which may contain nulls for
         * unfilled slots — those are skipped automatically.
         */
        public static CocoonData fromEntitySegments(List<CocoonMaterial> entitySegments) {
            Builder builder = builder();
            entitySegments.forEach(builder::addMaterial);
            return builder.build();
        }

        public static class Builder {
            private final List<CocoonMaterial> segments = new ArrayList<>();

            public Builder addMaterial(CocoonMaterial material) {
                if (material != null) {
                    segments.add(material);
                }
                return this;
            }

            public Builder addMaterials(Collection<CocoonMaterial> materials) {
                materials.forEach(this::addMaterial);
                return this;
            }

            public CocoonData build() {
                return new CocoonData(List.copyOf(segments));
            }
        }
    }
}