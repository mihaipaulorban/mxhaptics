package dev.mxhaptics.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Integrazione con ModMenu. Questa classe viene caricata da Fabric Loader
 * SOLO se ModMenu è presente (entrypoint "modmenu" in fabric.mod.json),
 * quindi il riferimento compile-only a ModMenuApi è sicuro.
 *
 * La schermata vera richiede anche Cloth Config: se manca, il factory
 * restituisce null e ModMenu non apre nulla (l'utente può comunque
 * modificare config/mxhaptics.json a mano).
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return ClothConfigScreen::create;
        }
        return parent -> null;
    }
}
