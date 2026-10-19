package net.follis.goldenscarabmod.enchantment;

import com.mojang.serialization.MapCodec;
import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.enchantment.custom.LightningStrikerEnchantmentEffect;
import net.follis.goldenscarabmod.enchantment.custom.LordOfTheFliesEnchantmentEffect;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEnchantmentEffects {
    public static final MapCodec<? extends EnchantmentEntityEffect> LIGHTNING_STRIKER =
            registerEntityEffect("lightning_striker", LightningStrikerEnchantmentEffect.CODEC);

    public static final MapCodec<? extends EnchantmentEntityEffect> LORD_OF_THE_FLIES =
            registerEntityEffect("lord_of_the_flies", LordOfTheFliesEnchantmentEffect.CODEC);

    private static MapCodec<? extends EnchantmentEntityEffect> registerEntityEffect(String name,
                                                                                    MapCodec<? extends EnchantmentEntityEffect> codec) {
        return Registry.register(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, Identifier.of(GoldenScarabMod.MOD_ID, name), codec);
    }

    public static void registerEnchantmentEffects() {
        GoldenScarabMod.LOGGER.info("Registering Mod Enchantment Effects for " + GoldenScarabMod.MOD_ID);
    }
}
