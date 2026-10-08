package dev.diggydwarff.createodometer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/** Registers the client renderer, configuration screen, and gauge menu. */
@Mod(value = CreateOdometer.ID, dist = Dist.CLIENT)
public final class OdometerClient {
    public OdometerClient(IEventBus bus, ModContainer container) {
        bus.addListener(OdometerClient::renderers);
        bus.addListener(OdometerClient::screens);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(CreateOdometer.GAUGE_ENTITY.get(), GaugeRenderer::new);
    }

    private static void screens(RegisterMenuScreensEvent e) {
        e.register(CreateOdometer.GAUGE_MENU.get(), GaugeScreen::new);
    }
}
