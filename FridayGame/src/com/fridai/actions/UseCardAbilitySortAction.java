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

package com.fridai.actions;

import com.fridai.Card;
import com.fridai.ui.ansi.AnsiCards;
import com.fridai.util.FridayUtil;

/** Player action to store 2 or 3 cards in a given order and place them back on the fighting card
 * deck, with an option to discard one of those cards. */
public class UseCardAbilitySortAction extends Action {

	private final Card[] sortOrder;

	/* nullable */
	private final Card discard;

	public UseCardAbilitySortAction(Card[] sortOrder, Card discard) {
		
		if(FridayUtil.RUNTIME_CHECK) {
			
			for(Card c : sortOrder) {
				if(c == null)  { FridayUtil.throwErr("Null in sort order"); };
			}
			
		}
		
		this.sortOrder = sortOrder;
		this.discard = discard;
	}

	public Card[] getSortOrder() {
		return sortOrder;
	}

	public Card getDiscard() {
		return discard;
	}

	@Override
	public ActionType getType() {
		return ActionType.USE_CARD_ABILITY_SORT;
	}

	@Override
	public String toString() {
		String result = getType().name() +":  ";
		
		int count = 0;
		for(Card c : sortOrder) {
			result += "  "+count+") "+AnsiCards.asFightingCard(c, null);
			count++;
		}
		
		if(discard != null) {
			result  += " discard) "+AnsiCards.asFightingCard(discard, null);
		}
		
		return result;
	}
}
