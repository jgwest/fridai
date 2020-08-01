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

import java.util.Arrays;

import com.fridai.json.JsonSlowGameState;
import com.fridai.util.FridayUtil;
import com.fridai.util.ImmutableGrowableListCards;
import com.fridai.util.ListCards;

/** This class is an optimization to reduce memory usage and improve performance. This class 
 * includes any objects which would otherwise be part of GameState, but that don't change often
 * enough during a game to make them worth cloning for every GameState change. 
 * 
 * For example, the array of active pirates are fixed through the lifetime of the game, and so 
 * is the gameLevel. The phase number changes once every 60 or so rounds. Thus including any of these values 
 * as part of GameState would require additional memory for each GameState object, increase GC, and require 
 * additional object allocations per GameState object.
 * 
 * By shifting infrequently changing objects to this class (rather than storing them in GameState), a significant 
 * amount of memory (and to a lesser extent performance) is saved. */
public final class SlowGameState {

	// Nullable
	private final ImmutableGrowableListCards activeRoundCards;
	
	private final ListCards agingCards;

	private final PirateCard[] activePirates;

	private final int phaseNumber;
	
	private final int gameLevel;
	
	/* Nullable - this will only be not-null if the wild card pirate is one of the activePirates, and the game state
	 * is a pirate game state */
	private final PirateCardInfo wildCardPirate;

	public SlowGameState(ListCards agingCards, int gameLevel, PirateCard[] activePirates,
			ImmutableGrowableListCards activeRoundCards, int phaseNumber, PirateCardInfo wildCardPirate) {
		
		if(FridayUtil.RUNTIME_CHECK) {
			if(!(gameLevel >= 1 && gameLevel <= 4) ) {
				FridayUtil.throwErr("Invalid game level: "+gameLevel);
			}
		}
		
		this.agingCards = agingCards;
		this.activePirates = activePirates;
		this.phaseNumber = phaseNumber;
		this.activeRoundCards = activeRoundCards;
		this.gameLevel = gameLevel;
		this.wildCardPirate = wildCardPirate;
	}

	public int getPhaseNumber() {
		return phaseNumber;
	}

	public ListCards getAgingCards() {
		return agingCards;
	}

	public PirateCard[] getActivePirates() {
		return activePirates;
	}

	public ImmutableGrowableListCards getActiveRoundCards() {
		return activeRoundCards;
	}
	
	public int getGameLevel() {
		return gameLevel;
	}
	
	public JsonSlowGameState toJson() {
		JsonSlowGameState result = new JsonSlowGameState();

		int[] activePiratesArray = new int[activePirates.length];
		for(int x = 0; x < activePiratesArray.length; x++) {
			activePiratesArray[x] = activePirates[x].getPirateCardId();
		}
		
		result.setActivePirates(activePiratesArray);
		
		if(activeRoundCards != null) {
			result.setActiveRoundCards(activeRoundCards.asIntArray());
		} else {
			result.setActiveRoundCards(null);
		}
		
		result.setAgingCards(agingCards.asIntArray());
		result.setPhaseNumber(getPhaseNumber());
		
		result.setGameLevel(getGameLevel());
		
		if(wildCardPirate != null) {
			result.setWildCardPirate(wildCardPirate.toJson());
		} else {
			result.setWildCardPirate(null);
		}
		
		return result;
	}
	
	public PirateCardInfo getWildCardPirate() {
		return wildCardPirate;
	}
	
	public boolean equalByContents(SlowGameState other) {
		
		// Active round cards
		{
			ImmutableGrowableListCards arcOne = getActiveRoundCards();
			ImmutableGrowableListCards arcTwo = other.getActiveRoundCards();
			if(!FridayUtil.bothAreNull(arcOne, arcTwo)) {
				
				if(FridayUtil.isXorNull(arcOne, arcTwo)) {
					return false;
				} else if(!FridayUtil.listsEqual(arcOne.getAsList(), arcTwo.getAsList())){
					return false;
				}
				
			}
		}

		// Pirate card info
		{
			PirateCardInfo one = getWildCardPirate();
			PirateCardInfo two = other.getWildCardPirate();
			if(!FridayUtil.bothAreNull(one, two)) {
			
				if(FridayUtil.isXorNull(one, two)) {
					return false;
				} else if(!one.equals(two)) {
					return false;
				}					
			}
		}
		
		// Aging cards
		if(!FridayUtil.listsEqual(getAgingCards().asList(), other.getAgingCards().asList())) {
			return false;
		}
		
		// Active pirates
		if(!FridayUtil.pirateListsEqual(Arrays.asList(this.getActivePirates()), Arrays.asList(other.getActivePirates()))) {
			return false;
		}
		
		// Phase
		if(this.getPhaseNumber() != other.getPhaseNumber()) {
			return false;
		}
		
		if(this.getGameLevel() != other.getGameLevel()) {
			return false;
		}
		
		return true;
		
	}
}
