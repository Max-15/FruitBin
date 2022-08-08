package com.max2341.fruitbin;

import com.max2341.fruitbin.Utils.Risk;

public class AuctionInfo {
	
	public AuctionInfo(AuctionItem auction, long profit, int profitPercent, float price, float lowestBin, int amountListed, Risk risk) {
		this.profitPercent = profitPercent;
		this.auction = auction;
		this.price = price;
		this.lowestBin = lowestBin;
		this.amountListed = amountListed;
		this.profit = profit;
		this.risk = risk;
	 }

	AuctionItem auction;
	long profit;
	int profitPercent;
	float price;
	float lowestBin;
	int amountListed;
	Risk risk;
}
