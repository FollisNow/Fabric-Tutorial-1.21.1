package net.follis.tutorialmod.particle;

import net.follis.tutorialmod.entity.misc.ActiveFlySwarm;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class FlyParticle extends SpriteBillboardParticle {
    private final double moveX;
    private final double moveY;
    private final double moveZ;

    public FlyParticle(ClientWorld clientWorld, double x, double y, double z,
                       SpriteProvider spriteProvider, double xSpeed, double ySpeed, double zSpeed) {
        super(clientWorld, x, y, z, xSpeed, ySpeed, zSpeed);

        // Never rely on this.velocityX/Y/Z for movement — Particle's own constructor
        // may jitter or renormalize those fields internally. Keep our own copy instead.
        this.moveX = xSpeed;
        this.moveY = ySpeed;
        this.moveZ = zSpeed;

        this.velocityMultiplier = 1f;
        this.gravityStrength = 0f;

        this.maxAge = ActiveFlySwarm.MAX_AGE; // matches ActiveFlySwarm.MAX_AGE, so visuals stop exactly when hit-detection does
        this.setSpriteForAge(spriteProvider);

        this.red = 1f;
        this.green = 1f;
        this.blue = 1f;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }

        this.move(this.moveX, this.moveY, this.moveZ);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new FlyParticle(world, x, y, z, this.spriteProvider, velocityX, velocityY, velocityZ);
        }
    }
}