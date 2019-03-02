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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fusesource.jansi.Ansi.*;

import com.fridai.actions.ActionResponse;
import com.fridai.actions.UseCardAbilityAction;
import com.fridai.actions.UseCardAbilitySortAction;
import com.fridai.actions.UseCardAbilityWithTargetAction;
import com.fridai.actions.UseCopyAbilityAction;
import com.fridai.json.JsonGameState;
import com.fridai.ui.ansi.AnsiCards;
import com.fridai.util.FridayUtil;
import com.fridai.util.ImmutableGrowableListCards;
import com.fridai.util.ListCards;
import com.fridai.util.MapCards;

/** An encapsulation of the full state of the game after each player action (each 'round'), except when
 * the player is fighting pirates -- the pirate phase is handled by PirateGameState. 
 * 
 * An instance of this class is immutable, and methods are called on this instance based on player actions.
 * A player action causes a new GameState to be created (by this class) that represents how the game has changed
 * after that player action. */
public final class GameState {

	public enum State {
		SELECT_A_HAZARD_CARD,
		DRAW_FREE_CARDS,
		SACRIFICE_LIFE_TO_DRAW,
		PAY_LIFE_POINTS_ON_HAZARD_MISS
	};

	private final State state;

	private final int lifePoints;

	private final ListCards yourFightingCards;

	private final ListCards hazardCards;

	private final ImmutableGrowableListCards discardHazards;

	private final SlowGameState slowGameState;
	
	private final ImmutableGrowableListCards discardFightCards;

	// Nullable
	private final MapCards/*<Integer - physical card id, Boolean>*/ lhsOrRhsFightingCardUsed;
	
	// Nullable
	private final MapCards/*<Integer - physical card id, Boolean>*/ lhsOrRhsFightingCardDoubled;
	
	// ------------------------
	
	private final ImmutableGrowableListCards lhs_fightCards;
	private final ImmutableGrowableListCards rhs_fightCards;

	// Nullable
	private final Card activeHazardCard;

	// Nullable
	private final AbilityObject abilityObject;
	
	// ------------------------
		
	public GameState(State state, ListCards yourFightingCards, ListCards hazardCards, ImmutableGrowableListCards discardHazards, 
			SlowGameState slowGameState, Card activeHazardCard, ImmutableGrowableListCards discardFightCards, 
			MapCards lhsOrRhsFightingCardUsed, int lifePoints, ImmutableGrowableListCards lhs_fightCards,
			ImmutableGrowableListCards rhs_fightCards, MapCards lhsOrRhsFightingCardDoubled, 
			AbilityObject abilityObject, GameState previousGameState, RuntimeObject newRuntimeObject) {
		
		this.state = state;

		if(FridayUtil.RUNTIME_CHECK) {
			if(this.state == State.SELECT_A_HAZARD_CARD && abilityObject != null) {
				FridayUtil.throwErr("AbilityObject should not be defined here.");
			}
		}
		
		if(FridayUtil.RUNTIME_CHECK) {
			if(previousGameState != null && newRuntimeObject == null) {
				RuntimeObject previousRuntimeObj = RuntimeObjectMap.getInstance().get(previousGameState);
				RuntimeObjectMap.getInstance().put(this, previousRuntimeObj);
			} 
		}

		this.yourFightingCards = yourFightingCards;
		this.hazardCards = hazardCards;
		this.discardHazards = discardHazards;
		this.discardFightCards = discardFightCards;
		this.slowGameState = slowGameState;
		this.lifePoints = (FridayUtil.ALLOW_LARGE_LIFE_POINTS ?  lifePoints : (lifePoints > 22) ? 22 : lifePoints);
		this.activeHazardCard = activeHazardCard;
		this.lhs_fightCards = lhs_fightCards;
		this.rhs_fightCards = rhs_fightCards;
		
		if(FridayUtil.RUNTIME_CHECK) {
			// TODO: LOWER - Runtime check on unmodifiable map on lhs/rhs used/doubled is currently disabled.
//			if(lhsOrRhsFightingCardUsed != null) {
//				Map<Integer, Boolean> newMap = new HashMap<>(lhsOrRhsFightingCardUsed);
//				this.lhsOrRhsFightingCardUsed = Collections.unmodifiableMap(newMap);
//			} else {
				this.lhsOrRhsFightingCardUsed = lhsOrRhsFightingCardUsed;	
//			}
			
//			if(lhsOrRhsFightingCardDoubled != null) {
//				Map<Integer, Boolean> newMap = new HashMap<>(lhsOrRhsFightingCardDoubled);
//				this.lhsOrRhsFightingCardDoubled = Collections.unmodifiableMap(newMap);
//			} else {
				this.lhsOrRhsFightingCardDoubled = lhsOrRhsFightingCardDoubled;	
//			}
		} else {
			this.lhsOrRhsFightingCardUsed = lhsOrRhsFightingCardUsed;
			this.lhsOrRhsFightingCardDoubled = lhsOrRhsFightingCardDoubled;			
		}
		
		this.abilityObject = abilityObject;
		
		if(FridayUtil.RUNTIME_CHECK) {
			
			if(newRuntimeObject != null) {
				RuntimeObjectMap.getInstance().put(this, newRuntimeObject);
			}
			
			validate(this, newRuntimeObject, previousGameState);
		}
	}
	
	
	public static void validate(GameState gs, RuntimeObject newRuntimeObject, GameState previousGameState) {
		if(!FridayUtil.RUNTIME_CHECK) { return; }
		
		RuntimeObject runtimeObject = newRuntimeObject != null ? newRuntimeObject : RuntimeObjectMap.getInstance().get(gs);
		
		runtimeCheckAllCardsPresent(gs.yourFightingCards, gs.hazardCards, gs.discardHazards, 
				gs.slowGameState, gs.activeHazardCard, gs.discardFightCards, gs.lhs_fightCards,
				gs.rhs_fightCards, gs.abilityObject, runtimeObject, false);
		
		if(gs.state == State.DRAW_FREE_CARDS && gs.activeHazardCard == null) {
			FridayUtil.throwErr("Bad state: active hazard is null in draw_free_cards");
		}
		
		if(gs.state == State.DRAW_FREE_CARDS && !gs.calculateCanDrawXFightingCards(1)) {
			FridayUtil.throwErr("Bad state: Can't be in DRAW_FREE_CARDS if drawing isn't possible");
		}
		
		if(gs.state == State.SELECT_A_HAZARD_CARD &&  (gs.lhs_fightCards.size() > 0 || gs.rhs_fightCards.size() > 0) ) {
			FridayUtil.throwErr("Bad state - lhs and rhs are > 0");
		}
		

		// Look for duplicate cards by physical card id
		
		HashMap<Integer, Boolean> cardsSeen = new HashMap<>();
		
		List<Object> cardsFromAllSources = new ArrayList<>();
		cardsFromAllSources.add("yfs");
		cardsFromAllSources.addAll(gs.yourFightingCards.asList());
		cardsFromAllSources.add("hc");
		cardsFromAllSources.addAll(gs.hazardCards.asList());
		cardsFromAllSources.add("dh");
		cardsFromAllSources.addAll(gs.discardHazards.getAsList());
		if(gs.activeHazardCard != null) {
			cardsFromAllSources.add("ahc");
			cardsFromAllSources.add(gs.activeHazardCard);
		}
		cardsFromAllSources.add("dfc");
		cardsFromAllSources.addAll(gs.discardFightCards.getAsList());
		cardsFromAllSources.add("lhs");
		cardsFromAllSources.addAll(gs.lhs_fightCards.getAsList());
		cardsFromAllSources.add("rhs");
		cardsFromAllSources.addAll(gs.rhs_fightCards.getAsList());
		cardsFromAllSources.add("ao");
		if(gs.abilityObject != null && gs.abilityObject.getDrawnSortCards() != null) {
			cardsFromAllSources.addAll(gs.abilityObject.getDrawnSortCards());
		}
		
		if(runtimeObject != null && runtimeObject.getDestroyedCards() != null) {
			
			cardsFromAllSources.add("destroyed");
			cardsFromAllSources.addAll(runtimeObject.getDestroyedCards());
			
		}

		
		boolean errorOccured = false;
		
		outer: for(Object o : cardsFromAllSources) {
			if(o instanceof Card) {
				Card c = (Card) o;
				
				if(null != cardsSeen.get(c.getPhysicalCardId())) {
					errorOccured = true;
					break outer;
				}
				cardsSeen.put(c.getPhysicalCardId(), true);
			} 
		}
		
		if(errorOccured) {
			cardsSeen.clear();
			for(Object o : cardsFromAllSources) {
				if(o instanceof Card) {
					Card c = (Card) o;
					System.out.println("- "+c);
					
					if(null != cardsSeen.get(c.getPhysicalCardId())) {
						System.out.println("duplicate card: "+c);
						FridayUtil.throwErr("Duplicate cards seen: "+c);
					}
					cardsSeen.put(c.getPhysicalCardId(), true);
				} else {
					System.out.println(o+":");
				}
			}
			
		}
		
	}

	
	public static void runtimeCheckAllCardsPresent_addCard(Map<Integer, Boolean> cardFound, Card c) {
		
		Boolean found = cardFound.get((Integer)c.getPhysicalCardId());
		if(found != null) {
			FridayUtil.throwErr("Duplicate card found:" +c);
		}

		cardFound.put((Integer)c.getPhysicalCardId(), Boolean.TRUE);

	}
	
