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

/** JSON representation of the overall game state, containing both pirate and non-pirate cards, destroyed cards,
 * and either a game state or pirate game state (depending on which phase the game is in). */
public class JsonGameStatePersistence {

	JsonCard[] cards;

	JsonPirateCard[] pirateCards;

	int[] destroyedCards;
	
	JsonGameState gameState;
	
	JsonPirateGameState pirateGameState;

	public JsonGameStatePersistence() {
	}
	
	public JsonCard[] getCards() {
		return cards;
	}

	public void setCards(JsonCard[] cards) {
		this.cards = cards;
	}

	public JsonPirateCard[] getPirateCards() {
		return pirateCards;
	}

	public void setPirateCards(JsonPirateCard[] pirateCards) {
		this.pirateCards = pirateCards;
	}

	public JsonGameState getGameState() {
		return gameState;
	}

	public void setGameState(JsonGameState gameState) {
		this.gameState = gameState;
	}

	public JsonPirateGameState getPirateGameState() {
		return pirateGameState;
	}
	
	public void setPirateGameState(JsonPirateGameState pirateGameState) {
		this.pirateGameState = pirateGameState;
	}

	public int[] getDestroyedCards() {
		return destroyedCards;
	}
	
	public void setDestroyedCards(int[] destroyedCards) {
		this.destroyedCards = destroyedCards;
	}
}
