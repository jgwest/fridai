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

package com.fridai.json;

/** JSON representation of an ability object. */
public class JsonAbilityObject {
	private int stage; // only used by sort

	/** only used by sort, nullable */
	private int[] drawnSortCards;

	private int numberOfCardsDrawn;
	private Integer activeCard;

	private boolean copied;

	public int getStage() {
		return stage;
	}

	public void setStage(int stage) {
		this.stage = stage;
	}

	public int[] getDrawnSortCards() {
		return drawnSortCards;
	}

	public void setDrawnSortCards(int[] drawnSortCards) {
		this.drawnSortCards = drawnSortCards;
	}

	public int getNumberOfCardsDrawn() {
		return numberOfCardsDrawn;
	}

	public void setNumberOfCardsDrawn(int numberOfCardsDrawn) {
		this.numberOfCardsDrawn = numberOfCardsDrawn;
	}

	public Integer getActiveCard() {
		return activeCard;
	}

	public void setActiveCard(Integer activeCard) {
		this.activeCard = activeCard;
	}

	public boolean isCopied() {
		return copied;
	}

	public void setCopied(boolean copied) {
		this.copied = copied;
	}

}
