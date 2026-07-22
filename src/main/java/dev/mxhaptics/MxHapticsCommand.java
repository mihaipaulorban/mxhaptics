package dev.mxhaptics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.mxhaptics.bridge.HapticBridge;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/**
 * Client-side command that fires a test vibration on demand.
 * Registered as {@code /mxhaptics_test [pattern] [intensity]}.
 * <ul>
 *   <li>No args: pattern 1, intensity 75 (the default "does haptic work?" test).</li>
 *   <li>One arg: given pattern, intensity 75.</li>
 *   <li>Two args: given pattern and intensity.</li>
 * </ul>
 * Bypasses the per-event enabled/cooldown checks so the test always fires
 * (as long as the bridge is connected).
 */
public final class MxHapticsCommand {

    private MxHapticsCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(
                        ClientCommands.literal("mxhaptics_test")
                                .then(ClientCommands.argument(
                                                "pattern", IntegerArgumentType.integer(0, 15))
                                        .then(ClientCommands.argument(
                                                        "intensity",
                                                        IntegerArgumentType.integer(0, 100))
                                                .executes(ctx -> fireTest(
                                                        IntegerArgumentType.getInteger(ctx, "pattern"),
                                                        IntegerArgumentType.getInteger(ctx, "intensity"),
                                                        ctx.getSource())))
                                        .executes(ctx -> fireTest(
                                                IntegerArgumentType.getInteger(ctx, "pattern"),
                                                75,
                                                ctx.getSource())))
                                .executes(ctx -> fireTest(1, 75, ctx.getSource()))));
    }

    private static int fireTest(int pattern, int intensity, FabricClientCommandSource src) {
        HapticBridge bridge = MxHapticsClient.bridge();
        if (bridge != null && bridge.isConnected()) {
            bridge.trigger(pattern, intensity);
            src.sendFeedback(Component.translatable(
                    "mxhaptics.command.test.fired", pattern, intensity));
        } else {
            src.sendFeedback(Component.translatable("mxhaptics.command.test.notconnected"));
        }
        return 1;
    }
}
