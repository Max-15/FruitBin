package com.max2341.fruitbin;

import com.max2341.fruitbin.Utils.Risk;

public class AuctionInfo {
	
	public AuctionInfo(AuctionItem auction, int profit, int profitPercent, float price, float lowestBin, Risk risk) {
		this.profitPercent = profitPercent;
		this.auction = auction;
		this.price = price;
		this.lowestBin = lowestBin;
		this.risk = risk;
		this.profit = profit;
	 }

	AuctionItem auction;
	int profit;
	int profitPercent;
	float price;
	float lowestBin;
	Risk risk;
}
