package net.follis.tutorialmod.item.custom;

import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.entity.ModEntities;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class BugJarItem extends AbstractEntityJarItem {
    private World world;


    public BugJarItem(Settings settings) {
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
        if(Screen.hasShiftDown()) {
            tooltip.add(Text.translatable("tooltip.tutorialmod.bug_jar.shift_down").withColor(darkGold.getRgb()));
            Formatting[] formatting = new Formatting[]{Formatting.GRAY};
            List<BugData> bugDataList = new ArrayList<>(stack.getOrDefault(ModDataComponentTypes.BUGS, new ArrayList<>()));
            if(!bugDataList.isEmpty()) {
                List<BugData> reversedList = bugDataList.reversed();
                for (BugData bugData: reversedList) {
                    if (Registries.ENTITY_TYPE.get(getIdentifier(bugData)) == ModEntities.BEETLE) {
                        var potionId = bugData.entityData().copyNbt().getString("PotionGene");
                        var statusEffect = Registries.STATUS_EFFECT.get(Identifier.of(potionId));
                        if (statusEffect != null) {
                            tooltip.add(Text.translatable(convertToKey(bugData)).withColor(BeetleColors.get(0).getRgb())
                                    .append(" ")
                                    .append(Text.literal(Text.translatable(statusEffect.getTranslationKey()).getString().toLowerCase())
                                            .setStyle(Style.EMPTY)
                                            .withColor(statusEffect.getColor())));
                        } else {
                            TextColor color = BeetleColors.getOrDefault(bugData.entityData().copyNbt().getInt("Variant"), TextColor.fromFormatting(Formatting.GRAY));
                            tooltip.add(Text.translatable(convertToKey(bugData)).withColor(color.getRgb()));
                        }

                    } else if (Registries.ENTITY_TYPE.get(getIdentifier(bugData)) == ModEntities.LOCUST) {
                        TextColor color = LocustColors.getOrDefault(bugData.entityData().copyNbt().getInt("Variant"), TextColor.fromFormatting(Formatting.GRAY));
                        tooltip.add(Text.translatable(convertToKey(bugData)).withColor(color.getRgb()));

                    } else if (Registries.ENTITY_TYPE.get(getIdentifier(bugData)) == ModEntities.SPIDERLING) {
                        tooltip.add(Text.translatable(convertToKey(bugData))
                                .append(" ")
                                .append(Text.literal(String.valueOf(bugData.entityData().copyNbt().getFloat("GrowthSize")))));
                    } else {
                        tooltip.add(Text.translatable(convertToKey(bugData)).formatted(formatting));
                    }
                }
            }
        } else {
            tooltip.add(Text.translatable("tooltip.tutorialmod.bug_jar"));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        this.world = user.getWorld();
        if (world instanceof ServerWorld && entity.getType().isIn(EntityTypeTags.ARTHROPOD)) {
            //Add logic for entityType or tag here
            captureEntity(entity, user);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        this.world = context.getWorld();
        if (context.getPlayer() != null && !context.getWorld().isClient) {
            if (tryReleaseBugs(context)) {
                return ActionResult.SUCCESS;
            }
        }
        return super.useOnBlock(context);
    }
}