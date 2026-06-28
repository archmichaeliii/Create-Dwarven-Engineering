package com.archmichaeliii.dwarvenengineering.content.turbine;

import com.archmichaeliii.dwarvenengineering.register.DwarvenBlockEntities;
import com.archmichaeliii.dwarvenengineering.register.DwarvenSimulatedDevices;
import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;
import com.george_vi.electroenergetics.foundation.base.SimpleElectricalDeviceBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * The brain + electrical terminal of the multiblock turbine.
 *
 * <p>Extends {@link SimpleElectricalDeviceBlock} so it is recognised by Electro Energetics as a
 * (non-kinetic) electrical device with two wire terminals; wires attach to node 0 (-) and node 1 (+)
 * on its top face. {@link #getDevice()} binds it to our {@link TurbineDevice} generator. The
 * {@link TurbineControllerBlockEntity} holds the fuel tank, scans the surrounding casing, burns fuel
 * and feeds the resulting Watts into the device.</p>
 */
public class TurbineControllerBlock extends SimpleElectricalDeviceBlock<TurbineDevice> implements IBE<TurbineControllerBlockEntity> {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TurbineControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public SimulatedDeviceType<TurbineDevice> getDevice() {
        return DwarvenSimulatedDevices.TURBINE.get();
    }

    /** Seed the device with a starting voltage so it can act as a source the moment it has energy. */
    @Override
    public CompoundTag getDefaultDeviceData(Level level, BlockPos pos, BlockState state) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Voltage", TurbineControllerBlockEntity.TURBINE_VOLTAGE);
        return tag;
    }

    @Override
    public Map<Integer, Vec3> getNodePositions(Level level, BlockPos pos, BlockState state) {
        // Two terminals on the top face. Positions are relative to the block's bottom corner.
        Map<Integer, Vec3> nodes = new HashMap<>();
        nodes.put(0, new Vec3(0.3, 1.0, 0.5)); // negative
        nodes.put(1, new Vec3(0.7, 1.0, 0.5)); // positive
        return nodes;
    }

    @Override
    public Vec3 getNodePosition(Level level, BlockPos pos, BlockState state, int id) {
        return getNodePositions(level, pos, state).get(id);
    }

    @Override
    public MutableComponent getNodeLabel(Level level, BlockPos pos, BlockState state, int id) {
        return id == 0
                ? Component.translatable("electroenergetics.nodes.negative")
                : Component.translatable("electroenergetics.nodes.positive");
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public Class<TurbineControllerBlockEntity> getBlockEntityClass() {
        return TurbineControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TurbineControllerBlockEntity> getBlockEntityType() {
        return DwarvenBlockEntities.TURBINE_CONTROLLER.get();
    }
}
