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

import com.fridai.PirateCard;

/** Player action to select a pirate card to fight, during the pirate fighting phase. */
public class SelectAPirateAction extends Action {

	private final PirateCard card;
	
	public SelectAPirateAction(PirateCard card) {
		this.card = card;
	}
	
	@Override
	public ActionType getType() {
		return ActionType.SELECT_A_PIRATE;
	}

	
	public PirateCard getCard() {
		return card;
	}
	
}