	public static void runtimeCheckAllCardsPresent_checkCard(Map<Integer, Boolean> cardFound, Card c) {
		Boolean found = cardFound.get((Integer)c.getPhysicalCardId());
		if(found == null) {
			FridayUtil.throwErr("Card not found:" +c);
		}		
	}
		
	public static void runtimeCheckAllCardsPresent(ListCards yourFightingCards, ListCards hazardCards, 
			ImmutableGrowableListCards discardHazards, SlowGameState slowGameState, Card activeHazardCard, 
			ImmutableGrowableListCards discardFightCards, ImmutableGrowableListCards lhs_fightCards,
			ImmutableGrowableListCards rhs_fightCards, AbilityObject abilityObject, RuntimeObject newRuntimeObject, 
			boolean isPirateGameState) {
		
		HashMap<Integer, Boolean> cardFound = new HashMap<>();
		
		yourFightingCards.asList().forEach( e -> {
			runtimeCheckAllCardsPresent_addCard(cardFound, e);
		});

		if(!isPirateGameState) {
			hazardCards.asList().forEach( e-> {
				runtimeCheckAllCardsPresent_addCard(cardFound, e);
			});
			
			discardHazards.getAsList().forEach(e -> {
				runtimeCheckAllCardsPresent_addCard(cardFound, e);
			});
		}
		
		slowGameState.getAgingCards().asList().forEach( e -> {
			runtimeCheckAllCardsPresent_addCard(cardFound, e);
		});

		if(!isPirateGameState) {
			if(activeHazardCard != null) {
				runtimeCheckAllCardsPresent_addCard(cardFound, activeHazardCard);
			}
		}
		
		discardFightCards.getAsList().forEach( e-> {
			runtimeCheckAllCardsPresent_addCard(cardFound, e);
		});
		
		lhs_fightCards.getAsList().forEach( e -> {
			runtimeCheckAllCardsPresent_addCard(cardFound, e);
		});
		
		rhs_fightCards.getAsList().forEach( e -> {
			runtimeCheckAllCardsPresent_addCard(cardFound, e);
		});
		
		if(abilityObject != null) {
			if(abilityObject.getDrawnSortCards() != null) {
				abilityObject.getDrawnSortCards().forEach( e -> {
					runtimeCheckAllCardsPresent_addCard(cardFound, e);		
				});
			}
		}
		
		if(newRuntimeObject != null) {
			
			newRuntimeObject.getDestroyedCards().forEach( e -> {
				runtimeCheckAllCardsPresent_addCard(cardFound, e);
			});
		}
		
		FridayUtil.ALL_CARDS.getAgingCards().forEach( e -> {
			if(e.getFightingValue() == -3) { return; } // Skip 'very stupid' 
			
			runtimeCheckAllCardsPresent_checkCard(cardFound, e);
		});
		
		FridayUtil.ALL_CARDS.getFightingCards().forEach( e -> {
			runtimeCheckAllCardsPresent_checkCard(cardFound, e);
		});

		if(!isPirateGameState) {
			// TODO: LOWER - For pirate cards, we can add the hazards from the previous GameState into the destroyed list, and reenable this.
			FridayUtil.ALL_CARDS.getHazardCards().forEach( e-> {
				runtimeCheckAllCardsPresent_checkCard(cardFound, e);
			});
		}
		
	}
	
	
	private final static boolean OUT_ENABLED = false;
	
	@SuppressWarnings("unused")
	private static void out(GameState gs) {
		if(!OUT_ENABLED) { return; }
		
//		System.out.println();
	}
	
	private static void out(String str, GameState gs) {
		if(!OUT_ENABLED) { return; }
		
//		System.out.println();
//		System.out.println(ansi().fgBrightGreen().a("* ").reset()+str);
	}
	
	private static GameStateContainer ensureMinimumHazardCards(GameState input) {
		if(input.getHazardCards().size() > 0)  { return new GameStateContainer(input); }
		
		// Shuffle the discarded hazards into a new deck
		List<Card> shuffledDiscardHazards = input.getDiscardHazards().getAsList();
		FridayUtil.shuffleCollection(shuffledDiscardHazards);
		
		// Decrease the phase
		int newPhase = input.getPhaseNumber()-1;
		SlowGameState newSlowGameState = new SlowGameState(input.slowGameState.getAgingCards(), 
				input.slowGameState.getGameLevel(), input.slowGameState.getActivePirates(), 
				input.slowGameState.getActiveRoundCards(),  newPhase, input.slowGameState.getWildCardPirate());

		if(OUT_ENABLED) {
			out("The hazard discard pile has been shuffled into the hazard pile, and the phase is now "+newPhase, input);
		}
				
		// Create a new empty discard hazard pile 
		ImmutableGrowableListCards newDiscardHazards = new ImmutableGrowableListCards( (int)(shuffledDiscardHazards.size()/2) + 1);

		// Convert the new hazard deck into a ListCards
		ListCards newHazardCards = new ListCards(shuffledDiscardHazards.toArray(new Card[shuffledDiscardHazards.size()]), 0);
		
		GameState result = new GameState(input.getState(), input.getYourFightingCards(), newHazardCards, 
				newDiscardHazards,  newSlowGameState, input.getActiveHazardCard(), input.discardFightCards, 
				input.lhsOrRhsFightingCardUsed, input.getLifePoints(), input.getLhsFightCards(), 
				input.getRhsFightCards(), input.lhsOrRhsFightingCardDoubled, input.abilityObject, input, null);

		if(newPhase < 0) {
			return new GameStateContainer(PirateGameState.transferFromGameState(result));
		} else {
			return new GameStateContainer(result);
		}
				
	}
	
