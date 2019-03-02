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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fridai.util.FridayUtil;

/**
 * Keeps track of which cards are destroyed for verification/debugging purposes. 
 * 
 * Instances of this class are immutable.
 */
public final class RuntimeObject {

	private final List<Card> destroyedCards = new ArrayList<>();
	
	public RuntimeObject() {
	}
	
	public List<Card> getDestroyedCards() {
		return Collections.unmodifiableList(destroyedCards);
	}
	
	public RuntimeObject mutateAddDestroyedCard(Card destroyed) {
		
		RuntimeObject result = new RuntimeObject();
		
		for(Card c : destroyedCards) {
			if(c.getPhysicalCardId() == destroyed.getPhysicalCardId()) {
				FridayUtil.throwErr("Attempt to add card which is already destroyed: "+c);
			}
		}
		
		result.destroyedCards.addAll(this.destroyedCards);
		result.destroyedCards.add(destroyed);
		
		return result;
		
	}
	
}
