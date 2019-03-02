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

/** This interface is implemented by the JSON representations of GameState and PirateGameState. 
 * The methods of this interface are those that are shared by the JSON representations of both the 
 * pirate and non-pirate game state. This allows methods to operate on JSON-originated game state data 
 * regardless of whether it is pirate/non-pirate. */
public interface IJsonSharedGameState {

	public String getState();

	public void setState(String state);

	public int getLifePoints();
	
	public void setLifePoints(int lifePoints);
	
	public int[] getYourFightingCards();
	
	public void setYourFightingCards(int[] yourFightingCards);
		
	public JsonSlowGameState getSlowGameState();
	
	public void setSlowGameState(JsonSlowGameState slowGameState);
	
	public int[] getDiscardFightCards();
	
	public void setDiscardFightCards(int[] discardFightCards);
	
	public Map<Integer, Boolean> getLhsOrRhsFightingCardUsed();
	
	public void setLhsOrRhsFightingCardUsed(Map<Integer, Boolean> lhsOrRhsFightingCardUsed);
	
	public Map<Integer, Boolean> getLhsOrRhsFightingCardDoubled();
	
	public void setLhsOrRhsFightingCardDoubled(Map<Integer, Boolean> lhsOrRhsFightingCardDoubled);
	
	public int[] getLhs_fightCards();
	
	public void setLhs_fightCards(int[] lhs_fightCards);
	
	public int[] getRhs_fightCards();
	
	public void setRhs_fightCards(int[] rhs_fightCards);
	
	public JsonAbilityObject getAbilityObject();
	
	public void setAbilityObject(JsonAbilityObject abilityObject);
}
