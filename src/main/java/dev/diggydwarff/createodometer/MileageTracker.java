package dev.diggydwarff.createodometer;

import java.util.*;
import org.joml.Vector3d;
import dev.ryanhcode.sable.api.sublevel.*;
import dev.ryanhcode.sable.sublevel.*;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
/** Capture the same local COM before/after each physics step, so turning and rebuilding do not add fictitious motion. */
public final class MileageTracker {
    private record Sample(Vector3d local, Vector3d world) {}
    private static final Map<SubLevelPhysicsSystem, Map<ServerSubLevel,Sample>> BEFORE = new WeakHashMap<>();
    public static void beforeStep(SubLevelPhysicsSystem system, double dt) {
        var samples = new IdentityHashMap<ServerSubLevel,Sample>();
        var container = SubLevelContainer.getContainer(system.getLevel());
        if (container instanceof ServerSubLevelContainer c) for (ServerSubLevel ship : c.getAllSubLevels()) {
            if (ship.isRemoved()) continue;
            var com = ship.getMassTracker().getCenterOfMass();
            if (com == null) continue;
            var local = new Vector3d(com);
            samples.put(ship, new Sample(local, ship.logicalPose().transformPosition(local, new Vector3d())));
        }
        BEFORE.put(system,samples);
    }
    public static void afterStep(SubLevelPhysicsSystem system, double dt) {
        var samples = BEFORE.remove(system);
        if (samples == null) return;
        var data = MileageData.get(system.getLevel());
        samples.forEach((ship,s) -> {
            if (ship.isRemoved()) return;
            var end = ship.logicalPose().transformPosition(s.local(), new Vector3d());
            double dx=end.x-s.world().x,dy=end.y-s.world().y,dz=end.z-s.world().z;
            double d=MileageRecord.acceptedCombined(dx,dy,dz,dt,OdometerConfig.MIN_SPEED.get(),OdometerConfig.MAX_SPEED.get());
            if(d>0 && data.record(ship.getUniqueId()).add(d,Math.hypot(dx,dz),Math.abs(dy))) data.setDirty();
        });
    }
}
