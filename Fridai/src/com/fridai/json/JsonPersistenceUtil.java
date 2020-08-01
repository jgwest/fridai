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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fridai.AbilityObject;
import com.fridai.Card;
import com.fridai.GameState;
import com.fridai.GameState.State;
import com.fridai.PirateGameState.PirateState;
import com.fridai.GameStateContainer;
import com.fridai.PirateCard;
import com.fridai.PirateCardInfo;
import com.fridai.PirateGameState;
import com.fridai.RuntimeObject;
import com.fridai.RuntimeObjectMap;
import com.fridai.SlowGameState;
import com.fridai.util.FridayUtil;
import com.fridai.util.ImmutableGrowableListCards;
import com.fridai.util.ListCards;
import com.fridai.util.MapCards;

/** Utility methods for converting the game state to and from JSON. */
public class JsonPersistenceUtil {

	public static GameStateContainer fromJson(JsonGameStatePersistence jgsp) {
	
		JsonTranslation translation = new JsonTranslation();
		
		Map<Integer /* json physical card id */, Card> jsonIdToRealIdMap = translation._jsonIdToRealCardMap;

		// Create mapping from json id to current state 
		{
			List<Card> cardList = new ArrayList<Card>(getAllFightingAndHazardCardsAsMap().values());
	
			for(JsonCard jsonCard : jgsp.getCards()) {
				
				boolean match = false;
				for(Iterator<Card> it = cardList.iterator(); it.hasNext(); ) {
					Card nonJsonCard = it.next();
					
					if(nonJsonCard.equalsJson(jsonCard)) {
						jsonIdToRealIdMap.put(jsonCard.getPhysicalCardId(), nonJsonCard);
						match = true;
						it.remove();
						break;
					}
					
				}
				
				if(!match) {					
					FridayUtil.throwErr("Unable to find match for JSON card "+jsonCard.getPhysicalCardId());
				}
				
			}
			
			if(cardList.size() > 0) {
				FridayUtil.throwErr("Card list was not empty at end of conversion: "+cardList.size());
			}
		}
		
		
		Map<Integer /*json pirate card id*/, PirateCard> jsonPirateIdToReadIdMap = translation._jsonPirateIdToReadIdMap;
		{
			List<PirateCard> cardList = new ArrayList<PirateCard>(getAllPirateCardsAsMap().values());
			
			for(JsonPirateCard jsonPirateCard : jgsp.getPirateCards()) {
				
				boolean match = false;
				
				for(Iterator<PirateCard> it = cardList.iterator(); it.hasNext(); ) {
					PirateCard nonJsonPirateCard = it.next();
					
					if(nonJsonPirateCard.equalsJson(jsonPirateCard)) {
						jsonPirateIdToReadIdMap.put(jsonPirateCard.getPirateCardId(), nonJsonPirateCard);
						match = true;
						it.remove();
						break;
						
					}
				}
				
				if(!match) {
					FridayUtil.throwErr("Unable to find match for JSON pirate card "+jsonPirateCard.getPirateCardId());
				}
				
			}
			
			if(cardList.size() > 0) {
				FridayUtil.throwErr("Card list was not empty at end of conversion: "+cardList.size());
			}

		}
		
		IJsonSharedGameState jgs;
		AbilityObject abilityObject = null;
		
		if(jgsp.getGameState() != null) {
			jgs = jgsp.getGameState();
		} else {
			jgs = jgsp.getPirateGameState();
		}
		
		if(jgs.getAbilityObject() != null) {
			List<Card> drawnSortCards = null;
			JsonAbilityObject json =  jgs.getAbilityObject();
			
			if(json.getDrawnSortCards() != null) {
				drawnSortCards = cardsAsList(json.getDrawnSortCards(), translation);
			}
			
			abilityObject = new AbilityObject(
					card(json.getActiveCard(), translation), json.getNumberOfCardsDrawn(), json.getStage(), drawnSortCards, json.isCopied());
		}

		PirateCard activePirateCard = null;
		
		Card activeHazardCard = null;
		ImmutableGrowableListCards discardHazards = null;
		ListCards hazardCards  = null;
		if(jgsp.getGameState() != null) {
			JsonGameState gameState = jgsp.getGameState();
			
			if(gameState.getActiveHazardCard() != null) {
				activeHazardCard = card(gameState.getActiveHazardCard(), translation);
			}

			discardHazards = GameState.defaultDiscardHazards();
			discardHazards = discardHazards.mutateAddAll(cardsAsList(gameState.getDiscardHazards(), translation));
			
			hazardCards = new ListCards(cardsAsArray(gameState.getHazardCards(), translation), 0);
			
		} else {
			
			JsonPirateGameState pirateGameState = jgsp.getPirateGameState();
			
			if(pirateGameState.getActivePirateCard() != null) {
				activePirateCard = jsonPirateIdToReadIdMap.get(pirateGameState.getActivePirateCard());
				
				if(activePirateCard == null)  { FridayUtil.throwErr("Unable to locate active pirate card by id "+pirateGameState.getActivePirateCard()); }
			}						
		}

		ImmutableGrowableListCards discardFightCards = GameState.defaultFightingDiscard();
		discardFightCards = discardFightCards.mutateAddAll(cardsAsList(jgs.getDiscardFightCards(), translation));
		
		
		ImmutableGrowableListCards lhs_fightCards = GameState.defaultLhsFightCards();
		lhs_fightCards = lhs_fightCards.mutateAddAll(cardsAsList(jgs.getLhs_fightCards(), translation));

		ImmutableGrowableListCards rhs_fightCards = GameState.defaultRhsFightCards();
		rhs_fightCards = rhs_fightCards.mutateAddAll(cardsAsList(jgs.getRhs_fightCards(), translation));

		
		MapCards/*<Integer - physical card id, Boolean>*/ lhsOrRhsFightingCardUsed = null;		
		if(jgs.getLhsOrRhsFightingCardUsed() != null) { 
			lhsOrRhsFightingCardUsed = new MapCards();
			for(Map.Entry<Integer, Boolean> e : jgs.getLhsOrRhsFightingCardUsed().entrySet()) {
				lhsOrRhsFightingCardUsed.put(card(e.getKey(), translation).getPhysicalCardId(), e.getValue());
			}
		}
		
		MapCards /*<Integer - physical card id, Boolean>*/ lhsOrRhsFightingCardDoubled = null;		
		if(jgs.getLhsOrRhsFightingCardDoubled() != null) {
			lhsOrRhsFightingCardDoubled = new MapCards();
			
			for(Map.Entry<Integer, Boolean> e : jgs.getLhsOrRhsFightingCardDoubled().entrySet()) {
				lhsOrRhsFightingCardDoubled.put(card(e.getKey(), translation).getPhysicalCardId(), e.getValue());
			}
		}

		int lifePoints = jgs.getLifePoints();
		
		SlowGameState slowGameState;
		{
			JsonSlowGameState jsgs = jgs.getSlowGameState();
			
			PirateCard[] activePirates;
			{
				List<PirateCard> activePiratesList = pirateCardsAsList(jsgs.getActivePirates(), translation);
				activePirates = activePiratesList.toArray(new PirateCard[activePiratesList.size()]);
			}
			
			ImmutableGrowableListCards activeRoundCards = null;
			if(jsgs.getActiveRoundCards() != null) {
				activeRoundCards = new ImmutableGrowableListCards(jsgs.getActiveRoundCards().length);
				
				for(Card c : cardsAsList(jsgs.getActiveRoundCards(), translation) ) {
					activeRoundCards = activeRoundCards.mutateAdd_allowDuplicates(c);
				}
				
//				activeRoundCards = activeRoundCards.mutateAddAll();
			}
			
			ListCards agingCards = new ListCards(cardsAsArray(jsgs.getAgingCards(), translation), 0);
			
			int phaseNumber = jsgs.getPhaseNumber();
			
			PirateCardInfo wildCardPirate = jsgs.getWildCardPirate() != null 
					? new PirateCardInfo(jsgs.getWildCardPirate()) : null;
			
			slowGameState = new SlowGameState(agingCards, jsgs.getGameLevel(), activePirates, activeRoundCards, 
					phaseNumber, wildCardPirate);
			
		}
		

		RuntimeObject ro = new RuntimeObject();
		if(jgsp.getDestroyedCards() != null) {
			for(int destroyedCardId : jgsp.getDestroyedCards()) {
				Card c = jsonIdToRealIdMap.get(destroyedCardId);
				if(c == null) { FridayUtil.throwErr("Unable to find card with id: "+destroyedCardId); }
				ro = ro.mutateAddDestroyedCard(c);
			}
		}
		
		ListCards yourFightingCards = new ListCards(cardsAsArray(jgs.getYourFightingCards(), translation), 0);
		
		if(jgsp.getGameState() != null) {
			State state = State.valueOf(jgs.getState());
			if(state == null) { FridayUtil.throwErr("State not found."); }
			
			GameState result = new GameState(state, yourFightingCards, hazardCards, discardHazards, 
					slowGameState, activeHazardCard, discardFightCards, lhsOrRhsFightingCardUsed, 
					lifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled, abilityObject, 
					null, ro);
			
			
			return new GameStateContainer(result);
		} else {
			PirateState state = PirateState.valueOf(jgs.getState());
			if(state == null) { FridayUtil.throwErr("State not found."); }
			
			PirateCardInfo pic = null;
			JsonPirateCardInfo jpci = jgsp.getPirateGameState().getPirateCardInfo();
			if(jpci != null) {
				pic = new PirateCardInfo(jpci);
			}
						
			PirateGameState result = new PirateGameState(state, yourFightingCards, slowGameState, activePirateCard,
					pic, discardFightCards, lhsOrRhsFightingCardUsed, lifePoints, lhs_fightCards, rhs_fightCards, 
					lhsOrRhsFightingCardDoubled, abilityObject, null, ro);
			
			return new GameStateContainer(result);
			
		}
		
		
		
	}
	
