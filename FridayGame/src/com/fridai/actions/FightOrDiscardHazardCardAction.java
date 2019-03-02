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

/** With this action, the player has drawn a single hazard card from the hazard deck (rather than the usual two)
 * and the player has the option of either fighting that single card or discarding it. */
public class FightOrDiscardHazardCardAction extends Action {
	
	final boolean fight;
	
	public static final FightOrDiscardHazardCardAction INSTANCE_FIGHT = new FightOrDiscardHazardCardAction(true);
	public static final FightOrDiscardHazardCardAction INSTANCE_DISCARD = new FightOrDiscardHazardCardAction(false);
	
	private FightOrDiscardHazardCardAction(boolean fight) {
		this.fight = fight;
	}
	
	@Override
	public ActionType getType() {
		return ActionType.FIGHT_OR_DISCARD_HAZARD_CARD;
	}

	public boolean isFight() {
		return fight;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+" fight: "+fight;
	}
}
