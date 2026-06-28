package com.archmichaeliii.dwarvenengineering.content.turbine;

import net.minecraft.world.level.block.Block;

/**
 * Structural body block of the multiblock turbine. On its own it is inert; the
 * {@link TurbineControllerBlockEntity} scans for a connected mass of these blocks and uses the
 * count to scale the turbine's fuel throughput and power output (bigger turbine = more power).
 */
public class TurbineCasingBlock extends Block {
    public TurbineCasingBlock(Properties properties) {
        super(properties);
    }
}
