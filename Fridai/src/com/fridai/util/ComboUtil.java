/**
 * Copyright (C) 2019  Jonathan West
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fridai.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Utility methods that generate every combination (not permutation) of a given number of cards, the result of which 
 * is cached in this method. This is used to examine (and potentially act on) every possible use of a set of 
 * card abilities. 
 * 
 * So as to save CPU time, these values are only calculated once (on first use), and then stored here. This is a 
 * trade-off of memory for performance. 
 **/
public class ComboUtil {

	public static final int MAX_COMBO_COUNT = 18;
	
	public static final int[][] PERMUTATIONS_OF_3 = new int[][] {
		new int[] {0, 1 , 2},
		new int[] {0, 2 , 1},
		new int[] {1, 0 , 2},
		new int[] {1, 2 , 0},
		new int[] {2, 0 , 1},
		new int[] {2, 1 , 0},
	};

	
	public static final int[][][] COMBOS = generateCombos();
	
	public static void main(String[] args) {
		
		int[][][] result = generateCombos();
		
		
		for(int x = 2; x <= result.length-1; x++) {
			
			
			long size = 0;
			
			int[][] innerCombo = result[x];

			
			for(int y = 0; y < innerCombo.length; y++) {
				
				int[] combo = innerCombo[y];
				
				for(int z = 0; z < combo.length; z++) {
					
					size++;
				
				}
				
				
			}
			
			System.out.println(x+ " -> " + size);			
		}

		
	}
	
	private static int[][][] generateCombos() {
		
		int[][][] result = new int[MAX_COMBO_COUNT][][];
		
		result[1] = new int[][] { new int[] { 0 }, new int[] {}  }; 

		for(int x = 2; x <= MAX_COMBO_COUNT-1; x++) { 
			
			int[][] innerResult = generateSingleCombo(x);
			
			result[x] = innerResult;
		}
		
		return result;
		
	}
	
	
	// Return an array of combinations (not permutations) of numbers less than the 'number' param
	private static int[][] generateSingleCombo(int number) {
		
		int[] powers = new int[number];
		for(int x = 0; x < powers.length; x++) {
			powers[x] = (int)Math.pow(2, x);
		}

		int val = (int)Math.pow(2, number)-1;


		List<List<Integer>> combinations = new ArrayList<>();
		
		while(val >= 0) {
			
			List<Integer> singleCombination = new ArrayList<>();
			
			int currVal = val;
			
			int totalVal = 0;
			
			for(int x = powers.length-1; x >= 0; x--) {
				int currPower = powers[x];
				if(currVal - currPower >= 0 ) {
					currVal -= currPower;
					totalVal += currPower;
					singleCombination.add(x);
				}
			}
			
			if(totalVal != val) {
				throw new RuntimeException("Totals don't match.");
			}

			// Select none of the items is a valid combination
			combinations.add(singleCombination);
			
			val--;
		}
		
		// Ensure that the result contains the empty set (no cards selected)
		{
			boolean containsEmptySet = false;
			for(List<Integer> singleCombo : combinations) {
				if(singleCombo.size() == 0) {
					containsEmptySet = true;
					break;
				}
			}
			if(!containsEmptySet) {
				combinations.addAll(Collections.emptyList());
			}
		}
		
		
		int[][] result = new int[combinations.size()][];
		
		int x = 0;
		for(List<Integer> combination : combinations) {
			
			result[x] = new int[combination.size()];
			
			for(int y = 0; y < combination.size(); y++) {
				result[x][y] = combination.get(y);
			}
			
			x++;
		}

		return result;
		
	}
}

