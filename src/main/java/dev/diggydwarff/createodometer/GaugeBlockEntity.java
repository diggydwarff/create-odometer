package dev.diggydwarff.createodometer;

import java.util.List;
import java.util.UUID;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public final class GaugeBlockEntity extends SmartBlockEntity implements MenuProvider, com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    public int selected, distanceMode;
    public final double[] readings=new double[9];
    public UUID shipId;
    public boolean attached;
    private int ticks;
    private long readingRevision,sentRevision;
    private UUID previousShip;
    public GaugeBlockEntity(BlockPos pos, BlockState state) { super(CreateOdometer.GAUGE_ENTITY.get(),pos,state); }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
    public SubLevel ship() { return level == null ? null : Sable.HELPER.getContaining(this); }
    public String shipLabel() { var s=ship(); return s==null ? "Unattached gauge" : (s.getName()==null ? "Ship" : s.getName()) + " [" + s.getUniqueId() + "]"; }
    public void refresh() {
        if (!(level instanceof ServerLevel sl)) return;
        boolean wasAttached=attached;UUID oldShip=shipId;
        var ship=ship(); attached=ship!=null && !ship.isRemoved(); shipId=attached ? ship.getUniqueId() : null;
        if(wasAttached!=attached||!java.util.Objects.equals(oldShip,shipId))readingRevision++;
        if (attached) {
            var data=MileageData.get(sl);
            var r=data.record(shipId);
            if (!shipId.equals(previousShip)) {
                r=data.bind(shipId,previousShip);
                previousShip=shipId;
                setChanged();
            }
            for(int mode=0;mode<3;mode++)for(int counter=0;counter<3;counter++){int i=mode*3+counter;double next=r.distance(counter,mode);if(readings[i]!=next){readings[i]=next;readingRevision++;}}
        }
        else for(int i=0;i<9;i++)if(readings[i]!=0){readings[i]=0;readingRevision++;}
    }
    @Override public void tick() {
        super.tick();
        if (level==null || level.isClientSide || ++ticks % OdometerConfig.SYNC_TICKS.get()!=0) return;
        var partner=groupStart();
        if(partner!=this && (selected!=partner.selected||distanceMode!=partner.distanceMode)) { selected=partner.selected;distanceMode=partner.distanceMode;setChanged();sendData(); }
        refresh();
        if(readingRevision!=sentRevision){sendData();sentRevision=readingRevision;}
    }
    @Override protected void write(CompoundTag t, HolderLookup.Provider p, boolean packet) {
        super.write(t,p,packet); t.putInt("Counter",selected); t.putInt("DistanceMode",distanceMode);
        if (!packet && previousShip!=null) t.putUUID("PreviousShip",previousShip);
        if (packet) { for(int i=0;i<9;i++)t.putDouble("Reading"+i,readings[i]); t.putBoolean("Attached",attached); if(shipId!=null)t.putUUID("Ship",shipId); }
    }
    @Override protected void read(CompoundTag t, HolderLookup.Provider p, boolean packet) {
        super.read(t,p,packet); selected=Math.clamp(t.getInt("Counter"),0,2); distanceMode=Math.clamp(t.getInt("DistanceMode"),0,2);
        if (!packet) previousShip=t.hasUUID("PreviousShip")?t.getUUID("PreviousShip"):null;
        if(packet) { for(int i=0;i<9;i++)readings[i]=t.getDouble("Reading"+i); attached=t.getBoolean("Attached"); shipId=t.hasUUID("Ship")?t.getUUID("Ship"):null; }
    }
    public double displayed() { return readings[distanceMode*3+selected]; }
    private GaugeBlockEntity neighbour(int offset) {
        if(level==null)return null;
        var state=getBlockState();
        var pos=getBlockPos().relative(state.getValue(GaugeBlock.FACING).getClockWise(),offset);
        if(!level.hasChunkAt(pos))return null;
        if(level.getBlockEntity(pos) instanceof GaugeBlockEntity other && other.getBlockState().getValue(GaugeBlock.FACING)==state.getValue(GaugeBlock.FACING) && other.getBlockState().getValue(GaugeBlock.CEILING).equals(state.getValue(GaugeBlock.CEILING)))return other;
        return null;
    }
    public GaugeBlockEntity groupStart() {
        var panel=getBlockState().getValue(GaugeBlock.PANEL);
        if(panel==GaugeBlock.Panel.SINGLE||panel==GaugeBlock.Panel.START)return this;
        for(int i=1;i<=2;i++) {
            var other=neighbour(-i);
            if(other==null)return this;
            var role=other.getBlockState().getValue(GaugeBlock.PANEL);
            if(role==GaugeBlock.Panel.START)return other;
            if(role!=GaugeBlock.Panel.MIDDLE)return this;
        }
        return this;
    }
    public int groupSize() {
        var start=groupStart();
        if(start!=this)return start.groupSize();
        if(getBlockState().getValue(GaugeBlock.PANEL)!=GaugeBlock.Panel.START)return 1;
        var next=neighbour(1);
        if(next==null)return 1;
        if(next.getBlockState().getValue(GaugeBlock.PANEL)==GaugeBlock.Panel.END)return 2;
        var end=neighbour(2);
        return next.getBlockState().getValue(GaugeBlock.PANEL)==GaugeBlock.Panel.MIDDLE&&end!=null&&end.getBlockState().getValue(GaugeBlock.PANEL)==GaugeBlock.Panel.END?3:1;
    }
    public void select(int counter,int mode) {
        var start=groupStart();
        for(int i=0,size=start.groupSize();i<size;i++) {
            var member=i==0?start:start.neighbour(i);
            if(member!=null){member.selected=counter;member.distanceMode=mode;member.setChanged();member.sendData();}
        }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        com.simibubi.create.foundation.utility.CreateLang.builder().add(Component.translatable("block.createodometer.odometer")).style(net.minecraft.ChatFormatting.GOLD).forGoggles(tooltip);
        if(!attached){com.simibubi.create.foundation.utility.CreateLang.text("Unattached gauge").style(net.minecraft.ChatFormatting.GRAY).forGoggles(tooltip);return true;}
        com.simibubi.create.foundation.utility.CreateLang.text(DistanceMode.byId(distanceMode).label).style(net.minecraft.ChatFormatting.GRAY).forGoggles(tooltip);
        for(int counter=0;counter<3;counter++)com.simibubi.create.foundation.utility.CreateLang.text((counter==0?"Total":counter==1?"Trip A":"Trip B")+": "+OdometerConfig.format(readings[distanceMode*3+counter])).style(net.minecraft.ChatFormatting.AQUA).forGoggles(tooltip,1);
        return true;
    }
    @Override public Component getDisplayName() { return Component.translatable("block.createodometer.odometer"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { refresh(); return new GaugeMenu(id,inventory,this); }
}
