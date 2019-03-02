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

package com.fridai;

import java.util.Collections;
import java.util.List;

import com.fridai.json.JsonAbilityObject;
import com.fridai.util.FridayUtil;

/**
 * When fighting against a hazard, some abilities (for example, EXCHANGE_X2, SORT_3_CARDS, CARDS_DRAW_2)
 * act across multiple game state rounds. This object is used to keep track of which
 * ability is active, and some additional state information specific to that ability.
 */
public final class AbilityObject {
	private final int stage; // only used by sort
	
	/** only used by sort, nullable */
	private final List<Card> drawnSortCards;
	
	private final int numberOfCardsDrawn;
	private final Card activeCard;
	
	private final boolean copied;
	
	public AbilityObject(Card activeCard, int numberOfCardsDrawn, int stage, List<Card> drawnSortCards, boolean isCopied) {
		this.activeCard = activeCard;
		this.numberOfCardsDrawn = numberOfCardsDrawn;
		this.stage = stage;
		this.drawnSortCards = drawnSortCards;
		this.copied = isCopied;
	}
	
	public Card getActiveCard() {
		return activeCard;
	}

	public int getStage() {
		return stage;
	}

	public int getNumberOfCardsDrawn() {
		return numberOfCardsDrawn;
	}
	
	@SuppressWarnings("unused")
	public List<Card> getDrawnSortCards() {
		if(FridayUtil.RUNTIME_CHECK && drawnSortCards != null) {
			return Collections.unmodifiableList(drawnSortCards);
		}
		return drawnSortCards;
	}
	
	public boolean isCopied() {
		return copied;
	}
	
	public JsonAbilityObject toJson() {
		JsonAbilityObject result = new JsonAbilityObject();
		
		result.setActiveCard(activeCard.getPhysicalCardId());
		result.setCopied(copied);
		
		if(drawnSortCards != null) {
			int[] drawnSortCardsArr = new int[drawnSortCards.size()];
			for(int x = 0; x < drawnSortCardsArr.length; x++) {
				drawnSortCardsArr[x] = drawnSortCards.get(x).getPhysicalCardId();
			}
			
			result.setDrawnSortCards(drawnSortCardsArr);
		} else {
			result.setDrawnSortCards(null);	
		}
		result.setNumberOfCardsDrawn(numberOfCardsDrawn);
		result.setStage(stage);
		
		return result;
	}
	
	public boolean equalsByContents(AbilityObject other) {
		if(other == null) { return false; }
		
		if(getStage() != other.getStage()) { return false; }

		if(!FridayUtil.listsEqual(getDrawnSortCards(), other.getDrawnSortCards())) { return false; }
		
		if(getNumberOfCardsDrawn() != other.getNumberOfCardsDrawn()) { return false; }
		
		if(!FridayUtil.objectEquals(getActiveCard(), other.getActiveCard())) { return false; }
			
		if(isCopied() != other.isCopied()) { return false; }
		
		return true;
	}
}
