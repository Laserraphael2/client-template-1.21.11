package mod.client;

import mod.client.shield.ShieldPatternData;
import mod.client.shield.ShieldPatternPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client implements ModInitializer {
	public static final String MOD_ID = "client";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(ShieldPatternPayload.TYPE, ShieldPatternPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ShieldPatternPayload.TYPE, (payload, context) -> {
			if (!ShieldPatternData.isValidPattern(payload.pattern())) {
				return;
			}
			context.server().execute(() -> {
				InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
				ItemStack stack = context.player().getItemInHand(hand);
				if (stack.is(Items.SHIELD)) {
					ShieldPatternData.write(stack, payload.pattern(), payload.color());
					context.player().getInventory().setChanged();
					context.player().containerMenu.broadcastChanges();
				}
			});
		});
		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
