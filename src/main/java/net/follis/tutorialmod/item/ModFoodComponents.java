package net.follis.tutorialmod.item;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class ModFoodComponents {
    public static final FoodComponent CAULIFLOWER = new FoodComponent.Builder().nutrition(3).saturationModifier(0.25f)
            .statusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 200), 0.15f).build();

    public static final FoodComponent LOCUST_GOLD = new FoodComponent.Builder().nutrition(2).saturationModifier(0.15f)
            .statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 2), 1f)
            .statusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200), 1f)
            .statusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 200), 1f).snack().build();
    public static final FoodComponent LOCUST_DREAM = new FoodComponent.Builder().nutrition(2).saturationModifier(0.15f)
            .statusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 200), 1f).snack().build();
    public static final FoodComponent LOCUST_GRASSHOPPER = new FoodComponent.Builder().nutrition(1).saturationModifier(0.15f).snack().build();
    public static final FoodComponent LOCUST_RED = new FoodComponent.Builder().nutrition(1).saturationModifier(0.15f)
            .statusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200, 3), 1f)
            .statusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 3), 1f).snack().build();
    public static final FoodComponent GRILLED_LOCUST = new FoodComponent.Builder().nutrition(4).saturationModifier(0.35f).snack().build();

    public static final FoodComponent HONEY_BERRY = new FoodComponent.Builder().nutrition(2).saturationModifier(0.15f)
            .snack().build();

}
