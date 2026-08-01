package com.example.autoanchor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;

/**
 * AutoAnchor
 * ----------
 * Normally, charging a Respawn Anchor with Glowstone takes 4 separate
 * right-clicks (one charge per click, max 4 charges). This mod detects
 * your first real right-click on a Respawn Anchor while holding
 * Glowstone, then automatically performs the remaining right-clicks for
 * you over the next few ticks until it's fully charged (or you run out
 * of Glowstone).
 *
 * Once fully charged, if DETONATE_AFTER_CHARGE is true, it automatically
 * switches your hotbar to DETONATE_SLOT (an empty/non-glowstone slot)
 * and right-clicks the anchor again. A fully-charged Respawn Anchor
 * used without Glowstone outside the Nether explodes - this is vanilla
 * game behaviour, not something this mod invents, just something it
 * triggers automatically.
 *
 * Press NUMPAD 3 to toggle the whole mod on/off.
 *
 * Written for Minecraft 26.2 (Fabric, unobfuscated official mappings).
 */
public class AutoAnchorClient implements ClientModInitializer {

    // Ticks to wait between each automatic extra charge.
    private static final int TICKS_BETWEEN_CHARGES = 2;

    // Ticks to wait after fully charging before triggering the detonation step.
    private static final int TICKS_BEFORE_DETONATE = 4;

    // Whether to auto-detonate once fully charged.
    private static final boolean DETONATE_AFTER_CHARGE = true;

    // Hotbar slot (0-indexed: 0 = slot "1" shown in-game) to switch to before
    // the detonating right-click. Make sure this slot does NOT contain Glowstone.
    private static final int DETONATE_SLOT = 0;

    private boolean enabled = true;
    private boolean lastToggleKeyState = false;

    // State for an in-progress auto-charge sequence.
    private BlockHitResult pendingHitResult = null;
    private InteractionHand pendingHand = null;
    private int remainingAutoCharges = 0;
    private int cooldown = 0;

    // State for the post-charge detonation step.
    private boolean awaitingDetonate = false;
    private int detonateCooldown = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!enabled) {
                return InteractionResult.PASS;
            }
            if (remainingAutoCharges > 0 || awaitingDetonate) {
                // Already mid-sequence, don't start another one.
                return InteractionResult.PASS;
            }

            BlockState state = world.getBlockState(hitResult.getBlockPos());
            if (!(state.getBlock() instanceof RespawnAnchorBlock)) {
                return InteractionResult.PASS;
            }
            if (!player.getItemInHand(hand).is(Items.GLOWSTONE)) {
                return InteractionResult.PASS;
            }

            int currentCharge = state.getValue(RespawnAnchorBlock.CHARGE);
            int maxCharge = 4;
            int chargesNeeded = maxCharge - currentCharge; // includes the click that's happening right now

            if (chargesNeeded <= 1) {
                // This single click will finish charging it - let it go through,
                // then queue up the detonation step directly.
                if (DETONATE_AFTER_CHARGE) {
                    pendingHitResult = hitResult;
                    pendingHand = hand;
                    awaitingDetonate = true;
                    detonateCooldown = TICKS_BEFORE_DETONATE;
                }
                return InteractionResult.PASS;
            }

            // Let this real click go through normally, then schedule the rest.
            remainingAutoCharges = chargesNeeded - 1;
            pendingHitResult = hitResult;
            pendingHand = hand;
            cooldown = TICKS_BETWEEN_CHARGES;

            return InteractionResult.PASS;
        });
    }

    private void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }

        handleToggleKey(client);

        if (!enabled) {
            remainingAutoCharges = 0;
            awaitingDetonate = false;
            return;
        }

        // Step 1: finish charging, if still in progress.
        if (remainingAutoCharges > 0 && pendingHitResult != null && pendingHand != null) {
            cooldown--;
            if (cooldown <= 0) {
                if (!client.player.getItemInHand(pendingHand).is(Items.GLOWSTONE)) {
                    // Ran out of glowstone, stop here.
                    remainingAutoCharges = 0;
                } else {
                    client.gameMode.useItemOn(client.player, pendingHand, pendingHitResult);
                    remainingAutoCharges--;
                    cooldown = TICKS_BETWEEN_CHARGES;

                    if (remainingAutoCharges <= 0 && DETONATE_AFTER_CHARGE) {
                        awaitingDetonate = true;
                        detonateCooldown = TICKS_BEFORE_DETONATE;
                    }
                }
            }
            return;
        }

        // Step 2: trigger the detonation once fully charged.
        if (awaitingDetonate && pendingHitResult != null) {
            detonateCooldown--;
            if (detonateCooldown > 0) {
                return;
            }

            // Switch hotbar to the designated (non-glowstone) slot.
            client.player.getInventory().setSelectedSlot(DETONATE_SLOT);
            client.player.connection.send(new ServerboundSetCarriedItemPacket(DETONATE_SLOT));

            // Right-click the anchor again - fully charged + no glowstone +
            // outside the Nether = vanilla explosion behaviour.
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, pendingHitResult);

            awaitingDetonate = false;
            pendingHitResult = null;
            pendingHand = null;
        }
    }

    private void handleToggleKey(Minecraft client) {
        long windowHandle = GLFW.glfwGetCurrentContext();
        boolean toggleKeyDown = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_KP_3) == GLFW.GLFW_PRESS;

        if (toggleKeyDown && !lastToggleKeyState) {
            enabled = !enabled;
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.literal("AutoAnchor: " + (enabled ? "ON" : "OFF"))
                );
            }
        }
        lastToggleKeyState = toggleKeyDown;
    }
}
