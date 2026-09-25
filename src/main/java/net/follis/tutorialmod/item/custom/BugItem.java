package net.follis.tutorialmod.item.custom;

import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.MothVariant;
import net.follis.tutorialmod.entity.custom.SpiderlingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

import static net.follis.tutorialmod.util.IBugVariants.*;
import static net.follis.tutorialmod.util.IBugVariants.LocustColors;
import static net.follis.tutorialmod.util.IBugVariants.getColorOrDefault;

public class BugItem extends Item {
    public BugItem(Settings settings) {
        super(settings);
    }

    public static ItemStack createFigurine(Item item, LivingEntity entity) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponentTypes.CAPTURED_BUG, AbstractEntityJarItem.BugData.of(entity));
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        AbstractEntityJarItem.BugData bugData = stack.get(ModDataComponentTypes.CAPTURED_BUG);
        Formatting[] formatting = new Formatting[]{Formatting.GRAY};

        if (bugData != null) {
            if (Registries.ENTITY_TYPE.get(bugData.getIdentifier()) == ModEntities.BEETLE) {
                var potionId = bugData.getNbt().getString("PotionGene");
                var statusEffect = Registries.STATUS_EFFECT.get(Identifier.of(potionId));
                if (statusEffect != null) {
                    tooltip.add(Text.translatable(bugData.convertToKey()).withColor(BeetleColors.get(0).getRgb())
                            .append(" ")
                            .append(Text.literal(Text.translatable(statusEffect.getTranslationKey()).getString().toLowerCase())
                                    .setStyle(Style.EMPTY)
                                    .withColor(statusEffect.getColor())));
                } else {
                    TextColor color = getColorOrDefault(BeetleColors, bugData.getNbt().getInt("Variant"));
                    tooltip.add(Text.translatable(bugData.convertToKey()).withColor(color.getRgb()));
                }

            } else if (Registries.ENTITY_TYPE.get(bugData.getIdentifier()) == ModEntities.MOTH) {
                TextColor color = getColorOrDefault(MothColors, bugData.getNbt().getInt("Variant"));
                tooltip.add(Text.translatable(bugData.convertToKey()).withColor(color.getRgb()).append(" ").append(Text.literal(MothVariant.byId(bugData.getNbt().getInt("Variant")).getName())));

            }
            else if (Registries.ENTITY_TYPE.get(bugData.getIdentifier()) == ModEntities.LOCUST) {
                TextColor color = getColorOrDefault(LocustColors, bugData.getNbt().getInt("Variant"));
                tooltip.add(Text.translatable(bugData.convertToKey()).withColor(color.getRgb()));

            } else if (Registries.ENTITY_TYPE.get(bugData.getIdentifier()) == ModEntities.SPIDERLING) {
                String formattedGrowthSize = String.format("%.2f", bugData.getNbt().getFloat("GrowthSize")); // Format to 3 decimal places
                int growthSizeColor = interpolateColor(bugData.getNbt().getFloat("GrowthSize"), SpiderlingEntity.MINIMUM_SIZE, SpiderlingEntity.MAXIMUM_SIZE);

                String formattedMaxHealth = String.format("%.2f", bugData.getNbt().getFloat("MaxHealth")); // Format to 3 decimal places
                int maxHealthColor = interpolateColor(bugData.getNbt().getFloat("MaxHealth"), SpiderlingEntity.MINIMUM_HEALTH, SpiderlingEntity.MAXIMUM_HEALTH);

                String formattedSpeed = String.format("%.2f", bugData.getNbt().getFloat("MovementSpeed")); // Format to 3 decimal places
                int speedColor = interpolateColor(bugData.getNbt().getFloat("MovementSpeed"), SpiderlingEntity.MINIMUM_SPEED, SpiderlingEntity.MAXIMUM_SPEED);

                String formattedJumpStrength = String.format("%.2f", bugData.getNbt().getFloat("JumpStrength")); // Format to 3 decimal places
                int jumpStrengthColor = interpolateColor(bugData.getNbt().getFloat("JumpStrength"), SpiderlingEntity.MINIMUM_JUMP, SpiderlingEntity.MAXIMUM_JUMP);

                tooltip.add(Text.translatable(bugData.convertToKey())
                        .append(" ")
                        .append(Text.literal("⇵" + formattedGrowthSize + "⇵  ").withColor(growthSizeColor))
                        .append(Text.literal("♥" + formattedMaxHealth + "♥  ").withColor(maxHealthColor))
                        .append(Text.literal("»" + formattedSpeed + "»  ").withColor(speedColor))
                        .append(Text.literal("⏶" + formattedJumpStrength + "⏶").withColor(jumpStrengthColor))
                );
            } else {
                tooltip.add(Text.translatable(bugData.convertToKey()).formatted(formatting));
            }
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    protected int interpolateColor(float value, float minValue, float maxValue) {
        float clampedValue = MathHelper.clamp((value - minValue) / (maxValue - minValue), 0F, 1F);

        int r = (int) (255 * (1 - clampedValue)); // Red decreases
        int g = (int) (255 * clampedValue);       // Green increases
        int b = 0; // Keep blue as 0

        return (r << 16) | (g << 8) | b; // Combine RGB to integer
    }
}