	private static Card[] cardsAsArray(int[] jsonCardIds, JsonTranslation jt) {
		List<Card> result =  cardsAsList(jsonCardIds, jt);
		
		return result.toArray(new Card[result.size()]);
		
	}
	
	private static List<Card> cardsAsList(int[] jsonCardIds, JsonTranslation jt) {
		
		List<Card> result = new ArrayList<>();
		
		if(jsonCardIds == null) { return result; } 
		
		for(int jsonCardId : jsonCardIds) {
			Card c = card(jsonCardId, jt); //jt.jsonIdToRealCardMap.get(jsonCardId);
//			if(c == null) { FridayUtil.throwErr("Could not find matching jsonCardId: "+jsonCardId); }
			result.add(c);
		}
		
		return result;
	}
	
	
	private static List<PirateCard> pirateCardsAsList(int[] jsonPirateCardIds, JsonTranslation jt) {
		
		List<PirateCard> result = new ArrayList<>();
		
		if(jsonPirateCardIds == null) { return result; } 
		
		for(int jsonPirateCardId : jsonPirateCardIds ) {
			PirateCard pc = jt.getPirateCard(jsonPirateCardId);
			result.add(pc);
		}
		
		return result;
		
	}
	
	private static Card card(int jsonCardId, JsonTranslation jt) {
		
		return jt.getCard(jsonCardId);
		
	}
	
