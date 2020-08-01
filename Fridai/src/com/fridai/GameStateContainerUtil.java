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

/** Simple utility method that operates on GameStateContainer regardless of whether it contains
 * a GameState or PirateGameState. */
public class GameStateContainerUtil {

	public static int getPhase(GameStateContainer gsc) {
		int phaseScore = gsc.isGameState() ? gsc.getGameState().getPhaseNumber()
				: gsc.getPirateGameState().getPhaseNumber();

		return phaseScore;

	}
	
	public static int getLifePoints(GameStateContainer gsc) {
		
		int life = gsc.isGameState() ? gsc.getGameState().getLifePoints() : gsc.getPirateGameState().getLifePoints();

		return life;
		
	}
}
