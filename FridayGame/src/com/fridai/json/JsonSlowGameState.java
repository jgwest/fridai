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

/** JSON representation of the "slow" game state, for serialization/deserialization by Jackson. 
 * See SlowGameState for definition of "slow" as a performance optimization. */
public class JsonSlowGameState {

	private int[] activeRoundCards;

	private int[] agingCards;

	private int[] activePirates;

	private int phaseNumber;
	
	private int gameLevel;
	
	private JsonPirateCardInfo wildCardPirate;

	public int[] getActiveRoundCards() {
		return activeRoundCards;
	}

	public void setActiveRoundCards(int[] activeRoundCards) {
		this.activeRoundCards = activeRoundCards;
	}

	public int[] getAgingCards() {
		return agingCards;
	}

	public void setAgingCards(int[] agingCards) {
		this.agingCards = agingCards;
	}

	public int[] getActivePirates() {
		return activePirates;
	}

	public void setActivePirates(int[] activePirates) {
		this.activePirates = activePirates;
	}

	public int getPhaseNumber() {
		return phaseNumber;
	}

	public void setPhaseNumber(int phaseNumber) {
		this.phaseNumber = phaseNumber;
	}
	
	public void setGameLevel(int gameLevel) {
		this.gameLevel = gameLevel;
	}
	
	public int getGameLevel() {
		return gameLevel;
	}
	
	public JsonPirateCardInfo getWildCardPirate() {
		return wildCardPirate;
	}
	
	public void setWildCardPirate(JsonPirateCardInfo wildCardPirate) {
		this.wildCardPirate = wildCardPirate;
	}

}
