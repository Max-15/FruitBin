package com.max2341.fruitbin;
import java.awt.Event;
import java.awt.event.KeyEvent;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class KeyMappings {
	public static KeyBinding ezViewMapping;
	public static void register() {
		ezViewMapping = create("EZView", KeyEvent.VK_CAPS_LOCK);
		
		ClientRegistry.registerKeyBinding(ezViewMapping);
	}
	static KeyBinding create(String name, int key) {
		return new KeyBinding("key." + Reference.MODID + "." + name, key, "key.category." + Reference.MODID);
	}
}
//KeyMapping#consumeClick()