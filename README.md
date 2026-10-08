# Create: Odometer — 1.1.1

Minecraft 1.21.1 / NeoForge 21.1.228 or newer.
Requires Create 6.0.10, Create: Aeronautics 1.3.0 (bundled distribution), and Sable 2.0.5. Install this addon on both server and clients. This first release targets those versions; broader compatibility is not claimed.

## Use

Craft an Odometer Gauge from a Create Nixie Tube, Brass Casing, and Redstone, arranged vertically. Place it on an Aeronautics ship and right-click it. The gauge can face any horizontal direction and supports Create wrench rotation/pickup.

The physical display shows Total, Trip A, or Trip B, with Overall, Horizontal, or Vertical distance selected independently for each display. Use the checkmark beside a reading to choose the display for that gauge. Click a trip's reset button twice within three seconds to reset that trip for the entire ship. Resetting one trip clears that trip in all three distance modes without changing the other trip or any lifetime total.

The units button switches your local displays between kilometers and miles. Other players retain their own preference. One block is one meter. Choose 0–3 decimals through Mods > Create: Odometer > Config.

There is no power requirement. Multiple gauges share ship counters but have independent selected displays. Place two or three adjacent gauges side by side with matching facing and floor/ceiling mounting to form one continuous wide panel. Any segment controls the whole panel; separating them restores individual gauges or a shorter joined panel. Rows of four or more do not join. Removing all gauges does not stop tracking or erase readings. A gauge installed on a different ship reads that ship's counters rather than carrying mileage with it.

## Create integration

Attach a Create Display Link to any gauge segment and select **Ship Mileage** as its source. The native Create controls let you choose Follow Gauge, Total, Trip A, or Trip B; Follow Gauge, Overall, Horizontal, or Vertical mode; meters, kilometers, or miles; and 0–3 decimal places. Follow Gauge uses the joined panel's shared selection. Each link updates once per second and uses its own saved units so a multiplayer display is consistent for everyone. An unattached gauge sends `--`. Display links read mileage and cannot reset counters.

Create handles the connected target, including Display Boards, Nixie Tubes, signs, and other compatible display targets. Its normal connection range and moving-ship limitations still apply; the addon does not add cross-ship routing.

Engineer’s Goggles show Total and both trips for the gauge's selected distance mode, using your local units. The gauge supports Create wrench rotation and pickup and uses Create's GUI controls and nixie rendering. No kinetic power is required.

## Commands

`<ship>` is a Sable UUID or `here` while riding/standing on a tracked ship or looking at one within eight blocks.

- `/shipmileage get <ship>` — read total and both trips for all three distance modes, in meters.
- `/shipmileage list` — list recorded UUIDs and totals; operator level 2.
- `/shipmileage reset <ship> all` — zero total and both trips; operator level 2.
- `/shipmileage reset <ship> trip_a` — zero Trip A; operator level 2.
- `/shipmileage reset <ship> trip_b` — zero Trip B; operator level 2.
- `/shipmileage set <ship> <distance> m|km|mi [overall|horizontal|vertical] (legacy `combined` alias also accepted)` — correct the selected mode's total (Overall by default); operator level 2. Preserve existing trip lengths where possible, clipping them to the corrected total if necessary.

Example: `/shipmileage reset here trip_a`.

## Measurement and persistence

The server samples every Sable physics substep, including ships with no gauges. It measures displacement of the same local center-of-mass point before and after each step, excluding rotation about that point. All three distances are recorded on every accepted physics step: Overall is the actual 3D path length, Horizontal is ground-plane distance, and Vertical is accumulated ascent plus descent (not net altitude). Overall is not the sum of Horizontal and Vertical. Selection is per display, not a server-wide tracking option. Unloaded or paused physics does not invent travel. Client lag has no effect on the server calculation. Server stalls add no wall-clock extrapolation.

Records are world SavedData keyed by Sable ship UUID, in the overworld's `data/createodometer_mileage.dat`. They survive normal saves, restarts, and ship unloading. Tracking begins when this addon is installed; earlier travel cannot be recovered. Like other world data, a crash can lose changes since the last save.

Disassembly/reassembly preserves lifetime mileage and both trips when at least one previously attached gauge stays in the craft. The gauge retains its old ship reference in block-entity NBT, and the new ship inherits the authoritative world record once. This also works across a save/restart while disassembled. Removing/replacing gauges on the same ship still does not reset its history.

A completely gauge-free craft has no identity marker after full disassembly and cannot automatically recover its old history. Copies, splits, and merges do not combine histories; moving a gauge from a live or unloaded ship does not transfer mileage. Keep an existing gauge through reassembly. New counters cannot recover mileage already lost before upgrading; old UUID records remain accessible through admin commands.

Small movements below `minimumSpeed` are excluded to avoid parked jitter. Discontinuous movement above `maximumPlausibleSpeed` is rejected; ordinary teleports outside a physics step are not bridged. A teleport performed inside a step and below that threshold cannot always be distinguished from legitimate travel. Curves are sampled at physics-step resolution rather than reconstructed exactly between samples.

## Configuration

Server world config: `serverconfig/createodometer-server.toml`.

- `minimumSpeed = 0.02` m/s
- `maximumPlausibleSpeed = 2000.0` m/s
- `allowTripReset = true` — nearby players may reset trips; operators always can.
- `displaySyncTicks = 5` — display refresh only; tracking still runs every physics step.

Client config: `config/createodometer-client.toml`.

- `units = "KILOMETERS"` (or `"MILES"`)
- `decimalPlaces = 2`

## Build

Install JDK 21, then run `gradlew.bat build` on Windows or `./gradlew build` on Linux/macOS. Gradle downloads the pinned development dependencies; do not put them inside the addon JAR. Output: `build/libs/create-odometer-neoforge-1.21.1-1.1.1.jar`.

`./gradlew verifyMileage` exercises distance validation, independent trip resets, saved NBT records, rotation exclusion, high-speed sampling, and 16-bit menu synchronization.

## Assets

The addon references Create textures directly, uses Create icon buttons and menu panels, and calls Create's glowing nixie text renderer. Create assets are not copied into the addon. The compact gauge geometry is custom.

Place against the underside of a block to mount the gauge on the ceiling. The display stays upright; horizontal facing and wrench rotation work as usual.

## Upgrading from 0.1.x

Existing totals and trips are retained in Overall, using whatever measurement basis was previously configured. Historical Horizontal/Vertical components cannot be reconstructed, so those counters begin at zero when upgrading. New movement is tracked independently in all three modes. The old `horizontalOnly` server option is replaced by per-gauge display selection. Update both clients and server together.

## Tracking cost

All gauges share a single set of ship records. The physics tracker still visits each active ship once per substep and reuses the same sampled displacement for all three lengths. Gauge joining updates on block changes, not by scanning rows every tick. Normal display sync stays at the configured interval; a stationary gauge does not send repeated unchanged readings. Open menus synchronize nine counters instead of three. No chunk force-loading or new physics simulation is added.

## Upgrading from 0.2.0

Overall is the new display name for the same full-path counter. All totals, trips, and mode selections are retained. Existing two-block panels continue to work and can be extended to three. Update server and clients together.
