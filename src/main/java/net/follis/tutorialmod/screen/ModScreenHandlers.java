package net.follis.tutorialmod.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.screen.custom.GoldenHotelScreenHandler;
import net.follis.tutorialmod.screen.custom.GrowthChamberScreenHandler;
import net.follis.tutorialmod.screen.custom.GoldenPedestalScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ModScreenHandlers {
    public static final ScreenHandlerType<GoldenPedestalScreenHandler> GOLDEN_PEDESTAL_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(TutorialMod.MOD_ID, "golden_pedestal_screen_handler"),
                    new ExtendedScreenHandlerType<>(GoldenPedestalScreenHandler::new, BlockPos.PACKET_CODEC));

    public static final ScreenHandlerType<GoldenHotelScreenHandler> GOLDEN_HOTEL_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(TutorialMod.MOD_ID, "golden_hotel_screen_handler"),
                    new ExtendedScreenHandlerType<>(GoldenHotelScreenHandler::new, BlockPos.PACKET_CODEC));

    public static final ScreenHandlerType<GrowthChamberScreenHandler> GROWTH_CHAMBER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(TutorialMod.MOD_ID, "growth_chamber_screen_handler"),
                    new ExtendedScreenHandlerType<>(GrowthChamberScreenHandler::new, BlockPos.PACKET_CODEC));


    public static void registerScreenHandlers() {
        TutorialMod.LOGGER.info("Registering Screen Handlers for " + TutorialMod.MOD_ID);
    }
}
