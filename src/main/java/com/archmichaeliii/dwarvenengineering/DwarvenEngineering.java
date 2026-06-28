package com.archmichaeliii.dwarvenengineering;

import com.archmichaeliii.dwarvenengineering.data.DwarvenDatagen;
import com.archmichaeliii.dwarvenengineering.register.DwarvenBlockEntities;
import com.archmichaeliii.dwarvenengineering.register.DwarvenBlocks;
import com.archmichaeliii.dwarvenengineering.register.DwarvenCreativeModeTabs;
import com.archmichaeliii.dwarvenengineering.register.DwarvenRecipeTypes;
import com.archmichaeliii.dwarvenengineering.register.DwarvenSimulatedDevices;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Create: Dwarven Engineering — entry point.
 *
 * <p>This addon bridges two sibling Create mods: its multiblock turbine burns the refined fuels
 * from <b>Petrochem</b> and feeds native electrical power into the <b>Create: Electro Energetics</b>
 * grid.</p>
 */
@Mod(DwarvenEngineering.MODID)
public class DwarvenEngineering {

    public static final String MODID = "dwarvenengineering";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);

    static {
        REGISTRATE
                .defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
                .setTooltipModifierFactory(item ->
                        new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                                .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
    }

    public DwarvenEngineering(IEventBus modEventBus, ModContainer modContainer) {
        REGISTRATE.registerEventListeners(modEventBus);

        DwarvenCreativeModeTabs.register(modEventBus);
        DwarvenBlocks.register();
        DwarvenBlockEntities.register();
        DwarvenRecipeTypes.register(modEventBus);
        DwarvenSimulatedDevices.register(modEventBus);

        modEventBus.addListener(DwarvenCommonEvents::registerCapabilities);
        modEventBus.addListener(EventPriority.HIGHEST, DwarvenDatagen::gatherDataHighPriority);
        modEventBus.addListener(EventPriority.LOWEST, DwarvenDatagen::gatherData);
    }

    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
