package dev.mxhaptics.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.mxhaptics.MxHapticsClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Carica e salva config/mxhaptics.json.
 * Gson è già presente nel classpath di Minecraft: nessuna dipendenza extra.
 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("mxhaptics.json");

    private static HapticConfig config = new HapticConfig();

    private ConfigManager() {
    }

    public static HapticConfig get() {
        return config;
    }

    public static synchronized void load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                HapticConfig loaded = GSON.fromJson(reader, HapticConfig.class);
                if (loaded != null) {
                    if (loaded.configVersion >= HapticConfig.CURRENT_VERSION) {
                        config = loaded;
                    } else {
                        // Older schema: reset per-event settings to new defaults
                        // but preserve the user's bridge URL / TLS choice.
                        HapticConfig migrated = new HapticConfig();
                        if (loaded.bridgeUrl != null && !loaded.bridgeUrl.isBlank()) {
                            migrated.bridgeUrl = loaded.bridgeUrl;
                        }
                        migrated.trustAllCertificates = loaded.trustAllCertificates;
                        config = migrated;
                        MxHapticsClient.LOGGER.info(
                                "[mxhaptics] config schema updated to v{}, "
                                        + "per-event settings reset to new defaults",
                                HapticConfig.CURRENT_VERSION);
                    }
                }
            } catch (IOException | RuntimeException e) {
                MxHapticsClient.LOGGER.warn(
                        "[mxhaptics] failed to read mxhaptics.json, using defaults", e);
                config = new HapticConfig();
            }
        }
        config.sanitize();
        // Write the file back so users without Cloth Config can see and
        // edit every field manually.
        save();
    }

    public static synchronized void save() {
        config.sanitize();
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            MxHapticsClient.LOGGER.warn("[mxhaptics] failed to save mxhaptics.json", e);
        }
    }
}
