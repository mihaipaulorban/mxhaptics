package dev.mxhaptics;

/**
 * Gli eventi di gioco che possono generare un feedback aptico.
 * Ogni evento porta con sé i propri valori di default:
 * abilitato, pattern (0-15), intensità (0-100), cooldown minimo in ms.
 */
public enum HapticEvent {

    /** Damage taken by the player. */
    DAMAGE("damage", true, 1, 100, 80),

    /** Block broken by the player. Disabled by default. */
    BLOCK_BREAK("blockBreak", false, 0, 75, 80),

    /** An air bubble popped (air supply icon decreased). */
    AIR_BUBBLE("airBubble", true, 1, 75, 200),

    /** Block placed by the player. Disabled by default. */
    BLOCK_PLACE("blockPlace", false, 0, 75, 80),

    /** Shield-blocked hit (melee or projectile). */
    SHIELD_BLOCK("shieldBlock", true, 1, 50, 80),

    /** Selected hotbar slot changed. Disabled by default. */
    HOTBAR_CHANGE("hotbarChange", false, 0, 5, 20);

    private final String key;
    private final boolean defaultEnabled;
    private final int defaultPattern;
    private final int defaultIntensity;
    private final int defaultCooldownMs;

    HapticEvent(String key, boolean defaultEnabled, int defaultPattern,
                int defaultIntensity, int defaultCooldownMs) {
        this.key = key;
        this.defaultEnabled = defaultEnabled;
        this.defaultPattern = defaultPattern;
        this.defaultIntensity = defaultIntensity;
        this.defaultCooldownMs = defaultCooldownMs;
    }

    /** Chiave usata nel file di configurazione JSON e nelle traduzioni. */
    public String key() {
        return key;
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    public int defaultPattern() {
        return defaultPattern;
    }

    public int defaultIntensity() {
        return defaultIntensity;
    }

    public int defaultCooldownMs() {
        return defaultCooldownMs;
    }
}
