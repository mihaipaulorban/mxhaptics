package dev.mxhaptics;

import dev.mxhaptics.bridge.HapticBridge;
import dev.mxhaptics.config.ConfigManager;
import dev.mxhaptics.config.EventSettings;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Punto unico di ingresso per i trigger: applica toggle, cooldown
 * e inoltra al bridge. Può essere chiamato da qualunque thread,
 * ma in pratica viene chiamato dal thread di gioco; l'invio vero e
 * proprio avviene sempre in modo asincrono nel bridge.
 */
public final class HapticManager {

    private static final Map<HapticEvent, Long> LAST_TRIGGER_NANOS =
            new ConcurrentHashMap<>(new EnumMap<>(HapticEvent.class));

    private static volatile HapticBridge bridge;

    private HapticManager() {
    }

    public static void init(HapticBridge hapticBridge) {
        bridge = hapticBridge;
    }

    /** Richiede un feedback aptico per l'evento, rispettando config e cooldown. */
    public static void trigger(HapticEvent event) {
        HapticBridge b = bridge;
        if (b == null || !b.isConnected()) {
            return;
        }
        EventSettings settings = ConfigManager.get().settings(event);
        if (!settings.enabled) {
            return;
        }
        long now = System.nanoTime();
        long cooldownNanos = settings.cooldownMs * 1_000_000L;
        Long last = LAST_TRIGGER_NANOS.get(event);
        if (last != null && (now - last) < cooldownNanos) {
            return;
        }
        LAST_TRIGGER_NANOS.put(event, now);
        b.trigger(settings.pattern, settings.intensity);
    }
}
