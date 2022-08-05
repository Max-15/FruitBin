package com.max2341.fruitbin;

public class AuctionItem {
	public String uuid;
	public String auctioneer;
	public String profile_id;
	public String[] coop;
	public long last_updated;
	public long start;
	public long end;
	public String item_name;
	public String item_lore;
	public String extra;
	public String category;
	public String tier;
	public float starting_bid;
	public String item_bytes;
	public boolean claimed;
	public float highest_bid_amount;
	//public long last_updated;
	public boolean bin;
	//public Bid[] bids;
}
//uuid is auction_id
//if it is BIN, then highest_bid_amount is always 0, and the price is the starting_bid.
