package com.max2341.fruitbin;

public class Pair<T,S> {
	public T key;
	public S value;
	
	public Pair(T t, S s) {
		this.key = t;
		this.value = s;
	}
	
	public T getKey() {
		return key;
	}
	public S getValue() {
		return value;
	}
	public T getLowerFloatBINAuction() {
		if(((AuctionItem)key).starting_bid <= ((AuctionItem)value).starting_bid) {
			return key;
		} else {
			return (T)value;
		}
	}
	public T getHigherFloatBINAuction() {
		if(((AuctionItem)key).starting_bid >= ((AuctionItem)value).starting_bid) {
			return key;
		} else {
			return (T)value;
		}
	}

}
