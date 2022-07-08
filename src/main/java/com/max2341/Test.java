package com.max2341;

import com.max2341.fruitbin.*;

public class Test {
	public static void main(String arg[]) {
		float number = 6000;
		float intrium = Math.round(number / 100)/10;
		float actual  = Math.round(number / 100)/10f;
		if (actual == intrium) {
			System.out.print( (int)intrium + "K");
		} else {
			System.out.print( actual + "K");
		}
		Pair<String, Integer> myPair = new Pair<String, Integer>("name", 5);
		System.out.print(myPair.getKey());
	}
	
}
