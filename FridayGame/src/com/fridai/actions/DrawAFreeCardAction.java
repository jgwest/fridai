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

/** Player action to draw a free card, during the draw free cards phase. */
public class DrawAFreeCardAction extends Action {
	
	public static final DrawAFreeCardAction INSTANCE = new DrawAFreeCardAction(); 

	private DrawAFreeCardAction() {
	}
	
	@Override
	public ActionType getType() {
		return ActionType.DRAW_A_FREE_CARD;
	}

}
