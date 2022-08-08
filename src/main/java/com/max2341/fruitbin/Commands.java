package com.max2341.fruitbin;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.max2341.fruitbin.Utils.Risk;
import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.config.Configuration;

public class Commands {
	public Commands() {
		ClientCommandHandler.instance.registerCommand(fruitbinCommand);
		ClientCommandHandler.instance.registerCommand(findAuctionFlipsCommand);
	}
	SimpleCommand.ProcessCommandRunnable getFlipsCommandRun = new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
        	if(Utils.isFindAuctionToBINFlipsRunning) {
        		sender.addChatMessage(new ChatComponentText(ChatFormatting.GREEN + "Already searching, on page " + Utils.findAuctionToBINFlipsPage));
        		return;
        	}
        	if(args.length == 0) {
    			ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    			executorService.schedule(getAuctionToBINFlips(120000), 1, TimeUnit.MILLISECONDS);
            } else if(args.length == 1){
            	ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    			executorService.schedule(getAuctionToBINFlips(Math.round(Float.parseFloat(args[0]) * 1000)), 1, TimeUnit.MILLISECONDS);
            } else {
            	sender.addChatMessage(new ChatComponentText(ChatFormatting.RED + "Invalid arguments! Try /flips <maxAuctionEndingTime> or /flips"));
        		return;
            }
        }
	};
		private static Runnable getAuctionToBINFlips(int maxEndingTimeMs) {
			return new Runnable(){
				public void run() {
					Utils.findAuctionFlips(FruitBin.itemLowestBins, maxEndingTimeMs);
				}
			};
		};
	SimpleCommand.ProcessCommandRunnable fruitbinCommandRun = new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
        	if(args.length == 0 || args[0].equalsIgnoreCase("help")) {
        		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA+"/flips, /flips <maxAuctionEndingTime> /ahf budget <long>, /ahf minprofit <int>, /ahf minprofit% <int>, /ahf delay <seconds>, /ahf maxrisk <low, medium, high, insane>, /ahf info, /ahf toggle, /ahf debug, /ahf what, /ahf ao, /ahf profile <snipe, full>, <budget>"));
                return;
            }
        	if(args[0].equalsIgnoreCase("budget")) {
        		FruitBin.budget = Utils.GetUnabbreviatedString(args[1]);
        		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "budget = " + Utils.GetUnabbreviatedString(args[1])));
            } else if(args[0].equalsIgnoreCase("minprofit")) {
            	FruitBin.minProfit = (int)Utils.GetUnabbreviatedString(args[1]);
            	sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "minProfit = " + Utils.GetUnabbreviatedString(args[1])));
            } else if(args[0].equalsIgnoreCase("delay")) {
            	try {
            		FruitBin.SleepSecondsBetweenScans = Float.parseFloat(args[1]);
            		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "delay = " + args[1]));
        		} catch (Exception e) {
        			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Can't parse delay: " + args[1]));
        		}
            } else if (args[0].equalsIgnoreCase("info")) {
            	sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "budget: " + FruitBin.budget));
            	sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "minProfit: " + FruitBin.minProfit));
            	sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "minProfit%: " + FruitBin.minProfitPercent));
            	sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "delay: " + FruitBin.SleepSecondsBetweenScans));
            	sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "maxRisk: " + FruitBin.maxRisk));
            	sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "on: " + FruitBin.on));
            	sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "ao: " + FruitBin.autoOpen));
            	sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "debug: " + FruitBin.showDebugMessages));
            } else if(args[0].equalsIgnoreCase("toggle")){
            	if(FruitBin.on) {
            		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Turned off FruitBin."));
            		FruitBin.on = false;
            	}
            	else {
            		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Turned on FruitBin."));
            		FruitBin.on = true;
            	}
            } else if (args[0].equalsIgnoreCase("debug")) {
            	if(FruitBin.showDebugMessages) {
            		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Turned off debug messages."));
            		FruitBin.showDebugMessages = false;
            	}
            	else {
            		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Turned on debug messages."));
            		FruitBin.showDebugMessages = true;
            	}
            } else if (args[0].equalsIgnoreCase("maxrisk")) {
            	boolean canParse = true;
            	if(args[1].equalsIgnoreCase("no") || args[1] == "0")
            		FruitBin.maxRisk = Risk.NO;
            	else if(args[1].equalsIgnoreCase("low") || args[1] == "1")
            		FruitBin.maxRisk = Risk.LOW;
            	else if(args[1].equalsIgnoreCase("medium") || args[1] == "2")
            		FruitBin.maxRisk = Risk.MEDIUM;
            	else if(args[1].equalsIgnoreCase("high") || args[1] == "3")
            		FruitBin.maxRisk = Risk.HIGH;
            	else {
            		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Can't parse risk: " + args[1]));
            		canParse = false;
            	}
            	if(canParse)
            		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "maxRisk = " + FruitBin.maxRisk.toString().toLowerCase()));
            } else if (args[0].equalsIgnoreCase("minprofit%")) {
            	try {
        		FruitBin.minProfitPercent = Integer.parseInt(args[1]);
        		sender.addChatMessage(new ChatComponentText(ChatFormatting.GREEN + "minProfit% = " + FruitBin.minProfitPercent));}
            	catch (Exception e) {
            		sender.addChatMessage(new ChatComponentText(ChatFormatting.RED + "Can't parse minProfit%: " + args[1]));
            	}
            } else if (args[0].equalsIgnoreCase("what")) {
            	Utils.quickChatMsg(FruitBin.whatAmIDoing, ChatFormatting.AQUA);
            } else if (args[0].equalsIgnoreCase("ao")) {
            	if(!FruitBin.autoOpen) {
            		sender.addChatMessage(new ChatComponentText(ChatFormatting.RED + "AO is ON."));
            		FruitBin.autoOpen = true;
            	}
            	else {
            		sender.addChatMessage(new ChatComponentText(ChatFormatting.GREEN + "AO is off."));
            		FruitBin.autoOpen = false;
            	}
            } else if (args[0].equalsIgnoreCase("profile")) {
            	
            	if(args[1].equalsIgnoreCase("snipe")) {
            		FruitBin.budget = Utils.GetUnabbreviatedString(args[2]);
            		FruitBin.autoOpen = true;
            		FruitBin.minProfit = 2500000;
            		FruitBin.minProfitPercent = 50;
            		FruitBin.maxRisk = Risk.MEDIUM;
            		FruitBin.on = true;
            	} else if (args[1].equalsIgnoreCase("full")) {
            		FruitBin.budget = Utils.GetUnabbreviatedString(args[2]);
            		FruitBin.autoOpen = false;
            		FruitBin.minProfit = 250000;
            		FruitBin.minProfitPercent = 5;
            		FruitBin.maxRisk = Risk.HIGH;
            		FruitBin.on = true;
            	} else {
                	sender.addChatMessage(new ChatComponentText(ChatFormatting.RED + "Invalid profile: " + args[1].toUpperCase() + "."));
            		return;
            	}
            	sender.addChatMessage(new ChatComponentText(ChatFormatting.GREEN + "Set profile to " + args[1].toUpperCase() + "."));
            }
        }
    };
    SimpleCommand findAuctionFlipsCommand = new SimpleCommand("flips", getFlipsCommandRun);
	SimpleCommand fruitbinCommand = new SimpleCommand("ahf", fruitbinCommandRun);
}
