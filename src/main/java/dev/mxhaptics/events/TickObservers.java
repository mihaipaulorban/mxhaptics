package dev.mxhaptics.events;

import dev.mxhaptics.HapticEvent;
import dev.mxhaptics.HapticManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Client-tick observer for events without a clean native hook.
 *
 * 1. Damage taken: the local player's health decreased vs the previous tick.
 *    Works in singleplayer and on any server (including vanilla) because it
 *    only reads client-synced state.
 *
 * 2. Air bubble popped: the number of air-bubble icons drawn on the HUD
 *    (ceil(air * 10 / maxAir)) decreased.
 *
 * 3. Shield block (melee and projectiles): while the player is blocking with
 *    a shield, the shield item's durability damage increased. Shields take
 *    damage for every hit they block — arrows included — and this reflects
 *    on the local inventory within a tick or two, so it's a very reliable
 *    signal even on vanilla servers. If a shield block is detected we skip
 *    the DAMAGE trigger for the same tick (partial blocks would otherwise
 *    fire both events at once).
 *
 * 4. Hotbar slot change: the selected slot changed (scroll or number keys).
 */
public final class TickObservers {

    private static UUID trackedPlayer;
    private static float lastHealth = -1.0f;
    private static int lastBubbles = -1;
    private static int lastSlot = -1;
    private static int lastShieldDamage = -1;

    private TickObservers() {
    }

    public static void onEndTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            reset();
            return;
        }

        // New world / new player / respawn: re-baseline without firing anything.
        if (trackedPlayer == null || !trackedPlayer.equals(player.getUUID())) {
            trackedPlayer = player.getUUID();
            lastHealth = player.getHealth();
            lastBubbles = bubbles(player);
            lastSlot = player.getInventory().getSelectedSlot();
            lastShieldDamage = -1;
            return;
        }

        // --- Shield block ------------------------------------------------
        // Check first so we can suppress DAMAGE if a shield block was
        // detected on the same tick (partial blocks still reduce health).
        boolean shieldedThisTick = false;
        if (player.isBlocking()) {
            ItemStack useItem = player.getUseItem();
            int damage = useItem.getDamageValue();
            if (lastShieldDamage >= 0 && damage > lastShieldDamage) {
                HapticManager.trigger(HapticEvent.SHIELD_BLOCK);
                shieldedThisTick = true;
            }
            lastShieldDamage = damage;
        } else {
            lastShieldDamage = -1;
        }

        // --- Damage taken ------------------------------------------------
        float health = player.getHealth();
        if (!shieldedThisTick
                && lastHealth >= 0.0f
                && health < lastHealth - 1.0e-4f
                && health > 0.0f) {
            HapticManager.trigger(HapticEvent.DAMAGE);
        }
        lastHealth = health;

        // --- Air bubbles -------------------------------------------------
        int bubbles = bubbles(player);
        if (lastBubbles >= 0 && bubbles < lastBubbles) {
            HapticManager.trigger(HapticEvent.AIR_BUBBLE);
        }
        lastBubbles = bubbles;

        // --- Hotbar slot change ------------------------------------------
        int slot = player.getInventory().getSelectedSlot();
        if (lastSlot >= 0 && slot != lastSlot) {
            HapticManager.trigger(HapticEvent.HOTBAR_CHANGE);
        }
        lastSlot = slot;
    }

    /** Air-bubble icons drawn on the HUD (0-10), same formula the game uses. */
    private static int bubbles(LocalPlayer player) {
        int max = Math.max(1, player.getMaxAirSupply());
        int air = Math.max(0, Math.min(max, player.getAirSupply()));
        return (int) Math.ceil(air * 10.0 / max);
    }

    private static void reset() {
        trackedPlayer = null;
        lastHealth = -1.0f;
        lastBubbles = -1;
        lastSlot = -1;
        lastShieldDamage = -1;
    }
}
