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

import com.fridai.Card;

/** Use a Copy ability card on another specified target card. */
public class UseCopyAbilityAction extends Action {

	private final Card card;
	private final Action containedAction;
	
	public UseCopyAbilityAction(Card card, Action containedAction) {
		this.containedAction = containedAction;
		this.card = card;
	}
	
	public Action getContainedAction() {
		return containedAction;
	}
	
	public Card getCard() {
		return card;
	}
	
	@Override
	public ActionType getType() {
		return ActionType.USE_COPY_ABILITY;
	}

	@Override
	public String toString() {
		return getType().name()+": "+containedAction;
	}
}
