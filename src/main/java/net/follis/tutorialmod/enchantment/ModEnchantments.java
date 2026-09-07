package net.follis.tutorialmod.enchantment;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.enchantment.custom.LightningStrikerEnchantmentEffect;
import net.follis.tutorialmod.enchantment.custom.LordOfTheFliesEnchantmentEffect;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.effect.EnchantmentEffectTarget;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

public class ModEnchantments {
    public static final RegistryKey<Enchantment> LIGHTNING_STRIKER =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(TutorialMod.MOD_ID, "lightning_striker"));

    public static final RegistryKey<Enchantment> LORD_OF_THE_FLIES =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(TutorialMod.MOD_ID, "lord_of_the_flies"));


    public static void bootstrap(Registerable<Enchantment> registerable) {
        var enchantments = registerable.getRegistryLookup(RegistryKeys.ENCHANTMENT);
        var items = registerable.getRegistryLookup(RegistryKeys.ITEM);

        register(registerable, LIGHTNING_STRIKER, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                items.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                5,
                2,
                Enchantment.leveledCost(5, 7),
                Enchantment.leveledCost(25, 9),
                2,
                AttributeModifierSlot.MAINHAND))
                .exclusiveSet(enchantments.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE_SET))
                .addEffect(EnchantmentEffectComponentTypes.POST_ATTACK,
                        EnchantmentEffectTarget.ATTACKER, EnchantmentEffectTarget.VICTIM,
                        new LightningStrikerEnchantmentEffect()));

        register(registerable, LORD_OF_THE_FLIES, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ItemTags.HEAD_ARMOR_ENCHANTABLE),
                5,
                1,
                Enchantment.constantCost(5),
                Enchantment.constantCost(5),
                2,
                AttributeModifierSlot.HEAD))
                .exclusiveSet(enchantments.getOrThrow(EnchantmentTags.ARMOR_EXCLUSIVE_SET)));
    }


    private static void register(Registerable<Enchantment> registry, RegistryKey<Enchantment> key, Enchantment.Builder builder) {
        registry.register(key, builder.build(key.getValue()));
    }
}
