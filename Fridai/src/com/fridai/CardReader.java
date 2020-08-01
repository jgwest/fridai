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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fridai.Card.Difficulty;
import com.fridai.Card.Type;
import com.fridai.PirateCard.PirateCardAbility;

/** Parse a text InputStream describing all the physical cards, and convert that into Card objects. This is 
 * the principal mechanism for creating Card instances (along with the Card.clone() method). */
public class CardReader {

	private final List<Card> agingCards = new ArrayList<Card>();
	
	private final List<Card> fightingCards = new ArrayList<Card>();
	
	private final List<Card> hazardCards = new ArrayList<Card>();
	
	private final List<PirateCard> pirateCards = new ArrayList<PirateCard>();
	
	public CardReader(InputStream is) throws IOException {
		readFile(is);
	}

	
	private void readFile(InputStream is) throws IOException {
		
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		boolean isParsingPirateCards = false;
		
		Card prototype = null;
		
		int nextPirateCardId = 0;
		
		int nextTraitId = 0;
		int nextPhysicalId = 0;
		
		String str;
		while(null != (str = br.readLine())) {
			
			str = str.trim();
			
			if(str.isEmpty()) { continue; }
			if(str.startsWith("#")) { continue; }
			
			if(str.equals("Hazard cards:")) {
				prototype = new Card(Type.HAZARD);
				prototype.setDifficulty(Difficulty.NONE);
				continue;
				
			} else if(str.equals("Fighting Cards:")) {
				
				prototype = new Card(Type.FIGHTING);
				prototype.setDifficulty(Difficulty.NONE);
				continue;
				
			} else if(str.equals("Aging Cards:")) {
				
				prototype = new Card(Type.AGING);
				continue;
				
			} else if(str.equals("Pirate Cards:")) {
				isParsingPirateCards = true;
				continue;
			}
			
			if(isParsingPirateCards && str.contains(",")) {
				
				List<String> substrings = new ArrayList<>();
				Arrays.asList(str.split(",")).stream().filter(e -> !e.trim().isEmpty()).forEach( e -> {
					substrings.add(e);
				});
				
				PirateCard pc = new PirateCard(nextPirateCardId);
				nextPirateCardId++;
				
				String freeCardsStr = substrings.get(0).trim();
				String hazardValueStr = substrings.get(1).trim();
				String abilityStr = substrings.get(2).trim();
				
				if(freeCardsStr.equals("*")) {
					pc.setFreeCards(-2);
				} else {
					pc.setFreeCards(Integer.parseInt(freeCardsStr));
				}
				
				if(hazardValueStr.equals("*")) {
					pc.setHazardValue(-2);
				} else {
					pc.setHazardValue(Integer.parseInt(hazardValueStr));
				}
				
				pc.setAbility(PirateCardAbility.fromString(abilityStr));
			
				pirateCards.add(pc);
				
				continue;
			}
			
			String afterColon = null;
			if(str.contains(":")) {
				afterColon = str.substring(str.indexOf(":")+1).trim();
			}
			
			if(str.startsWith("fighting-value:")) {
				prototype.setFightingValue(Integer.parseInt(afterColon));
				continue;
			} else if(str.startsWith("hazard-title:")) {
				prototype.setHazardTitle(afterColon);
				continue;
			} else if(str.startsWith("hazard-values:")) {
				List<Integer> l = Arrays.asList(afterColon.split("\\s+")).stream().map(e -> Integer.parseInt(e)).collect(Collectors.toList());
				int[] arr = new int[l.size()];
				for(int index = 0; index < l.size();  index++) {
					arr[index] = l.get(index);
				}
				prototype.setHazardValues(arr);
				continue;
			} else if(str.startsWith("free-cards:")) {
				prototype.setFreeCards(Integer.parseInt(afterColon));
				continue;
			} else if(str.startsWith("difficulty:")) {
				prototype.setDifficulty(Difficulty.createFromString(afterColon));
				continue;
			} else if(str.contains(":")) {
				throw new RuntimeException("Unrecognized string: "+str);
			}
			
			if(!str.contains(",")) {
				throw new RuntimeException("Unrecognized string: "+str);
			}
			
			Card currentCard = prototype.cloneCard();
			
			// Extract quantity
			int quantity = 1;
			{
				int commaIndex = str.indexOf(",");
				int braceIndex = str.indexOf(")");
				if(braceIndex != -1 && braceIndex < commaIndex) {
					quantity = Integer.parseInt(str.substring(0, braceIndex).trim());
					str = str.substring(braceIndex+1).trim();
				}
			}
			
			// Extract title and ability
			{
				List<String> substrings = new ArrayList<>();
				Arrays.asList(str.split(",")).stream().filter(e -> !e.trim().isEmpty()).forEach( e -> {
					substrings.add(e);
				});
				currentCard.setTitle(substrings.get(0));
				currentCard.setAbility(Ability.create(substrings.get(1)));
			}
			
			while(quantity > 0) {
				
				Card c = currentCard.cloneCard();
				c.setTraitId(nextTraitId);
				c.setPhysicalCardId(nextPhysicalId++);
			
				if(currentCard.getType() == Type.AGING) {
					agingCards.add(c);
				} else if(currentCard.getType() == Type.FIGHTING) {
					fightingCards.add(c);
				} else if(currentCard.getType() == Type.HAZARD) {
					hazardCards.add(c);
				} else {
					throw new RuntimeException("Unrecognize card type: "+c);
				}
				
				quantity--;
			}
			
			nextTraitId++;
			
		}
		
		
		br.close();
	}


	public List<Card> getAgingCards() {
		return agingCards;
	}


	public List<Card> getFightingCards() {
		return fightingCards;
	}


	public List<Card> getHazardCards() {
		return hazardCards;
	}


	public List<PirateCard> getPirateCards() {
		return pirateCards;
	}

}
