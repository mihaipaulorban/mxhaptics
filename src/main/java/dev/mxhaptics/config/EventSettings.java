package dev.mxhaptics.config;

import dev.mxhaptics.HapticEvent;

/**
 * Impostazioni configurabili per un singolo evento aptico.
 * Serializzata/deserializzata con Gson (campi pubblici).
 */
public class EventSettings {

    /** Se l'evento genera feedback aptico. */
    public boolean enabled = true;

    /** Indice del pattern di vibrazione da inviare al plugin (0-15). */
    public int pattern = 0;

    /** Intensità 0-100. Vedi README: viene inviata come secondo byte opzionale. */
    public int intensity = 50;

    /** Tempo minimo in millisecondi tra due trigger dello stesso evento. */
    public int cooldownMs = 100;

    public EventSettings() {
    }

    public static EventSettings defaultsFor(HapticEvent event) {
        EventSettings s = new EventSettings();
        s.enabled = event.defaultEnabled();
        s.pattern = event.defaultPattern();
        s.intensity = event.defaultIntensity();
        s.cooldownMs = event.defaultCooldownMs();
        return s;
    }

    /** Riporta i valori entro i limiti validi (dopo il load da JSON o dalla GUI). */
    public void clamp() {
        pattern = Math.max(0, Math.min(15, pattern));
        intensity = Math.max(0, Math.min(100, intensity));
        cooldownMs = Math.max(0, Math.min(60_000, cooldownMs));
    }
}
