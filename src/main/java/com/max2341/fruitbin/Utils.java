package com.max2341.fruitbin;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.mojang.realmsclient.gui.ChatFormatting;

import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.scoreboard.GoalColor;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import scala.tools.nsc.doc.model.Public;

public class Utils {
	//<auctionBidIncrements> Key: auction highest bids equal to or above this amount will have <Value>% bid increments. 
	public static HashMap<Long, Float>auctionBidIncrements = new HashMap<Long, Float>(){{
	    put(10000000l, 2.5f);
	    put(1000000l, 5f);
	    put(100000l, 10f);
	    put(0l, 15f);
	}};
	public static boolean isFindAuctionToBINFlipsRunning = false;
	public static int findAuctionToBINFlipsPage = 0;
	public static Set<Character> numbers = new HashSet<Character>(Arrays.asList(new Character[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}));
	public static Set<Character> letters = new HashSet<Character>(Arrays.asList(new Character[] {' ', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'}));
	public static HashMap<String, Integer> itemAmounts = new HashMap<String, Integer>();
	public static long lastResponseTimestamp;
	static Pair<String, Long> bestAuctionIDAndProfit = new Pair<String, Long>("404", 0l);
	public enum Risk {NO, LOW, MEDIUM, HIGH}
	public static final float auctionListTax = 0.01f;
	public static final float auctionCollectTax = 0.01f;
	public static boolean DEV_DEBUG = true;
	public static long lastBinAuctionTimestamp = 0;
	
	private static Runnable sendFlipRunnable (IChatComponent component, final String id) {
		return new Runnable(){
			public void run() {
				Minecraft.getMinecraft().thePlayer.addChatMessage(component);
				if(FruitBin.autoOpen) {
					EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
					final GuiScreen guiScreen = Minecraft.getMinecraft().currentScreen;
					if(player != null && (guiScreen == null || guiScreen instanceof GuiChat))
						player.sendChatMessage("/viewauction " + id);
				}
			}
		};
	}
	
	public static void print(Object msg) {
		if(msg != null) {
			if (DEV_DEBUG)
				System.out.println(msg);
		}
		else  System.out.println("null");
	}
	
	public static void sendFlip(AuctionInfo auctionInfo, boolean isAuction) {
		EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
		if(player == null) {
			print("sendFlip: MC player is null");
			return;
		}
		ChatFormatting itemColor = Utils.getColorByRarity(auctionInfo.auction.tier);
		ChatFormatting riskColor = auctionInfo.risk == Risk.NO ? ChatFormatting.AQUA : auctionInfo.risk == Risk.LOW ? ChatFormatting.GREEN : auctionInfo.risk == Risk.MEDIUM ? ChatFormatting.GRAY : ChatFormatting.GRAY;
		if(!isAuction) {
			boolean gracePeriodOver = System.currentTimeMillis() >= auctionInfo.auction.start + FruitBin.auctionGracePeriodMillis;
			String timestampText = ChatFormatting.DARK_GRAY + (Math.round((float)(System.currentTimeMillis() - auctionInfo.auction.start)/1000f * 100)/100 + "s ago ");
			if(!gracePeriodOver)
				timestampText = "§6§lSNIPE! ";
			IChatComponent comp = new ChatComponentText(
					timestampText + itemColor + auctionInfo.auction.item_name + " " + ChatFormatting.WHITE + Utils.GetAbbreviatedFloat(auctionInfo.price) + " -> " + Utils.GetAbbreviatedFloat(auctionInfo.lowestBin) + ChatFormatting.GOLD + 
					" [" + String.format("%,d", auctionInfo.profit) + " coins]" + (auctionInfo.profitPercent >= 50 ? ChatFormatting.AQUA : ChatFormatting.GRAY) +" [" + auctionInfo.profitPercent + "%] " 
					+ riskColor + auctionInfo.amountListed + " listed (" + auctionInfo.risk.toString().toLowerCase() + " risk)");
			ChatStyle style = Utils.createClickStyle(ClickEvent.Action.RUN_COMMAND, "/viewauction " + auctionInfo.auction.uuid);
			comp.setChatStyle(style);
			if(gracePeriodOver){
				player.addChatMessage(comp);
				
				if(FruitBin.autoOpen) {
					final GuiScreen guiScreen = Minecraft.getMinecraft().currentScreen;
					if(player != null && (guiScreen == null || guiScreen instanceof GuiChat))
						player.sendChatMessage("/viewauction " + auctionInfo.auction.uuid);
				}
				
			} else {
				ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
				executorService.schedule(sendFlipRunnable(comp, auctionInfo.auction.uuid), (auctionInfo.auction.start + FruitBin.auctionGracePeriodMillis) - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			}
			print("FOUND FLIP! " + auctionInfo.auction.item_name + " " + auctionInfo.price + " -> " + auctionInfo.lowestBin);
		} else {
			float bidUnder = auctionInfo.lowestBin * (1 - (auctionInfo.lowestBin >= 1000000 ? auctionCollectTax : 0));
			String timestampText = ChatFormatting.DARK_GRAY + ("Ends in " + ChatFormatting.RESET + Math.round((float)(auctionInfo.auction.end - System.currentTimeMillis())/100f) + "s ");
			IChatComponent comp = new ChatComponentText(timestampText + itemColor + auctionInfo.auction.item_name + " " +
					ChatFormatting.RESET + GetAbbreviatedFloat(auctionInfo.price) + " -> " + Utils.GetAbbreviatedFloat(auctionInfo.lowestBin) + 
					ChatFormatting.DARK_GRAY + " Bid under " + ChatFormatting.YELLOW + Utils.GetAbbreviatedFloat(bidUnder) + ChatFormatting.DARK_GRAY + " to make profit");
			
			ChatStyle style = Utils.createClickStyle(ClickEvent.Action.RUN_COMMAND, "/viewauction " + auctionInfo.auction.uuid);
			comp.setChatStyle(style);
			player.addChatMessage(comp);
		}
	}
	//Minecraft.getMinecraft().thePlayer.currentScreen == null
	public static long GetUnabbreviatedString(String string) {
		float number;
		long multiplier;
		char[] charArray = string.toLowerCase().toCharArray();
		char lastChar = charArray[string.length() - 1];
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < string.length() - 1; i++) {
			sb.append(charArray[i]);
		}
		if(numbers.contains(lastChar))
			sb.append(lastChar);
		number = Float.parseFloat(sb.toString());
		multiplier = (lastChar == 'k') ? 1000 : (lastChar == 'm') ? 1000000 : (lastChar == 'b') ? 1000000000 : (lastChar == 't') ? 1000000000000l : 1;
		
		return Math.round((number) * multiplier);
	}
	public static String GetAbbreviatedFloat(float number) {
		DecimalFormat df = new DecimalFormat("0.#");
		if(number >= 1000000000) 
			return df.format(Math.round(number / 1000000f)/1000f) + "B";	
		else if(number >= 1000000) 
			return df.format(Math.round(number / 1000f)/1000f) + "M";
		else if(number >= 1000) 
			return df.format(Math.round(number / 100f)/10f) + "K";
		else
			return df.format(Math.round(number * 10f)/10f) + "";
	}
	public static ChatFormatting getColorByRarity(String rarity) {
		if(rarity.equalsIgnoreCase("LEGENDARY"))
			return ChatFormatting.GOLD;
		else if (rarity.equalsIgnoreCase("EPIC"))
			return ChatFormatting.DARK_PURPLE;
		else if (rarity.equalsIgnoreCase("RARE"))
			return ChatFormatting.BLUE;
		else if (rarity.equalsIgnoreCase("UNCOMMON"))
			return ChatFormatting.GREEN;
		else if (rarity.equalsIgnoreCase("COMMON"))
			return ChatFormatting.GRAY;
		else if (rarity.equalsIgnoreCase("MYTHIC"))
			return ChatFormatting.LIGHT_PURPLE;
		else if (rarity.equalsIgnoreCase("DIVINE"))
			return ChatFormatting.AQUA;
		else {
			return ChatFormatting.RED;
		}
	}
	public static ChatComponentText getChatMessage(String message) {
		return new ChatComponentText(ChatFormatting.DARK_GRAY + "FruitBin: " + ChatFormatting.RESET + "" + message);
	}
	public static ChatComponentText getChatMessage(String message, boolean reset) {
		return new ChatComponentText(ChatFormatting.DARK_GRAY + "FruitBin: " + (reset ? ChatFormatting.RESET : "") + "" + message);
	}
	public static String getHTML(String urlToRead) throws IOException {
	      URL url = new URL(urlToRead);
	      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	      conn.setRequestMethod("GET");
	      int status = conn.getResponseCode();	
	      
	      Reader streamReader = null;

	      if (status > 299) {
	          streamReader = new InputStreamReader(conn.getErrorStream());
	      } else {
	          streamReader = new InputStreamReader(conn.getInputStream());
	      }
	      BufferedReader in = new BufferedReader( new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuffer content = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
			    content.append(inputLine);
			}
			in.close();
		    return content.toString();
		 }
	static String getFlipSound() {
		return Reference.MODID + ":alerts.flipalert";
	}
	public static HashMap<String, Float> initializeAuctions(String url) throws Exception {
		
		int totalPages = 1;
		HashMap<String, Float> itemLowestBins = new HashMap<String, Float>();
		HashMap<String, Integer>newItemAmounts = new HashMap<String, Integer>();
		lastResponseTimestamp = 0;
			
		try {
			for(int page = 0; page < totalPages; page++) {
				String json = null;
				FruitBin.whatAmIDoing = "initializing auctions, page " + page;
				json = Utils.getHTML(url + "?page=" + page);
				Gson gson = new Gson();
				Auctions auctions = gson.fromJson(json, Auctions.class);
			
				totalPages = auctions.totalPages;
				for(AuctionItem auction : auctions.auctions) {
					NBTCompound extraInfo = NBTReader.readBase64(auction.item_bytes);
					String myID = getMyID(extraInfo);
					if(auction.bin) {
						lastBinAuctionTimestamp = Math.max(auction.start, lastBinAuctionTimestamp);
						
						if(!itemLowestBins.containsKey(myID) || auction.starting_bid < itemLowestBins.get(myID))
							itemLowestBins.put(myID, auction.starting_bid);
						
						Integer itemsSoFar = newItemAmounts.get(myID);
						int newValue = (itemsSoFar != null ? itemsSoFar : 0) + 1; 
						newItemAmounts.put(myID, newValue);
					}
				}
				if(FruitBin.showDebugMessages)
					quickChatMsg("Finished page " + page + " of initializing auctions.", ChatFormatting.GREEN);
			}
			itemAmounts = newItemAmounts;
			if(FruitBin.showDebugMessages)
				quickChatMsg("Initialized Auctions", ChatFormatting.GREEN);
		} catch (Throwable t) {
			FruitBin.whatAmIDoing = "exception in initializing: " + t.toString();
			if(FruitBin.showDebugMessages)
				quickChatMsg("Exception while initializing auctions: " + t.toString(), ChatFormatting.RED);
			print(t);

		}
		return itemLowestBins;
	}
	public static float getProfit(float buyPrice, float sellPrice) {
		sellPrice = sellPrice * (1 - auctionListTax - ((sellPrice >= 1000000) ? auctionCollectTax : 0)) - 100;
		return sellPrice - buyPrice;
	}
	public static float getProfitPercent(float buyPrice, float sellPrice) {
		sellPrice = sellPrice * (1 - auctionListTax - ((sellPrice >= 1000000) ? auctionCollectTax : 0)) - 100;
		return ((sellPrice / buyPrice) - 1) * 100;
	}
	public static float getBidIncrement(float highestBid) {
		//returns bid increment as multiplier(1.025, 1.15) format
		float bidIncrementPercent = 0;
		for(long minAuctionHighestBid : auctionBidIncrements.keySet()) {
			if(highestBid >= minAuctionHighestBid) {
				bidIncrementPercent = Math.max(auctionBidIncrements.get(minAuctionHighestBid), bidIncrementPercent);
			}
		}
		return 1 + (bidIncrementPercent / 100f);
	}
	public static void findAuctionFlips(HashMap<String, Float> itemLowestBins, int maxEndingTimeMs){
		if(itemLowestBins == null) {
			quickChatMsg("FruitBin does not know lowest BIN prices, wait until it initializes.", ChatFormatting.RED);
			return;
		}
		isFindAuctionToBINFlipsRunning = true;
		long startTime = System.currentTimeMillis();
		List<AuctionInfo> result = new ArrayList<AuctionInfo>();
		HashMap<String, Integer> binAmounts = new HashMap<String, Integer>();
		HashMap<String, Pair<AuctionItem, AuctionItem>>itemLowestTwoBins = new HashMap<String, Pair<AuctionItem, AuctionItem>>();
		int auctionsChecked = 0;
		Gson gson = new Gson();

		int totalPages = 1;
		int totalFlips = 0;
		try {
			for(int page = 0; page < totalPages; page++) {
				findAuctionToBINFlipsPage = page;
				String newjson = Utils.getHTML(FruitBin.url + "?page=" + page);
				Auctions auctions = gson.fromJson(newjson, Auctions.class);
					
				totalPages = auctions.totalPages;
					
				if(page == 0) {
					if(FruitBin.showDebugMessages)
						quickChatMsg("Started searching for flips (This can take a while)", ChatFormatting.DARK_GREEN);
				}				
				for (AuctionItem auction : auctions.auctions) {
					String myID = getMyID(NBTReader.readBase64(auction.item_bytes));
					auctionsChecked++;
					if(auction.bin) {
						//logic to find two lowest bins
						
						Integer previousAmount = binAmounts.get(myID);
						binAmounts.put(myID, previousAmount == null ? 1 : previousAmount + 1);
						
						Float price = auction.starting_bid;
						Pair<AuctionItem, AuctionItem> pair = itemLowestTwoBins.get(myID);
						Float keyPrice = 0f;
						Float valuePrice = 0f;
						if(pair != null) {
							AuctionItem key = pair.key;
							
							if(key == null) 
								keyPrice = Float.POSITIVE_INFINITY;
							else 
								keyPrice = key.starting_bid;
							AuctionItem value = itemLowestTwoBins.get(myID).value;
							
							if(value == null)
								valuePrice = Float.POSITIVE_INFINITY;
							else 
								valuePrice = value.starting_bid;
							if(price < keyPrice || price < valuePrice) {
								if(keyPrice >= valuePrice)
									itemLowestTwoBins.put(myID, new Pair<AuctionItem, AuctionItem>(auction, value));
								else 
									itemLowestTwoBins.put(myID, new Pair<AuctionItem, AuctionItem>(key, auction));
							}

						} else {
							keyPrice = Float.POSITIVE_INFINITY;
							valuePrice = Float.POSITIVE_INFINITY;
							if(price < keyPrice || price < valuePrice) {
								if(keyPrice >= valuePrice)
									itemLowestTwoBins.put(myID, new Pair<AuctionItem, AuctionItem>(auction, null));
								else 
									itemLowestTwoBins.put(myID, new Pair<AuctionItem, AuctionItem>(null, auction));
							}

						}
					}
					float itemPrice = Math.max(auction.starting_bid, auction.highest_bid_amount);
					if(auction.highest_bid_amount != 0)/*means there are bids on item*/ {
						itemPrice = itemPrice * getBidIncrement(auction.highest_bid_amount);
					}
						
					if (itemPrice <= FruitBin.budget && !auction.claimed && (auction.end - System.currentTimeMillis() < maxEndingTimeMs)) {

						float lowestBin;
						if(itemLowestBins.containsKey(myID))
							lowestBin = itemLowestBins.get(myID);
						else 
							lowestBin = Float.MIN_VALUE;
							
						int profit = (int)getProfit(itemPrice, lowestBin);
							
						float profitPercent = getProfitPercent(itemPrice, lowestBin);
						if (profit >= FruitBin.minProfit && profitPercent >= FruitBin.minProfitPercent && !auction.item_lore.contains("§8Furniture")) {
						final int amount = itemAmounts.containsKey(myID) ? itemAmounts.get(myID) : 0;
						final Risk risk = getRiskFromAmount(amount);
							if (risk.ordinal() <= FruitBin.maxRisk.ordinal()) {
								AuctionInfo info = new AuctionInfo(auction, profit, Math.round(profitPercent), itemPrice, itemLowestBins.get(myID), amount, risk);
								totalFlips++;
								sendFlip(info, true);
							}
						}
					}
				}
			}
			for(String id : binAmounts.keySet()) {
				if(itemLowestTwoBins.get(id).key == null || itemLowestTwoBins.get(id).value == null)
					continue;
				AuctionItem lowerAuction = itemLowestTwoBins.get(id).getLowerFloatBINAuction();
				AuctionItem higherAuction = itemLowestTwoBins.get(id).getHigherFloatBINAuction();
				long profit = Math.round(getProfit(lowerAuction.starting_bid, higherAuction.starting_bid));
				float profitPercent = getProfitPercent(lowerAuction.starting_bid, higherAuction.starting_bid);
				final int amount = binAmounts.get(id);
				final Risk risk = getRiskFromAmount(amount);
				if(profit >= FruitBin.minProfit && profitPercent >= FruitBin.minProfitPercent && risk.ordinal() <= FruitBin.maxRisk.ordinal()) {
					AuctionInfo info = new AuctionInfo(lowerAuction, profit, Math.round(profitPercent), lowerAuction.starting_bid, higherAuction.starting_bid, amount, risk);
					sendFlip(info, false);
				}
			}
			
			if(FruitBin.showDebugMessages) {
				quickChatMsg("Searched through " + auctionsChecked + " auctions in " 
						+ (float)(System.currentTimeMillis() - startTime) / 1000f + "s and found " + totalFlips + " flips", ChatFormatting.DARK_GREEN);
			}
			isFindAuctionToBINFlipsRunning = false;
		} catch (Exception e) {
			isFindAuctionToBINFlipsRunning = false;
			if(FruitBin.showDebugMessages)
				quickChatMsg("EXCEPTION: " + e, ChatFormatting.RED);
			print("EXCEPTION: " + e);
			e.printStackTrace();
		}
	}
	public static HashMap<String, Float> scan(String url, long budget, int minProfit, HashMap<String, Float> itemLowestBins) {
		List<AuctionInfo> result = new ArrayList<AuctionInfo>();
		long newLastUpdated = 0;
		int auctionsChecked = 0;
		long startTime = System.currentTimeMillis();
//		for(AuctionItem auction : prevAuctions) {
//			
//		}
		
		try {
			Gson gson = new Gson();

			int totalPages = 1;
			int totalFlips = 0;
			long maxBinAuctionTimestamp = lastBinAuctionTimestamp;
			try {
				pageloop:
				for(int page = 0; page < totalPages; page++) {
					String newjson = Utils.getHTML(url + "?page=" + page);
					Auctions newAuctions = gson.fromJson(newjson, Auctions.class);
					
					totalPages = newAuctions.totalPages;
					
					if(page == 0) {
						newLastUpdated = newAuctions.lastUpdated;
						if(newAuctions.lastUpdated == lastResponseTimestamp) {
							return null;
						}
						if(FruitBin.showDebugMessages)
							quickChatMsg("Started Filtering " + (System.currentTimeMillis() - newAuctions.lastUpdated + "ms late"), ChatFormatting.GREEN);
					}				
					for (AuctionItem auction : newAuctions.auctions) {
						auctionsChecked++;
						if(auction.bin) {
							maxBinAuctionTimestamp = Math.max(auction.start, maxBinAuctionTimestamp);
							if(auction.start < lastBinAuctionTimestamp) {
								break pageloop;
							}
						}
						NBTCompound extraInfo = NBTReader.readBase64(auction.item_bytes);
						if (auction.bin && auction.starting_bid <= budget && !auction.claimed) {
							//HashMap<String, Integer> enchantments = (HashMap<String, Integer>) extraInfo.get("enchantments");
							
															
							String myID = getMyID(extraInfo);
							
							float minPrice;
							if(itemLowestBins.containsKey(myID))
								minPrice = itemLowestBins.get(myID);
							else {
								itemLowestBins.put(myID, auction.starting_bid);
								minPrice = auction.starting_bid;
							}
							
							int profit = (int)getProfit(auction.starting_bid, minPrice);
							
							float profitPercent = getProfitPercent(auction.starting_bid, minPrice);
							if (profit >= minProfit && profitPercent >= FruitBin.minProfitPercent && !auction.item_lore.contains("§8Furniture")) {
								final int amount = itemAmounts.containsKey(myID) ? itemAmounts.get(myID) : 0;
								final Risk risk = getRiskFromAmount(amount);
								if (risk.ordinal() <= FruitBin.maxRisk.ordinal()) {
									AuctionInfo info = new AuctionInfo(auction, profit, Math.round(profitPercent), auction.starting_bid, itemLowestBins.get(myID), amount, risk);
									if(info.profit > bestAuctionIDAndProfit.value) {
										bestAuctionIDAndProfit.key = auction.uuid;
										bestAuctionIDAndProfit.value = info.profit;
									}
	//								if(totalFlips < 3) {
	//									ISound sound = new PositionedSound(new ResourceLocation(getFlipSound())){};
	//									Minecraft.getMinecraft().getSoundHandler().playSound(sound);
	//								}
									totalFlips++;
									sendFlip(info, false);
								}
							}
						}
						//LOWESTBIN & ITEMAMOUNTS
						String myID = getMyID(extraInfo);
						if(auction.bin) {
							if(!itemLowestBins.containsKey(myID) || auction.starting_bid < itemLowestBins.get(myID))
								itemLowestBins.put(myID, auction.starting_bid);
							
							if(itemAmounts.containsKey(myID)) {
								int newValue = itemAmounts.get(myID) + 1;
								itemAmounts.put(myID, newValue);
							}
							else
								itemAmounts.put(myID, 1);
						}
					}
				}
			}
			finally {
				lastBinAuctionTimestamp = maxBinAuctionTimestamp;
			}
			if(FruitBin.showDebugMessages) {
				quickChatMsg("Searched through " + auctionsChecked + " auctions in " 
						+ (System.currentTimeMillis() - startTime) / 1000f + "s and found " + totalFlips + " flips", ChatFormatting.GREEN);
			}
			lastResponseTimestamp = newLastUpdated;
		} catch (Exception e) {
			if(FruitBin.showDebugMessages)
				quickChatMsg("EXCEPTION: " + e, ChatFormatting.RED);
			print("EXCEPTION: " + e);
			e.printStackTrace();
		}
		return itemLowestBins;
	}
	public static Risk getRiskFromAmount(int amount) {
		return
			amount < 10   ? Risk.HIGH :
			amount < 20   ? Risk.MEDIUM :
			amount < 100  ? Risk.LOW :
					        Risk.NO;
	}
	public static void openBestAuction() {
		EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
		if(player != null)
			player.sendChatMessage("/viewauction " + bestAuctionIDAndProfit.getKey());
	}
	public static void quickChatMsg(String message, ChatFormatting color) {
		EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
		if(player != null)
			player.addChatMessage(getChatMessage(color + message, false));
		else {
			print("quickchat: mc player is null");
		}
	}
	
	public static String getMyID(NBTCompound bytes) {
		NBTCompound attributes = bytes.getList("i").getCompound(0).getCompound("tag").getCompound("ExtraAttributes");
		String id = attributes.getString("id");
		return id;
	}
	
    public static ChatStyle createClickStyle(ClickEvent.Action action, String value) {
        ChatStyle style = new ChatStyle();
        style.setChatClickEvent(new ClickEvent(action, value));
        style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.GOLD+value)));
        return style;
    }
}


