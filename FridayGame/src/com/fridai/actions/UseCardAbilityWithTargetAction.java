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
import com.fridai.ui.ansi.AnsiCards;

/** Many card abilities have targets (eg DESTROY); this card uses a specific card's ability 
 * on a specific target card. */
public class UseCardAbilityWithTargetAction extends Action {

	private final Card card;
	private final Card target;
	
	public UseCardAbilityWithTargetAction(Card card, Card target) {
		this.card = card;
		this.target = target;
	}
	
	@Override
	public ActionType getType() {
		return ActionType.USE_CARD_ABILITY_WITH_TARGET;
	}
	
	@Override
	public String toString() {
		return this.getType().name()+": "+card.getAbility().name()+" to "+AnsiCards.asFightingCard(target, null);
	}

	public Card getCard() {
		return card;
	}

	public Card getTarget() {
		return target;
	}

	
}
