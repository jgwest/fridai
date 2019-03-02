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

import com.fridai.json.JsonPirateCardInfo;
import com.fridai.util.FridayUtil;

/** Some pirate cards have a fixed number of fighting cards and hazard points, while others are dynamic and
 * based on game conditions (for example, fight all remaining hazard cards). This class stores the calculated
 * hazard points and free fighting cards from static and dynamic pirate cards, as calculated when the pirate
 * is selected by the player during the 'select a pirate' phase. */
public final class PirateCardInfo {

	private final int hazardPoints;
	
	private final int freeFightingCards;

	public PirateCardInfo(int hazardPoints, int freeFightingCards) {
		this.hazardPoints = hazardPoints;
		this.freeFightingCards = freeFightingCards;
	}
	
	public PirateCardInfo(JsonPirateCardInfo info) {
		if(info == null) { FridayUtil.throwErr("Null param"); }
		
		this.hazardPoints = info.getHazardPoints();
		this.freeFightingCards = info.getFreeFightingCards();

	}

	public int getHazardPoints() {
		return hazardPoints;
	}

	public int getFreeFightingCards() {
		return freeFightingCards;
	}
	
	public JsonPirateCardInfo toJson() {
		JsonPirateCardInfo result = new JsonPirateCardInfo();
		result.setHazardPoints(hazardPoints);
		result.setFreeFightingCards(freeFightingCards);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof PirateCardInfo)) { return false;  }
		PirateCardInfo other = (PirateCardInfo)obj;
		
		return getHazardPoints() == other.getHazardPoints() &&
				getFreeFightingCards() == other.getFreeFightingCards();
	}
}
