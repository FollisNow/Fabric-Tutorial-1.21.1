package net.follis.tutorialmod.world.tree;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.world.ModConfiguredFeatures;
import net.minecraft.block.SaplingGenerator;

import java.util.Optional;

public class ModSaplingGenerators {
    public static final SaplingGenerator GOLDEN_TREE = new SaplingGenerator(TutorialMod.MOD_ID + ":golden_tree",
            Optional.empty(), Optional.of(ModConfiguredFeatures.GOLDEN_TREE_KEY), Optional.empty());
}
