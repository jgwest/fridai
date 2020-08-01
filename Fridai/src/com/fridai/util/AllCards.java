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

import java.util.Collections;
import java.util.List;

import com.fridai.Card;
import com.fridai.PirateCard;

/** An unmodifiable list of all the cards in the game. */
public class AllCards {

	private final List<Card> agingCards;
	
	private final List<PirateCard> pirateCards;
	
	private final List<Card> fightingCards;
	
	private final List<Card> hazardCards;

	public AllCards(List<Card> agingCards, List<PirateCard> pirateCards, List<Card> fightingCards, List<Card> hazardCards) {
		this.agingCards = Collections.unmodifiableList(agingCards);
		this.pirateCards = Collections.unmodifiableList(pirateCards);
		this.fightingCards = Collections.unmodifiableList(fightingCards);
		this.hazardCards = Collections.unmodifiableList(hazardCards);
	}

	public List<Card> getAgingCards() {
		return agingCards;
	}

	public List<PirateCard> getPirateCards() {
		return pirateCards;
	}

	public List<Card> getFightingCards() {
		return fightingCards;
	}

	public List<Card> getHazardCards() {
		return hazardCards;
	}
	
}
