package net.follis.goldenscarabmod.entity.client;

import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;

public class GoldBreederAnimations {
    public static final Animation GOLD_BREEDER_WALK = Animation.Builder.create(0.5F).looping()
            .addBoneAnimation("root", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("wheel", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.125F, AnimationHelper.createRotationalVector(90.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.25F, AnimationHelper.createRotationalVector(180.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.375F, AnimationHelper.createRotationalVector(270.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.5F, AnimationHelper.createRotationalVector(360.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .build();

    public static final Animation GOLD_BREEDER_HARVEST_CROP = Animation.Builder.create(1.0F)
            .addBoneAnimation("right_arm_root", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("right_arm_shoulder", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.25F, AnimationHelper.createRotationalVector(-207.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.75F, AnimationHelper.createRotationalVector(-207.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("right_arm_end", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("right_arm_forearm", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.25F, AnimationHelper.createRotationalVector(0.0F, -90.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.75F, AnimationHelper.createRotationalVector(0.0F, -90.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("right_arm_blade", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.2917F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.3333F, AnimationHelper.createRotationalVector(0.0F, 35.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.375F, AnimationHelper.createRotationalVector(0.0F, 70.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.4167F, AnimationHelper.createRotationalVector(0.0F, 105.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.4583F, AnimationHelper.createRotationalVector(0.0F, 140.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.5F, AnimationHelper.createRotationalVector(0.0F, 175.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.5417F, AnimationHelper.createRotationalVector(0.0F, 210.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.5833F, AnimationHelper.createRotationalVector(0.0F, 245.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.625F, AnimationHelper.createRotationalVector(0.0F, 280.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.6667F, AnimationHelper.createRotationalVector(0.0F, 305.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.7083F, AnimationHelper.createRotationalVector(0.0F, 340.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .build();

    public static final Animation GOLD_BREEDER_HARVEST_MEAT = Animation.Builder.create(1.0F)
            .addBoneAnimation("right_arm_root", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("right_arm_shoulder", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.25F, AnimationHelper.createRotationalVector(-60.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.75F, AnimationHelper.createRotationalVector(-60.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("right_arm_end", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("right_arm_forearm", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.25F, AnimationHelper.createRotationalVector(0.0F, -90.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.75F, AnimationHelper.createRotationalVector(0.0F, -90.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("right_arm_blade", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.2917F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.3333F, AnimationHelper.createRotationalVector(0.0F, 35.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.375F, AnimationHelper.createRotationalVector(0.0F, 70.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.4167F, AnimationHelper.createRotationalVector(0.0F, 105.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.4583F, AnimationHelper.createRotationalVector(0.0F, 140.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.5F, AnimationHelper.createRotationalVector(0.0F, 175.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.5417F, AnimationHelper.createRotationalVector(0.0F, 210.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.5833F, AnimationHelper.createRotationalVector(0.0F, 245.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.625F, AnimationHelper.createRotationalVector(0.0F, 280.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.6667F, AnimationHelper.createRotationalVector(0.0F, 305.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.7083F, AnimationHelper.createRotationalVector(0.0F, 340.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .build();

    public static final Animation GOLD_BREEDER_LOVE_GUN = Animation.Builder.create(0.4167F)
            .addBoneAnimation("left_love_gun", new Transformation(Transformation.Targets.SCALE,
                    new Keyframe(0.0F, AnimationHelper.createScalingVector(1.0F, 1.0F, 1.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.0833F, AnimationHelper.createScalingVector(1.0F, 1.0F, 1.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.125F, AnimationHelper.createScalingVector(1.0F, 0.7F, 1.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.1667F, AnimationHelper.createScalingVector(1.0F, 1.0F, 1.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.25F, AnimationHelper.createScalingVector(1.0F, 1.0F, 1.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.2917F, AnimationHelper.createScalingVector(1.0F, 0.7F, 1.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.3333F, AnimationHelper.createScalingVector(1.0F, 1.0F, 1.0F), Transformation.Interpolations.LINEAR)
            ))
            .build();

    public static final Animation GOLD_BREEDER_IDLE = Animation.Builder.create(1.0F).looping()
            .addBoneAnimation("flowers", new Transformation(Transformation.Targets.SCALE,
                    new Keyframe(0.0F, AnimationHelper.createScalingVector(1.0F, 1.0F, 1.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.5F, AnimationHelper.createScalingVector(1.0F, 1.02F, 1.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createScalingVector(1.0F, 1.0F, 1.0F), Transformation.Interpolations.LINEAR)
            ))
            .build();
}
