# Create: Dwarven Engineering

A [Create](https://github.com/Creators-of-Create/Create) addon for **NeoForge 1.21.1** whose
flagship machine is a **multiblock fuel turbine** that bridges two sibling addons:

- it **burns the refined fuels from [Petrochem](https://github.com/archmichaeliii/petrochem)**, and
- it **outputs native electrical power into the [Create: Electro Energetics](https://github.com/archmichaeliii/create-electro-energetics) grid** (Volts/Watts, not Forge Energy).

Petrochem turns crude into gasoline, diesel, kerosene, LPG and more; Electro Energetics provides a
physically-simulated DC grid. Until now there was no way to turn one into the other — the Dwarven
turbine is that link.

## How it works

```
Petrochem fuel fluid ──► Turbine Casing (fuel reservoir, scales power)
                         + Turbine Controller (burns fuel, makes Watts)
                         └─► TurbineDevice : GeneratingDevice ──► EE wire grid
```

- **Turbine Casing** (`dwarvenengineering:turbine_casing`) — structural blocks. The controller
  flood-fills the connected casing mass; a bigger turbine produces more power.
- **Turbine Controller** (`dwarvenengineering:turbine_controller`) — holds the fuel tank, matches
  the fuel against `turbine_fuel` recipes, drains it, and feeds the resulting Watts into Electro
  Energetics. It is an EE electrical device with two wire terminals (− / +) on its top face; attach
  EE wires there to draw power.

Power scales with the casing count: `MIN_CASING` (4) blocks = 1×, double the casing = 2×, etc.

### Fuels

Fuels are data-driven via the `dwarvenengineering:turbine_fuel` recipe type. Each fuel names a fluid
by id and carries its own burn rate (mB/tick) and electrical output (Watts), so premium fuels beat
crude. Bundled defaults (see `src/main/resources/data/dwarvenengineering/recipe/turbine_fuel/`):

| Fuel (`petrochem:`) | Burn rate (mB/t) | Power (W) |
|---------------------|------------------|-----------|
| petroleum           | 20               | 1024      |
| fuel_oil            | 8                | 2048      |
| diesel              | 6                | 3072      |
| kerosene            | 5                | 3584      |
| gasoline            | 5                | 4096      |
| lpg                 | 10               | 4608      |

Because fuels are referenced by id, this mod has **no compile-time dependency on Petrochem** — add
or override fuels from any datapack.

## Building

Requires JDK 21. The Create stack (Create, Ponder, Flywheel, Registrate, JEI) resolves from the
public mavens already listed in `build.gradle`. You must additionally provide the two sibling
addons; pick **one** strategy and enable it in `build.gradle` (and, for option C, `settings.gradle`):

- **(A) Local jars** *(default)* — drop the built mod jars into `libs/`:
  `electroenergetics-neoforge-1.21.1-*.jar` and `petrochem-neoforge-1.21.1-*.jar`.
- **(B) CurseForge maven** — uncomment the `curse.maven:...` lines and fill in the project/file IDs.
- **(C) Gradle composite build** — if the sibling repos sit next to this one, uncomment the
  `includeBuild` lines in `settings.gradle` and switch to the sibling module coordinates.

> Electro Energetics is needed on the **compile** classpath (the turbine subclasses its
> `GeneratingDevice` / `ElectricalDeviceBlock`); Petrochem is only needed at **runtime**.

```bash
./gradlew runData      # generate blockstates/models/lang
./gradlew build        # compile + package
./gradlew runClient    # launch with Create + Electro Energetics + Petrochem installed
```

## Verifying in-game

1. Build a box of **Turbine Casing** (≥ 4 connected blocks).
2. Place a **Turbine Controller** touching the casing.
3. Pipe a Petrochem fuel (e.g. **diesel** or **gasoline**) into the controller from any side.
4. Attach EE **wires** to the controller's two top terminals and run them to an **energy meter** or
   **electric motor**.
5. Goggles on the controller show casing count and live Watt output. Confirm higher-grade fuel and a
   larger casing both raise the output, and that breaking casing below the minimum stops it.

## Implementation notes

- The turbine is a power **source** for Electro Energetics: its block implements EE's
  `ElectricalDeviceBlock` and registers a `SimulatedDeviceType`; EE's chunk hook then instantiates a
  `TurbineDevice extends GeneratingDevice` automatically (no EE changes required).
- v1 uses a **controller-driven casing scan** to form the multiblock (lighter than Create's
  `ConnectivityHandler`/`FluidTankBlockEntity` machinery). Moving to a full Create connectivity
  multiblock is a natural future enhancement; the in-game experience is the same.
- Follow-ups: bespoke models/textures + animated rotor renderer, a Ponder scene, a JEI fuel
  category, sounds, config, and a balance pass.

## License

MIT — see [LICENSE](LICENSE).