	public GameState selectAHazardCard_selectFromTwoHazardCards(int index) {
		
		Card newActiveHazardCard = hazardCards.get(index);
		Card discardedCard = hazardCards.get(1-index);
		
		ImmutableGrowableListCards newDiscardHazards = discardHazards.mutateAdd(discardedCard);
		
		ListCards newHazardCards = hazardCards.mutateRemoveFromFront(2);
		
		MapCards newLhsOrRhsFightingCardUsed = new MapCards();
		MapCards newLhsOrRhsFightingCardDoubled = new MapCards();

		if(newActiveHazardCard == null) {
			FridayUtil.throwErr("nahc ic null");
		}
		
		if(OUT_ENABLED) {
			out("Hazard has been selected: "+AnsiCards.asHazardCard(newActiveHazardCard, this), this);
		}
		
		GameState result = new GameState(State.DRAW_FREE_CARDS, yourFightingCards, newHazardCards, newDiscardHazards, 
				slowGameState, newActiveHazardCard, discardFightCards, newLhsOrRhsFightingCardUsed, lifePoints, 
				lhs_fightCards, rhs_fightCards, newLhsOrRhsFightingCardDoubled, abilityObject, this, null);
		
		return result;
		
	}

	public GameStateContainer selectAHazardCard_selectFromOneHazardCard(boolean fightCard) {
		Card card = hazardCards.get(0);		
		State newState;
		Card newActiveHazardCard;
		
		if(fightCard) {
			if(OUT_ENABLED) {
				out("Hazard has been selected: "+AnsiCards.asHazardCard(card, this), this);
			}

			newState = State.DRAW_FREE_CARDS;
			newActiveHazardCard = card;
			MapCards newLhsOrRhsFightingCardUsed = new MapCards();
			MapCards newLhsOrRhsFightingCardDoubled = new MapCards();

			ListCards newHazardCards = hazardCards.mutateRemoveFromFront(1);

			if(newActiveHazardCard == null) {
				FridayUtil.throwErr("nahc ic null");
			}

			GameState result = new GameState(newState, yourFightingCards, newHazardCards, discardHazards, 
					slowGameState, newActiveHazardCard, discardFightCards, newLhsOrRhsFightingCardUsed, 
					lifePoints, lhs_fightCards, rhs_fightCards, newLhsOrRhsFightingCardDoubled, abilityObject, this, null);

			return new GameStateContainer(result);

		} else {

			// At this point, the hazard card that we are choosing not to fight is still
			// at the top of the hazard stack. 
			
			if(OUT_ENABLED) {
				out("Hazard has been discarded: "+AnsiCards.asHazardCard(card, this), this);
			}

			newState = State.SELECT_A_HAZARD_CARD;
			ImmutableGrowableListCards newDiscardHazards = discardHazards.mutateAdd(card);
			newActiveHazardCard = null;
			
			ListCards newHazardCards = hazardCards.mutateRemoveFromFront(1);

			GameState result = new GameState(newState, yourFightingCards, newHazardCards, newDiscardHazards, 
					slowGameState, newActiveHazardCard, discardFightCards, lhsOrRhsFightingCardUsed, 
					lifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled, abilityObject, this, null);
			
			// This will necessarily decrease the phase and reshuffle the hazard cards
			return ensureMinimumHazardCards(result);

		}
		
	}
	
	
	public GameState drawFreeCards_endDrawFreeCardsPhase() {

		GameState result = new GameState(State.SACRIFICE_LIFE_TO_DRAW, yourFightingCards, hazardCards, discardHazards, 
				slowGameState, activeHazardCard, discardFightCards, lhsOrRhsFightingCardUsed, lifePoints, 
				lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled, abilityObject, this, null);

		return result;
	}
	
	public GameState drawFreeCards_drawACard() {
		return inner_drawFreeCards_drawACard(this);
	}

	
	private static GameState inner_drawFreeCards_drawACard(GameState gs) {
		
		gs = gs.ensureFightStackHasOneCard();

		// Remove a card from the front of fighting card stack
		Card card = gs.getYourFightingCards().get(0);
		ListCards newYourFightingCards = gs.getYourFightingCards().mutateRemoveFromFront(1);
		
		// Add it Lhs
		ImmutableGrowableListCards newLhs_fightCards = gs.getLhsFightCards().mutateAdd(card);

		State newState = gs.getState();
		
		// Have we drawn all our free cards, or have we run out of cards to draw?
		if(newLhs_fightCards.size() == gs.getActiveHazardCard().getFreeCards() 
				|| (newYourFightingCards.size() == 0 && gs.getDiscardFightCards().size() == 0)) {
			newState = State.SACRIFICE_LIFE_TO_DRAW;
		}
		
		GameState result = new GameState(newState, newYourFightingCards, gs.getHazardCards(), gs.getDiscardHazards(), 
				gs.slowGameState, gs.getActiveHazardCard(), gs.discardFightCards, gs.lhsOrRhsFightingCardUsed, 
				gs.getLifePoints(), newLhs_fightCards, gs.getRhsFightCards(), gs.lhsOrRhsFightingCardDoubled, 
				gs.abilityObject, gs, null);

		
		return result;

	}
	
	public boolean calculateCanDrawXFightingCards(int x) {
		return yourFightingCards.size() + discardFightCards.size() >= x;
	}
	
	public int calculateEffectivePhase() {
		int numberOfPhaseMinusCards = 0;
		
		for(Card c : getLhsFightCards().getAsList()) {
			if(c.getAbility() == Ability.PHASE_MINUS_1) {  numberOfPhaseMinusCards++; }
		}
		
		for(Card c : getRhsFightCards().getAsList()) {
			if(c.getAbility() == Ability.PHASE_MINUS_1) {  numberOfPhaseMinusCards++; }
		}
		
		if(slowGameState.getActiveRoundCards() != null) {
			numberOfPhaseMinusCards +=  slowGameState.getActiveRoundCards().size();
		}
		
		int phaseNumber = Math.min(2, getPhaseNumber()+numberOfPhaseMinusCards);

		return phaseNumber;
	}
	
