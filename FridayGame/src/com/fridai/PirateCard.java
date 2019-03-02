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

import com.fridai.json.JsonPirateCard;

/** Represents a physical pirate card. 
 * An instance of this class is immutable, and only a single instance of a card will ever be created
 * (thus the number of Card instances is <50ish throughout the lifetime of the application). 
 * 
 * Cards instances are created by the CardReader class. */
public final class PirateCard {

	private int freeCards;
	private int hazardValue;

	private final int pirateCardId;
	private PirateCardAbility ability;

	public PirateCard(int pirateCardId) {
		this.pirateCardId = pirateCardId;
	}

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

	public PirateCardAbility getAbility() {
		return ability;
	}

	public void setAbility(PirateCardAbility ability) {
		this.ability = ability;
	}
	
	public int getPirateCardId() {
		return pirateCardId;
	}
	
	@Override
	public String toString() {
		return freeCards+" / "+ hazardValue+" "+ability.name();
	}

	public enum PirateCardAbility {
		TWO_HAZARD_POINTS_FOR_EACH_AGING_CARD, EACH_ADDITIONAL_FIGHTING_CARD_COSTS_2_LIFE_POINTS, ONLY_HALF_OF_FACE_UP_FIGHTING_CARDS_COUNT, EACH_FACE_UP_FIGHTING_CARD_COUNTS_PLUS1_FIGHTING_POINT, FIGHT_AGAINST_ALL_REMAINING_HAZARD_CARDS, DOT_DOT_DOT;

		public static PirateCardAbility fromString(String str) {
			switch (str) {
			case "+2 hazard points for each aging card":
				return TWO_HAZARD_POINTS_FOR_EACH_AGING_CARD;
			case "Each additional fighting card costs 2 life points":
				return EACH_ADDITIONAL_FIGHTING_CARD_COSTS_2_LIFE_POINTS;
			case "Only half of the face up fighting cards count (face up aging cards must be part of this)":
				return ONLY_HALF_OF_FACE_UP_FIGHTING_CARDS_COUNT;
			case "Each face up fighting card counts +1 fighting point":
				return EACH_FACE_UP_FIGHTING_CARD_COUNTS_PLUS1_FIGHTING_POINT;
			case "Fight against all remaining hazard cards":
				return FIGHT_AGAINST_ALL_REMAINING_HAZARD_CARDS;
			case "...":
				return DOT_DOT_DOT;
			}

			throw new RuntimeException("Unrecognized string: " + str);
		}
	}

	public JsonPirateCard toJson() {
		JsonPirateCard result = new JsonPirateCard();
		
		result.setAbility(getAbility().name());
		result.setFreeCards(getFreeCards());
		result.setHazardValue(getHazardValue());
		result.setPirateCardId(getPirateCardId());
		
		return result;
		
	}
	
	public boolean equalsJson(JsonPirateCard json) {
		if(!json.getAbility().equals(getAbility().name())) { return false; }
		
		if(json.getFreeCards() != getFreeCards()) { return false; }
		
		if(json.getHazardValue() != getHazardValue()) { return false; }
		
		return true;
	}

	public boolean equalsByContents(PirateCard other) {
		if(!other.getAbility().equals(getAbility())) { return false; }
		
		if(other.getFreeCards() != getFreeCards()) { return false; }
		
		if(other.getHazardValue() != getHazardValue()) { return false; }
		
		return true;
	}
}
