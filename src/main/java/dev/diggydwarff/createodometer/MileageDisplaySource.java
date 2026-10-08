package dev.diggydwarff.createodometer;

import java.util.List;
import java.util.Locale;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Server-side output uses the link's units, never a player's client config. */
public final class MileageDisplaySource extends SingleLineDisplaySource {
    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof GaugeBlockEntity gauge)) return Component.literal("--");
        gauge.refresh();
        if (!gauge.attached) return Component.literal("--");
        var controller = gauge.groupStart();
        var config = context.sourceConfig();
        int counter = Math.clamp(config.getInt("Counter"), 0, 3);
        int mode = Math.clamp(config.getInt("Mode"), 0, 3);
        counter = counter == 0 ? controller.selected : counter - 1;
        mode = mode == 0 ? controller.distanceMode : mode - 1;
        return Component.literal(format(gauge.readings[mode * 3 + counter], config.getInt("Units"),
                config.contains("Decimals") ? config.getInt("Decimals") : 2));
    }

    public static String format(double meters, int units, int decimals) {
        units = Math.clamp(units, 0, 2);
        decimals = Math.clamp(decimals, 0, 3);
        if (!Double.isFinite(meters) || meters < 0) return "--";
        double divisor = units == 1 ? 1609.344 : units == 2 ? 1 : 1000;
        return String.format(Locale.ROOT, "%." + decimals + "f",
                meters / divisor) + (units == 1 ? " mi" : units == 2 ? " m" : " km");
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return false;
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
    // Create calls this to refresh the source; it does not reset mileage.
    @Override
    public boolean shouldPassiveReset() {
        return true;
    }

    @Override
    public void onSignalReset(DisplayLinkContext context) {
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean first) {
        if (!context.sourceConfig().contains("Decimals")) context.sourceConfig().putInt("Decimals", 2);
        if (first) {
            builder.addSelectionScrollInput(0, 67, (input,
                    label) -> input.forOptions(List.of(Component.translatable("createodometer.display_link.follow"),
                    Component.literal("Total"), Component.literal("Trip A"), Component.literal("Trip B")))
                    .titled(Component.translatable("createodometer.display_link.counter")), "Counter");
            builder.addSelectionScrollInput(70, 67, (input,
                    label) -> input.forOptions(List.of(Component.translatable("createodometer.display_link.follow"),
                    Component.literal("Overall"), Component.literal("Horizontal"), Component.literal("Vertical")))
                    .titled(Component.translatable("createodometer.display_link.mode")), "Mode");
        } else {
            builder.addSelectionScrollInput(0, 67, (input, label) -> input.forOptions(List.of(Component.literal("km"),
                    Component.literal("mi"), Component.literal("m")))
                    .titled(Component.translatable("createodometer.display_link.units")), "Units");
            builder.addScrollInput(70, 67, (input, label) -> input.withRange(0, 4).setState(2)
                    .titled(Component.translatable("createodometer.display_link.decimals")), "Decimals");
        }
    }
}
