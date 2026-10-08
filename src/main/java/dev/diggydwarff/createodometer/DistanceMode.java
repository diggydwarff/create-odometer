package dev.diggydwarff.createodometer;

public enum DistanceMode {
    COMBINED("Overall", "ALL"), HORIZONTAL("Horizontal", "H"), VERTICAL("Vertical", "V");
    public final String label, shortLabel;
    DistanceMode(String label, String shortLabel) { this.label=label; this.shortLabel=shortLabel; }
    public static DistanceMode byId(int id) { return values()[Math.clamp(id,0,2)]; }
}
