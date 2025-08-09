package net.follis.tutorialmod.entity.client;

import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;

public class BambooTrapAnimations {
    public static final Animation IDLE_ANIMATION = Animation.Builder.create(0f).build();
    public static final Animation CLOSED_ANIMATION = Animation.Builder.create(0f)
            .addBoneAnimation("right_maw",
                    new Transformation(Transformation.Targets.ROTATE,
                            new Keyframe(0f, AnimationHelper.createRotationalVector(-60f, 0f, 0f),
                                    Transformation.Interpolations.LINEAR)))
            .addBoneAnimation("left_maw",
                    new Transformation(Transformation.Targets.ROTATE,
                            new Keyframe(0f, AnimationHelper.createRotationalVector(60f, 0f, 0f),
                                    Transformation.Interpolations.LINEAR))).build();
}
