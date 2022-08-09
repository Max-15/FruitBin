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
	public static final int auctionGracePeriodMillis = 15000;
	public static boolean isTesting = false;
	public static Risk maxRisk = Risk.HIGH;
	public static boolean showDebugMessages = true;
	public static boolean on = true;
	public static int minProfit = 2500000;
	public static int minProfitPercent = 90;
	public static float SleepSecondsBetweenScans = 0;
	public static float sleepSecondsBetweenApiUpdateChecks = 0.2f;
	public static long budget = Integer.MAX_VALUE;
	public static final String url = "https://api.hypixel.net/skyblock/auctions";
	public static String whatAmIDoing = "nothing";
	public static boolean autoOpen = false;
	public static HashMap<String, Float> itemLowestBins = null;

	@EventHandler
	public static void preInit(FMLPreInitializationEvent event) {
		Commands commands = new Commands();
	}

	@EventHandler
	public static void init(FMLInitializationEvent event) {
//		KeyMappings.register();
	}

	@EventHandler
	public static void postInit(FMLPostInitializationEvent event) {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.schedule(everything, 10, TimeUnit.SECONDS);
	}
	public static boolean skyblockCheck() {
		String s = Utils.getScoreBoardTitle();
		boolean isInSkyblock = Utils.removeAllCodes(s).toLowerCase().contains("skyblock");
		if(!isInSkyblock && !isTesting) {
			whatAmIDoing = "not in skyblock - detected";
		}
		return isInSkyblock && !isTesting;
	}
	static Runnable everything = new Runnable() {
		public void run() {
//			Utils.prevAuctions = Utils.initializeAuctions(url);
			try {
				itemLowestBins = Utils.initializeAuctions(url);
			} catch (Exception e) {
				whatAmIDoing = "exception in initializing: " + e.toString();
				if (showDebugMessages)
					Utils.quickChatMsg("Exception while initializing auctions: " + e.toString(), ChatFormatting.RED);
			}
			while (true) {
				try {
					whatAmIDoing = "nothing in while loop";
					if (on && Minecraft.getMinecraft().theWorld != null && skyblockCheck()) {
						whatAmIDoing = "scanning for flips";
						HashMap<String, Float> lowestBins = Utils.scan(url, budget, minProfit, itemLowestBins);
						if (lowestBins != null) {
							itemLowestBins = lowestBins;
							whatAmIDoing = "sleeping after finding lowest bins";
							Thread.sleep((int) (SleepSecondsBetweenScans * 1000));
						} else {
							whatAmIDoing = "sleeping after api check";
							Thread.sleep((int) (sleepSecondsBetweenApiUpdateChecks * 1000));
						}
						if (!skyblockCheck() || !on)
							continue;
					} else {
						whatAmIDoing = "not in skyblock or turned off, sleeping";
						Thread.sleep((2 * 1000));
					}
				} catch (Throwable e) {
					whatAmIDoing = "Exception in the forever loop: " + e.toString();
					if (showDebugMessages)
						Utils.quickChatMsg("Exception in the forever loop: " + e.toString(), ChatFormatting.RED);
					Utils.print(e.toString());
//					e.printStackTrace();
				}
			}
		}
	};
}
