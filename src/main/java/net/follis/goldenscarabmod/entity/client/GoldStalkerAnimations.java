package net.follis.goldenscarabmod.entity.client;

import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;

public class GoldStalkerAnimations {
    public static final Animation WALK_ANIMATION = Animation.Builder.create(0.5F).looping()
            .addBoneAnimation("root", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("wheel", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.125F, AnimationHelper.createRotationalVector(90.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.25F, AnimationHelper.createRotationalVector(180.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.375F, AnimationHelper.createRotationalVector(270.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.5F, AnimationHelper.createRotationalVector(360.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("gold_stalker", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("gold_stalker", new Transformation(Transformation.Targets.TRANSLATE,
                    new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, -6.0F), Transformation.Interpolations.LINEAR)
            ))
            .build();

    public static final Animation PICKUP_ANIMATION = Animation.Builder.create(1.0F)
            .addBoneAnimation("right_arm_joint", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.08F, AnimationHelper.createRotationalVector(-47.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.38F, AnimationHelper.createRotationalVector(-47.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.58F, AnimationHelper.createRotationalVector(-140.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.92F, AnimationHelper.createRotationalVector(-140.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("right_arm_end", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.08F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.21F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -227.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.38F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -227.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.71F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -227.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.83F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("left_arm_joint", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.08F, AnimationHelper.createRotationalVector(-47.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.38F, AnimationHelper.createRotationalVector(-47.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.58F, AnimationHelper.createRotationalVector(-140.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.92F, AnimationHelper.createRotationalVector(-140.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("left_arm_end", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.08F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.21F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 227.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.38F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 227.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.71F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 227.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.83F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("left_hand", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.08F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.21F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -217.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.38F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -217.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.71F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -217.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.83F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("right_hand", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.08F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.21F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 217.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.38F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 217.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.71F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 217.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.83F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .build();

    /**
     * PICKUP_ANIMATION played backwards: every keyframe time t becomes (1.0 - t) and the keyframe order is reversed,
     * same length (1.0 s), same bones, same values. The arms start raised, come down, and the hands unfold.
     */
    public static final Animation DEPOSIT_ANIMATION = Animation.Builder.create(1.0F)
            .addBoneAnimation("right_arm_joint", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.08F, AnimationHelper.createRotationalVector(-140.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.42F, AnimationHelper.createRotationalVector(-140.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.62F, AnimationHelper.createRotationalVector(-47.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.92F, AnimationHelper.createRotationalVector(-47.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("right_arm_end", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.17F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.29F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -227.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.62F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -227.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.79F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -227.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.92F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("left_arm_joint", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.08F, AnimationHelper.createRotationalVector(-140.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.42F, AnimationHelper.createRotationalVector(-140.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.62F, AnimationHelper.createRotationalVector(-47.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.92F, AnimationHelper.createRotationalVector(-47.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("left_arm_end", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.17F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.29F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 227.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.62F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 227.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.79F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 227.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.92F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("left_hand", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.17F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.29F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -217.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.62F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -217.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.79F, AnimationHelper.createRotationalVector(0.0F, 0.0F, -217.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.92F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("right_hand", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.17F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.29F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 217.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.62F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 217.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.79F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 217.5F), Transformation.Interpolations.LINEAR),
                    new Keyframe(0.92F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .build();
}