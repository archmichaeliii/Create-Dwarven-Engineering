package com.archmichaeliii.dwarvenengineering.data;

import com.archmichaeliii.dwarvenengineering.DwarvenEngineering;
import com.tterrag.registrate.providers.ProviderType;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Data generation hooks.
 *
 * <p>Blockstates, block/item models and block-name lang are produced automatically by Registrate
 * (wired up in {@code registerEventListeners}). This class only injects the extra interface/tooltip
 * lang keys that Registrate doesn't know about.</p>
 *
 * <p>The bundled {@code turbine_fuel} recipes that map Petrochem fluids to power values are authored
 * by hand under {@code src/main/resources/data/dwarvenengineering/recipe/turbine_fuel/} so they need
 * no Petrochem classes on the datagen classpath.</p>
 */
public class DwarvenDatagen {

    public static void gatherDataHighPriority(GatherDataEvent event) {
        if (!event.getMods().contains(DwarvenEngineering.MODID))
            return;
        DwarvenEngineering.registrate().addDataGenerator(ProviderType.LANG, provider -> {
            provider.add("itemGroup.dwarvenengineering.main", "Create: Dwarven Engineering");
            provider.add("dwarvenengineering.gui.turbine.structure", "Casing Blocks: %1$s (min %2$s)");
            provider.add("dwarvenengineering.gui.turbine.output", "Output: %1$s W");
        });
    }

    public static void gatherData(GatherDataEvent event) {
        // Reserved for future providers (recipes are hand-authored; Registrate handles the rest).
    }
}