	@SuppressWarnings("unused")
	public int calculateRemainingHazardValue() {

		int numberOfPhaseMinusCards = 0;
		int currHazardVal = 0;

		MapCards isDoubled = lhsOrRhsFightingCardDoubled;

		boolean containsHighestFightCardEquals0 = false;
		Card highestCard = null;

		for (Card c : getLhsFightCards().getAsList()) {
			if(c.getAbility() == Ability.PHASE_MINUS_1) { numberOfPhaseMinusCards++; }
			if(c.getAbility() == Ability.HIGHEST_CARD_IS_0) { containsHighestFightCardEquals0 = true; }

			int fightingVal = c.getFightingValue();

			if(isDoubled.get(c.getPhysicalCardId()) == Boolean.TRUE) {
				fightingVal *= 2;
			}

			if(highestCard == null || c.getFightingValue() > highestCard.getFightingValue()) {
				highestCard = c;
			}

			currHazardVal -= fightingVal;
		}

		for(Card c : getRhsFightCards().getAsList()) {
			if(c.getAbility() == Ability.PHASE_MINUS_1) { numberOfPhaseMinusCards++; }
			if(c.getAbility() == Ability.HIGHEST_CARD_IS_0) { containsHighestFightCardEquals0 = true; }

			int fightingVal = c.getFightingValue();

			if(isDoubled.get(c.getPhysicalCardId()) == Boolean.TRUE) {
				fightingVal *= 2;
			}

			currHazardVal -= fightingVal;

			if(highestCard == null || c.getFightingValue() > highestCard.getFightingValue()) {
				highestCard = c;
			}

		}

		if(slowGameState.getActiveRoundCards() != null) {
			numberOfPhaseMinusCards += slowGameState.getActiveRoundCards().size();
		}

		int phaseNumber = Math.min(2, getPhaseNumber() + numberOfPhaseMinusCards);

		if (FridayUtil.RUNTIME_CHECK && phaseNumber != calculateEffectivePhase()) {
			FridayUtil.throwErr("Phase calcuation methods should produce the same value.");
		}

		currHazardVal += getActiveHazardCard().getHazardValues()[phaseNumber];

		if(containsHighestFightCardEquals0 && highestCard != null && highestCard.getFightingValue() > 0) {
			currHazardVal += highestCard.getFightingValue(); // Yes this is correct as '+='
		}

		return currHazardVal;
	}

	
	@SuppressWarnings("unused")
	public GameStateContainer sacrificeLifeToDraw_endSacrificePhase() {
	
		GameState result;
		
		int remainingHazardValue = calculateRemainingHazardValue();
		
		// If there were any cards in active round cards, then clear them
		SlowGameState newSlowGameState = slowGameState;
		if(newSlowGameState.getActiveRoundCards() != null && newSlowGameState.getActiveRoundCards().size() > 0) {
			newSlowGameState = new SlowGameState(newSlowGameState.getAgingCards(), newSlowGameState.getGameLevel(),
					newSlowGameState.getActivePirates(), null, newSlowGameState.getPhaseNumber(), 
					newSlowGameState.getWildCardPirate());
		}
		
		int lifePointsPaidForAgingCards = 0;
		
		int newLifePoints = lifePoints;
		
		for(int x = 0; x < lhs_fightCards.size(); x++) {
			Card c = lhs_fightCards.get(x);
			if(c.getAbility() == Ability.LIFE_MINUS_1 || c.getAbility() == Ability.LIFE_MINUS_2) {
				lifePointsPaidForAgingCards += c.getAbility().getMagnitude(); // add because magnitude is negative 
			}
		}
		
		for(int x = 0; x < rhs_fightCards.size(); x++) {
			Card c = rhs_fightCards.get(x);
			if(c.getAbility() == Ability.LIFE_MINUS_1 || c.getAbility() == Ability.LIFE_MINUS_2) {
				lifePointsPaidForAgingCards += c.getAbility().getMagnitude(); // add because magnitude is negative
			}
		}
		
		newLifePoints += lifePointsPaidForAgingCards; // add because magnitude is negative

		if(lifePointsPaidForAgingCards < 0 && OUT_ENABLED) {
			out("Paid "+ansi().fgBrightRed().a(lifePointsPaidForAgingCards).reset().toString()+" life points due to aging cards, now at "+newLifePoints, this);
		}

		// Player must pay life for hazard values
		if(remainingHazardValue > 0) {
		
			newLifePoints = newLifePoints - remainingHazardValue;
			
			if(remainingHazardValue > 0 && OUT_ENABLED) {
				out("Paid "+ansi().fgBrightRed().a(remainingHazardValue).reset()+" life points due to remaining hazard cards, now at "+newLifePoints, this);
			}
			
			result = new GameState(State.PAY_LIFE_POINTS_ON_HAZARD_MISS, yourFightingCards, hazardCards, 
					discardHazards, newSlowGameState, activeHazardCard, discardFightCards, lhsOrRhsFightingCardUsed, 
					newLifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled, abilityObject, 
					this, null);
			
			return new GameStateContainer(result);
		} else {
			// Player must not pay life for hazard values (but may still pay for aging cards above)
			
			// Add hazard card to fight card discard pile
			ImmutableGrowableListCards newDiscardFightCards = discardFightCards.mutateAdd(activeHazardCard);
			Card newActiveHazardCard = null;
			
			if(OUT_ENABLED) {
				out("Hazard was beaten, added to fight discard pile as fight card: "+AnsiCards.asFightingCard(activeHazardCard, this), this);
			}
			
			// Add used (non-destroyed) fighting cards to fighting card discard pile
			newDiscardFightCards = newDiscardFightCards.mutateAddAll(lhs_fightCards);
			
			if(rhs_fightCards.size() > 0) {
				newDiscardFightCards = newDiscardFightCards.mutateAddAll(rhs_fightCards);	
			}
			
			// Clear LHS and RHS
			
			result = new GameState(State.SELECT_A_HAZARD_CARD, yourFightingCards, hazardCards, discardHazards, 
					newSlowGameState, newActiveHazardCard, newDiscardFightCards, lhsOrRhsFightingCardUsed, 
					newLifePoints, defaultLhsFightCards(), defaultRhsFightCards(), lhsOrRhsFightingCardDoubled, 
					abilityObject, this, null);

			return ensureMinimumHazardCards(result);
			
		}
		
	}
	
	public GameState sacrificeLifeToDraw_useCardAbility(UseCardAbilityAction action, boolean isCopiedAbility) {
		
		return innerSacrificeLifeToDraw_useCardAbility(action, this, isCopiedAbility);
	}

