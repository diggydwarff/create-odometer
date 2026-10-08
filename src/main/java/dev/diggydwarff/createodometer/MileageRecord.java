package dev.diggydwarff.createodometer;

/** Independent path lengths in raw meters; trip resets cover every distance mode. */
public final class MileageRecord {
    private final double[] totals = new double[3];
    private final double[][] origins = new double[2][3];
    public MileageRecord() {
    }
    /** Legacy counters retain their original value in Combined; missing components start at zero. */
    public MileageRecord(double total, double a, double b) {
        this(new double[]{total, 0, 0}, new double[][]{
            {a, 0, 0}, {b, 0, 0}
        });
    }

    public MileageRecord(double[] totals, double[][] origins) {
        for (int mode = 0; mode < 3; mode++) {
            this.totals[mode] = clean(totals[mode]);
            for (int trip = 0; trip < 2; trip++) this.origins[trip][mode] = Math.min(this.totals[mode],
                    clean(origins[trip][mode]));
        }
    }

    private static double clean(double x) {
        return Double.isFinite(x) && x >= 0 ? x : 0;
    }

    public double total() {
        return total(0);
    }

    public double tripA() {
        return distance(1, 0);
    }

    public double tripB() {
        return distance(2, 0);
    }

    public double originA() {
        return origin(1, 0);
    }

    public double originB() {
        return origin(2, 0);
    }

    public double total(int mode) {
        return totals[mode];
    }

    public double origin(int trip, int mode) {
        return origins[trip - 1][mode];
    }

    public double distance(int counter, int mode) {
        return counter == 0 ? totals[mode] : Math.max(0, totals[mode] - origins[counter - 1][mode]);
    }

    public boolean add(double distance) {
        return add(distance, 0, 0);
    }

    public boolean add(double combined, double horizontal, double vertical) {
        if (!validIncrement(combined, 0) || !validIncrement(horizontal, 1) || !validIncrement(vertical,
                2)) return false;
        if (combined == 0 && horizontal == 0 && vertical == 0) return false;
        totals[0] += combined;
        totals[1] += horizontal;
        totals[2] += vertical;
        return true;
    }

    private boolean validIncrement(double d, int mode) {
        return Double.isFinite(d) && d >= 0 && Double.isFinite(totals[mode] + d);
    }

    /** Carry forward a retired ship's history without losing progress recorded after reassembly. */
    public MileageRecord continuedWith(MileageRecord early) {
        double[] next = new double[3];
        for (int mode = 0; mode < 3; mode++) {
            next[mode] = totals[mode] + early.totals[mode];
            if (!Double.isFinite(next[mode])) return null;
        }
        return new MileageRecord(next, origins);
    }

    public void resetTrip(int trip) {
        if (trip == 1 || trip == 2) System.arraycopy(totals, 0, origins[trip - 1], 0, 3);
    }

    public void resetAll() {
        java.util.Arrays.fill(totals, 0);
        for (var row : origins) java.util.Arrays.fill(row, 0);
    }

    public void setTotal(double meters) {
        setTotal(0, meters);
    }

    public void setTotal(int mode, double meters) {
        if (!Double.isFinite(meters) || meters < 0) throw new IllegalArgumentException("Invalid distance");
        double a = distance(1, mode), b = distance(2, mode);
        totals[mode] = meters;
        origins[0][mode] = Math.max(0, meters - a);
        origins[1][mode] = Math.max(0, meters - b);
    }

    public static double acceptedDistance(double dx, double dy, double dz, double dt, boolean horizontal,
            double minSpeed, double maxSpeed) {
        double full = acceptedCombined(dx, dy, dz, dt, minSpeed, maxSpeed);
        if (full == 0) return 0;
        double d = horizontal ? Math.hypot(dx, dz) : full;
        return d / dt < minSpeed ? 0 : d;
    }

    public static double acceptedCombined(double dx, double dy, double dz, double dt, double minSpeed,
            double maxSpeed) {
        if (!Double.isFinite(dt) || dt <= 0 || !Double.isFinite(dx) || !Double.isFinite(dy)
                || !Double.isFinite(dz)) return 0;
        double distance = Math.hypot(Math.hypot(dx, dz), dy);
        double speed = distance / dt;
        return !Double.isFinite(speed) || speed > maxSpeed || speed < minSpeed ? 0 : distance;
    }
}
