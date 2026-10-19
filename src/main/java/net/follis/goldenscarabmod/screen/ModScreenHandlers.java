package net.follis.goldenscarabmod.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.screen.custom.GoldenHotelScreenHandler;
import net.follis.goldenscarabmod.screen.custom.GrowthChamberScreenHandler;
import net.follis.goldenscarabmod.screen.custom.GoldenPedestalScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ModScreenHandlers {
    public static final ScreenHandlerType<GoldenPedestalScreenHandler> GOLDEN_PEDESTAL_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(GoldenScarabMod.MOD_ID, "golden_pedestal_screen_handler"),
                    new ExtendedScreenHandlerType<>(GoldenPedestalScreenHandler::new, BlockPos.PACKET_CODEC));

    public static final ScreenHandlerType<GoldenHotelScreenHandler> GOLDEN_HOTEL_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(GoldenScarabMod.MOD_ID, "golden_hotel_screen_handler"),
                    new ExtendedScreenHandlerType<>(GoldenHotelScreenHandler::new, BlockPos.PACKET_CODEC));

    public static final ScreenHandlerType<GrowthChamberScreenHandler> GROWTH_CHAMBER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(GoldenScarabMod.MOD_ID, "growth_chamber_screen_handler"),
                    new ExtendedScreenHandlerType<>(GrowthChamberScreenHandler::new, BlockPos.PACKET_CODEC));


    public static void registerScreenHandlers() {
        GoldenScarabMod.LOGGER.info("Registering Screen Handlers for " + GoldenScarabMod.MOD_ID);
    }
}