	private static GameState innerSacrificeLifeToDraw_useCardAbility(UseCardAbilityAction ucaa, GameState gs,
			boolean isCopiedAbility) {

		Card c = ucaa.getCard();

		AbilityObject newAbilityObject = gs.getAbilityObject();

		int newLifePoints = gs.getLifePoints();
		ListCards newYourFightingCards = null;
		ImmutableGrowableListCards newRhsFightCards = null;

		SlowGameState newSlowGameState = null;

		// We only flag a card as used if:
		// - the exchange is not part of a copy action
		// - the exchange is not in the second step from a copy action
		MapCards newLhsOrRhsFightCardsUsed = gs.lhsOrRhsFightingCardUsed;
		if(!isCopiedAbility && (newAbilityObject == null || !newAbilityObject.isCopied())) {
			newLhsOrRhsFightCardsUsed = new MapCards(gs.lhsOrRhsFightingCardUsed);
			newLhsOrRhsFightCardsUsed.put((Integer) c.getPhysicalCardId(), true);
		}

		if(c.getAbility() == Ability.CARDS_DRAW_1) {
			gs = gs.ensureFightStackHasOneCard();
			newYourFightingCards = gs.getYourFightingCards();

			// Get the top card from fight stack, remove the top card, and add it to RHS
			Card newCard = newYourFightingCards.get(0);
			newYourFightingCards = gs.getYourFightingCards().mutateRemoveFromFront(1);
			newRhsFightCards = gs.getRhsFightCards().mutateAdd(newCard);

		} else if(c.getAbility() == Ability.CARDS_DRAW_2) {
			gs = gs.ensureFightStackHasOneCard();
			newYourFightingCards = gs.getYourFightingCards();

			// Get the top card from fight stack, remove the top card, and add it to RHS
			Card newCard = newYourFightingCards.get(0);
			newYourFightingCards = gs.getYourFightingCards().mutateRemoveFromFront(1);
			newRhsFightCards = gs.getRhsFightCards().mutateAdd(newCard);

			if (newAbilityObject != null) {
				// We have already drawn two cards, so this ability is complete.
				newAbilityObject = null;
			} else {
				newAbilityObject = new AbilityObject(c, 1, 0, null, isCopiedAbility);
			}

		} else if(c.getAbility() == Ability.LIFE_ADD_1) {
			newLifePoints++;
		} else if(c.getAbility() == Ability.LIFE_ADD_2) {
			newLifePoints += 2;
		} else if(c.getAbility() == Ability.SORT_3_CARDS) {

			// This branch will be enter when 0, 1, or 2 cards have been draw (and the user
			// wants to draw, in the case of 1 and 2)

			gs = gs.ensureFightStackHasOneCard();
			newYourFightingCards = gs.getYourFightingCards();

			int newPhase = 0;

			// Get the top card from fight stack, remove the top card, and add it to RHS
			Card newCard = newYourFightingCards.get(0);
			newYourFightingCards = gs.getYourFightingCards().mutateRemoveFromFront(1);

			List<Card> newDrawnSortCards = new ArrayList<>();

			if (newAbilityObject != null) {
				newDrawnSortCards.addAll(newAbilityObject.getDrawnSortCards());
			}
			newDrawnSortCards.add(newCard);

			if (newDrawnSortCards.size() == 3) {
				// Switch to the next phase once 3 cards have been drawn.
				newPhase = 1;
			}

			isCopiedAbility = isCopiedAbility || (newAbilityObject != null && newAbilityObject.isCopied());

			newAbilityObject = new AbilityObject(c, newDrawnSortCards.size(), newPhase, newDrawnSortCards, isCopiedAbility);
		} else if(c.getAbility() == Ability.PHASE_MINUS_1 && isCopiedAbility) {

			SlowGameState sgs = gs.getSlowGameState();

			ImmutableGrowableListCards newActiveRoundCards = sgs.getActiveRoundCards();
			if (newActiveRoundCards == null) {
				newActiveRoundCards = new ImmutableGrowableListCards(4);
			}

			newActiveRoundCards = newActiveRoundCards.mutateAdd_allowDuplicates(c);

			newSlowGameState = new SlowGameState(sgs.getAgingCards(), sgs.getGameLevel(), sgs.getActivePirates(), 
					newActiveRoundCards, sgs.getPhaseNumber(), sgs.getWildCardPirate());

			// The only way that phase minus 1 is valid is through a copy action

		} else {
			FridayUtil.throwErr("Invalid action response for this method: " + ucaa);
		}

		if(newYourFightingCards == null) {
			newYourFightingCards = gs.getYourFightingCards();
		}

		if(newSlowGameState == null) {
			newSlowGameState = gs.getSlowGameState();
		}
		
		if(newRhsFightCards == null) {
			newRhsFightCards = gs.getRhsFightCards();
		}

		GameState result = new GameState(gs.getState(), newYourFightingCards, gs.getHazardCards(),
				gs.getDiscardHazards(), newSlowGameState, gs.getActiveHazardCard(), gs.discardFightCards,
				newLhsOrRhsFightCardsUsed, newLifePoints, gs.getLhsFightCards(), newRhsFightCards,
				gs.lhsOrRhsFightingCardDoubled, newAbilityObject, gs, null);

		return result;
	}
	
	@SuppressWarnings("unused")
	public GameState sacrificeLifeToDraw_useCaseAbilitySort(ActionResponse r) {
		UseCardAbilitySortAction ucasa = (UseCardAbilitySortAction) r.getAction();

		// Add the sorted card in their new order		
		List<Card> newList = new ArrayList<>();
		for(Card c : ucasa.getSortOrder()) {
			if(FridayUtil.RUNTIME_CHECK && c == null) { FridayUtil.throwErr("null in sort order"); }
			newList.add(c);
		}
		
		// The cards in getSortOrder() have already been removed from the fighting card stack
		newList.addAll(yourFightingCards.asList());
		
		ListCards newYourFightingCards = new ListCards(newList.toArray(new Card[newList.size()]), 0);
		
		ImmutableGrowableListCards newDiscardFightCards = discardFightCards;
		if(ucasa.getDiscard() != null) {
			newDiscardFightCards = newDiscardFightCards.mutateAdd( ucasa.getDiscard()  );
		}

		
		if(OUT_ENABLED) {
			String text = "Cards sorted to fighting card stack: ";
			
			int x = 1;
			for(Card c : ucasa.getSortOrder()) { 
				text += x+") "+AnsiCards.asFightingCard(c, this)+"  ";
				x++;
			}
			if(ucasa.getDiscard() != null) {
				text += "Discard: "+AnsiCards.asFightingCard(ucasa.getDiscard(), this);
			}
			out(text, this);
		}
		
		AbilityObject newAbilityObject = null;

		GameState result = new GameState(this.state, newYourFightingCards, hazardCards, discardHazards, 
				slowGameState, activeHazardCard, newDiscardFightCards, lhsOrRhsFightingCardUsed, 
				lifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled, newAbilityObject, 
				this, null);

		return result;
		
	}
	
	public GameState sacrificeLifeToDraw_endMultistageAbilityResponse(ActionResponse r) {

		AbilityObject newAbilityObject = abilityObject;
		Card c = abilityObject.getActiveCard();
		if(c.getAbility() == Ability.SORT_3_CARDS) {
			// Advance to the next stage without drawing any more cards
			newAbilityObject = new AbilityObject(c, abilityObject.getNumberOfCardsDrawn(), abilityObject.getStage()+1, 
					abilityObject.getDrawnSortCards(), abilityObject.isCopied());
			
		} else {
			// For all other abilities, the stage is complete, so reset the object
			newAbilityObject = null;
		}

		GameState result = new GameState(this.state, yourFightingCards, hazardCards, discardHazards, 
				slowGameState, activeHazardCard, discardFightCards, lhsOrRhsFightingCardUsed, 
				lifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled, newAbilityObject, 
				this, null);
		
		return result;
	}
	
