package com.archmichaeliii.dwarvenengineering.register;

import com.archmichaeliii.dwarvenengineering.DwarvenEngineering;
import com.archmichaeliii.dwarvenengineering.content.turbine.TurbineControllerBlockEntity;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class DwarvenBlockEntities {

    private static final CreateRegistrate REGISTRATE = DwarvenEngineering.registrate();

    public static final BlockEntityEntry<TurbineControllerBlockEntity> TURBINE_CONTROLLER =
            REGISTRATE.blockEntity("turbine_controller", TurbineControllerBlockEntity::new)
                    .validBlocks(DwarvenBlocks.TURBINE_CONTROLLER)
                    .register();

    public static void register() {}
}
