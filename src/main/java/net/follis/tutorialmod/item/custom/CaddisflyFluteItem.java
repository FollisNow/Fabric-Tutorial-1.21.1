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
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CaddisflyFluteItem extends Item {

    public CaddisflyFluteItem(Settings settings) {
        super(settings);
    }
    @Override
    public Text getName() {
        return Text.translatable(this.getTranslationKey()).formatted(Formatting.GOLD);
    }
    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        CaddisflyCocoonItem.CocoonData data = stack.get(ModDataComponentTypes.COCOON);
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

        CaddisflyCocoonItem.CocoonData.Builder builder = CaddisflyCocoonItem.CocoonData.builder();
        for (int i = 0; i < segmentCount; i++) {
            builder.addMaterial(materials[random.nextInt(materials.length)]);
        }
        return builder.build();
    }
}