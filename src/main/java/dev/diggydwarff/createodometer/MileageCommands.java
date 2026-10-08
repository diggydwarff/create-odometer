package dev.diggydwarff.createodometer;

import java.util.UUID;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.*;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import static net.minecraft.commands.Commands.*;

/** Ship mileage lookup, reset, and correction commands. */
public final class MileageCommands {
    private static final SimpleCommandExceptionType INVALID =
            new SimpleCommandExceptionType(Component.literal("Use a recorded ship UUID or 'here' while aboard/looking at a ship."));
    public static void register(RegisterCommandsEvent e) {
        var root = literal("shipmileage");
        root.then(literal("get").then(argument("ship", StringArgumentType.word()).suggests((ctx, b) ->
                SharedSuggestionProvider.suggest(MileageData.get(ctx.getSource().getLevel()).ids().stream()
                .map(UUID::toString), b)).executes(ctx -> get(ctx))));
        root.then(literal("list").requires(s -> s.hasPermission(2)).executes(ctx -> {
            var data = MileageData.get(ctx.getSource().getLevel());
            for (UUID id : data.ids()) ctx.getSource().sendSuccess(() -> Component.literal(id + " = " + data.existing(id).total() + " m"), false);
            return data.ids().size();
        }));
        root.then(literal("reset").requires(s -> s.hasPermission(2)).then(argument("ship", StringArgumentType.word())
                .then(literal("all").executes(c -> reset(c, 0))).then(literal("trip_a").executes(c -> reset(c,
                1))).then(literal("trip_b").executes(c -> reset(c, 2)))));
        var set = argument("ship", StringArgumentType.word());
        var distance = argument("distance", DoubleArgumentType.doubleArg(0, 1.0E12));
        for (String unit : new String[]{
            "m", "km", "mi"
        }) {
            var unitNode = literal(unit).executes(c -> set(c, unit, 0));
            for (int i = 0; i < 3; i++) {
                final int mode = i;
                unitNode.then(literal(DistanceMode.byId(i).name().toLowerCase(java.util.Locale.ROOT)).executes(c ->
                        set(c, unit, mode)));
            }
            unitNode.then(literal("overall").executes(c -> set(c, unit, 0)));
            distance.then(unitNode);
        }
        root.then(literal("set").requires(s -> s.hasPermission(2)).then(set.then(distance)));
        e.getDispatcher().register(root);
    }

    private static UUID resolve(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String input = StringArgumentType.getString(c, "ship");
        UUID id;
        if (input.equals("here")) {
            ServerPlayer p = c.getSource().getPlayerOrException();
            var ship = Sable.HELPER.getTrackingOrVehicleSubLevel(p);
            if (ship == null && p.pick(8, 0, false) instanceof BlockHitResult hit) ship =
                    Sable.HELPER.getContaining(p.level(), hit.getBlockPos());
            if (ship == null) throw INVALID.create();
            id = ship.getUniqueId();
            MileageData.get(c.getSource().getLevel()).record(id);
        } else {
            try {
                id = UUID.fromString(input);
            } catch (IllegalArgumentException ex) {
                throw INVALID.create();
            }
            var data = MileageData.get(c.getSource().getLevel());
            if (data.existing(id) == null) {
                var container = SubLevelContainer.getContainer(c.getSource().getLevel());
                if (container == null || container.getSubLevel(id) == null) throw INVALID.create();
                data.record(id);
            }
        }
        return id;
    }

    private static int get(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        UUID id = resolve(c);
        var r = MileageData.get(c.getSource().getLevel()).record(id);
        for (int i = 0; i < 3; i++) {
            final int mode = i;
            c.getSource().sendSuccess(() -> Component.literal(id + " " + DistanceMode.byId(mode)
                    .label + ": Total " + r.total(mode) + " m; Trip A " + r.distance(1,
                    mode) + " m; Trip B " + r.distance(2, mode) + " m"), false);
        }
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> c, int trip) throws CommandSyntaxException {
        UUID id = resolve(c);
        var data = MileageData.get(c.getSource().getLevel());
        var r = data.record(id);
        if (trip == 0) r.resetAll();
        else r.resetTrip(trip);
        data.setDirty();
        c.getSource().sendSuccess(() -> Component.literal("Reset " + (trip == 0 ? "all counters" : trip == 1 ? "Trip A" : "Trip B") + " for " + id), true);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> c, String unit, int mode) throws CommandSyntaxException {
        UUID id = resolve(c);
        double d = DoubleArgumentType.getDouble(c, "distance") *(unit.equals("km") ? 1000 : unit.equals("mi") ? 1609.344 : 1);
        var data = MileageData.get(c.getSource().getLevel());
        data.record(id).setTotal(mode, d);
        data.setDirty();
        c.getSource().sendSuccess(() -> Component.literal("Set " + DistanceMode.byId(mode)
                .label + " total to " + d + " m for " + id), true);
        return 1;
    }
}
