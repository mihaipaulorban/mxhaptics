package dev.mxhaptics.config;

import dev.mxhaptics.HapticEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configurazione completa della mod (serializzata in config/mxhaptics.json).
 */
public class HapticConfig {

    /** Current schema version. Bumped when defaults change so we can migrate. */
    public static final int CURRENT_VERSION = 3;

    /** Default endpoint of the HapticWeb plugin for Logi Options+. */
    public static final String DEFAULT_URL = "wss://local.jmw.nz:41443/ws";

    /** Schema version of this file. Older files (missing the field) load as 0
     *  and get migrated on load; sanitize() bumps this to CURRENT_VERSION before save. */
    public int configVersion = 0;

    /**
     * WebSocket endpoint exposed by the local plugin.
     * Change this if using a plugin other than HapticWeb or a different port.
     */
    public String bridgeUrl = DEFAULT_URL;

    /**
     * If true, the WebSocket client accepts any TLS certificate.
     * Only useful for local plugins with a self-signed cert.
     * Leave false for HapticWeb (which has a valid certificate).
     */
    public boolean trustAllCertificates = false;

    /** Impostazioni per evento, indicizzate con {@link HapticEvent#key()}. */
    public Map<String, EventSettings> events = new LinkedHashMap<>();

    /**
     * Restituisce le impostazioni per l'evento, creando i default
     * se mancanti (es. file di config vecchio o modificato a mano).
     */
    public EventSettings settings(HapticEvent event) {
        return events.computeIfAbsent(event.key(), k -> EventSettings.defaultsFor(event));
    }

    /** Ensures all events exist and all values are valid. Also bumps configVersion. */
    public void sanitize() {
        if (events == null) {
            events = new LinkedHashMap<>();
        }
        for (HapticEvent event : HapticEvent.values()) {
            settings(event).clamp();
        }
        if (bridgeUrl == null || bridgeUrl.isBlank()) {
            bridgeUrl = DEFAULT_URL;
        }
        // Always mark the file with the current schema version on save.
        configVersion = CURRENT_VERSION;
    }
}
