package net.follis.goldenscarabmod.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.follis.goldenscarabmod.GoldenScarabMod;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {
    public static final SimpleParticleType PINK_GARNET_PARTICLE =
            registerParticle("pink_garnet_particle", FabricParticleTypes.simple());

    public static final SimpleParticleType FLY_PARTICLE =
            registerParticle("fly_particle", FabricParticleTypes.simple());

    public static final SimpleParticleType GOLDEN_LEAVES_PARTICLE =
            registerParticle("golden_leaves_particle", FabricParticleTypes.simple());

    public static final SimpleParticleType GOLDEN_CHAIN_PARTICLE =
            registerParticle("golden_chain_particle", FabricParticleTypes.simple());

    private static SimpleParticleType registerParticle(String name, SimpleParticleType particleType) {
        return Registry.register(Registries.PARTICLE_TYPE, Identifier.of(GoldenScarabMod.MOD_ID, name), particleType);
    }

    public static void registerParticles() {
        GoldenScarabMod.LOGGER.info("Registering Particles for " + GoldenScarabMod.MOD_ID);
    }
}
