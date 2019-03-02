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

package com.fridai.json;

/** JSON representation of the (non-Pirate) of a pirate card, for serialization/deserialization by Jackson. */
public class JsonPirateCard {

	int freeCards;
	int hazardValue;

	String ability;
	int pirateCardId;

	public int getFreeCards() {
		return freeCards;
	}

	public void setFreeCards(int freeCards) {
		this.freeCards = freeCards;
	}

	public int getHazardValue() {
		return hazardValue;
	}

	public void setHazardValue(int hazardValue) {
		this.hazardValue = hazardValue;
	}

	public String getAbility() {
		return ability;
	}

	public void setAbility(String ability) {
		this.ability = ability;
	}

	public int getPirateCardId() {
		return pirateCardId;
	}

	public void setPirateCardId(int pirateCardId) {
		this.pirateCardId = pirateCardId;
	}

}
