package com.max2341.fruitbin;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.max2341.fruitbin.Utils.Risk;
import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Reference.MODID, name = Reference.NAME, version = Reference.VERSION)

public class FruitBin {
	public static boolean isTesting = false;
	public static Risk maxRisk = Risk.MEDIUM;
	public static boolean showDebugMessages = true;
	public static boolean on = true;
	public static int minProfit = 200000;
	public static int minProfitPercent = 5;
	public static float SleepSecondsBetweenScans = 0;
	public static float sleepSecondsBetweenApiUpdateChecks = 0.2f;
	public static long budget = Integer.MAX_VALUE;
	public static final String url = "https://api.hypixel.net/skyblock/auctions";
	public static Minecraft mc;
	@EventHandler
	public static void preInit(FMLPreInitializationEvent event) {
		Commands commands = new Commands();
	}
	@EventHandler
	public static void init(FMLInitializationEvent event) {
		mc = Minecraft.getMinecraft();
		KeyMappings.register();
	}
	@EventHandler
	public static void postInit(FMLPostInitializationEvent event) {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
			executor.schedule(scan, 15, TimeUnit.SECONDS);
	}
	
	static Runnable scan = new Runnable() {
		public void run() {
//			Utils.prevAuctions = Utils.initializeAuctions(url);
			HashMap<String, Float> itemLowestBins= null;
			try {
				itemLowestBins = Utils.initializeAuctions(url);
			} catch (IOException e1) {
				Utils.print(e1);
			}
			while(true) {
				try {
					if(on) {
						if(Minecraft.getMinecraft().theWorld != null && Minecraft.getMinecraft().isSingleplayer() == false || isTesting) {
						HashMap<String, Float> lowestBins = Utils.scan(url, budget, minProfit, itemLowestBins);
						if(lowestBins != null) {							
							itemLowestBins = lowestBins;
							Thread.sleep((int)(SleepSecondsBetweenScans * 1000));
						} else {
							Thread.sleep((int)(sleepSecondsBetweenApiUpdateChecks * 1000));
						}
						} else {
							Thread.sleep((2 * 1000));
						}
					}	
				} catch (Throwable e) {
					Utils.print(e.toString());
//					e.printStackTrace();
				}
			}
		}
	};
}
