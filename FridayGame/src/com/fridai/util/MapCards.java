package com.fridai.util;

import java.util.HashMap;
import java.util.Map;

/** A memory efficient mapping of Cards to boolean values. Benchmarking of this class 
 * versus a HashMap<Card, Boolean> showed significant Java heap reductions and small CPU performance improvements.
 * 
 * This class may only contain mappings of up to 64 cards, but this is perfectly fine as this number is greater than the number
 * of Card instances that exist (hence why this is an effective optimization). */
public final class MapCards {

	private final boolean[] barr = new boolean[64];

	public MapCards() {
	}

	public MapCards(MapCards param) {
		
		boolean[] other = param.barr;
		System.arraycopy(other, 0, barr, 0, other.length);
		
	}

	public final boolean get(int index) {
		return barr[index];
	}

	public final void put(int index, boolean b) {
		barr[index] = b;
	}
			

	public final Map<Integer, Boolean> toMapForJson() {
		Map<Integer, Boolean> result = new HashMap<>();
		
		for(int x = 0; x < barr.length; x++) {
		
			if(barr[x] == true) {
				result.put((Integer)x, true);
			}
		}
		
		return result;
		
	}
	
	public int size() {
		return barr.length;
	}
}
