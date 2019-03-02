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

/** 
 * This is a simple typed representation of either a GameState or a PirateGameState.
 * 
 * If a method may return either a GameState or a PirateGameState, then this object
 * is used as that method's return value. */
public final class GameStateContainer {

	private final boolean isGameState;
	
	private final GameState gameState;
	
	private final PirateGameState pirateGameState;
	
	public GameStateContainer(PirateGameState pgs ) {
		this.pirateGameState = pgs;
		this.gameState = null;
		this.isGameState = false;
	}
	
	public GameStateContainer(GameState gs) {
		this.gameState = gs;
		this.pirateGameState = null;
		this.isGameState = true;
	}
	
	public final boolean isGameState() {
		return isGameState;
	}
	
	public final PirateGameState getPirateGameState() {
		return pirateGameState;
	}

	public final GameState getGameState() {
		return gameState;
	}
	
}
