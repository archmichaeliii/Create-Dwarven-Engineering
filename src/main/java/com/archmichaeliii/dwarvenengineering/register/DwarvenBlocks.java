package com.archmichaeliii.dwarvenengineering.register;

import com.archmichaeliii.dwarvenengineering.DwarvenEngineering;
import com.archmichaeliii.dwarvenengineering.content.turbine.TurbineCasingBlock;
import com.archmichaeliii.dwarvenengineering.content.turbine.TurbineControllerBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class DwarvenBlocks {

    private static final CreateRegistrate REGISTRATE =
            DwarvenEngineering.registrate().setCreativeTab(DwarvenCreativeModeTabs.MAIN_TAB);

    public static final BlockEntry<TurbineCasingBlock> TURBINE_CASING =
            REGISTRATE.block("turbine_casing", TurbineCasingBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops())
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .blockstate((c, p) -> p.simpleBlock(c.get()))
                    .lang("Turbine Casing")
                    .item()
                    .build()
                    .register();

    public static final BlockEntry<TurbineControllerBlock> TURBINE_CONTROLLER =
            REGISTRATE.block("turbine_controller", TurbineControllerBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().noOcclusion())
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .blockstate((c, p) -> p.simpleBlock(c.get()))
                    .lang("Turbine Controller")
                    .item()
                    .build()
                    .register();

    public static void register() {}
}
