package dev.diggydwarff.createodometer;

import java.util.List;
import java.util.ArrayList;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.client.gui.GuiGraphics;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Create frame segments and icon buttons, including the per-display distance selector. */
public final class GaugeScreen extends AbstractSimiContainerScreen<GaugeMenu> {
    private int armedReset = -1, headerExtra;
    private long armedUntil;
    private List<FormattedCharSequence> shipLines = List.of();
    private final List<IconButton> resetButtons = new ArrayList<>();
    public GaugeScreen(GaugeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 256;
        imageHeight = 230;
    }

    private IconButton button(int x, int y, AllIcons icon, String tip, Runnable action) {
        IconButton b = new IconButton(leftPos + x, topPos + y + headerExtra, icon);
        b.setToolTip(Component.literal(tip));
        b.withCallback(action);
        addRenderableWidget(b);
        return b;
    }

    @Override
    protected void init() {
        // Give the UUID its own line. Wrap the name and ID separately without truncating either.
        var lines = new ArrayList<FormattedCharSequence>();
        int idStart = menu.shipLabel.lastIndexOf(" [");
        if (idStart >= 0) {
            lines.addAll(font.split(Component.literal(menu.shipLabel.substring(0, idStart)), 228));
            lines.addAll(font.split(Component.literal(menu.shipLabel.substring(idStart + 1)), 228));
        } else lines.addAll(font.split(Component.literal(menu.shipLabel), 228));
        shipLines = lines;
        headerExtra = Math.max(0, lines.size() - 1) * 10;
        imageHeight = 230 + headerExtra;
        super.init();
        resetButtons.clear();
        for (int i = 0; i < 3; i++) {
            final int counter = i;
            button(207, 45 + i * 29, AllIcons.I_CONFIRM,
                    "Show " + (i == 0 ? "Total" : i == 1 ? "Trip A" : "Trip B") + " on this display",
                    () -> send(counter));
        }
        resetButtons.add(button(229, 74, AllIcons.I_REFRESH, "Reset Trip A in all distance modes (click twice)",
                () -> reset(1)));
        resetButtons.add(button(229, 103, AllIcons.I_REFRESH, "Reset Trip B in all distance modes (click twice)",
                () -> reset(2)));
        for (int i = 0; i < 3; i++) {
            final int mode = i;
            button(16 + i * 79, 154, AllIcons.I_CONFIRM, DistanceMode.byId(i)
                    .label + ": " + (i == 0 ? "full path length, including climbing and descending" : i == 1 ? "ground-plane path length" : "all climbing and descending"), () -> send(20 + mode));
        }
        button(16, 188, AllIcons.I_REFRESH, "Switch local units: km / mi", () -> {
            OdometerConfig.UNIT.set(OdometerConfig.UNIT.get() == OdometerConfig.Unit.MILES ? OdometerConfig.Unit.KILOMETERS : OdometerConfig.Unit.MILES);
            OdometerConfig.CLIENT_SPEC.save();
        });
        button(229, 203, AllIcons.I_CONFIRM, "Close", this::onClose);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (var button : resetButtons) button.active = menu.canReset();
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void reset(int trip) {
        long now = net.minecraft.Util.getMillis();
        if (armedReset == trip && now < armedUntil) {
            send(trip == 1 ? 10 : 11);
            armedReset = -1;
        } else {
            armedReset = trip;
            armedUntil = now + 3000;
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        renderPanel(g);
        g.drawString(font, title, leftPos + 12, topPos + 4, AllGuiTextures.FONT_COLOR, false);
        for (int i = 0; i < shipLines.size(); i++) g.drawString(font, shipLines.get(i), leftPos + 12,
                topPos + 23 + i * 10, 0xffe9d6b6, false);
        int offset = topPos + headerExtra;
        g.fill(leftPos + 12, offset + 40, leftPos + 204, offset + 132, 0xff614433);
        for (int i = 0; i < 3; i++) {
            int y = offset + 45 + i * 29;
            g.fill(leftPos + 15, y, leftPos + 201, y + 24, 0xff25252c);
            String name = (menu.selected() == i ? "> " : "") + (i == 0 ? "TOTAL" : i == 1 ? "TRIP A" : "TRIP B");
            g.drawString(font, name, leftPos + 20, y + 3, 0xffb5b2ac, false);
            String reading = menu.attached() ? OdometerConfig.format(menu.distance(i)) : "--";
            g.drawString(font, reading, leftPos + 198 - font.width(reading), y + 13, 0xffffa04c, false);
        }
        g.drawString(font, "Distance mode", leftPos + 15, offset + 137, 0xffe9d6b6, false);
        String[] labels = {
            "Overall", "Horiz.", "Vert."
        };
        for (int i = 0; i < 3; i++) g.drawString(font, labels[i], leftPos + 40 + i * 79, offset + 159,
                menu.distanceMode() == i ? 0xffffa04c : 0xffe9d6b6, false);
        g.drawString(font, menu.attached() ? "Ship distance recorded on server" : "Install on an Aeronautics ship",
                leftPos + 15, offset + 177, 0xffe9d6b6, false);
        g.drawString(font, "Units: " + (OdometerConfig.UNIT.get() == OdometerConfig.Unit.MILES ? "miles" : "kilometers"), leftPos + 40, offset + 193, 0xffe9d6b6, false);
        if (armedReset != - 1 && net.minecraft.Util.getMillis() < armedUntil) g.drawString(font,
                "Click reset again to confirm", leftPos + 15, offset + 215, 0xffffce87, false);
    }

    private void renderPanel(GuiGraphics g) {
        var texture = AllGuiTextures.STOCK_KEEPER_REQUEST_HEADER.location;
        int footer = imageHeight - 9, body = footer - 15;
        g.fill(leftPos + 8, topPos + 15, leftPos + 248, topPos + footer, 0xff855d49);
        g.blit(texture, leftPos, topPos, 24, 0, 8, 15);
        g.blit(texture, leftPos + 248, topPos, 224, 0, 8, 15);
        g.blit(texture, leftPos, topPos + footer, 24, 151, 8, 9);
        g.blit(texture, leftPos + 248, topPos + footer, 224, 151, 8, 9);
        g.blit(texture, leftPos + 8, topPos, 240, 15, 40f, 0f, 1, 15, 256, 256);
        g.blit(texture, leftPos + 8, topPos + footer, 240, 9, 40f, 151f, 1, 9, 256, 256);
        g.blit(texture, leftPos, topPos + 15, 8, body, 24f, 56f, 8, 1, 256, 256);
        g.blit(texture, leftPos + 248, topPos + 15, 8, body, 224f, 56f, 8, 1, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
    }
}
