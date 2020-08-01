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

/** Player action: select one of the two given hazard cards. Only two instances of this object will
 * ever exist, and they are declared as static here. */
public final class SelectFromTwoHazardCardsAction extends Action {
	
	public static final SelectFromTwoHazardCardsAction INSTANCE_ZERO = new SelectFromTwoHazardCardsAction(0);
	
	public static final SelectFromTwoHazardCardsAction INSTANCE_ONE = new SelectFromTwoHazardCardsAction(1);
	
	private final int index; 
		
	private SelectFromTwoHazardCardsAction(int index) {
		this.index = index;
	}
	
	@Override
	public final ActionType getType() {
		return ActionType.SELECT_FROM_TWO_HAZARD_CARDS;
	}

	public int getIndex() {
		return index;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+" index: "+index;
	}
}
