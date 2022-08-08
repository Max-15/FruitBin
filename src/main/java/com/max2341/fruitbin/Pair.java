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
	public T getLowerFloat() {
		if((float)key <= (float)value) {
			return key;
		} else {
			return (T)value;
		}
	}
	public T getHigherFloat() {
		if((float)key >= (float)value) {
			return key;
		} else {
			return (T)value;
		}
	}

}
