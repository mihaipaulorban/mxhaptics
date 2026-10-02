package dev.mxhaptics;

import com.mojang.blaze3d.platform.InputConstants;
import dev.mxhaptics.bridge.HapticBridge;
import dev.mxhaptics.config.ConfigManager;
import dev.mxhaptics.events.TickObservers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint. The mod is entirely client-side: no server logic runs,
 * and it works on vanilla servers as well.
 */
public class MxHapticsClient implements ClientModInitializer {

    public static final String MOD_ID = "mxhaptics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static HapticBridge bridge;
    private static KeyMapping testKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        bridge = new HapticBridge();
        HapticManager.init(bridge);
        bridge.start();

        // Block broken by the local player (client-side Fabric API event).
        ClientPlayerBlockBreakEvents.AFTER.register((world, player, pos, state) ->
                HapticManager.trigger(HapticEvent.BLOCK_BREAK));

        // Damage, air bubbles, hotbar and shield block are handled by the tick observer.
        ClientTickEvents.END_CLIENT_TICK.register(TickObservers::onEndTick);

        // "Test haptic vibration" keybind. Unbound by default — the user can
        // assign a key from Options > Controls > Key Binds > MX Haptics.
        // Since Minecraft 26.x, KeyMapping takes a KeyMapping.Category object
        // (registered with a ResourceLocation) instead of a translation-key string.
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "controls"));
        testKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mxhaptics.test",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (testKey.consumeClick()) {
                fireTestVibration();
            }
        });

        // Client command /mxhaptics_test [pattern] [intensity].
        MxHapticsCommand.register();

        // Clean bridge shutdown when the game stops.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> bridge.shutdown());

        LOGGER.info("[mxhaptics] initialized (bridge: {})",
                ConfigManager.get().bridgeUrl);
    }

    /** Fires a fixed test vibration (pattern 1, intensity 75) via the keybind. */
    public static void fireTestVibration() {
        HapticBridge b = bridge;
        if (b != null && b.isConnected()) {
            b.trigger(1, 75);
        }
    }

    public static HapticBridge bridge() {
        return bridge;
    }
}
