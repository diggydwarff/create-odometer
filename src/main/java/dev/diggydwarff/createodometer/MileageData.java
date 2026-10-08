package dev.diggydwarff.createodometer;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
/** Global world file, keyed by Sable UUID. No gauge is required for tracking. */
public final class MileageData extends SavedData {
    private final Map<UUID, MileageRecord> records = new HashMap<>();
    private final Set<UUID> retiredShips = new HashSet<>();
    private final Set<UUID> boundShips = new HashSet<>();
    private final Set<UUID> transferredSources = new HashSet<>();
    public static MileageData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(MileageData::new, MileageData::load), "createodometer_mileage");
    }
    public MileageRecord record(UUID id) { return records.computeIfAbsent(id, ignored -> { setDirty(); return new MileageRecord(); }); }
    public MileageRecord existing(UUID id) { return records.get(id); }
    public void retire(UUID id) { if (retiredShips.add(id)) setDirty(); }
    /** Resolve once per assembled ship; moving a gauge between live ships cannot import history. */
    public MileageRecord bind(UUID target, UUID previous) {
        MileageRecord current = record(target);
        if (!boundShips.contains(target) && previous != null && !previous.equals(target)
                && retiredShips.contains(previous) && !transferredSources.contains(previous)) {
            MileageRecord source = existing(previous);
            MileageRecord continued=source==null?null:source.continuedWith(current);
            if (continued!=null) {
                current = continued;
                records.put(target, current);
                transferredSources.add(previous);
                setDirty();
            }
        }
        if (boundShips.add(target)) setDirty();
        return current;
    }
    public Set<UUID> ids() { return Collections.unmodifiableSet(records.keySet()); }
    static MileageData load(CompoundTag tag, HolderLookup.Provider lookup) {
        var data = new MileageData();
        ListTag list = tag.getList("Ships", Tag.TAG_COMPOUND);
        for (int i=0; i<list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (t.hasUUID("Id")) data.records.put(t.getUUID("Id"), readRecord(t));
        }
        readIds(tag, "RetiredShips", data.retiredShips);
        readIds(tag, "BoundShips", data.boundShips);
        readIds(tag, "TransferredSources", data.transferredSources);
        return data;
    }
    private static MileageRecord readRecord(CompoundTag tag) {
        if(!tag.contains("Meters0"))return new MileageRecord(tag.getDouble("Meters"),tag.getDouble("OriginA"),tag.getDouble("OriginB"));
        double[] totals=new double[3];double[][] origins=new double[2][3];
        for(int mode=0;mode<3;mode++){totals[mode]=tag.getDouble("Meters"+mode);origins[0][mode]=tag.getDouble("OriginA"+mode);origins[1][mode]=tag.getDouble("OriginB"+mode);}
        return new MileageRecord(totals,origins);
    }
    private static void readIds(CompoundTag tag, String key, Set<UUID> ids) {
        var list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) if (list.getCompound(i).hasUUID("Id")) ids.add(list.getCompound(i).getUUID("Id"));
    }
    private static ListTag writeIds(Set<UUID> ids) {
        var list = new ListTag();
        for (UUID id : ids) { var entry = new CompoundTag(); entry.putUUID("Id", id); list.add(entry); }
        return list;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        records.forEach((id,r) -> { CompoundTag t = new CompoundTag(); t.putUUID("Id",id); for(int mode=0;mode<3;mode++){t.putDouble("Meters"+mode,r.total(mode));t.putDouble("OriginA"+mode,r.origin(1,mode));t.putDouble("OriginB"+mode,r.origin(2,mode));} list.add(t); });
        tag.put("Ships",list); tag.put("RetiredShips", writeIds(retiredShips)); tag.put("BoundShips", writeIds(boundShips)); tag.put("TransferredSources", writeIds(transferredSources)); tag.putInt("Schema",3); return tag;
    }
}
