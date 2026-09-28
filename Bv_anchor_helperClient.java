package com.bvents.bv_anchor_helper.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * BV Anchor Helper 1.03 (Minecraft 1.21.11).
 *
 * 0 зарядов у якоря под прицелом -> слот со светокамнем.
 * 1+ зарядов у якоря под прицелом -> слот с тотемом бессмертия (если он в хотбаре).
 * Прицел ушёл с якоря -> возвращается прежний слот.
 */
public class Bv_anchor_helperClient implements ClientModInitializer {

    private static final int HOTBAR_SIZE = 9;

    private static int previousSelectedSlot = -1;
    private static int targetSlot = -1;
    private static boolean autoSwapped = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(Bv_anchor_helperClient::onEndTick);
    }

    public static void onSuccessfulBlockUse(ItemStack stack, Hand hand) {
        if (hand != Hand.MAIN_HAND || stack.isEmpty() || !stack.isOf(Items.RESPAWN_ANCHOR)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        int slot = findHotbarSlot(player, Items.GLOWSTONE);
        if (slot < 0) {
            return;
        }

        targetSlot = slot;
        if (!autoSwapped) {
            previousSelectedSlot = player.getInventory().getSelectedSlot();
            autoSwapped = true;
        }
        setSelectedSlot(client, player, targetSlot);
    }

    private static void onEndTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            resetState();
            return;
        }

        int charge = getLookedAnchorCharge(client);

        int slot = -1;
        if (charge == 0) {
            slot = findHotbarSlot(player, Items.GLOWSTONE);
        } else if (charge >= 1) {
            slot = findHotbarSlot(player, Items.TOTEM_OF_UNDYING);
        }

        if (slot >= 0) {
            targetSlot = slot;
            if (!autoSwapped) {
                previousSelectedSlot = player.getInventory().getSelectedSlot();
                autoSwapped = true;
            }
            setSelectedSlot(client, player, targetSlot);
            return;
        }

        if (autoSwapped) {
            restorePreviousSlot(client, player);
        }
        resetState();
    }

    /** Заряд якоря под прицелом (0..4) или -1, если прицел не на якоре. */
    private static int getLookedAnchorCharge(MinecraftClient client) {
        if (client.crosshairTarget instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = client.world.getBlockState(blockHit.getBlockPos());
            if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
                return state.get(RespawnAnchorBlock.CHARGES);
            }
        }
        return -1;
    }

    private static int findHotbarSlot(ClientPlayerEntity player, Item item) {
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private static void restorePreviousSlot(MinecraftClient client, ClientPlayerEntity player) {
        if (previousSelectedSlot < 0 || previousSelectedSlot >= HOTBAR_SIZE) {
            return;
        }
        setSelectedSlot(client, player, previousSelectedSlot);
    }

    private static void setSelectedSlot(MinecraftClient client, ClientPlayerEntity player, int slot) {
        if (slot < 0 || slot >= HOTBAR_SIZE) {
            return;
        }
        if (player.getInventory().getSelectedSlot() == slot) {
            return;
        }
        player.getInventory().setSelectedSlot(slot);
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private static void resetState() {
        previousSelectedSlot = -1;
        targetSlot = -1;
        autoSwapped = false;
    }
}
