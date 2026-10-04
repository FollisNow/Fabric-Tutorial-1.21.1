package net.follis.tutorialmod.component;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.follis.tutorialmod.TutorialMod;
import net.minecraft.util.Identifier;

public class ModAttachments {
    public static final AttachmentType<Boolean> KILLED_LOCUST = AttachmentRegistry.create(
            Identifier.of(TutorialMod.MOD_ID, "killed_locust"));
}