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

package com.fridai.actions;

/** Abstract class for all player actions; every action sub-class has a corresponding ActionType, which 
 * can be retrieved using getType(). This is used in preference to 'instanceof' for performance reasons. */
public abstract class Action {

	public static enum ActionType { 
		SELECT_FROM_TWO_HAZARD_CARDS, 
		FIGHT_OR_DISCARD_HAZARD_CARD, 
		DRAW_A_FREE_CARD,
		END_DRAW_FREE_CARDS_PHASE,
		SACRIFICE_LIFE_TO_DRAW, 
		END_SACRIFICE_LIFE_PHASE, 
		DESTROY_CARDS_WITH_PAID_LIFE,
		USE_CARD_ABILITY,
		USE_CARD_ABILITY_WITH_TARGET,
		USE_CARD_ABILITY_SORT,
		END_MULTISTAGE_ABILITY_ACTION,
		USE_COPY_ABILITY, 
		SELECT_A_PIRATE,
		END_PIRATE_ROUND
	};

	protected Action() {
	}
	
	public abstract ActionType getType();
	
	
	@Override
	public String toString() {
		return getType().name();
	}
	
	public String prettyPrint() {
		return toString();
	}
}