	private GameState ensureFightStackHasOneCard() {

		GameState result;
				
		// If we have run out of fighting cards
		if(yourFightingCards.size() == 0) {
			
			if(discardFightCards.size() == 0) {
				FridayUtil.throwErr("Cannot ensure fight stack has one card: discard fight stack is empty.");
			}

			
			if(OUT_ENABLED) {
				out("Fighting card stack been reshuffled, and an aging card has been added.", this);
			}

			int newLifePoints = lifePoints;
			SlowGameState newSlowGameState = slowGameState;			
			ListCards newYourFightingCards;
	
			List<Card> innerNewFightingCards = discardFightCards.getAsList();
			
			// Remove an aging card and add it to the fighting card stack (if possible) 
			if(slowGameState.getAgingCards().size() > 0) {
				
				Card agingCard = slowGameState.getAgingCards().get(0);
				ListCards newAgingCards = slowGameState.getAgingCards().mutateRemoveFromFront(1);
				innerNewFightingCards.add(agingCard);
				
				newSlowGameState = new SlowGameState(newAgingCards, slowGameState.getGameLevel(), 
						slowGameState.getActivePirates(), slowGameState.getActiveRoundCards(), 
						slowGameState.getPhaseNumber(), slowGameState.getWildCardPirate());
			} else {
				// If the aging stack is empty, and we are drawing a card, then the game ends, as per
				// new rule: https://boardgamegeek.com/thread/1808313/friday-official-rules-change-friedemann-friese
				newLifePoints = -999;
			}
			
			FridayUtil.shuffleCollection(innerNewFightingCards);

			newYourFightingCards = new ListCards(innerNewFightingCards.toArray(new Card[innerNewFightingCards.size()]), 0);

			ImmutableGrowableListCards newDiscardFightCards = new ImmutableGrowableListCards( 60 );
			
			if(newYourFightingCards.size() == 0) {
				FridayUtil.throwErr("Fighting card stack is empty after reshuffle?");				
			}
			
			result = new GameState(this.state, newYourFightingCards, hazardCards, discardHazards, 
						newSlowGameState, activeHazardCard, newDiscardFightCards, lhsOrRhsFightingCardUsed, 
						newLifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled, abilityObject, 
						this, null);

		} else { 
			result = this;
		}
		
		return result;
	}
	
	public GameState sacrificeLifeToDraw_drawACard() {

		return inner_sacrificeLifeToDraw_drawACard(this);
		
	}
	
	private static GameState inner_sacrificeLifeToDraw_drawACard(GameState gs) {
		
		gs = gs.ensureFightStackHasOneCard();
		
		Card card = gs.getYourFightingCards().get(0);
		ListCards newYourFightingCards = gs.getYourFightingCards().mutateRemoveFromFront(1);
		
		ImmutableGrowableListCards newRhs_fightCards = gs.getRhsFightCards().mutateAdd(card);
		
		int newLifePoints = gs.getLifePoints()-1;
		
		if(OUT_ENABLED) {
			out("Your life points are now at "+newLifePoints, gs);
		}
		
		GameState result = new GameState(gs.getState(), newYourFightingCards, gs.getHazardCards(), 
				gs.getDiscardHazards(), gs.slowGameState, gs.getActiveHazardCard(), gs.discardFightCards, 
				gs.lhsOrRhsFightingCardUsed, newLifePoints, gs.getLhsFightCards(), newRhs_fightCards, 
				gs.lhsOrRhsFightingCardDoubled, gs.abilityObject, gs, null);

		return result;
	}
	
	@SuppressWarnings("unused")
	public GameState sacrificeLifeToDraw_useCardAbilityWithTarget(UseCardAbilityWithTargetAction ucawta, 
			boolean isCopiedAction) {
		
		if(ucawta.getCard().getAbility() == Ability.DESTROY_1x) {
			
			MapCards newLhsOrRhsFightingCardUsed = lhsOrRhsFightingCardUsed;
			if(!isCopiedAction) {
				newLhsOrRhsFightingCardUsed = new MapCards(lhsOrRhsFightingCardUsed);
				newLhsOrRhsFightingCardUsed.put(ucawta.getCard().getPhysicalCardId(), true);
			}
			
			Card cardToDestroy = ucawta.getTarget();

			ImmutableGrowableListCards newLhs_fightCards = lhs_fightCards;
			ImmutableGrowableListCards newRhs_fightCards = rhs_fightCards;
			
			
			if(lhs_fightCards.containsPhysicalId(cardToDestroy.getPhysicalCardId())) {
				newLhs_fightCards = newLhs_fightCards.mutateRemoveCardByPhysicalId(cardToDestroy.getPhysicalCardId());
			} else if(rhs_fightCards.containsPhysicalId(cardToDestroy.getPhysicalCardId())) {
				newRhs_fightCards = newRhs_fightCards.mutateRemoveCardByPhysicalId(cardToDestroy.getPhysicalCardId());
			}
			
			if(OUT_ENABLED) {
				out("Card "+AnsiCards.asFightingCard(cardToDestroy, this)+" has been destroyed.", this);
			}
			
			
			RuntimeObject newRuntimeObject = null;
			if(FridayUtil.RUNTIME_CHECK) {
				newRuntimeObject = RuntimeObjectMap.getInstance().get(this);
				newRuntimeObject = newRuntimeObject.mutateAddDestroyedCard(cardToDestroy);
			}
			
			GameState result = new GameState(this.state, yourFightingCards, hazardCards, discardHazards, 
					slowGameState, activeHazardCard, discardFightCards, newLhsOrRhsFightingCardUsed, lifePoints, 
					newLhs_fightCards, newRhs_fightCards, lhsOrRhsFightingCardDoubled, abilityObject, this, newRuntimeObject);
			
			return result;
			
		} else if(ucawta.getCard().getAbility() == Ability.BELOW_THE_PILE_1x) {

			return inner_sacrificeLifeToDraw_useCardAbilityWithTarget_belowThePile(ucawta, this, isCopiedAction);
			
		} else if(ucawta.getCard().getAbility() == Ability.DOUBLE_1x) {

			MapCards newLhsOrRhsFightingCardUsed = lhsOrRhsFightingCardUsed;
			
			if(!isCopiedAction) {
				newLhsOrRhsFightingCardUsed = new MapCards(lhsOrRhsFightingCardUsed);
				newLhsOrRhsFightingCardUsed.put(ucawta.getCard().getPhysicalCardId(), true);
				
			}
			
			// Flag the target card as doubled
			Card targetCard = ucawta.getTarget();
			if(FridayUtil.RUNTIME_CHECK && lhsOrRhsFightingCardDoubled.get(targetCard.getPhysicalCardId()) == true) { FridayUtil.throwErr("Target card is already doubled: "+targetCard); }				
			MapCards newLhsOrRhsFightingCardDoubled = new MapCards(lhsOrRhsFightingCardDoubled);
			newLhsOrRhsFightingCardDoubled.put(targetCard.getPhysicalCardId(), true);
			
			if(OUT_ENABLED) {
				out("Card "+AnsiCards.asFightingCard(targetCard, this)+" has been doubled.", this);
			}
			
			GameState result = new GameState(this.state, yourFightingCards, hazardCards, discardHazards, 
					slowGameState, activeHazardCard, discardFightCards, newLhsOrRhsFightingCardUsed, lifePoints, 
					lhs_fightCards, rhs_fightCards, newLhsOrRhsFightingCardDoubled, abilityObject, this, null);
			
			return result;
			
		} else if(ucawta.getCard().getAbility() == Ability.EXCHANGE_X1 
				|| ucawta.getCard().getAbility() == Ability.EXCHANGE_X2) {
			
			GameState gs = ensureFightStackHasOneCard();
			return inner_sacrificeLifeToDraw_useCardAbilityWithTarget_exchange(ucawta, gs, isCopiedAction);
			
		} else {
			FridayUtil.throwErr("Unrecognized card ability: "+ucawta.getCard());
		}
			
		return null;
	}
	
