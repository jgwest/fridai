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
import java.util.List;

import com.fridai.Card;

/** This class maintains an immutable list of cards. This class is designed to reduce the memory/cpu 
 * cost of removing objects, by sharing the `cards` array with child objects, and using the `index`
 * variable to determine where in the array to retrieve cards from.
 *  
 * Thus removing a card from an array involves only incrementing a variable and storing 
 * that variable (along with the backing array) in a child object. 
 * 
 * See ImmutableGrowableListCards for a similar description of how this immutablility optimization works.
 * 
 * This class presents an immutable interface, and should be treated as immutable by callers. This class
 * is not thread safe.
 **/
public final class ListCards {

	private final Card[] cards;
	
	// The index of the first valid element in the array 
	private int index;
	
	public ListCards(Card[] cards, int index) {
		this.cards = cards;
		this.index = index;
		
		if(FridayUtil.RUNTIME_CHECK) {		
			
			for(Card c : cards) {
				if(c == null) { FridayUtil.throwErr("One of the cards is null in "+c); }
			}

			for(int x = index; x < cards.length; x++) {
				for(int y = x+1; y < cards.length; y++) {
					Card cx = cards[x];
					Card cy = cards[y];
					if(cx == cy) {
						FridayUtil.throwErr("Matching cards at indices "+x+" "+y);
					}
					
				}
			}

		}
	}
	
	public int size() {
		return cards.length-index;
	}
	
	public Card get(int x) {
		// no point in bound checking: JVM will do it for us
		return cards[x+index];
	}
	
	public ListCards mutateRemoveFromFront(int cardsToRemove) {
		int newIndex = index+cardsToRemove;
		if(newIndex > this.cards.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return new ListCards(cards, newIndex);
	}
	
	public ListCards mutateAddToBottom(Card c) {
		List<Card> newList = asList();
		newList.add(c);
		return new ListCards(newList.toArray(new Card[newList.size()]), 0);
	}
	
	public ListCards fullClone() {
		Card[] newArray = new Card[cards.length];
		System.arraycopy(cards, 0, newArray, 0, cards.length);
		
		return new ListCards(newArray, index);
		
	}
	
	public List<Card> asList() {
		List<Card> result = new ArrayList<>();
		for(int x = index; x < cards.length; x++) {
			result.add(cards[x]);
		}
		
		return result;
//		return Arrays.asList(cards);
	}
	
	@Override
	public String toString() {
		String result = " size:"+size();
		for(Card c : asList()) {
			result += c.toString()+"\n";
		}
		
		return result;
		
	}
	
	public int[] asIntArray() {
		List<Card> list = asList();
		int[] result = new int[list.size()];
		
		for(int x = 0; x < list.size(); x++) {
			result[x] = list.get(x).getPhysicalCardId();
		}
		
		return result;
	}
}