	/** A mapping from the IDs in the JSON objects, to the corresponding non-JSON Card and PirateCard objects. */
	private static class JsonTranslation {
		private final Map<Integer, Card> _jsonIdToRealCardMap = new HashMap<>();
		private final Map<Integer, PirateCard> _jsonPirateIdToReadIdMap = new HashMap<>();
		
		public Card getCard(int jsonId) {
			Card result = _jsonIdToRealCardMap.get(jsonId);
			if(result == null) {
				FridayUtil.throwErr("Could not find json card with id: "+jsonId);
			}
			return result;
		}
		
		public PirateCard getPirateCard(int pirateJsonId) {
			PirateCard result = _jsonPirateIdToReadIdMap.get(pirateJsonId);
			
			if(result == null) {
				FridayUtil.throwErr("Could not find json pirate card with id: "+pirateJsonId);
			}

			return result;
		}
		
	}
	
	
	public static JsonGameStatePersistence toJson(PirateGameState gs) {
		JsonGameStatePersistence result = new JsonGameStatePersistence();
	
		// Create cards
		{
			JsonCard[] cards;

			Map<Integer, Card> cardMap = getAllFightingAndHazardCardsAsMap();
			
			cards = new JsonCard[cardMap.size()];
			
			int index = 0;
			for(Card c : cardMap.values()) {
				cards[index] = c.toJson(); 
				index++;
			}
			result.setCards(cards);
		}

		// Create pirate cards
		{
			JsonPirateCard[] pirateCards;
			Map<Integer, PirateCard> pirateMap = getAllPirateCardsAsMap(); 
			
			pirateCards = new JsonPirateCard[pirateMap.size()];
			
			int index = 0;
			for(PirateCard pc : pirateMap.values()) {
				pirateCards[index] = pc.toJson();
				index++;
			}
			result.setPirateCards(pirateCards);
		}
		
		// Create destroyed cards (optional)
		if(FridayUtil.RUNTIME_CHECK) { // Destroyed cards are only logged by runtime_check
			
			RuntimeObject ro = RuntimeObjectMap.getInstance().get(gs);

			int[] destroyedCards = new int[ro.getDestroyedCards().size()];
			
			int index = 0;
			for(Card c : ro.getDestroyedCards()) {
				destroyedCards[index] = c.getPhysicalCardId();
				index++;
			}
			
			result.setDestroyedCards(destroyedCards);
		}
		
		result.setPirateGameState(gs.toJson());
		
		return result;
		
	}

	
	public static JsonGameStatePersistence toJson(GameState gs) {
		JsonGameStatePersistence result = new JsonGameStatePersistence();
			
		// Create cards
		{
			JsonCard[] cards;

			Map<Integer, Card> cardMap = getAllFightingAndHazardCardsAsMap();			
			cards = new JsonCard[cardMap.size()];
			
			int index = 0;
			for(Card c : cardMap.values()) {
				cards[index] = c.toJson(); 
				index++;
			}
			result.setCards(cards);
		}

		// Create pirate cards
		{
			JsonPirateCard[] pirateCards;
			Map<Integer, PirateCard> pirateMap = getAllPirateCardsAsMap(); 			
			pirateCards = new JsonPirateCard[pirateMap.size()];
			
			int index = 0;
			for(PirateCard pc : pirateMap.values()) {
				pirateCards[index] = pc.toJson();
				index++;
			}
			result.setPirateCards(pirateCards);
		}
		
		// Create destroyed cards (optional)
		if(FridayUtil.RUNTIME_CHECK) { // Destroyed cards are only logged by runtime_check
			
			RuntimeObject ro = RuntimeObjectMap.getInstance().get(gs);

			int[] destroyedCards = new int[ro.getDestroyedCards().size()];
			
			int index = 0;
			for(Card c : ro.getDestroyedCards()) {
				destroyedCards[index] = c.getPhysicalCardId();
				index++;
			}
			
			result.setDestroyedCards(destroyedCards);
		}
		
		result.setGameState(gs.toJson());
		
		return result;
		
	}
	
