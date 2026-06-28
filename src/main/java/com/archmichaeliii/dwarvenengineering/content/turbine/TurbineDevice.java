package com.archmichaeliii.dwarvenengineering.content.turbine;

import com.george_vi.electroenergetics.devices.device.DevicesSavedData;
import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;
import com.george_vi.electroenergetics.foundation.base.GeneratingDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * The Electro Energetics electrical device backing a {@link TurbineControllerBlock}.
 *
 * <p>Extending {@link GeneratingDevice} means EE treats us as an energy-limited voltage source
 * between node 0 and node 1: each tick it offers up to {@link #getPower()} Watts at
 * {@link #getVoltage()} Volts, and the surrounding circuit decides how much current actually flows.</p>
 *
 * <p>The values are <i>pushed in</i> by {@link TurbineControllerBlockEntity#tick()} on the server
 * (via {@link DevicesSavedData#getDevice(BlockPos, Class)}) — the BE owns the fuel logic and tells
 * the device how much power it is currently making. This mirrors how EE's own AlternatorBrushes
 * block feeds its device.</p>
 */
public class TurbineDevice extends GeneratingDevice {

    /** Power currently produced, in Watts. Pushed in by the controller BE each server tick. */
    public double powerWatts;
    /** Output voltage in Volts. Pushed in by the controller BE each server tick. */
    public double voltage;

    public TurbineDevice(Level level, BlockPos pos, DevicesSavedData deviceSD, SimulatedDeviceType<?> type) {
        super(level, pos, deviceSD, type);
    }

    @Override
    protected double getVoltage() {
        return voltage;
    }

    @Override
    protected double getPower() {
        return powerWatts;
    }

    @Override
    public void read(CompoundTag tag) {
        this.storedEnergy = tag.getDouble("StoredEnergy");
        this.powerWatts = tag.getDouble("PowerWatts");
        this.voltage = tag.getDouble("Voltage");
    }

    @Override
    public void write(CompoundTag tag) {
        tag.putDouble("StoredEnergy", this.storedEnergy);
        tag.putDouble("PowerWatts", this.powerWatts);
        tag.putDouble("Voltage", this.voltage);
    }
}
