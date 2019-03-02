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

import com.fridai.json.JsonCard;
import com.fridai.util.FridayUtil;

/** Represents a hazard card, fighting card, or aging card. The only physical card not represented by this class
 * is the pirate card, which is represented by the PirateCard class.
 * 
 * An instance of this class is immutable, and only a single instance of a card will ever be created
 * (thus the number of Card instances is <50 throughout the lifetime of the application). 
 * 
 * Cards instances are created either by CardReader, or by the Card.clone() method. */
public final class Card {

	enum Type { HAZARD, FIGHTING, AGING };

	private int fightingValue = Integer.MIN_VALUE;
	private int[] hazardValues = null;
	private int freeCards = Integer.MIN_VALUE;

	private String hazardTitle = null;

	private String title = null;

	private Ability ability = null;

	private Difficulty difficulty = null;

	private final Type type;

	private int ratingSimple = 0;
	
	/** A card has a unique trait ID iff it is not a duplicate: cards that are exact duplicates of each other have the same trait id. */
	private int traitId = Integer.MIN_VALUE;
	
	/** Physical card id is a unique ID for every physical card: thus all 59 fighting cards each have their own unique physical card ID, with no dupes. */
	private int physicalCardId = Integer.MIN_VALUE;
	
	Card(Type type) {
		this.type = type;
	}
	
	protected final Card cloneCard()  {
		Card result = new Card(this.getType());
		
		result.setFightingValue(this.getFightingValue());
		result.setHazardValues(this.getHazardValues());
		result.setFreeCards(this.getFreeCards());

		result.setHazardTitle(this.getHazardTitle());
		result.setTitle(this.getTitle());

		result.setAbility(this.getAbility());

		result.setDifficulty(this.getDifficulty());
		
		result.setTraitId(this.getTraitId());
		result.setPhysicalCardId(this.getPhysicalCardId());
		
		result.setRatingSimple(FridayUtil.calculateFightingCardValue_simpleNew(result));
		
		return result;
		
	}
	
	@Override
	public String toString() {
		
		String result = "["+type+"@"+physicalCardId+"]";
		
		if(freeCards != Integer.MIN_VALUE) {
			result += " free-cards:"+freeCards;
		}
		
		if(fightingValue != Integer.MIN_VALUE) {
			result += " fighting-value:"+fightingValue;
		}
		
		if(hazardValues != null) {
			result += " hazard-values:";
			for(int e : hazardValues) {
				result += " "+e;
			}
		}
		
		if(difficulty != null) {
			result += " difficulty:"+difficulty.name();
		}
		
		if(ability != null) {
			result += " ability:"+ability.name();
		}
		
		if(title != null) {
			result += " title:"+title;
		}
		
		if(hazardTitle != null) {
			result += " hazardTitle:"+hazardTitle;
		}
		
		
		return result;
		
	}
	


	public static enum Difficulty { NONE, NORMAL, DIFFICULT;
		
		public static Difficulty createFromString(String str) {
			str = str.trim().toLowerCase();
			
			if(str.equals(NORMAL.name().toLowerCase())) {
				return Difficulty.NORMAL;
			} else if(str.equals(DIFFICULT.name().toLowerCase())) {
				return Difficulty.DIFFICULT;
			} 
			
			throw new RuntimeException("Unrecognized string: "+str);
			
		}
	
	}; 
	
	public void setRatingSimple(int ratingSimple) {
		this.ratingSimple = ratingSimple;
	}
	
	public int getRatingSimple() {
		return ratingSimple;
	}
	
	public final Type getType() {
		return type;
	}
	
	public final int getFightingValue() {
		return fightingValue;
	}

	public final void setFightingValue(int fightingValue) {
		this.fightingValue = fightingValue;
	}

	public final int[] getHazardValues() {
		return hazardValues;
	}

	public final void setHazardValues(int[] hazardValues) {
		this.hazardValues = hazardValues;
	}

	public final int getFreeCards() {
		return freeCards;
	}

	public final void setFreeCards(int freeCards) {
		this.freeCards = freeCards;
	}

	public final String getHazardTitle() {
		return hazardTitle;
	}

	public final void setHazardTitle(String hazardTitle) {
		this.hazardTitle = hazardTitle;
	}

	public final String getTitle() {
		return title;
	}

	public final void setTitle(String title) {
		this.title = title;
	}

	public final Ability getAbility() {
		return ability;
	}

