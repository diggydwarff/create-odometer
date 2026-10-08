package dev.diggydwarff.createodometer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import dev.ryanhcode.sable.platform.SableEventPlatform;

/** Registers the gauge and hooks ship physics updates into mileage tracking. */
@Mod(CreateOdometer.ID)
public final class CreateOdometer {
    static final DeferredRegister<com.simibubi.create.api.behaviour.display.DisplaySource> DISPLAY_SOURCES =
            DeferredRegister.create(com.simibubi.create.api.registry.CreateRegistries.DISPLAY_SOURCE, "createodometer");
    public static final DeferredHolder<com.simibubi.create.api.behaviour.display.DisplaySource,
            MileageDisplaySource> MILEAGE_SOURCE = DISPLAY_SOURCES.register("mileage", MileageDisplaySource::new);
    public static final String ID = "createodometer";
    static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ID);
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
    static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredHolder<Block, GaugeBlock> GAUGE = BLOCKS.register("odometer", () ->
            new GaugeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2)
            .sound(SoundType.METAL).noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> GAUGE_ITEM = ITEMS.register("odometer", () ->
            new BlockItem(GAUGE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GaugeBlockEntity>> GAUGE_ENTITY =
            ENTITIES.register("odometer", () -> BlockEntityType.Builder.of(GaugeBlockEntity::new,
            GAUGE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<GaugeMenu>> GAUGE_MENU = MENUS.register("odometer", () ->
            IMenuTypeExtension.create(GaugeMenu::new));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("instruments", () ->
            CreativeModeTab.builder().title(Component.translatable("itemGroup.createodometer")).icon(() ->
            new ItemStack(GAUGE_ITEM.get())).displayItems((p, o) -> o.accept(GAUGE_ITEM.get())).build());
    public CreateOdometer(IEventBus bus, ModContainer container) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
        DISPLAY_SOURCES.register(bus);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
                com.simibubi.create.api.behaviour.display.DisplaySource.BY_BLOCK_ENTITY.add(GAUGE_ENTITY.get(),
                MILEAGE_SOURCE.get())));
        container.registerConfig(ModConfig.Type.SERVER, OdometerConfig.SERVER_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, OdometerConfig.CLIENT_SPEC);
        SableEventPlatform.INSTANCE.onSubLevelContainerReady((level, ships) -> {
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                ships.addObserver(new dev.ryanhcode.sable.api.sublevel.SubLevelObserver() {
                    @Override
                    public void onSubLevelRemoved(dev.ryanhcode.sable.sublevel.SubLevel ship,
                            dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason reason) {
                        if (reason == dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED) MileageData.get(server).retire(ship.getUniqueId());
                    }
                });
            }
        });
        SableEventPlatform.INSTANCE.onPhysicsTick(MileageTracker::beforeStep);
        SableEventPlatform.INSTANCE.onPostPhysicsTick(MileageTracker::afterStep);
        NeoForge.EVENT_BUS.addListener(MileageCommands::register);
    }
}
