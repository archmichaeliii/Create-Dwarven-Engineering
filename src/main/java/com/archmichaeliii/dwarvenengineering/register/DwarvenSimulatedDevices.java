package com.archmichaeliii.dwarvenengineering.register;

import com.archmichaeliii.dwarvenengineering.DwarvenEngineering;
import com.archmichaeliii.dwarvenengineering.content.turbine.TurbineDevice;
import com.george_vi.electroenergetics.CEERegistries;
import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers our turbine as a Create: Electro Energetics "simulated device". Once registered, EE's
 * {@code LevelChunkMixin} placement hook automatically instantiates a {@link TurbineDevice} for any
 * placed block whose {@code getDevice()} returns {@link #TURBINE} — i.e. our turbine controller —
 * and the device then participates in EE's electrical network as a voltage source.
 */
public class DwarvenSimulatedDevices {

    private static final DeferredRegister<SimulatedDeviceType<?>> DEVICES =
            DeferredRegister.create(CEERegistries.SIMULATED_DEVICE_TYPE, DwarvenEngineering.MODID);

    public static final DeferredHolder<SimulatedDeviceType<?>, SimulatedDeviceType<TurbineDevice>> TURBINE =
            DEVICES.register("turbine", () -> new SimulatedDeviceType<>(
                    DwarvenEngineering.asResource("turbine"),
                    (type, level, pos, sd) -> new TurbineDevice(level, pos, sd, type)));

    public static void register(IEventBus modEventBus) {
        DEVICES.register(modEventBus);
    }
}