	private static GameState inner_sacrificeLifeToDraw_useCardAbilityWithTarget_exchange(UseCardAbilityWithTargetAction ucawta, GameState gs, boolean isCopiedAction) {

		// Flag the exchange as used, but only if:
		// - the exchange is not part of a copy action
		// - the exchange is not in the second step from a copy action
		MapCards newLhsOrRhsFightingCardUsed = new MapCards(gs.lhsOrRhsFightingCardUsed);
		if(!isCopiedAction && (gs.getAbilityObject() == null || !gs.getAbilityObject().isCopied())) {
			newLhsOrRhsFightingCardUsed.put(ucawta.getCard().getPhysicalCardId(), true);
		}
		
		// Mark the target card as unused, in case we happen to pull it again after a reshuffle
		newLhsOrRhsFightingCardUsed.put(ucawta.getTarget().getPhysicalCardId(), false);
		
		Card cardToDiscard = ucawta.getTarget();
		
		ImmutableGrowableListCards newLhsFightCards = gs.getLhsFightCards();
		ImmutableGrowableListCards newRhsFightCards = gs.getRhsFightCards();
		
		ListCards newYourFightingCards = gs.getYourFightingCards();
		
		Card replacementCard = newYourFightingCards.get(0);
		newYourFightingCards = newYourFightingCards.mutateRemoveFromFront(1);
		
		// Remove the target card from either the LHS or the RHS
		if(gs.getLhsFightCards().containsPhysicalId(cardToDiscard.getPhysicalCardId())) {
			newLhsFightCards = newLhsFightCards.mutateRemoveCardByPhysicalId(cardToDiscard.getPhysicalCardId());
			newLhsFightCards = newLhsFightCards.mutateAdd(replacementCard);
			
		} else if(gs.getRhsFightCards().containsPhysicalId(cardToDiscard.getPhysicalCardId())) {
			newRhsFightCards = newRhsFightCards.mutateRemoveCardByPhysicalId(cardToDiscard.getPhysicalCardId());
			newRhsFightCards = newRhsFightCards.mutateAdd(replacementCard);
		} else {
			FridayUtil.throwErr("Could not find card to discard in Rhs or Lhs: "+cardToDiscard);
		}
		
		if(OUT_ENABLED) {
			out("Card "+AnsiCards.asFightingCard(cardToDiscard, gs)+" has been exchanged.", gs);
		}
		
		// Add to discard
		ImmutableGrowableListCards newDiscardFightCards = gs.discardFightCards.mutateAdd(cardToDiscard);

		AbilityObject newAbilityObject = gs.getAbilityObject();
		
		if(ucawta.getCard().getAbility() == Ability.EXCHANGE_X2) {
			if(newAbilityObject == null) {
				newAbilityObject = new AbilityObject(ucawta.getCard(), 1, 0, null, isCopiedAction); 
			} else {
				// The user has exchanged 2 cards, so end the ability. 
				newAbilityObject = null;
			}
		}
		
		gs = new GameState(gs.getState(), newYourFightingCards, gs.getHazardCards(), gs.getDiscardHazards(), 
				gs.getSlowGameState(), gs.getActiveHazardCard(), newDiscardFightCards, newLhsOrRhsFightingCardUsed,
				gs.getLifePoints(), newLhsFightCards, newRhsFightCards, gs.lhsOrRhsFightingCardDoubled, 
				newAbilityObject, gs, null);

		
		return gs;
		
	}
	
	private static GameState inner_sacrificeLifeToDraw_useCardAbilityWithTarget_belowThePile(UseCardAbilityWithTargetAction ucawta, GameState gs, boolean isCopiedAction) {
		
		Card cardToBury = ucawta.getTarget();

		boolean isCardToBuryOnLhs = gs.getLhsFightCards().containsPhysicalId(cardToBury.getPhysicalCardId());
		
		// If the Robinson stack is empty, shuffle the discard pile in before adding it
		// to the bottom
		if(gs.getYourFightingCards().size() == 0 && isCardToBuryOnLhs) {
			gs = gs.ensureFightStackHasOneCard();
		}

		// Mark BTP card as used
		MapCards newLhsOrRhsFightingCardUsed = new MapCards(gs.lhsOrRhsFightingCardUsed);
		
		if(!isCopiedAction) {
			newLhsOrRhsFightingCardUsed.put(ucawta.getCard().getPhysicalCardId(), true);
		}

		if(OUT_ENABLED) {
			out("Card "+AnsiCards.asFightingCard(cardToBury, gs)+" has been moved to the bottom of the pile.", gs);
		}
		
		// The card we are burying gets marked as unused, as if we see it again it will be from a redraw.
		newLhsOrRhsFightingCardUsed.put(cardToBury.getPhysicalCardId(), false);
		
		// If a BTP is replacing a card on the LHS, then draw a replacement to the LHS
		if(isCardToBuryOnLhs) {

			// Add target card to the bottom of fighting card stack
			ListCards newYourFightingCards = gs.getYourFightingCards();
			newYourFightingCards = newYourFightingCards.mutateAddToBottom(cardToBury);
			
			// Draw a card from fighting cards
			Card replacementCard = newYourFightingCards.get(0);
			newYourFightingCards = newYourFightingCards.mutateRemoveFromFront(1);
			
			// Remove the buried card from the LHS
			ImmutableGrowableListCards newLhs_fightCards = gs.getLhsFightCards().mutateRemoveCardByPhysicalId(cardToBury.getPhysicalCardId());
			
			// Add new card to LHS
			newLhs_fightCards = newLhs_fightCards.mutateAdd(replacementCard);

			GameState result = new GameState(gs.getState(), newYourFightingCards, gs.getHazardCards(), 
					gs.getDiscardHazards(),  gs.getSlowGameState(), gs.getActiveHazardCard(), gs.discardFightCards, 
					newLhsOrRhsFightingCardUsed, gs.getLifePoints(), newLhs_fightCards, 
					gs.getRhsFightCards(), gs.lhsOrRhsFightingCardDoubled, gs.getAbilityObject(), gs, null);

		
			return result;
			
		} else if(gs.getRhsFightCards().containsPhysicalId(cardToBury.getPhysicalCardId())) {
			// Remove the card from RHS
			ImmutableGrowableListCards newRhs_fightCards = gs.getRhsFightCards().
					mutateRemoveCardByPhysicalId(cardToBury.getPhysicalCardId());

			// Add to bottom of stack
			ListCards newYourFightingCards = gs.getYourFightingCards().mutateAddToBottom(cardToBury);
			
			GameState result = new GameState(gs.getState(), newYourFightingCards, gs.getHazardCards(), 
					gs.getDiscardHazards(), gs.getSlowGameState(), gs.getActiveHazardCard(), gs.discardFightCards, 
					newLhsOrRhsFightingCardUsed, gs.getLifePoints(), gs.getLhsFightCards(), 
					newRhs_fightCards, gs.lhsOrRhsFightingCardDoubled, gs.getAbilityObject(), gs, null);
		
			return result;
			
		} 
		
		FridayUtil.throwErr("Target card not found on RHS or LHS: "+cardToBury);
		
		return null;
	}

	public GameState sacrificeLifeToDraw_flagCopyAbilityAsUsed(UseCopyAbilityAction action) {
		
		MapCards newLhsOrRhsFightingCardUsed = new MapCards(lhsOrRhsFightingCardUsed);
		newLhsOrRhsFightingCardUsed.put( action.getCard().getPhysicalCardId(), true);
		
		GameState result = new GameState(this.state, yourFightingCards, hazardCards, discardHazards, 
				slowGameState, activeHazardCard, discardFightCards, newLhsOrRhsFightingCardUsed, 
				lifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled, abilityObject, 
				this, null);

		return result;
	}

