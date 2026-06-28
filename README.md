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

Requires JDK 21. Every dependency resolves automatically from public mavens declared in
`build.gradle` — the Create stack (Create, Ponder, Flywheel, Registrate, JEI) plus the two sibling
addons via [cursemaven](https://www.cursemaven.com/):

- **Create: Electro Energetics** — `curse.maven:create-electro-energetics-1443327:<file>` *(compile +
  runtime — the turbine subclasses its `GeneratingDevice` / `ElectricalDeviceBlock`)*.
- **Petrochem** — `curse.maven:create-petrochem-1494004:<file>` *(runtime only — fuels are referenced
  by id)*.

The CurseForge **file ids** are pinned in `gradle.properties` (`electroenergetics_file`,
`petrochem_file`). To move to a newer release, open the mod's **Files** tab on CurseForge, click the
file, and copy the trailing number from its URL into the matching property.

```bash
./gradlew runData      # generate blockstates/models/lang
./gradlew build        # compile + package
./gradlew runClient    # launch with Create + Electro Energetics + Petrochem (all auto-resolved)
```

## Verifying in-game

1. Build a **hollow box of Turbine Casing**, 3–7 blocks per side, with one **Turbine Controller** set
   into a wall and an empty interior (the combustion chamber). The goggles read **Formed: w×h×d** once
   it's valid, or **Unformed** with a hint (too small / walls incomplete / chamber not empty).
2. Pipe a Petrochem fuel (e.g. **diesel** or **gasoline**) into the controller from any side.
3. Attach EE **wires** to the controller's two top terminals and run them to an **energy meter** or
   **electric motor**.
4. Confirm that higher-grade fuel and a larger chamber both raise the Watt output, and that breaking a
   casing block un-forms the turbine and drops output to zero.

## Implementation notes

- The turbine is a power **source** for Electro Energetics: its block implements EE's
  `ElectricalDeviceBlock` and registers a `SimulatedDeviceType`; EE's chunk hook then instantiates a
  `TurbineDevice extends GeneratingDevice` automatically (no EE changes required).
- The multiblock is a **hollow casing box** validated by the controller itself in its block-entity
  `lazyTick` (it re-checks the shape ~twice a second to form and disassemble). Because the controller
  is also the fixed-position electrical device, it can't use Create's corner-reassigning
  `ConnectivityHandler`, so validation is self-contained — no member block-entities required.
- Follow-ups: bespoke models/textures + animated rotor renderer, a Ponder scene, a JEI fuel
  category, sounds, config, and a balance pass.

## License

MIT — see [LICENSE](LICENSE).
