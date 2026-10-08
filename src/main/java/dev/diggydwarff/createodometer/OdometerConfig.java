package dev.diggydwarff.createodometer;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side tracking limits and client-side display preferences. */
public final class OdometerConfig {
    public enum Unit {KILOMETERS, MILES}

    public static final ModConfigSpec SERVER_SPEC, CLIENT_SPEC;
    public static final ModConfigSpec.BooleanValue ALLOW_TRIP_RESET;
    public static final ModConfigSpec.DoubleValue MIN_SPEED, MAX_SPEED;
    public static final ModConfigSpec.IntValue SYNC_TICKS, DECIMALS;
    public static final ModConfigSpec.EnumValue<Unit> UNIT;
    static {
        var s = new ModConfigSpec.Builder();
        MIN_SPEED = s.comment("Ignore movement below this simulation speed (m/s) to suppress parked jitter. Very slow travel below this is also excluded.").defineInRange("minimumSpeed", 0.02, 0, 10);
        MAX_SPEED = s.comment("Reject discontinuous physics-step movement above this speed (m/s). Not a vehicle speed limit. Raise for unusually fast aircraft.").defineInRange("maximumPlausibleSpeed", 2000.0, 1, 1000000);
        ALLOW_TRIP_RESET = s.comment("Allow nearby players to reset trips. Operators always can.")
                .define("allowTripReset", true);
        SYNC_TICKS = s.comment("Gauge refresh period in game ticks. Tracking always uses physics substeps.")
                .defineInRange("displaySyncTicks", 5, 1, 100);
        SERVER_SPEC = s.build();
        var c = new ModConfigSpec.Builder();
        UNIT = c.comment("Local display units for all gauges and menus; 1 block = 1 meter.").defineEnum("units",
                Unit.KILOMETERS);
        DECIMALS = c.defineInRange("decimalPlaces", 2, 0, 3);
        CLIENT_SPEC = c.build();
    }

    public static String format(double meters) {
        boolean miles = UNIT.get() == Unit.MILES;
        return String.format(java.util.Locale.ROOT, "% ." + DECIMALS.get() + "f", meters /(miles ? 1609.344 : 1000.0))
                .trim() + (miles ? " mi" : " km");
    }
}
