package com.max2341.fruitbin;


import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class InputEvents {
	@SubscribeEvent
	public static void onKeyPress(net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		if(KeyMappings.ezViewMapping.isPressed()) {
			if(FruitBin.showDebugMessages)
				Utils.quickChatMsg("Pressed ezview key", ChatFormatting.GREEN);
			Utils.openBestAuction();
		}
	}
}
