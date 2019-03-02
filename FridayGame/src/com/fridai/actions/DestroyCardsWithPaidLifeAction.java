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

/** Player action to destroy cards after a combat round has ended. */
public class DestroyCardsWithPaidLifeAction extends Action {

	private final Card[] cardsToDestroy;
	
	public DestroyCardsWithPaidLifeAction(Card[] cardsToDestroy) {
		this.cardsToDestroy = cardsToDestroy;
	}
	
	@Override
	public ActionType getType() {
		return ActionType.DESTROY_CARDS_WITH_PAID_LIFE;
	}

	public Card[] getCardsToDestroy() {
		return cardsToDestroy;
	}
	
	@Override
	public String toString() {
		String result =  getType().name()+" { ";
		
		for(Card c : cardsToDestroy) {
			result += "["+c+"] ";
		}
		
		result+="}";
		
		return result;
		
	}
	
	@Override
	public String prettyPrint() {
		String result =  getType().name()+" { \n";
		
		for(Card c : cardsToDestroy) {
			result += "  - "+c+" \n";
		}
		
		result+="}\n";
		
		return result;
		
	}
}
