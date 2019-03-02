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

import com.fridai.Ability;
import com.fridai.Card;

/** 
 * This class maintains an immutable list of cards, but is performance optimized for both heap and CPU usage reduction 
 * by sharing the 'cards' data storage array with child objects created by mutate*(...) calls.
 * 
 * Thus, if a is an instance of this class, and a is called as follow:
 * 		`b = a.mutateAdd( someCard )`
 * The `b` object will be a new instance of the ImmutableGrowableListCards class, but it will 
 * share its internal array `cards` with `a`. Fortunately, since both instances are functionally 
 * immutable, they can never step on each others toes.
 * 
 * This class presents an immutable interface, and should be treated as immutable by callers. This class
 * is not thread safe.
 **/
public final class ImmutableGrowableListCards {
	
	private final Card[] cards;
	
	private final boolean allowDuplicates; // currently only used for 
	
	// The index of the last card in the array (-1 if array is empty)
	private final int endPos;

	public ImmutableGrowableListCards(int maxSize) {
		cards = new Card[maxSize];
		endPos = -1;
		allowDuplicates = false;
	}
	
	private ImmutableGrowableListCards(Card[] cards, int endPos, boolean allowDuplicates) {
		this.cards = cards;
		this.endPos = endPos;
		this.allowDuplicates = allowDuplicates;
		
		if(FridayUtil.RUNTIME_CHECK) {
			if(cards == null) { FridayUtil.throwErr("Cards is null"); }
			
			for(int x = 0; x <= endPos; x++ ) {
				if(cards[x] == null) {
					FridayUtil.throwErr("Card array contains a null at "+x);
				}
			}

			if(!allowDuplicates) {
				for(int x = 0; x <= endPos; x++) {
					for(int y = x+1; y <= endPos; y++) {
						Card cx = cards[x];
						Card cy = cards[y];
						if(cx == cy) {
							
							FridayUtil.throwErr("Matching cards at indices "+x+" "+y);
						}
						
					}
				}
			}
		}
		
	}

	public final Card get(int x) {
		if(x > endPos) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return cards[x];
	}
	
	public final int size() {
		return endPos+1;
	}
	
	public final List<Card> getAsList() {
		List<Card> result = new ArrayList<>();
		
		for(int x = 0; x <= endPos; x++) {
			result.add(cards[x]);
		}
		return result;
	}

	
	private final void printGrowMessage() {
		if(!FridayUtil.RUNTIME_CHECK) { return; }
		
		
//		System.err.println("Immutable list grew, old size: "+cards.length);
//		System.err.println("Stack trace:");
//		Thread.dumpStack();

//		System.exit(0);
		
	}
	
	public final Card findCardByAbility(Ability ability) {
		
		for(Card c : getAsList()) {
			if(c.getAbility() == ability) {
				return c;
			}
		}
		
		return null;
		
	}
	
	public final ImmutableGrowableListCards mutateAddAll(List<Card> listParam) {
		
		ImmutableGrowableListCards result = this;
		
		for(Card c : listParam) {
			
			result = result.mutateAdd(c);
		}
		
		return result;
		

		// Previous (IIRC) broken attempt at doing this in bulk, preserved in case
		// I want to tackle this again for speed improvements:
		
//		Card[] newCards = cards;
//		
//		if( (endPos + 1)  + listParam.size()  > cards.length ) {
//			// Grow if the operation will otherwise fail if we dont
//
//			// Double until it can hold all the required cards
//			int newSize;
//			do {
//				newSize = cards.length*2;
//			} while(newSize <= (endPos+1) + listParam.size() );
//			
//			newCards = new Card[newSize];
//			System.arraycopy(cards, 0, newCards, 0, cards.length);
//			
//			printGrowMessage();
//		}
//
//		int newEndPos = endPos;
//		
//		if(newCards[newEndPos+1] != null) {
//			newCards = new Card[newCards.length];
//			System.arraycopy(cards, 0, newCards, 0, cards.length);
//		}
//		
//		for(int x = 0; x < listParam.size(); x++) {
//			newCards[newEndPos+1] = listParam.get(x);
//			newEndPos++;
//		}
//		
//		return new ImmutableGrowableListCards(newCards, newEndPos);

	}
	
