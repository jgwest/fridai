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

import java.util.Map;

/** JSON representation of the Pirate game state, for serialization/deserialization by Jackson. */
public class JsonPirateGameState implements IJsonSharedGameState {

	private String state;

	private int lifePoints;

	private int[] yourFightingCards;

	private JsonSlowGameState slowGameState;

	private int[] discardFightCards;

	private Integer activePirateCard;
	
	private JsonPirateCardInfo pirateCardInfo;
	
	// Nullable
	private Map<Integer /* physical card id */, Boolean> lhsOrRhsFightingCardUsed;

	// Nullable
	private Map<Integer /* physical card id */, Boolean> lhsOrRhsFightingCardDoubled;

	// ------------------------

	private int[] lhs_fightCards;
	private int[] rhs_fightCards;

	// Nullable
	private JsonAbilityObject abilityObject;

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public int getLifePoints() {
		return lifePoints;
	}

	public void setLifePoints(int lifePoints) {
		this.lifePoints = lifePoints;
	}

	public int[] getYourFightingCards() {
		return yourFightingCards;
	}

	public void setYourFightingCards(int[] yourFightingCards) {
		this.yourFightingCards = yourFightingCards;
	}

	public Integer getActivePirateCard() {
		return activePirateCard;
	}
	
	public void setActivePirateCard(Integer activePirateCard) {
		this.activePirateCard = activePirateCard;
	}
	
	public JsonSlowGameState getSlowGameState() {
		return slowGameState;
	}

	public void setSlowGameState(JsonSlowGameState slowGameState) {
		this.slowGameState = slowGameState;
	}

	public int[] getDiscardFightCards() {
		return discardFightCards;
	}

	public void setDiscardFightCards(int[] discardFightCards) {
		this.discardFightCards = discardFightCards;
	}

	public Map<Integer, Boolean> getLhsOrRhsFightingCardUsed() {
		return lhsOrRhsFightingCardUsed;
	}

	public void setLhsOrRhsFightingCardUsed(Map<Integer, Boolean> lhsOrRhsFightingCardUsed) {
		this.lhsOrRhsFightingCardUsed = lhsOrRhsFightingCardUsed;
	}

	public Map<Integer, Boolean> getLhsOrRhsFightingCardDoubled() {
		return lhsOrRhsFightingCardDoubled;
	}

	public void setLhsOrRhsFightingCardDoubled(Map<Integer, Boolean> lhsOrRhsFightingCardDoubled) {
		this.lhsOrRhsFightingCardDoubled = lhsOrRhsFightingCardDoubled;
	}

	public int[] getLhs_fightCards() {
		return lhs_fightCards;
	}

	public void setLhs_fightCards(int[] lhs_fightCards) {
		this.lhs_fightCards = lhs_fightCards;
	}

	public int[] getRhs_fightCards() {
		return rhs_fightCards;
	}

	public void setRhs_fightCards(int[] rhs_fightCards) {
		this.rhs_fightCards = rhs_fightCards;
	}

	public JsonAbilityObject getAbilityObject() {
		return abilityObject;
	}

	public void setAbilityObject(JsonAbilityObject abilityObject) {
		this.abilityObject = abilityObject;
	}

	public JsonPirateCardInfo getPirateCardInfo() {
		return pirateCardInfo;
	}
	
	public void setPirateCardInfo(JsonPirateCardInfo pirateCardInfo) {
		this.pirateCardInfo = pirateCardInfo;
	}
}