	public GameStateContainer payLifePointsOnHazardMiss_destroyCards(Card[] cardsToDestroy) {

		// Move hazard card to hazard card discard pile, and remove it as active
		ImmutableGrowableListCards newDiscardHazards = discardHazards.mutateAdd(activeHazardCard);		
		Card newActiveHazardCard = null;

		// Destroy cards that are marked as destroyed
		Map<Integer /* phys card id*/, Boolean> isCardRemoved = new HashMap<>();
		for(Card cardToDestroy : cardsToDestroy) {
			isCardRemoved.put((Integer)cardToDestroy.getPhysicalCardId(), false);
			if(OUT_ENABLED) {
				out("Card "+AnsiCards.asFightingCard(cardToDestroy, this)+" has been destroyed.", this);
			}
		}
		
		List<Card> discardedFightCards = new ArrayList<>();
		
		// Move remaining LHS fight cards to fight card discard pile
		for(int x = 0; x < lhs_fightCards.size(); x++) {
			Card curr = lhs_fightCards.get(x);
			
			Boolean isDestroyed = isCardRemoved.get(curr.getPhysicalCardId());
			if(isDestroyed != null && isDestroyed == false) {
				isCardRemoved.put((Integer)curr.getPhysicalCardId(), true);
			} else {
				discardedFightCards.add(curr);
			}
		}
		
		// Move remaining RHS fight cards to fight card discard pile
		for(int x = 0; x < rhs_fightCards.size(); x++) {
			Card curr = rhs_fightCards.get(x);
			
			Boolean isDestroyed = isCardRemoved.get(curr.getPhysicalCardId());
			if(isDestroyed != null && isDestroyed == false) {
				isCardRemoved.put((Integer)curr.getPhysicalCardId(), true);
			} else {
				discardedFightCards.add(curr);
			}
		}
		
		ImmutableGrowableListCards newDiscardFightCards = discardFightCards.mutateAddAll(discardedFightCards);

		RuntimeObject newRuntimeObject = null;
		if(FridayUtil.RUNTIME_CHECK) {
			newRuntimeObject = RuntimeObjectMap.getInstance().get(this);
			for(Card cardToDestroy : cardsToDestroy) {
				newRuntimeObject = newRuntimeObject.mutateAddDestroyedCard(cardToDestroy);
			}
		}
		
		GameState result = new GameState(State.SELECT_A_HAZARD_CARD, yourFightingCards, hazardCards, 
				newDiscardHazards, slowGameState, newActiveHazardCard, newDiscardFightCards, 
				lhsOrRhsFightingCardUsed, lifePoints, defaultLhsFightCards(), defaultRhsFightCards(), 
				lhsOrRhsFightingCardDoubled, abilityObject, this, newRuntimeObject);
		
		return ensureMinimumHazardCards(result);
		
	}
	
	public boolean isFightingCardAbilityUsed(Card c) {
		if(lhsOrRhsFightingCardUsed == null) {
			FridayUtil.throwErr("Fighting card used map is null");
		}
		
		Boolean val = lhsOrRhsFightingCardUsed.get(c.getPhysicalCardId());
		if(val == null || val == false) {
			return false;
		}
		
		return true;
	}
	
	public boolean isFightingCardDoubled(Card c) {
		if(lhsOrRhsFightingCardDoubled == null) {
			FridayUtil.throwErr("Fighting card doubled map is null");
		}
		
		Boolean val = lhsOrRhsFightingCardDoubled.get(c.getPhysicalCardId());
		if(val == null || val == false) {
			return false;
		}
		
		return true;
		
	}
	
	// ------------------------------------------------------------------------
	

	public JsonGameState toJson() {
		JsonGameState result = new JsonGameState();
		
		if(abilityObject != null) {
			result.setAbilityObject(abilityObject.toJson());
		} else {
			result.setAbilityObject(null);
		}
		
		if(activeHazardCard != null) {
			result.setActiveHazardCard(activeHazardCard.getPhysicalCardId());
		} else {
			result.setActiveHazardCard(null);
		}
		
		result.setDiscardFightCards(discardFightCards.asIntArray());
		result.setDiscardHazards(discardHazards.asIntArray());
		result.setHazardCards(hazardCards.asIntArray());
		result.setLhs_fightCards(lhs_fightCards.asIntArray());
		
		if(lhsOrRhsFightingCardDoubled != null) {
			result.setLhsOrRhsFightingCardDoubled(lhsOrRhsFightingCardDoubled.toMapForJson());	
		} else {
			result.setLhsOrRhsFightingCardDoubled(null);
		}
		
		if(lhsOrRhsFightingCardUsed != null) {
			result.setLhsOrRhsFightingCardUsed(lhsOrRhsFightingCardUsed.toMapForJson());			
		} else {
			result.setLhsOrRhsFightingCardUsed(null);
		}

		result.setLifePoints(lifePoints);
		result.setRhs_fightCards(rhs_fightCards.asIntArray());
		result.setSlowGameState(slowGameState.toJson());
		result.setState(state.name());
		result.setYourFightingCards(yourFightingCards.asIntArray());
		
		return result;
	}
	
	public final AbilityObject getAbilityObject() {
		return abilityObject;
	}
	
	public final static ImmutableGrowableListCards defaultLhsFightCards() {
		return new ImmutableGrowableListCards(6);
	}
	
	public final static ImmutableGrowableListCards defaultRhsFightCards() {
		return new ImmutableGrowableListCards(6);
	}
	
	public final static ImmutableGrowableListCards defaultDiscardHazards() { 
		return new ImmutableGrowableListCards(15);
	}
	
	public final static ImmutableGrowableListCards defaultFightingDiscard() {
		return new ImmutableGrowableListCards(60);
	}
	
	public final ImmutableGrowableListCards getLhsFightCards() {
		return lhs_fightCards;
	}
	
	public final ImmutableGrowableListCards getRhsFightCards() {
		return rhs_fightCards;
	}

	public final int getPhaseNumber() {
		return slowGameState.getPhaseNumber();
	}

	public final ListCards getAgingCards() {
		return slowGameState.getAgingCards();
	}

	public final ListCards getYourFightingCards() {
		return yourFightingCards;
	}
	
	public final Card getActiveHazardCard() {
		return activeHazardCard;
	}

	public final ListCards getHazardCards() {
		return hazardCards;
	}
	
	public final PirateCard[] getActivePirates() {
		return slowGameState.getActivePirates();
	}

	public final int getLifePoints() {
		return lifePoints;
	}

	public final ImmutableGrowableListCards getDiscardHazards() {
		return discardHazards;
	}

	public final State getState() {
		return state;
	}
	
	public final SlowGameState getSlowGameState() {
		return slowGameState;
	}
	
	public final ImmutableGrowableListCards getDiscardFightCards() {
		return discardFightCards;
	}
	
	public final MapCards getLhsOrRhsFightingCardDoubled() {
		return lhsOrRhsFightingCardDoubled;
	}
	
	public final MapCards getLhsOrRhsFightingCardUsed() {
		return lhsOrRhsFightingCardUsed;
	}

}