	public final ImmutableGrowableListCards mutateAddAll(ImmutableGrowableListCards listParam) {
		
		// TODO: LOWER - The mutateAddAll(...) methods could be faster in ImmutableGrowableListCard
		
		ImmutableGrowableListCards result = this;
		
		for(int x = 0; x < listParam.size(); x++) {
			Card c = listParam.get(x);
			result = result.mutateAdd(c);
		}
		
		return result;

		// Previous (IIRC) broken attempt at doing this in bulk, preserved in case
		// I want to tackle this again for speed improvements:
	
//		Card[] newCards = cards;
//		
//		if( (endPos + 1)  + listParam.size()  > cards.length ) {
//			// Grow if the operation will otherwise fail if we don't
//
//			// Double until it can hold all the required cards
//			int newSize;
//			do {
//				newSize = cards.length*2;
//			} while(newSize <= (endPos+1) + listParam.size() );
//			
//			newCards = new Card[newSize];
//			System.arraycopy(cards, 0, newCards, 0, cards.length);
//			printGrowMessage();
//		}
//		
//		int newEndPos = endPos;
//		
//		if(newCards[newEndPos+1] != null) {
//			newCards = new Card[newCards.length];
//			System.arraycopy(cards, 0, newCards, 0, cards.length);
//		}
//		
//		for(int x = 0; x < listParam.size(); x++) {
//			newCards[newEndPos+1] = listParam.get(x);
//			newEndPos++;
//		}
//		
//		return new ImmutableGrowableListCards(newCards, newEndPos);
		
	}

	public final boolean containsPhysicalId(int id) {
		for(int x = 0; x <= endPos; x++) {
			if(cards[x].getPhysicalCardId() == id) {
				return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("unused")
	public ImmutableGrowableListCards mutateRemoveCardByPhysicalId(int physicalCardId) {
		
		boolean match = false;
		
		List<Card> list = new ArrayList<>();
		for(int x = 0; x < size(); x++) {
			Card curr = get(x);
			if(curr.getPhysicalCardId() != physicalCardId) {
				list.add(curr);
			} else {
				if(FridayUtil.RUNTIME_CHECK && match) {
					FridayUtil.throwErr("Multiple matches found.");
				}
				
				match = true;
			}
		}
		
		if(FridayUtil.RUNTIME_CHECK && !match) {
			FridayUtil.throwErr("Could not find card with id "+physicalCardId+" in list.");
		}
		
		Card[] result = new Card[cards.length];
		
		int x = 0;
		for(Card c : list) {
			result[x] = c;
			x++;
		}
				
		return new ImmutableGrowableListCards( result, list.size()-1, allowDuplicates);
		
	}

	
	public final ImmutableGrowableListCards mutateAdd_allowDuplicates(Card c) {
		return mutateAdd(c, true);
	}

	private final ImmutableGrowableListCards mutateAdd(Card c, boolean allowDuplicates) {
		
		Card[] newCards = cards;

		if(cards.length == endPos+2) {
			newCards = new Card[cards.length*2];
			System.arraycopy(cards, 0, newCards, 0, cards.length);
			printGrowMessage();
			
		} else if(cards[endPos+1] != null)  {
			
			newCards = new Card[cards.length];
			System.arraycopy(cards, 0, newCards, 0, cards.length);
		}
		
		newCards[(endPos+1)] = c;

		// Grow if necessary
		return new ImmutableGrowableListCards(newCards, endPos+1, allowDuplicates);
		
	}

	
	public final ImmutableGrowableListCards mutateAdd(Card c) {
		
		return mutateAdd(c, false);

		// Previous (IIRC) broken attempt at doing this in bulk, preserved in case
		// I want to tackle this again for speed improvements:

//		Card[] newCards = cards;
//
//		if(cards.length == endPos+2) {
//			newCards = new Card[cards.length*2];
//			System.arraycopy(cards, 0, newCards, 0, cards.length);
//			printGrowMessage();
//			
//		} else if(cards[endPos+1] != null)  {
//			
//			newCards = new Card[cards.length];
//			System.arraycopy(cards, 0, newCards, 0, cards.length);
//		}
//		
//		newCards[(endPos+1)] = c;
//
//		// Grow if necessary
//		return new ImmutableGrowableListCards(newCards, endPos+1, allowDuplicates);
//		
	}
	
	public final ImmutableGrowableListCards fullClone(int newSize) {
		
		Card[] newCards = new Card[newSize];
		System.arraycopy(cards, 0, newCards, 0, cards.length);
		
		return new ImmutableGrowableListCards(newCards, endPos, allowDuplicates);
		
	} 
	
	@Override
	public String toString() {
		String result = "size:"+size()+": \n";
		for(int x = 0; x < size(); x++) {
			Card c = get(x);
			
			result += "("+x+")"+c+",  \n";
		}
		return result.trim();
	}
	
	public int[] asIntArray() {
		List<Card> list = getAsList();
		int[] result = new int[list.size()];
		
		for(int x = 0; x < list.size(); x++) {
			result[x] = list.get(x).getPhysicalCardId();
		}
		
		return result;
	}
}