//public static AuctionInfo[] filter(AuctionItem[] auctions, int minProfit) {
//HashMap<String, Float>itemLowestBins = new HashMap<String, Float>();
//HashMap<String, Integer>itemAmounts = new HashMap<String, Integer>();
//List<AuctionInfo> result = new ArrayList<AuctionInfo>();
//List<AuctionItem> newAuctionItems = new ArrayList<AuctionItem>();
//for(AuctionItem auction : prevAuctions) {
//	String myID = getMyIDFromName(auction.item_name);
//	
//	if(itemLowestBins.containsKey(myID)) {
//		if(auction.starting_bid < itemLowestBins.get(myID)) {
//			itemLowestBins.put(myID, auction.starting_bid);
//		}
//	}
//	else {
//		itemLowestBins.put(myID, auction.starting_bid);
//		
//	}
//	if(itemAmounts.containsKey(myID)) {
//		int newValue = itemAmounts.get(myID) + 1;
//		itemAmounts.put(myID, newValue);
//	} else itemAmounts.put(myID, 1);
//}
//for(int i = 0; i < auctions.length; i++) {
//	newAuctionItems.add(auctions[i]);
//	String myID = getMyIDFromName(auctions[i].item_name);
//	float minPrice;
//	if(itemLowestBins.containsKey(myID))
//		minPrice = itemLowestBins.get(myID);
//	else minPrice = 0;
//	
//	if(auctions[i].starting_bid <= minPrice - minProfit) {
//		Risk risk;
//		int amount;
//		if(itemAmounts.containsKey(myID))
//			amount = itemAmounts.get(myID);
//		else
//			amount = 0;
//		if(amount < 5) 
//			risk = Risk.Insane;
//		else if(amount < 25)
//			risk = Risk.High;
//		else if (amount < 100) 
//			risk = Risk.Medium;
//		else 
//			risk = Risk.Low;
//		if(risk.ordinal() <= FruitBin.maxRisk.ordinal())
//			result.add(new AuctionInfo(auctions[i], auctions[i].starting_bid, itemLowestBins.get(myID), risk));
//	}
//	
//newAuctionItems.add(auctions[i]);
//}
//prevAuctions = newAuctionItems.toArray(new AuctionItem[result.size()]);
//return result.toArray(new AuctionInfo[result.size()]);
//}