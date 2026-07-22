package dev.mxhaptics.integration;

import dev.mxhaptics.HapticEvent;
import dev.mxhaptics.MxHapticsClient;
import dev.mxhaptics.bridge.HapticBridge;
import dev.mxhaptics.config.ConfigManager;
import dev.mxhaptics.config.EventSettings;
import dev.mxhaptics.config.HapticConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Builds the Cloth Config screen.
 *
 * A true "click once and vibrate" button would need a custom widget extending
 * Cloth Config's AbstractConfigListEntry. In Minecraft 26.1+ the rendering
 * pipeline moved to an extract/draw split (GuiGraphicsExtractor,
 * extractWidgetRenderState, etc.), and Cloth Config's internal render signature
 * is not yet publicly documented for 26.2 — implementing a real button widget
 * here would be pure guesswork. So we take the pragmatic route:
 *
 *  - For a real one-click test, use the keybind:
 *      Options > Controls > Key Binds > MX Haptics > "Test haptic vibration".
 *  - Inside this screen, the "Test" widgets are boolean toggles hooked to
 *    setSaveConsumer — toggle → click "Save Changes" and the vibration fires.
 */
public final class ClothConfigScreen {

    /** Deferred test parameters staged by a toggle's save consumer,
     *  applied together after the whole screen finishes saving. */
    private static int pendingTestPattern = -1;
    private static int pendingTestIntensity = -1;

    private ClothConfigScreen() {
    }

    public static Screen create(Screen parent) {
        HapticConfig config = ConfigManager.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("mxhaptics.config.title"));

        ConfigEntryBuilder eb = builder.entryBuilder();

        // Reset any pending test from a previous opening.
        pendingTestPattern = -1;
        pendingTestIntensity = -1;

        // --- General category --------------------------------------------
        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("mxhaptics.config.category.general"));

        // How-to-test blurb at the top of General.
        general.addEntry(eb.startTextDescription(
                        Component.translatable("mxhaptics.config.test.keybindHint")
                                .withStyle(ChatFormatting.YELLOW))
                .build());

        general.addEntry(eb.startTextDescription(
                        Component.translatable("mxhaptics.config.test.intro")
                                .withStyle(ChatFormatting.GRAY))
                .build());

        // Global "quick test" toggle. Trigger vibration on save.
        general.addEntry(eb.startBooleanToggle(
                        Component.translatable("mxhaptics.config.test.quickButton"),
                        false)
                .setDefaultValue(false)
                .setYesNoTextSupplier(v -> Component.translatable(
                        "mxhaptics.config.test.quickButton.click"))
                .setTooltip(Component.translatable(
                        "mxhaptics.config.test.quickButton.tooltip"))
                .setSaveConsumer(v -> {
                    if (Boolean.TRUE.equals(v)) {
                        stageTest(1, 75);
                    }
                })
                .build());

        general.addEntry(eb.startStrField(
                        Component.translatable("mxhaptics.config.bridgeUrl"),
                        config.bridgeUrl)
                .setDefaultValue(HapticConfig.DEFAULT_URL)
                .setTooltip(Component.translatable("mxhaptics.config.bridgeUrl.tooltip"))
                .setSaveConsumer(v -> config.bridgeUrl = v)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        Component.translatable("mxhaptics.config.trustAllCertificates"),
                        config.trustAllCertificates)
                .setDefaultValue(false)
                .setTooltip(Component.translatable(
                        "mxhaptics.config.trustAllCertificates.tooltip"))
                .setSaveConsumer(v -> config.trustAllCertificates = v)
                .build());

        // --- One category per event --------------------------------------
        for (HapticEvent event : HapticEvent.values()) {
            EventSettings settings = config.settings(event);
            ConfigCategory category = builder.getOrCreateCategory(
                    Component.translatable("mxhaptics.event." + event.key()));

            // Mutable holders so the per-event test uses the values as edited
            // in this session (not the values saved before opening the screen).
            int[] editedPattern = {settings.pattern};
            int[] editedIntensity = {settings.intensity};

            category.addEntry(eb.startBooleanToggle(
                            Component.translatable("mxhaptics.config.enabled"),
                            settings.enabled)
                    .setDefaultValue(event.defaultEnabled())
                    .setSaveConsumer(v -> settings.enabled = v)
                    .build());

            category.addEntry(eb.startIntSlider(
                            Component.translatable("mxhaptics.config.pattern"),
                            settings.pattern, 0, 15)
                    .setDefaultValue(event.defaultPattern())
                    .setTooltip(Component.translatable("mxhaptics.config.pattern.tooltip"))
                    .setSaveConsumer(v -> {
                        settings.pattern = v;
                        editedPattern[0] = v;
                    })
                    .build());

            category.addEntry(eb.startIntSlider(
                            Component.translatable("mxhaptics.config.intensity"),
                            settings.intensity, 0, 100)
                    .setDefaultValue(event.defaultIntensity())
                    .setTooltip(Component.translatable("mxhaptics.config.intensity.tooltip"))
                    .setSaveConsumer(v -> {
                        settings.intensity = v;
                        editedIntensity[0] = v;
                    })
                    .build());

            category.addEntry(eb.startIntField(
                            Component.translatable("mxhaptics.config.cooldownMs"),
                            settings.cooldownMs)
                    .setDefaultValue(event.defaultCooldownMs())
                    .setMin(0)
                    .setMax(60_000)
                    .setTooltip(Component.translatable("mxhaptics.config.cooldownMs.tooltip"))
                    .setSaveConsumer(v -> settings.cooldownMs = v)
                    .build());

            // Per-event test toggle. Uses the values just edited above (the
            // slider save consumers run before this one on the same Save click).
            category.addEntry(eb.startBooleanToggle(
                            Component.translatable("mxhaptics.config.test.eventButton"),
                            false)
                    .setDefaultValue(false)
                    .setYesNoTextSupplier(v -> Component.translatable(
                            "mxhaptics.config.test.eventButton.click"))
                    .setTooltip(Component.translatable(
                            "mxhaptics.config.test.eventButton.tooltip"))
                    .setSaveConsumer(v -> {
                        if (Boolean.TRUE.equals(v)) {
                            stageTest(editedPattern[0], editedIntensity[0]);
                        }
                    })
                    .build());
        }

        builder.setSavingRunnable(() -> {
            ConfigManager.save();
            // Bridge URL or TLS flag may have changed: reconnect.
            if (MxHapticsClient.bridge() != null) {
                MxHapticsClient.bridge().restart();
            }
            // Fire any staged test vibration LAST, so the reconnect above has
            // time to kick in. Use a tiny delay so a fresh WebSocket handshake
            // has room to complete before the trigger goes out.
            if (pendingTestPattern >= 0) {
                int p = pendingTestPattern;
                int i = pendingTestIntensity;
                pendingTestPattern = -1;
                pendingTestIntensity = -1;
                fireTestDeferred(p, i);
            }
        });

        return builder.build();
    }

    private static void stageTest(int pattern, int intensity) {
        pendingTestPattern = pattern;
        pendingTestIntensity = intensity;
    }

    /** Fire the test after a very short delay to let a reconnect finish. */
    private static void fireTestDeferred(int pattern, int intensity) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            HapticBridge bridge = MxHapticsClient.bridge();
            if (bridge != null && bridge.isConnected()) {
                bridge.trigger(pattern, intensity);
            }
        }, "mxhaptics-test");
        t.setDaemon(true);
        t.start();
    }
}
