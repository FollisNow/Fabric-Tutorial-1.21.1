package net.follis.goldenscarabmod.world.tree;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.world.ModConfiguredFeatures;
import net.minecraft.block.SaplingGenerator;

import java.util.Optional;

public class ModSaplingGenerators {
    public static final SaplingGenerator GOLDEN_TREE = new SaplingGenerator(GoldenScarabMod.MOD_ID + ":golden_tree",
            Optional.empty(), Optional.of(ModConfiguredFeatures.GOLDEN_TREE_KEY), Optional.empty());
}
