package net.follis.goldenscarabmod.item.custom;

import net.follis.goldenscarabmod.component.ModDataComponentTypes;
import net.follis.goldenscarabmod.entity.custom.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.List;

import static net.follis.goldenscarabmod.util.IBugVariants.getColorOrDefault;

public class BugItem extends Item {
    public BugItem(Settings settings) {
        super(settings);
    }

    public static ItemStack createFigurine(Item item, LivingEntity entity) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponentTypes.CAPTURED_BUG, AbstractEntityJarItem.BugData.of(entity));
        if (entity instanceof LarvaeEntity larvae) {
            List<CocoonMaterial> segments = larvae.getCocoonSegments();
            boolean hasAnyMaterial = segments.stream().anyMatch(m -> m != null);
            if (hasAnyMaterial) {
                stack.set(ModDataComponentTypes.COCOON, CaddisflyCocoonItem.CocoonData.fromEntitySegments(segments));
            }
        }
        return stack;
    }

    @Override
    public Text getName(ItemStack stack) {
        AbstractEntityJarItem.BugData bugData = stack.get(ModDataComponentTypes.CAPTURED_BUG);
        if (bugData == null)
            return super.getName(stack);

        Text bugName = Registries.ENTITY_TYPE.get(bugData.getIdentifier()).getName();
        return Text.translatable("item.goldenscarabmod.specimen", bugName);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        BugTooltips.appendFigurine(tooltip, stack, context.getRegistryLookup());
        super.appendTooltip(stack, context, tooltip, type);
    }

}