	private static Map<Integer, Card> getAllFightingAndHazardCardsAsMap() {
		Map<Integer, Card> cardMap = new HashMap<>();
		FridayUtil.ALL_CARDS.getFightingCards().forEach( e -> { addToCardMap(cardMap, e); });
		FridayUtil.ALL_CARDS.getHazardCards().forEach( e -> { addToCardMap(cardMap, e); });

		FridayUtil.ALL_CARDS.getAgingCards().forEach( e -> { addToCardMap(cardMap, e); });
		
		return cardMap;
	}
	
	private static Map<Integer, PirateCard> getAllPirateCardsAsMap() {
		Map<Integer, PirateCard> pirateMap = new HashMap<>();
		FridayUtil.ALL_CARDS.getPirateCards().forEach( e -> {
			addToPirateCardMap(pirateMap, e);
		});
		return pirateMap;
	}
	
	
	private static void addToPirateCardMap(Map<Integer, PirateCard> pirateCardMap, PirateCard c) {
		PirateCard cardFromMap = pirateCardMap.get(c.getPirateCardId());
		if(cardFromMap != null) {
			FridayUtil.throwErr("Duplicate card found in pirate map: "+c);
		}
	
		pirateCardMap.put(c.getPirateCardId(), c);
		
	}
	
	private static void addToCardMap(Map<Integer,Card> cardMap, Card c) {
		
		Card cardFromMap = cardMap.get(c.getPhysicalCardId());
		if(cardFromMap != null) {
			FridayUtil.throwErr("Duplicate card found in map: "+c);
		}
	
		cardMap.put(c.getPhysicalCardId(), c);
		
	}
}
