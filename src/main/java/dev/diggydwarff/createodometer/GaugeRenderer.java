package dev.diggydwarff.createodometer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.DyeColor;

/** Renders the physical gauge reading on the instrument face. */
public final class GaugeRenderer implements BlockEntityRenderer<GaugeBlockEntity> {
    public GaugeRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(GaugeBlockEntity be) {
        var box = new net.minecraft.world.phys.AABB(be.getBlockPos());
        var start = be.groupStart();
        var end = start.getBlockPos().relative(start.getBlockState().getValue(GaugeBlock.FACING).getClockWise(),
                start.groupSize() - 1);
        return box.minmax(new net.minecraft.world.phys.AABB(start.getBlockPos()))
                .minmax(new net.minecraft.world.phys.AABB(end));
    }

    @Override
    public void render(GaugeBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffer,
            int light, int overlay) {
        if (be.groupStart() != be) return;
        int width = be.groupSize();
        String text = be.attached ? OdometerConfig.format(be.displayed()) : "--";
        String mode =(be.selected == 1 ? "TRIP A" : be.selected == 2 ? "TRIP B" : "TOTAL") + " " + DistanceMode.byId(be.distanceMode).shortLabel;
        pose.pushPose();
        pose.translate(.5, 0, .5);
        float angle = switch (be.getBlockState().getValue(GaugeBlock.FACING)) {
            case SOUTH -> 180;
            case EAST -> 270;
            case WEST -> 90;
            default -> 0;
        };
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate((width - 1) * .5, be.getBlockState().getValue(GaugeBlock.CEILING) ? .84 : .39, -.377);
        float scale = Math.min(.019f, (.60f + (width - 1) * .98f) / Math.max(1, Minecraft.getInstance()
                .font.width(text)));
        pose.pushPose();
        pose.scale(scale, -scale, scale);
        NixieTubeRenderer.drawTube(pose, buffer, text, 0, DyeColor.ORANGE, be.getLevel().random);
        pose.popPose();
        pose.translate(0, -.17, -.001);
        pose.scale(.009f, -.009f, .009f);
        NixieTubeRenderer.drawTube(pose, buffer, mode, 0, DyeColor.ORANGE, be.getLevel().random);
        pose.popPose();
    }
}
