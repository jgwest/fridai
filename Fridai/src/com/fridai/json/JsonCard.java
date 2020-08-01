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

/** A JSON representation of an individual physical game card. */
public class JsonCard {

	private int fightingValue = Integer.MIN_VALUE;
	private int[] hazardValues = null;
	private int freeCards = Integer.MIN_VALUE;

	private String hazardTitle = null;

	private String title = null;

	private String ability = null;

	private String difficulty = null;

	private String type;

	private int ratingSimple = 0;

	private int physicalCardId;
	
	public int getFightingValue() {
		return fightingValue;
	}

	public void setFightingValue(int fightingValue) {
		this.fightingValue = fightingValue;
	}

	public int[] getHazardValues() {
		return hazardValues;
	}

	public void setHazardValues(int[] hazardValues) {
		this.hazardValues = hazardValues;
	}

	public int getFreeCards() {
		return freeCards;
	}

	public void setFreeCards(int freeCards) {
		this.freeCards = freeCards;
	}

	public String getHazardTitle() {
		return hazardTitle;
	}

	public void setHazardTitle(String hazardTitle) {
		this.hazardTitle = hazardTitle;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAbility() {
		return ability;
	}

	public void setAbility(String ability) {
		this.ability = ability;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(String difficulty) {
		this.difficulty = difficulty;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getRatingSimple() {
		return ratingSimple;
	}

	public void setRatingSimple(int ratingSimple) {
		this.ratingSimple = ratingSimple;
	}

	public int getPhysicalCardId() {
		return physicalCardId;
	}

	public void setPhysicalCardId(int physicalCardId) {
		this.physicalCardId = physicalCardId;
	}

	
}
