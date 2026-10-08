package dev.diggydwarff.createodometer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

/** Exchanges gauge readings and controls between server menus and clients. */
public final class GaugeMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public final String shipLabel;
    private final GaugeBlockEntity gauge;
    private final ContainerData data;
    private long lastReset = -100;
    public GaugeMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(CreateOdometer.GAUGE_MENU.get(), id);
        pos = buf.readBlockPos();
        shipLabel = buf.readUtf();
        gauge = null;
        data = new SimpleContainerData(40);
        addDataSlots(data);
    }

    public GaugeMenu(int id, Inventory inv, GaugeBlockEntity be) {
        super(CreateOdometer.GAUGE_MENU.get(), id);
        gauge = be;
        pos = be.getBlockPos();
        shipLabel = be.shipLabel();
        // Nine doubles use 36 short slots; slots 36-39 carry the gauge controls.
        data = new ContainerData() {
            public int get(int i) {
                if (i == 36) return be.selected;
                if (i == 38) return be.distanceMode;
                if (i == 39) return OdometerConfig.ALLOW_TRIP_RESET.get() || inv.player.hasPermissions(2) ? 1 : 0;
                if (i == 37) return be.attached ? 1 : 0;
                long bits = Double.doubleToLongBits(be.readings[i / 4]);
                return (int)((bits >>>((i % 4) * 16)) & 0xffff);
            }
            public void set(int i, int v) {
            }
            public int getCount() {
                return 40;
            }
        };
        addDataSlots(data);
    }

    @Override
    public void broadcastChanges() {
        if (gauge != null && gauge.getLevel() != null && gauge.getLevel()
                .getGameTime() % OdometerConfig.SYNC_TICKS.get() == 0) gauge.refresh();
        super.broadcastChanges();
    }

    /** Rebuild a double from four synced 16-bit menu data slots. */
    public double distance(int counter) {
        long bits = 0;
        for (int i = 0; i < 4; i++) bits |=((long) data.get((distanceMode() * 3 + counter) * 4 + i) & 0xffffL) <<(i * 16);
        double d = Double.longBitsToDouble(bits);
        return Double.isFinite(d) && d >= 0 ? d : 0;
    }

    public int selected() {
        return data.get(36);
    }

    public boolean attached() {
        return data.get(37) != 0;
    }

    public int distanceMode() {
        return Math.clamp(data.get(38), 0, 2);
    }

    public boolean canReset() {
        return attached() && data.get(39) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        if (gauge == null) return true;
        if (gauge.isRemoved() || player.level() != gauge.getLevel()) return false;
        Vec3 center = Vec3.atCenterOf(pos);
        var ship = gauge.ship();
        if (ship != null) center = ship.logicalPose().transformPosition(center);
        return player.distanceToSqr(center) <= 64;
    }

    @Override
    public boolean clickMenuButton(Player p, int button) {
        if (gauge == null || !(p.level() instanceof ServerLevel sl) || !stillValid(p)) return false;
        if (button >= 0 && button <= 2) {
            gauge.select(button, gauge.distanceMode);
            return true;
        }
        if (button >= 20 && button <= 22) {
            gauge.select(gauge.selected, button - 20);
            return true;
        }
        if (button != 10 && button != 11) return false;
        if (!OdometerConfig.ALLOW_TRIP_RESET.get() && !p.hasPermissions(2)) return false;
        var ship = gauge.ship();
        if (ship == null || ship.isRemoved()) return false;
        if (sl.getGameTime() - lastReset < 10) return false;
        lastReset = sl.getGameTime();
        var data = MileageData.get(sl);
        data.record(ship.getUniqueId()).resetTrip(button == 10 ? 1 : 2);
        data.setDirty();
        gauge.refresh();
        gauge.sendData();
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int i) {
        return ItemStack.EMPTY;
    }
}
