package dev.diggydwarff.createodometer;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Placement and panel shape rules for single, double, and triple odometer displays. */
public final class GaugeBlock extends HorizontalDirectionalBlock implements IBE<GaugeBlockEntity>, IWrenchable {
    public enum Panel implements net.minecraft.util.StringRepresentable {
        SINGLE, START, MIDDLE, END;
        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Panel> PANEL =
            net.minecraft.world.level.block.state.properties.EnumProperty.create("panel", Panel.class);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty CEILING =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("ceiling");
    public static final MapCodec<GaugeBlock> CODEC = simpleCodec(GaugeBlock::new);
    public GaugeBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CEILING, false)
                .setValue(PANEL, Panel.SINGLE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, CEILING, PANEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        var state = defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()).setValue(CEILING,
                c.getClickedFace() == Direction.DOWN);
        return state.setValue(PANEL, panel(c.getLevel(), c.getClickedPos(), state));
    }

    private static boolean matches(BlockGetter level, BlockPos pos, BlockState state) {
        var other = level.getBlockState(pos);
        return other.getBlock() == state.getBlock() && other.getValue(FACING) == state.getValue(FACING)
                && other.getValue(CEILING).equals(state.getValue(CEILING));
    }

    /** Adjacent aligned panels form a strip of at most three gauges. */
    private static Panel panel(BlockGetter level, BlockPos pos, BlockState state) {
        Direction positive = state.getValue(FACING).getClockWise(), negative = positive.getOpposite();
        int before = 0, after = 0;
        while (before < 3 && matches(level, pos.relative(negative, before + 1), state)) before++;
        while (after < 3 && matches(level, pos.relative(positive, after + 1), state)) after++;
        if (before + after == 0 || before + after >= 3) return Panel.SINGLE;
        return before == 0 ? Panel.START : after == 0 ? Panel.END : Panel.MIDDLE;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
            BlockPos pos, BlockPos neighbourPos) {
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos).setValue(PANEL,
                panel(level, pos, state));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from,
            boolean moving) {
        var updated = state.setValue(PANEL, panel(level, pos, state));
        if (updated != state) level.setBlock(pos, updated, 3);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        double low = s.getValue(CEILING) ? 8 : 0, high = low + 8;
        Panel panel = s.getValue(PANEL);
        double left = panel == Panel.END || panel == Panel.MIDDLE ? 0 : 2, right = panel == Panel.START
                || panel == Panel.MIDDLE ? 16 : 14;
        return switch (s.getValue(FACING)) {
            case SOUTH -> Block.box(16 - right, low, 6, 16 - left, high, 14);
            case EAST -> Block.box(6, low, left, 14, high, right);
            case WEST -> Block.box(2, low, 16 - right, 10, high, 16 - left);
            default -> Block.box(left, low, 2, right, high, 10);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
        if (l.getBlockEntity(p) instanceof GaugeBlockEntity be
                && player instanceof ServerPlayer sp) sp.openMenu(be, buf -> {
            buf.writeBlockPos(p);
            buf.writeUtf(be.shipLabel());
        });
        return InteractionResult.sidedSuccess(l.isClientSide);
    }

    @Override
    protected void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
        IBE.onRemove(s, l, p, next);
    }

    @Override
    public Class<GaugeBlockEntity> getBlockEntityClass() {
        return GaugeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GaugeBlockEntity> getBlockEntityType() {
        return CreateOdometer.GAUGE_ENTITY.get();
    }
}
