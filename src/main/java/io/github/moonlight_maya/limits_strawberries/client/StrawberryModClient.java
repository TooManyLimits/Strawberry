package io.github.moonlight_maya.limits_strawberries.client;

import com.mojang.blaze3d.platform.InputUtil;
import io.github.moonlight_maya.limits_strawberries.StrawberryMod;
import io.github.moonlight_maya.limits_strawberries.client.screens.BerryJournalScreen;
import io.github.moonlight_maya.limits_strawberries.data.BerryMap;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBind;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.util.UUID;

public class StrawberryModClient implements ClientModInitializer {

	public static final EntityModelLayer MODEL_STRAWBERRY_LAYER = new EntityModelLayer(new Identifier(StrawberryMod.MODID, "strawberry"), "main");
	public static BerryMap CLIENT_BERRIES;

	public static KeyBind keyBinding;

	@Override
	public void onInitializeClient(ModContainer mod) {

		keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBind(
				"key." + StrawberryMod.MODID + ".journal",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_J,
				"category." + StrawberryMod.MODID + ".journal"
		));

		ClientTickEvents.END.register(client -> {
			while (keyBinding.wasPressed())
				if (client.currentScreen == null)
					client.setScreen(new BerryJournalScreen());
		});

		EntityRendererRegistry.register(StrawberryMod.STRAWBERRY, StrawberryEntityRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(MODEL_STRAWBERRY_LAYER, StrawberryEntityRenderer::getTexturedModelData);

		CLIENT_BERRIES = new BerryMap();

		ClientPlayNetworking.registerGlobalReceiver(StrawberryMod.S2C_ADD_PACKET_ID, (client, handler, buf, responseSender) -> {
			CLIENT_BERRIES.addBerryIfNeeded(buf.readUuid());
		});
		ClientPlayNetworking.registerGlobalReceiver(StrawberryMod.S2C_DELETE_PACKET_ID, (client, handler, buf, responseSender) -> {
			CLIENT_BERRIES.deleteBerry(buf.readUuid());
		});
		ClientPlayNetworking.registerGlobalReceiver(StrawberryMod.S2C_RESET_PACKET_ID, (client, handler, buf, responseSender) -> {
			CLIENT_BERRIES.resetPlayer(buf.readUuid());
		});
		ClientPlayNetworking.registerGlobalReceiver(StrawberryMod.S2C_UPDATE_PACKET_ID, (client, handler, buf, responseSender) -> {
			UUID berryUUID = buf.readUuid();
			byte flags = buf.readByte();
			String name = null, clue = null, desc = null, placer = null;
			if ((flags & 1) > 0)
				name = buf.readString();
			if ((flags & 2) > 0)
				clue = buf.readString();
			if ((flags & 4) > 0)
				desc = buf.readString();
			if ((flags & 8) > 0)
				placer = buf.readString();
			CLIENT_BERRIES.updateBerry(berryUUID, name, clue, desc, placer);
		});
		ClientPlayNetworking.registerGlobalReceiver(StrawberryMod.S2C_COLLECT_PACKET_ID, (client, handler, buf, responseSender) -> {
			CLIENT_BERRIES.collect(buf.readUuid(), buf.readUuid());
		});
		ClientPlayNetworking.registerGlobalReceiver(StrawberryMod.S2C_SYNC_PACKET_ID, (client, handler, buf, responseSender) -> {
			CLIENT_BERRIES.loadFrom(buf.readNbt());
		});

	}
}
