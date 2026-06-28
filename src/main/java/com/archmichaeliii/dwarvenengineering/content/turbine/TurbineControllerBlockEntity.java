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
 * Heart of the multiblock turbine: validates the casing structure, stores fuel, burns matching
 * {@code turbine_fuel} recipes and pushes the resulting Watts into its {@link TurbineDevice} so
 * Electro Energetics can pull power off the grid.
 *
 * <p><b>Structure:</b> a hollow rectangular box of {@link TurbineCasingBlock} between {@link #MIN_DIM}
 * and {@link #MAX_DIM} on each axis, with this controller embedded in one of the six walls and an
 * empty (air) interior — the combustion chamber. Power scales with the chamber volume. Validation
 * runs entirely here (the controller is a fixed-position electrical device, so Create's
 * corner-reassigning {@code ConnectivityHandler} is not used); it re-checks every {@code lazyTick}
 * (~½ s), which forms the turbine when the box is completed and tears it down when it is broken.</p>
 */
public class TurbineControllerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    /** Output voltage in Volts. */
    public static final double TURBINE_VOLTAGE = 120.0;
    /** Smallest allowed box edge (must be >= 3 so a hollow interior exists). */
    public static final int MIN_DIM = 3;
    /** Largest allowed box edge. */
    public static final int MAX_DIM = 7;
    /** Cap on the power multiplier so very large turbines don't scale without bound. */
    public static final int MAX_POWER_MULT = 16;
    /** Fuel tank capacity in millibuckets. */
    public static final int FUEL_CAPACITY = 8000;

    // Reasons a structure failed to form (synced for the goggle hint).
    private static final int REASON_NONE = 0;
    private static final int REASON_SIZE = 1;     // too small, too large, or not box-shaped
    private static final int REASON_SHELL = 2;    // a wall position isn't casing/controller
    private static final int REASON_CHAMBER = 3;  // the interior isn't empty

    public SmartFluidTankBehaviour tank;
    public TurbineFuelRecipe currentFuel = null;

    public boolean formed = false;
    public int dimX, dimY, dimZ;          // assembled box dimensions (0 when unformed)
    public int interiorVolume = 0;        // (dimX-2)*(dimY-2)*(dimZ-2)
    private int unformedReason = REASON_SIZE;
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
        validateStructure();
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

    /** Power scales with the combustion-chamber volume (tunable; capped at {@link #MAX_POWER_MULT}). */
    public double powerMultiplier() {
        return Math.min(MAX_POWER_MULT, Math.max(1, interiorVolume));
    }

    // --- Multiblock validation ---------------------------------------------------------------

    /**
     * Re-evaluate the structure: flood-fill the casing/controller shell starting from this block,
     * take its bounding box, then confirm it is a hollow box of valid size with this controller in a
     * wall and an empty interior. Sets {@link #formed}, {@link #dimX}/{@link #dimY}/{@link #dimZ},
     * {@link #interiorVolume} and {@link #unformedReason}.
     */
    private void validateStructure() {
        // Flood-fill the connected casing + controller blocks (the box shell is 6-connected).
        Set<BlockPos> members = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        members.add(worldPosition);
        queue.add(worldPosition);
        int cap = (MAX_DIM + 2) * (MAX_DIM + 2) * (MAX_DIM + 2);
        while (!queue.isEmpty()) {
            if (members.size() > cap) { // leaky / oversized structure
                setUnformed(REASON_SIZE);
                return;
            }
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (members.add(n)) {
                    if (isShellBlock(n))
                        queue.add(n);
                    else
                        members.remove(n); // not part of the structure; don't traverse
                }
            }
        }

        // Bounding box of the gathered shell.
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : members) {
            minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
            minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
        }
        int dx = maxX - minX + 1, dy = maxY - minY + 1, dz = maxZ - minZ + 1;

        if (dx < MIN_DIM || dy < MIN_DIM || dz < MIN_DIM
                || dx > MAX_DIM || dy > MAX_DIM || dz > MAX_DIM) {
            setUnformed(REASON_SIZE);
            return;
        }

        // Every position in the box must be: wall -> casing/this-controller, interior -> air.
        int controllers = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onShell = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    p.set(x, y, z);
                    BlockState state = level.getBlockState(p);
                    if (onShell) {
                        if (state.getBlock() instanceof TurbineControllerBlock) {
                            if (!p.equals(worldPosition)) { // a second controller in the shell
                                setUnformed(REASON_SHELL);
                                return;
                            }
                            controllers++;
                        } else if (!(state.getBlock() instanceof TurbineCasingBlock)) {
                            setUnformed(REASON_SHELL);
                            return;
                        }
                    } else if (!state.isAir()) {
                        setUnformed(REASON_CHAMBER);
                        return;
                    }
                }
            }
        }
        if (controllers != 1) {
            setUnformed(REASON_SHELL);
            return;
        }

        formed = true;
        dimX = dx; dimY = dy; dimZ = dz;
        interiorVolume = (dx - 2) * (dy - 2) * (dz - 2);
        unformedReason = REASON_NONE;
    }

    private void setUnformed(int reason) {
        formed = false;
        dimX = dimY = dimZ = 0;
        interiorVolume = 0;
        unformedReason = reason;
    }

    private boolean isShellBlock(BlockPos pos) {
        var block = level.getBlockState(pos).getBlock();
        return block instanceof TurbineCasingBlock || block instanceof TurbineControllerBlock;
    }

    // --- Display ----------------------------------------------------------------------------

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (formed) {
            DwarvenLang.translate("gui.turbine.formed", dimX, dimY, dimZ)
                    .style(ChatFormatting.GREEN)
                    .forGoggles(tooltip);
        } else {
            DwarvenLang.translate("gui.turbine.unformed")
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip);
            DwarvenLang.translate(unformedHintKey())
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
        }
        if (currentPowerWatts > 0)
            DwarvenLang.translate("gui.turbine.output", (int) Math.round(currentPowerWatts))
                    .style(ChatFormatting.AQUA)
                    .forGoggles(tooltip);
        containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
        return true;
    }

    private String unformedHintKey() {
        return switch (unformedReason) {
            case REASON_SHELL -> "gui.turbine.hint.shell";
            case REASON_CHAMBER -> "gui.turbine.hint.chamber";
            default -> "gui.turbine.hint.size";
        };
    }

    // --- Persistence / sync -----------------------------------------------------------------

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("Formed", formed);
        compound.putInt("DimX", dimX);
        compound.putInt("DimY", dimY);
        compound.putInt("DimZ", dimZ);
        compound.putInt("Interior", interiorVolume);
        compound.putInt("Reason", unformedReason);
        compound.putDouble("Power", currentPowerWatts);
        if (clientPacket)
            compound.putBoolean("Burning", currentPowerWatts > 0);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        formed = compound.getBoolean("Formed");
        dimX = compound.getInt("DimX");
        dimY = compound.getInt("DimY");
        dimZ = compound.getInt("DimZ");
        interiorVolume = compound.getInt("Interior");
        unformedReason = compound.getInt("Reason");
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