	public final void setAbility(Ability ability) {
		this.ability = ability;
	}

	public final Difficulty getDifficulty() {
		return difficulty;
	}

	public final void setDifficulty(Difficulty difficulty) {
		this.difficulty = difficulty;
	}

	public final int getTraitId() {
		return traitId;
	}

	public final int getPhysicalCardId() {
		return physicalCardId;
	}

	public final void setTraitId(int traitId) {
		this.traitId = traitId;
	}

	public final void setPhysicalCardId(int physicalCardId) {
		this.physicalCardId = physicalCardId;
	}

	
	public JsonCard toJson() {
		JsonCard result = new JsonCard();
		
		result.setAbility(ability.name());
		result.setDifficulty(difficulty.name());
		result.setFightingValue(fightingValue);
		result.setFreeCards(freeCards);
		result.setHazardTitle(hazardTitle);
		result.setHazardValues(hazardValues);
		result.setPhysicalCardId(physicalCardId);
		result.setRatingSimple(ratingSimple);
		result.setTitle(title);
		result.setType(type.name());
		
		return result;
	}
	
	/** Does not compare by physical id, but rather by the contents of the card itself. 
	 * Note that since duplicates exist, there may be non-unique matches */
	public boolean equalsByContents(Card card) {
		if(card.getFightingValue() != getFightingValue()) { return false; }
		if(card.getFreeCards() != getFreeCards()) { return false; }
		
		if(!objectEquals(card.getTitle(), getTitle())) { return false; }
		
		if(!card.getType().equals(getType())) { return false; }
		
		// Compare difficulty
		{
			Difficulty jsonDifficulty = card.getDifficulty();
			Difficulty thisDifficulty = getDifficulty() != null ? getDifficulty() : null;
			
			if(!objectEquals(jsonDifficulty, thisDifficulty)) { return false; }
		}
		
		// Compare ability
		{
			Ability jsonAbility = card.getAbility();
			Ability thisAbility = getAbility() != null ? getAbility() : null;
			if(!objectEquals(jsonAbility, thisAbility)) { return false; }
		}
		
		// Compare hazard values
		{
			int[] jsonHazardValues = card.getHazardValues();
			if(jsonHazardValues == null) { jsonHazardValues = new int[0]; };
			
			int[] hazardValues = getHazardValues();
			if(hazardValues == null) { hazardValues = new int[0]; } 
			
			if(jsonHazardValues.length != hazardValues.length) { return false; }
			
			for(int x = 0; x < jsonHazardValues.length; x++) {
				
				if(jsonHazardValues[x] != hazardValues[x]) {
					return false;
				}
				
			}
		}
		
		if(!objectEquals(card.getHazardTitle(), getHazardTitle())) { return false; }
		
		return true;
		
	}
	
	public boolean equalsJson(JsonCard json) { 
		if(json.getFightingValue() != getFightingValue()) { return false; }
		if(json.getFreeCards() != getFreeCards()) { return false; }
		
		if(!objectEquals(json.getTitle(), getTitle())) { return false; }
		
		if(!json.getType().equals(getType().name())) { return false; }
		
		// Compare difficulty
		{
			String jsonDifficulty = json.getDifficulty();
			String thisDifficulty = getDifficulty() != null ? getDifficulty().name() : null;
			
			if(!objectEquals(jsonDifficulty, thisDifficulty)) { return false; }
		}
		
		// Compare ability
		{
			String jsonAbility = json.getAbility();
			String thisAbility = getAbility() != null ? getAbility().name() : null;
			if(!objectEquals(jsonAbility, thisAbility)) { return false; }
		}
		
		// Compare hazard values
		{
			int[] jsonHazardValues = json.getHazardValues();
			if(jsonHazardValues == null) { jsonHazardValues = new int[0]; };
			
			int[] hazardValues = getHazardValues();
			if(hazardValues == null) { hazardValues = new int[0]; } 
			
			if(jsonHazardValues.length != hazardValues.length) { return false; }
			
			for(int x = 0; x < jsonHazardValues.length; x++) {
				
				if(jsonHazardValues[x] != hazardValues[x]) {
					return false;
				}
				
			}
		}
		
		if(!objectEquals(json.getHazardTitle(), getHazardTitle())) { return false; }
		
		return true;
				
	}
	
	private static boolean objectEquals(Object one, Object two) {
		return FridayUtil.objectEquals(one, two);
	}
}
