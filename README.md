# Create: Odometer

A gauge for tracking how far your Create: Aeronautics ships have traveled. Tracks lifetime mileage and two resettable trips, even when no gauge is installed.

**Minecraft 1.21.1 · NeoForge 21.1.228+**  
Requires **Create 6.0.10**, **Create: Aeronautics 1.3.0**, and **Sable 2.0.5**. Install on both the server and clients.

## Using the gauge

Craft it with a Nixie Tube, Brass Casing, and Redstone in a vertical line. Place it on a ship and right-click to configure it.

- Display **Total**, **Trip A**, or **Trip B** mileage.
- Choose **Overall**, **Horizontal**, or **Vertical** distance. Vertical counts both ascent and descent.
- Reset either trip by clicking its reset button twice within three seconds. Resets apply ship-wide, across all distance modes.
- Switch between kilometers and miles. Units and decimal places are client preferences.
- Join **two or three gauges** side by side for a wider display. Each segment opens the same controls.
- Use Create's wrench to rotate or pick up gauges. Floor and ceiling mounting are supported.

Gauges share the ship's counters but can show different readings. No power is needed.

## Create integration

**Display Links:** Attach a link to any gauge segment and select **Ship Mileage**. Choose the counter, distance mode, units, and decimal places, or follow the gauge's current selection. Links update once per second and work with normal Create display targets and connection limits.

**Engineer's Goggles:** Show Total, Trip A, and Trip B for the selected distance mode.

## Commands

Use a Sable ship UUID or `here` while on or looking at a nearby tracked ship.

| Command | Action |
| --- | --- |
| `/shipmileage get <ship>` | Show all mileage counters |
| `/shipmileage list` | List saved ships |
| `/shipmileage reset <ship> all` | Clear all counters |
| `/shipmileage reset <ship> trip_a` | Reset Trip A |
| `/shipmileage reset <ship> trip_b` | Reset Trip B |
| `/shipmileage set <ship> <distance> m\|km\|mi [overall\|horizontal\|vertical]` | Correct a lifetime total |

`list`, `reset`, and `set` require operator level 2.

## Saving mileage

Mileage is calculated on the server from ship movement and saved in `world/data/createodometer_mileage.dat`. It survives restarts and unloading. Travel before installing the mod can't be recovered.

**Disassembly/reassembly:** Keep at least one original gauge on the craft. Its block data retains the old ship ID so the reassembled ship can inherit its saved mileage and trips. Breaking every gauge before reassembly loses that automatic link. Simply moving a gauge to a different ship does not transfer mileage.

## Configuration

**Server:** `serverconfig/createodometer-server.toml`

- `minimumSpeed = 0.02` — ignores small movements and jitter (m/s)
- `maximumPlausibleSpeed = 2000.0` — rejects implausible jumps (m/s)
- `allowTripReset = true` — allows nearby players to reset trips
- `displaySyncTicks = 5` — display refresh interval, not tracking rate

**Client:** `config/createodometer-client.toml`

- `units = "KILOMETERS"` — or `"MILES"`
- `decimalPlaces = 2` — 0 to 3

## Building

Use **JDK 21** and run `gradlew.bat build` on Windows or `./gradlew build` on Linux/macOS. The JAR is written to `build/libs/`.

Run `./gradlew verifyMileage` for the mileage tests.

**Upgrading:** Update server and clients together. Existing Overall mileage and trips are preserved; historical Horizontal and Vertical distances cannot be reconstructed from older releases.
