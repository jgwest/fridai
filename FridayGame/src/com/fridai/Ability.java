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

public enum Ability {
	
	// The good
	
	// Copy the special ability of another card (target)
	COPY_1x, 
	
	// (target)
	DESTROY_1x, // done
	
	PHASE_MINUS_1, 
	
	// Move one of the fighting cards to the bottom of the fighting card stack (reshuffle to fighting card stack if empty, first). 
	// If the src card was on the left hand side, draw a replacement in its place (target)
	BELOW_THE_PILE_1x, // done
	
	// Double the fighting points once per fight: you can only double the fighting points of a card once. (target)
	DOUBLE_1x, // done 
	
	// Draw 3 cards: 0 or 1 of those, then place them back on the deck in any order (unique)
	SORT_3_CARDS, // Done
	
	CARDS_DRAW_1(1), CARDS_DRAW_2(2), // Both done.
	
	// Move a fighting card to the discard pile and draw a new card in it's place (target)
	EXCHANGE_X1(1), // Done 
	EXCHANGE_X2(2), // Done 
	
	LIFE_ADD_1(1), LIFE_ADD_2(2),  // Both done.
	DOT_DOT_DOT,

	// The bad
	
	// Stop the drawing of cards on the LHS; on RHS it just has a fighting value of 0.
	// if this card is on the LHS and was destroyed, the player may then choose to draw the remaining cards;
	STOP, 
	LIFE_MINUS_1(-1), LIFE_MINUS_2(-2), HIGHEST_CARD_IS_0;
	
	int magnitude = 0;
	
	Ability() {
		magnitude = 0;
	}
	
	Ability(int magnitude) {
		this.magnitude = magnitude;
	}
	
	public int getMagnitude() {
		return magnitude;
	}
	
	public static Ability create(String value) {
		value = value.trim().toLowerCase();
		switch(value) {
		case "1x copy":
			return Ability.COPY_1x;
		case "1x destroy":
			return Ability.DESTROY_1x;
		case "phase -1":
			return Ability.PHASE_MINUS_1;
		case "1x below the pile":
			return BELOW_THE_PILE_1x;
		case "+1 cards":
			return CARDS_DRAW_1;
		case "+2 cards":
			return CARDS_DRAW_2;
		case "1x exchange":
			return  Ability.EXCHANGE_X1;
		case "2x exchange":
			return Ability.EXCHANGE_X2;
		case "+1 life":
			return Ability.LIFE_ADD_1;
		case "+2 life":
			return Ability.LIFE_ADD_2;
		case "1x double":
			return DOUBLE_1x;
		case "sort 3 cards":
			return SORT_3_CARDS;
		case "...":
			return Ability.DOT_DOT_DOT;
		case "-1 life":
			return Ability.LIFE_MINUS_1;
		case "-2 life":
			return Ability.LIFE_MINUS_2;
		case "stop":
			return Ability.STOP;
		case "highest card = 0":
			return Ability.HIGHEST_CARD_IS_0;
		default:
		}
		
		throw new RuntimeException("Unrecognized value: "+value);
	}
	
}