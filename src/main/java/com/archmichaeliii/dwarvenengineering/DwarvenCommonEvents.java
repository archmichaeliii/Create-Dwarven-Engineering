package com.archmichaeliii.dwarvenengineering;

import com.archmichaeliii.dwarvenengineering.content.turbine.TurbineControllerBlockEntity;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Mod-bus event handlers. Currently just exposes block-entity capabilities (the turbine's fuel tank)
 * so Create fluid pipes can fill the turbine.
 */
public class DwarvenCommonEvents {

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        TurbineControllerBlockEntity.registerCapabilities(event);
    }
}
