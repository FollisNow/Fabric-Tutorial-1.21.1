package net.follis.tutorialmod.entity;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<MantisEntity> MANTIS = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "mantis"),
            EntityType.Builder.create(MantisEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 2.5f).build());

    public static final EntityType<LocustEntity> LOCUST = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "locust"),
            EntityType.Builder.create(LocustEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.5f, 0.5f).build());

    public static final EntityType<AmethystBeeEntity> AMETHYST_BEE = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "amethyst_bee"),
            EntityType.Builder.create(AmethystBeeEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.5f, 0.5f).build());

    public static final EntityType<BeetleEntity> BEETLE = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "beetle"),
            EntityType.Builder.create(BeetleEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.5f, 0.5f).build());

    public static final EntityType<SpiderlingEntity> SPIDERLING = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "spiderling"),
            EntityType.Builder.create(SpiderlingEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.7f, 0.7f).build());

    public static final EntityType<MothEntity> MOTH = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "moth"),
            EntityType.Builder.create(MothEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.35f, 0.75f).build());


    public static final EntityType<TomahawkProjectileEntity> TOMAHAWK = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "tomahawk"),
            EntityType.Builder.<TomahawkProjectileEntity>create(TomahawkProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 1.15f).build());

    public static final EntityType<BambooTrapEntity> BAMBOO_TRAP = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "bamboo_trap"),
            EntityType.Builder.<BambooTrapEntity>create(BambooTrapEntity::new, SpawnGroup.MISC)
                    .dimensions(0.4f, 0.2f).build());

    public static final EntityType<DartEntity> DART = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "dart"),
            EntityType.Builder.<DartEntity>create(DartEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25f, 0.2f).build());

    public static final EntityType<GoldenNeedleProjectileEntity> GOLDEN_NEEDLE = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "golden_needle"),
            EntityType.Builder.<GoldenNeedleProjectileEntity>create(GoldenNeedleProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f).build());

    public static final EntityType<MiniSunProjectileEntity> MINI_SUN = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "mini_sun"),
            EntityType.Builder.<MiniSunProjectileEntity>create(MiniSunProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f).build());

    public static final EntityType<ChairEntity> CHAIR = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TutorialMod.MOD_ID, "chair_entity"),
            EntityType.Builder.create(ChairEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f).build());



    public static void registerModEntities() {
        TutorialMod.LOGGER.info("Registering Mod Entities for " + TutorialMod.MOD_ID);
    }
}
