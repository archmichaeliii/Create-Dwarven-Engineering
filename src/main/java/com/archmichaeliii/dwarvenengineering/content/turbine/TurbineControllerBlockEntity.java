package com.archmichaeliii.dwarvenengineering.content.turbine;

import com.archmichaeliii.dwarvenengineering.DwarvenLang;
import com.archmichaeliii.dwarvenengineering.content.turbine.fuel.TurbineFuelRecipe;
import com.archmichaeliii.dwarvenengineering.register.DwarvenBlockEntities;
import com.archmichaeliii.dwarvenengineering.register.DwarvenRecipeTypes;
import com.george_vi.electroenergetics.devices.device.DevicesSavedData;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Heart of the multiblock turbine: stores fuel, validates the casing structure, burns matching
 * {@code turbine_fuel} recipes and pushes the resulting Watts into its {@link TurbineDevice} so
 * Electro Energetics can pull power off the grid.
 */
public class TurbineControllerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    /** Output voltage in Volts. */
    public static final double TURBINE_VOLTAGE = 120.0;
    /** Minimum connected casing blocks required for the turbine to run. */
    public static final int MIN_CASING = 4;
    /** Upper bound on the casing flood-fill, both a balance cap and a safety bound. */
    public static final int MAX_CASING_SCAN = 64;
    /** Fuel tank capacity in millibuckets. */
    public static final int FUEL_CAPACITY = 8000;

    public SmartFluidTankBehaviour tank;
    public TurbineFuelRecipe currentFuel = null;

    public int structureSize = 0;
    public boolean formed = false;
    public double currentPowerWatts = 0;

    // client-only visual state (kept for a future rotor renderer)
    public LerpedFloat turbineSpeed = LerpedFloat.linear();
    public float turbineAngle = 0;
    private float lastSentSpeed = -1;

    public TurbineControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
        turbineSpeed.chase(0f, 1 / 64f, LerpedFloat.Chaser.EXP);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.TYPE, this, 1, FUEL_CAPACITY, true);
        tank.whenFluidUpdates(this::refreshFuel);
        behaviours.add(tank);
    }

    /** Match the tank's fluid against the {@code turbine_fuel} recipes, caching the result. */
    public void refreshFuel() {
        if (level == null)
            return;
        FluidStack fluid = tank.getPrimaryHandler().getFluidInTank(0);
        if (fluid.isEmpty()) {
            currentFuel = null;
            return;
        }
        currentFuel = level.getRecipeManager()
                .getAllRecipesFor(DwarvenRecipeTypes.TURBINE_FUEL_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.matchesFluid(fluid))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide)
            return;
        structureSize = scanStructure();
        formed = structureSize >= MIN_CASING;
        refreshFuel();
        sendData();
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
            turbineSpeed.tickChaser();
            turbineAngle += turbineSpeed.getValue() * 3 / 10f;
            turbineAngle %= 360;
            return;
        }

        boolean burning = false;
        if (formed && currentFuel != null && !tank.isEmpty()) {
            FluidStack drained = tank.getPrimaryHandler().drain(currentFuel.burnRate(), IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                currentPowerWatts = currentFuel.power() * powerMultiplier();
                burning = true;
            }
        }
        if (!burning)
            currentPowerWatts = 0;

        // Hand the current output to the Electro Energetics device backing this block.
        if (level instanceof ServerLevel serverLevel) {
            TurbineDevice device = DevicesSavedData.load(serverLevel).getDevice(worldPosition, TurbineDevice.class);
            if (device != null) {
                device.powerWatts = currentPowerWatts;
                device.voltage = TURBINE_VOLTAGE;
            }
        }

        // Snappy client spin update when the running state flips.
        float targetSpeed = burning ? 1024f : 0f;
        if (targetSpeed != lastSentSpeed) {
            lastSentSpeed = targetSpeed;
            sendData();
        }
    }

    /** Power scales with the assembled casing count: MIN_CASING blocks = 1x, double the casing = 2x, etc. */
    public double powerMultiplier() {
        return Math.max(1.0, structureSize / (double) MIN_CASING);
    }

    /** Bounded flood-fill of casing blocks connected to this controller. */
    private int scanStructure() {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            if (isCasing(n))
                queue.add(n);
        }
        while (!queue.isEmpty() && visited.size() < MAX_CASING_SCAN) {
            BlockPos p = queue.poll();
            if (!visited.add(p))
                continue;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!visited.contains(n) && isCasing(n))
                    queue.add(n);
            }
        }
        return visited.size();
    }

    private boolean isCasing(BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof TurbineCasingBlock;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        DwarvenLang.translate("gui.turbine.structure", structureSize, MIN_CASING)
                .style(formed ? ChatFormatting.GREEN : ChatFormatting.RED)
                .forGoggles(tooltip);
        if (currentPowerWatts > 0)
            DwarvenLang.translate("gui.turbine.output", (int) Math.round(currentPowerWatts))
                    .style(ChatFormatting.AQUA)
                    .forGoggles(tooltip);
        containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
        return true;
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("Formed", formed);
        compound.putInt("StructureSize", structureSize);
        compound.putDouble("Power", currentPowerWatts);
        if (clientPacket)
            compound.putBoolean("Burning", currentPowerWatts > 0);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        formed = compound.getBoolean("Formed");
        structureSize = compound.getInt("StructureSize");
        currentPowerWatts = compound.getDouble("Power");
        if (clientPacket)
            turbineSpeed.updateChaseTarget(compound.getBoolean("Burning") ? 1024f : 0f);
    }

    /** Expose the fuel tank so Create fluid pipes can fill the turbine from any side. */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                DwarvenBlockEntities.TURBINE_CONTROLLER.get(),
                (be, context) -> be.tank.getCapability());
    }
}
