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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.fridai.Card.Type;
import com.fridai.PirateCard.PirateCardAbility;
import com.fridai.actions.ActionResponse;
import com.fridai.actions.UseCardAbilityAction;
import com.fridai.actions.UseCardAbilitySortAction;
import com.fridai.actions.UseCardAbilityWithTargetAction;
import com.fridai.actions.UseCopyAbilityAction;
import com.fridai.json.JsonPirateGameState;
import com.fridai.ui.ansi.AnsiCards;
import com.fridai.util.FridayUtil;
import com.fridai.util.ImmutableGrowableListCards;
import com.fridai.util.ListCards;
import com.fridai.util.MapCards;

/** An encapsulation of the full state of the game after each player action, while the player is fighting pirates. 
 * 
 * An instance of this class is immutable, and methods are called on this instance based on player actions.
 * A player action causes a new PirateGameState to be created (by this class) that represents how the game has changed
 * after that player action. */

public final class PirateGameState {

	public enum PirateState {
		SELECT_A_PIRATE,
		DRAW_FREE_CARDS,
		SACRIFICE_LIFE_TO_DRAW
	};

	private final PirateState state;

	private final int lifePoints;

	private final ListCards yourFightingCards;

	private final SlowGameState slowGameState;
	
	private final ImmutableGrowableListCards discardFightCards;

	// Nullable
	private final MapCards/*<Integer - physical card id, Boolean>*/ lhsOrRhsFightingCardUsed;
	
	// Nullable
	private final MapCards/*<Integer - physical card id, Boolean>*/ lhsOrRhsFightingCardDoubled;
	// TODO: LOWER - Can a card w/ DOUBLE ability be used, and then discarded, or must it stick around until the end of the fight in order to be used?

	// TODO: LOWER - We should prevent drawing more than the alloted cards in the draw free cards phase, regardless of what stop does.
	
	// ------------------------
	
	private final ImmutableGrowableListCards lhs_fightCards;
	private final ImmutableGrowableListCards rhs_fightCards;

	// Nullable (null until the first pirate is selected)
	private final PirateCard activePirateCard;
	
	// Nullable (null until the first pirate is selected)
	private final PirateCardInfo pirateCardInfo;
	
	// Nullable
	private final AbilityObject abilityObject;
	
	// ------------------------
	
	
	public PirateGameState(PirateState state, ListCards yourFightingCards, SlowGameState slowGameState, 
			PirateCard activePirateCard, PirateCardInfo pirateCardInfo, ImmutableGrowableListCards discardFightCards, 
			MapCards lhsOrRhsFightingCardUsed,  int lifePoints, ImmutableGrowableListCards lhs_fightCards, 
			ImmutableGrowableListCards rhs_fightCards, MapCards lhsOrRhsFightingCardDoubled, 
			AbilityObject abilityObject, Object previousGameState, RuntimeObject newRuntimeObject) {
		
		this.state = state;

		if(FridayUtil.RUNTIME_CHECK) {
			if(this.state == PirateState.SELECT_A_PIRATE && abilityObject != null) {
				FridayUtil.throwErr("AbilityObject should not be defined here.");
			}
		}
		
//		this.pirateGameStateId = nextPirateGameId.get();
//		nextPirateGameId.set(this.pirateGameStateId+1);

		if(FridayUtil.RUNTIME_CHECK) {
			if(previousGameState != null) {
				RuntimeObject previousRuntimeObj = RuntimeObjectMap.getInstance().getByObject(previousGameState);
				RuntimeObjectMap.getInstance().put(this, previousRuntimeObj);
			}
		}
		
		this.yourFightingCards = yourFightingCards;
		this.discardFightCards = discardFightCards;
		this.slowGameState = slowGameState;
		
		this.lifePoints = (FridayUtil.ALLOW_LARGE_LIFE_POINTS ?  lifePoints : (lifePoints > 22) ? 22 : lifePoints);
		
		this.pirateCardInfo = pirateCardInfo;
		this.activePirateCard = activePirateCard;
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
	
	public static void validate(PirateGameState gs, RuntimeObject newRuntimeObject, Object previousGameStateNotUsed) {
		if(!FridayUtil.RUNTIME_CHECK) { return; }
		
		RuntimeObject runtimeObject = newRuntimeObject != null ? newRuntimeObject : RuntimeObjectMap.getInstance().get(gs);
		
		GameState.runtimeCheckAllCardsPresent(gs.yourFightingCards, null, null, 
				gs.slowGameState, null, gs.discardFightCards, gs.lhs_fightCards,
				gs.rhs_fightCards, gs.abilityObject, runtimeObject, true);
		
		if(gs.state == PirateState.DRAW_FREE_CARDS && gs.activePirateCard == null) {
			FridayUtil.throwErr("Bad state: pirate is null in draw_free_cards");
		}
		
		if(gs.state == PirateState.DRAW_FREE_CARDS && !gs.calculateCanDrawXFightingCards(1)) {
			FridayUtil.throwErr("Bad state: Can't be in DRAW_FREE_CARDS if drawing isn't possible");
		}
		
		if(gs.state == PirateState.SELECT_A_PIRATE &&  (gs.lhs_fightCards.size() > 0 || gs.rhs_fightCards.size() > 0) ) {
			FridayUtil.throwErr("Bad state - lhs and rhs are > 0");
		}
		
		if(gs.pirateCardInfo == null && gs.activePirateCard != null) {
			FridayUtil.throwErr("Pirate card info should not be null if a pirate is selected.");
		}
		
		if(gs.pirateCardInfo != null && gs.getActivePirateCard() == null) {
			FridayUtil.throwErr("Active pirate card should not be null if there is pirate info.");
		}
		
		HashMap<Integer, Boolean> cardsSeen = new HashMap<>();
		
		List<Object> cardsFromAllSources = new ArrayList<>();
		cardsFromAllSources.add("yfs");
		cardsFromAllSources.addAll(gs.yourFightingCards.asList());
//		cardsFromAllSources.add("hc");
//		cardsFromAllSources.addAll(gs.hazardCards.asList());
//		cardsFromAllSources.add("dh");
//		cardsFromAllSources.addAll(gs.discardHazards.getAsList());
//		if(gs.activeHazardCard != null) {
//			cardsFromAllSources.add("ahc");
//			cardsFromAllSources.add(gs.activeHazardCard);
//		}
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


	public PirateGameState sacrificeLifeToDraw_flagCopyAbilityAsUsed(UseCopyAbilityAction action) {

		MapCards newLhsOrRhsFightingCardUsed = new MapCards(lhsOrRhsFightingCardUsed);
		newLhsOrRhsFightingCardUsed.put(action.getCard().getPhysicalCardId(), true);

		PirateGameState result = new PirateGameState(this.state, yourFightingCards, slowGameState, 
				activePirateCard, pirateCardInfo, discardFightCards,  newLhsOrRhsFightingCardUsed,
				lifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled,
				abilityObject, this, null /* runtime object*/);
		
		return result;
	}

	
	public PirateGameState selectAPirate_selectPirate(PirateCard selectedCard) {

		// Remove the selected card
		ArrayList<PirateCard> newList = new ArrayList<>();
		newList.addAll( Arrays.asList(slowGameState.getActivePirates()));
		for(Iterator<PirateCard> it = newList.iterator(); it.hasNext();) {
			if(it.next().getPirateCardId() == selectedCard.getPirateCardId()) {
				it.remove();
			}
		}
		
		int pirateHazardValue, freeCards;
		
		if(selectedCard.getAbility() == PirateCardAbility.FIGHT_AGAINST_ALL_REMAINING_HAZARD_CARDS) {
			
			PirateCardInfo wildCardInfo = getSlowGameState().getWildCardPirate();
			if(wildCardInfo == null) { FridayUtil.throwErr("Wild card info was null for wild card pirate.");}
			
			pirateHazardValue = wildCardInfo.getHazardPoints();
			freeCards = wildCardInfo.getFreeFightingCards();
			
		} else if(selectedCard.getAbility() == PirateCardAbility.TWO_HAZARD_POINTS_FOR_EACH_AGING_CARD) {
			
			int agingCardsDrawn = (slowGameState.getGameLevel() >= 3 ? 11 : 10) - slowGameState.getAgingCards().size();

			pirateHazardValue = agingCardsDrawn*2;
			freeCards = selectedCard.getFreeCards();
			
		} else {
			pirateHazardValue = selectedCard.getHazardValue();
			freeCards = selectedCard.getFreeCards();
		}
		
				
		SlowGameState newSlowGameState = new SlowGameState(slowGameState.getAgingCards(), slowGameState.getGameLevel(),
				newList.toArray(new PirateCard[newList.size()]), slowGameState.getActiveRoundCards(), 
				slowGameState.getPhaseNumber(), slowGameState.getWildCardPirate());
		
		PirateGameState result = new PirateGameState(PirateState.DRAW_FREE_CARDS, yourFightingCards, newSlowGameState, 
				selectedCard, new PirateCardInfo(pirateHazardValue, freeCards), discardFightCards,  
				lhsOrRhsFightingCardUsed, lifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled,
				abilityObject, this, null /* runtime object*/);
		
		return result;
		
	}
	
	public PirateGameState drawFreeCards_drawACard() {
		return inner_drawFreeCards_drawACard(this);
 	}

	private static PirateGameState inner_drawFreeCards_drawACard(PirateGameState gs) {

		gs = gs.ensureFightStackHasOneCard();

		// Remove a card from the front of fighting card stack
		Card card = gs.getYourFightingCards().get(0);
		ListCards newYourFightingCards = gs.getYourFightingCards().mutateRemoveFromFront(1);

		// Add it LHS
		ImmutableGrowableListCards newLhs_fightCards = gs.getLhsFightCards().mutateAdd(card);

		PirateState newState = gs.getState();

		// Have we drawn all our free cards, or have we run out of cards to draw?
		if(newLhs_fightCards.size() == gs.getPirateCardInfo().getFreeFightingCards() || (newYourFightingCards.size() == 0 && gs.getDiscardFightCards().size() == 0)) {
			newState = PirateState.SACRIFICE_LIFE_TO_DRAW;
		}

		
		PirateGameState result = new PirateGameState(newState, newYourFightingCards, gs.getSlowGameState(), 
				gs.getActivePirateCard(), gs.pirateCardInfo, gs.getDiscardFightCards(),  gs.getLhsOrRhsFightingCardUsed(),
				gs.getLifePoints(), newLhs_fightCards, gs.getRhsFightCards(), gs.getLhsOrRhsFightingCardDoubled(),
				gs.getAbilityObject(), gs, null /* runtime object*/);

		return result;

	}
	
	public PirateGameState drawFreeCards_endDrawFreeCardsPhase() {
		
		PirateGameState result = new PirateGameState(PirateState.SACRIFICE_LIFE_TO_DRAW, yourFightingCards, slowGameState, 
				activePirateCard, pirateCardInfo, discardFightCards,  lhsOrRhsFightingCardUsed,
				lifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled,
				abilityObject, this, null /* runtime object*/);

		return result;
	}

	
	private PirateGameState ensureFightStackHasOneCard() {

		PirateGameState result;

		// If we have run out of fighting cards
		if(yourFightingCards.size() == 0) {

			if(discardFightCards.size() == 0) {
				FridayUtil.throwErr("Cannot ensure fight stack has one card: discard fight stack is empty.");
			}
			
			if (OUT_ENABLED) {
				out("Fighting card stack been reshuffled, and an aging card has been added.", this);
			}

			SlowGameState newSlowGameState = slowGameState;
			ListCards newYourFightingCards;

			List<Card> innerNewFightingCards = discardFightCards.getAsList();

			int newLifePoints = lifePoints;
			
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

			ImmutableGrowableListCards newDiscardFightCards = new ImmutableGrowableListCards(60);

			if(newYourFightingCards.size() == 0 && slowGameState.getAgingCards().size() > 0 && discardFightCards.size() > 0) {
				FridayUtil.throwErr("Fighting card stack is empty after reshuffle?");
			}

			result = new PirateGameState(this.state, newYourFightingCards, newSlowGameState, 
					activePirateCard, pirateCardInfo, newDiscardFightCards,  lhsOrRhsFightingCardUsed,
					newLifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled,
					abilityObject, this, null /* runtime object*/);

		} else {
			result = this;
		}

		return result;
	}

	public PirateGameState sacrificeLifeToDraw_drawACard() {
		return inner_sacrificeLifeToDraw_drawACard(this);
	}

	private static PirateGameState inner_sacrificeLifeToDraw_drawACard(PirateGameState gs) {

		gs = gs.ensureFightStackHasOneCard();

		Card card = gs.getYourFightingCards().get(0);
		ListCards newYourFightingCards = gs.getYourFightingCards().mutateRemoveFromFront(1);

		ImmutableGrowableListCards newRhs_fightCards = gs.getRhsFightCards().mutateAdd(card);

		int pointsPerFightingCards = gs.getActivePirateCard().getAbility() == PirateCardAbility.EACH_ADDITIONAL_FIGHTING_CARD_COSTS_2_LIFE_POINTS 
				? 2 : 1;
		
		int newLifePoints = gs.getLifePoints() - pointsPerFightingCards;

		if(OUT_ENABLED) {
			out("Your life points are now at " + newLifePoints, gs);
		}

		PirateGameState result = new PirateGameState(gs.getState(), newYourFightingCards, gs.getSlowGameState(), 
				gs.getActivePirateCard(), gs.pirateCardInfo, gs.getDiscardFightCards(), gs.getLhsOrRhsFightingCardUsed(),
				newLifePoints, gs.getLhsFightCards(), newRhs_fightCards, gs.getLhsOrRhsFightingCardDoubled(),
				gs.getAbilityObject(), gs, null /* runtime object*/);

		
		return result;
	}

	public PirateGameState sacrificeLifeToDraw_useCardAbility(UseCardAbilityAction action, boolean isCopiedAbility) {

		return innerSacrificeLifeToDraw_useCardAbility(action, this, isCopiedAbility);
	}

	private static PirateGameState innerSacrificeLifeToDraw_useCardAbility(UseCardAbilityAction ucaa, PirateGameState gs, boolean isCopiedAbility) {

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
			newLhsOrRhsFightCardsUsed.put((Integer) c.getPhysicalCardId(), true); // mark c as used
		}

		if(c.getAbility() == Ability.CARDS_DRAW_1) {
			gs = gs.ensureFightStackHasOneCard();

			// Get the top card from fight stack, remove the top card, and add it to RHS
			Card newCard = gs.getYourFightingCards().get(0);
			newYourFightingCards = gs.getYourFightingCards().mutateRemoveFromFront(1);
			newRhsFightCards = gs.getRhsFightCards().mutateAdd(newCard);

		} else if (c.getAbility() == Ability.CARDS_DRAW_2) {
			gs = gs.ensureFightStackHasOneCard();
			
			// Get the top card from fight stack, remove the top card, and add it to RHS
			Card newCard = gs.getYourFightingCards().get(0);
			newYourFightingCards = gs.getYourFightingCards().mutateRemoveFromFront(1);
			newRhsFightCards = gs.getRhsFightCards().mutateAdd(newCard);

			if(newAbilityObject != null) {
				// We have already drawn two cards, so this ability is complete.
				newAbilityObject = null;
			} else {
				newAbilityObject = new AbilityObject(c, 1, 0, null, isCopiedAbility);
			}

		} else if (c.getAbility() == Ability.LIFE_ADD_1) {
			newLifePoints++;
		} else if (c.getAbility() == Ability.LIFE_ADD_2) {
			newLifePoints += 2;
		} else if (c.getAbility() == Ability.SORT_3_CARDS) {

			// This branch will be enter when 0, 1, or 2 cards have been draw (and the user
			// wants to draw, in the case of 1 and 2)

			gs = gs.ensureFightStackHasOneCard();
			newYourFightingCards = gs.getYourFightingCards();

			int newPhase = 0;

			// Get the top card from fight stack, remove the top card, and add it to RHS
			Card newCard = newYourFightingCards.get(0);
			newYourFightingCards = newYourFightingCards.mutateRemoveFromFront(1);

			List<Card> newDrawnSortCards = new ArrayList<>();

			if(newAbilityObject != null) {
				newDrawnSortCards.addAll(newAbilityObject.getDrawnSortCards());
			}
			newDrawnSortCards.add(newCard);

			if(newDrawnSortCards.size() == 3) {
				// Switch to the next phase once 3 cards have been drawn.
				newPhase = 1;
			}

			isCopiedAbility = isCopiedAbility || (newAbilityObject != null && newAbilityObject.isCopied());

			newAbilityObject = new AbilityObject(c, newDrawnSortCards.size(), newPhase, newDrawnSortCards,
					isCopiedAbility);
		} else if(c.getAbility() == Ability.PHASE_MINUS_1 && isCopiedAbility) {
			FridayUtil.throwErr("PHASE_MINUS_1 is not supported during pirate phase.");

		} else {
			FridayUtil.throwErr("Invalid action response for this method: " + ucaa);
		}

		if (newYourFightingCards == null) {
			newYourFightingCards = gs.getYourFightingCards();
		}

		if (newSlowGameState == null) {
			newSlowGameState = gs.getSlowGameState();
		}
	
		if(newRhsFightCards == null) {
			newRhsFightCards = gs.getRhsFightCards();
		}
		
		PirateGameState result = new PirateGameState(gs.getState(), newYourFightingCards, newSlowGameState, 
				gs.getActivePirateCard(), gs.pirateCardInfo, gs.getDiscardFightCards(), newLhsOrRhsFightCardsUsed,
				newLifePoints, gs.getLhsFightCards(), newRhsFightCards, gs.getLhsOrRhsFightingCardDoubled(),
				newAbilityObject, gs, null /* runtime object*/);
	
		
		return result;
	}
	
	@SuppressWarnings("unused")
	public PirateGameState sacrificeLifeToDraw_useCardAbilityWithTarget(UseCardAbilityWithTargetAction ucawta,
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
			} else if (rhs_fightCards.containsPhysicalId(cardToDestroy.getPhysicalCardId())) {
				newRhs_fightCards = newRhs_fightCards.mutateRemoveCardByPhysicalId(cardToDestroy.getPhysicalCardId());
			}

			if(OUT_ENABLED) {
				out("Card " + AnsiCards.asFightingCard(cardToDestroy, null) + " has been destroyed.", this);
			}

			RuntimeObject newRuntimeObject = null;
			if(FridayUtil.RUNTIME_CHECK) {
				newRuntimeObject = RuntimeObjectMap.getInstance().get(this);
				newRuntimeObject = newRuntimeObject.mutateAddDestroyedCard(cardToDestroy);
			}

			PirateGameState result = new PirateGameState(this.state, yourFightingCards, slowGameState, 
					activePirateCard, pirateCardInfo, discardFightCards, newLhsOrRhsFightingCardUsed,
					lifePoints, newLhs_fightCards, newRhs_fightCards, lhsOrRhsFightingCardDoubled,
					abilityObject, this, newRuntimeObject);
			
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
			if(FridayUtil.RUNTIME_CHECK && lhsOrRhsFightingCardDoubled.get(targetCard.getPhysicalCardId()) == true) {
				FridayUtil.throwErr("Target card is already doubled: " + targetCard);
			}
			MapCards newLhsOrRhsFightingCardDoubled = new MapCards(lhsOrRhsFightingCardDoubled);
			newLhsOrRhsFightingCardDoubled.put(targetCard.getPhysicalCardId(), true);

			if(OUT_ENABLED) {
				out("Card " + AnsiCards.asFightingCard(targetCard, null) + " has been doubled.", this);
			}

			PirateGameState result = new PirateGameState(this.state, yourFightingCards, slowGameState, 
					activePirateCard, pirateCardInfo, discardFightCards, newLhsOrRhsFightingCardUsed,
					lifePoints, lhs_fightCards, rhs_fightCards, newLhsOrRhsFightingCardDoubled,
					abilityObject, this, null);
			
			return result;

		} else if(ucawta.getCard().getAbility() == Ability.EXCHANGE_X1
				|| ucawta.getCard().getAbility() == Ability.EXCHANGE_X2) {

			PirateGameState gs = ensureFightStackHasOneCard();
			return inner_sacrificeLifeToDraw_useCardAbilityWithTarget_exchange(ucawta, gs, isCopiedAction);

		} else {
			FridayUtil.throwErr("Unrecognized card ability: " + ucawta.getCard());
		}

		return null;
	}
	
	private static PirateGameState inner_sacrificeLifeToDraw_useCardAbilityWithTarget_exchange(
			UseCardAbilityWithTargetAction ucawta, PirateGameState gs, boolean isCopiedAction) {

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
			FridayUtil.throwErr("Could not find card to discard in Rhs or Lhs: " + cardToDiscard);
		}

		if(OUT_ENABLED) {
			out("Card " + AnsiCards.asFightingCard(cardToDiscard, null) + " has been exchanged.", gs);
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

		gs = new PirateGameState(gs.getState(), newYourFightingCards, gs.getSlowGameState(), 
				gs.getActivePirateCard(), gs.pirateCardInfo, newDiscardFightCards, newLhsOrRhsFightingCardUsed,
				gs.getLifePoints(), newLhsFightCards, newRhsFightCards, gs.getLhsOrRhsFightingCardDoubled(),
				newAbilityObject, gs, null /* runtime object*/);

		return gs;

	}


	private static PirateGameState inner_sacrificeLifeToDraw_useCardAbilityWithTarget_belowThePile(
			UseCardAbilityWithTargetAction ucawta, PirateGameState gs, boolean isCopiedAction) {

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
			out("Card " + AnsiCards.asFightingCard(cardToBury, null) + " has been moved to the bottom of the pile.", gs);
		}

		// The card we are burying gets marked as unused, as if we see it again it will
		// be from a redraw.
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
			ImmutableGrowableListCards newLhs_fightCards = gs.getLhsFightCards()
					.mutateRemoveCardByPhysicalId(cardToBury.getPhysicalCardId());

			// Add new card to LHS
			newLhs_fightCards = newLhs_fightCards.mutateAdd(replacementCard);

			PirateGameState result = new PirateGameState(gs.getState(), newYourFightingCards, gs.getSlowGameState(), 
					gs.getActivePirateCard(), gs.pirateCardInfo, gs.getDiscardFightCards(), newLhsOrRhsFightingCardUsed,
					gs.getLifePoints(), newLhs_fightCards, gs.getRhsFightCards(), gs.getLhsOrRhsFightingCardDoubled(),
					gs.getAbilityObject(), gs, null /* runtime object*/);
			
			return result;

		} else if (gs.getRhsFightCards().containsPhysicalId(cardToBury.getPhysicalCardId())) {
			// Remove the card from RHS
			ImmutableGrowableListCards newRhs_fightCards = gs.getRhsFightCards()
					.mutateRemoveCardByPhysicalId(cardToBury.getPhysicalCardId());

			// Add to bottom of stack
			ListCards newYourFightingCards = gs.getYourFightingCards().mutateAddToBottom(cardToBury);

			PirateGameState result = new PirateGameState(gs.getState(), newYourFightingCards, gs.getSlowGameState(), 
					gs.getActivePirateCard(), gs.pirateCardInfo, gs.getDiscardFightCards(), newLhsOrRhsFightingCardUsed,
					gs.getLifePoints(), gs.getLhsFightCards(), newRhs_fightCards, gs.getLhsOrRhsFightingCardDoubled(),
					gs.getAbilityObject(), gs, null /* runtime object*/);
			
			return result;

		}

		FridayUtil.throwErr("Target card not found on RHS or LHS: " + cardToBury);

		return null;
	}
	
	public PirateGameState sacrificeLifeToDraw_endMultistageAbilityResponse(ActionResponse r) {

		AbilityObject newAbilityObject = abilityObject;
		Card c = abilityObject.getActiveCard();
		if(c.getAbility() == Ability.SORT_3_CARDS) {
			// Advance to the next stage without drawing any more cards
			newAbilityObject = new AbilityObject(c, abilityObject.getNumberOfCardsDrawn(), abilityObject.getStage() + 1,
					abilityObject.getDrawnSortCards(), abilityObject.isCopied());

		} else {
			// For all other abilities, the stage is complete, so reset the object
			newAbilityObject = null;
		}

		PirateGameState result = new PirateGameState(this.state, yourFightingCards, slowGameState, 
				activePirateCard, pirateCardInfo, discardFightCards, lhsOrRhsFightingCardUsed,
				lifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled,
				newAbilityObject, this, null);

		
		return result;
	}


	@SuppressWarnings("unused")
	public PirateGameState sacrificeLifeToDraw_useCaseAbilitySort(ActionResponse r) {
		UseCardAbilitySortAction ucasa = (UseCardAbilitySortAction) r.getAction();

		// Add the sorted card in their new order
		List<Card> newList = new ArrayList<>();
		for (Card c : ucasa.getSortOrder()) {
			if (FridayUtil.RUNTIME_CHECK && c == null) {
				FridayUtil.throwErr("null in sort order");
			}
			newList.add(c);
		}

		// The cards in getSortOrder() have already been removed from the fighting card stack
		newList.addAll(yourFightingCards.asList());

		ListCards newYourFightingCards = new ListCards(newList.toArray(new Card[newList.size()]), 0);

		ImmutableGrowableListCards newDiscardFightCards = discardFightCards;
		if(ucasa.getDiscard() != null) {
			newDiscardFightCards = newDiscardFightCards.mutateAdd(ucasa.getDiscard());
		}

		if(OUT_ENABLED) {
			String text = "Cards sorted to fighting card stack: ";

			int x = 1;
			for (Card c : ucasa.getSortOrder()) {
				text += x + ") " + AnsiCards.asFightingCard(c, null) + "  ";
				x++;
			}
			if (ucasa.getDiscard() != null) {
				text += "Discard: " + AnsiCards.asFightingCard(ucasa.getDiscard(), null);
			}
			out(text, this);
		}

		AbilityObject newAbilityObject = null;

		PirateGameState result = new PirateGameState(this.state, newYourFightingCards, slowGameState, 
				activePirateCard, pirateCardInfo, newDiscardFightCards, lhsOrRhsFightingCardUsed,
				lifePoints, lhs_fightCards, rhs_fightCards, lhsOrRhsFightingCardDoubled,
				newAbilityObject, this, null);
		

		return result;

	}

	
	/** Returns null on win */
	public PirateGameState sacrificeLifeToDraw_endPirateRound() {
		// If There are no active pirates left, you win!	
		
		if(slowGameState.getActivePirates().length == 0) {
			return null;
		}
		
		if(OUT_ENABLED) { out("Pirate was beaten", this); }
		
		// Add used (non-destroyed) fighting cards to fighting card discard pile
		ImmutableGrowableListCards newDiscardFightCards = discardFightCards.mutateAddAll(lhs_fightCards);
		
		if(rhs_fightCards.size() > 0) {
			newDiscardFightCards = newDiscardFightCards.mutateAddAll(rhs_fightCards);	
		}
		
		// Clear LHS and RHS
		
		MapCards newLhsOrRhsFightingCardUsed = new MapCards();
		MapCards newLhsOrRhsFightingCardDoubled = new MapCards();

		PirateGameState result = new PirateGameState(PirateState.SELECT_A_PIRATE, yourFightingCards, slowGameState, 
				null /* active pirate card */, null /*pirateCardInfo */, newDiscardFightCards, newLhsOrRhsFightingCardUsed, 
				lifePoints, defaultLhsFightCards(), defaultRhsFightCards(), newLhsOrRhsFightingCardDoubled, 
				abilityObject, this, null);
		
		return result;

	}

	
	public static PirateGameState transferFromGameState(GameState gs) {
		
		boolean containsWildcardPirate = false;
		
		for(PirateCard pc : gs.getSlowGameState().getActivePirates()) {
			if(pc.getAbility() == PirateCardAbility.FIGHT_AGAINST_ALL_REMAINING_HAZARD_CARDS) {
				containsWildcardPirate = true;
			}
		}
		
		PirateCardInfo pirateCardInfo = null;
		
		SlowGameState newSlowGameState = gs.getSlowGameState();
		
		if(containsWildcardPirate) {
			
			if(gs.getActiveHazardCard() != null) { FridayUtil.throwErr("Active hazard card should be null;"); }
			if(gs.getDiscardHazards().size() > 0) { FridayUtil.throwErr("The dicard hazards should be empty."); }
			
			int hazardValue = 0;
			int freeCards = 0;
			for(int x = 0; x < gs.getHazardCards().size(); x++) {
				Card c = gs.getHazardCards().get(x);
				hazardValue += c.getHazardValues()[0];
				freeCards += c.getFreeCards();
			}
			
			pirateCardInfo = new PirateCardInfo(hazardValue, freeCards);
			
			newSlowGameState = new SlowGameState(newSlowGameState.getAgingCards(), newSlowGameState.getGameLevel(),
					newSlowGameState.getActivePirates(), newSlowGameState.getActiveRoundCards(), 
					newSlowGameState.getPhaseNumber(), pirateCardInfo);
			
		}
		
		if(gs.getRhsFightCards().size() > 0) { FridayUtil.throwErr("Non-empty rhs fight cards."); }
		if(gs.getLhsFightCards().size() > 0) { FridayUtil.throwErr("Non-empty rhs fight cards."); }
		
		if(gs.getAbilityObject() != null) { FridayUtil.throwErr("Ability object is not null"); }
				
		return new PirateGameState(PirateState.SELECT_A_PIRATE, gs.getYourFightingCards(), newSlowGameState, 
				/* active pirate card*/ null, /* pirate card info*/ null, gs.getDiscardFightCards(), 
				new MapCards() /* fighting cards used */, gs.getLifePoints(), gs.getLhsFightCards(), 
				gs.getRhsFightCards(), new MapCards() /* fighting cards doubled */,
				gs.getAbilityObject(), gs /* previous game state*/, null /* runtime object*/);
				 
	}


	
	// -----------------------------------------------------------------------------------------
	
	public JsonPirateGameState toJson() {
		JsonPirateGameState result = new JsonPirateGameState();
		
		if(abilityObject != null) {
			result.setAbilityObject(abilityObject.toJson());
		} else {
			result.setAbilityObject(null);
		}

		if(activePirateCard != null) {
			result.setActivePirateCard(activePirateCard.getPirateCardId());
		} else {
			result.setActivePirateCard(null);
		}
		
		if(pirateCardInfo != null) {
			result.setPirateCardInfo(pirateCardInfo.toJson());
		} else {
			result.setPirateCardInfo(null);
		}
		
		result.setDiscardFightCards(discardFightCards.asIntArray());
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

	
	@SuppressWarnings("unused")
	public int calculateRemainingHazardValue_halfFaceUpFighting() {
		if(!(activePirateCard.getAbility() == PirateCardAbility.ONLY_HALF_OF_FACE_UP_FIGHTING_CARDS_COUNT)) {
			FridayUtil.throwErr("Invalid method called.");
			return -1;
		}
		
		boolean containsHighestFightCardEquals0 = false;

		List<Card> finalList = new ArrayList<>();
		
		List<Card> lhsAndRhsCards = new ArrayList<>();

		final MapCards isDoubled = lhsOrRhsFightingCardDoubled;
		final CalculateRemainingHazardValues_HalfFaceUpFighting_Comparator comparator = new CalculateRemainingHazardValues_HalfFaceUpFighting_Comparator(isDoubled);

		// All aging cards go into finalList, all non-aging are sorted ascending into lhsAndRhsCards 
		{
			
			for(Card c : getLhsFightCards().getAsList()) {
				if(c.getType() == Type.AGING) { 
					finalList.add(c); 
					if(c.getAbility() == Ability.HIGHEST_CARD_IS_0) { containsHighestFightCardEquals0 = true;}  
				} else { lhsAndRhsCards.add(c); }
			}
			
			for(Card c : getRhsFightCards().getAsList()) {
				if(c.getType() == Type.AGING) { 
					finalList.add(c);
					if(c.getAbility() == Ability.HIGHEST_CARD_IS_0) { containsHighestFightCardEquals0 = true;}
				} else { lhsAndRhsCards.add(c); }			
			}
			
			Collections.sort(lhsAndRhsCards, comparator);
		}

		int maxCardsInFinalList = (int) Math.ceil((finalList.size() + lhsAndRhsCards.size())/2d);

		// If we are excluding the value of the highest fighting card, then calculate the value to add
		int highestFightValueToAdd = 0;
		
		if(finalList.size() >= maxCardsInFinalList) {
			
			// Since we already have more cards in the final list then we need, sort and remove them...
			
			Collections.sort(finalList, comparator); // We have sorted ascending, for example: -4, -3, -2, -1
			
			while(finalList.size() > maxCardsInFinalList) {
				finalList.remove(0); // Remove the most negative aging cards first; it is assumed this is the desired behaviour of the player.
			}
			
			if(FridayUtil.RUNTIME_CHECK && maxCardsInFinalList != finalList.size()) { FridayUtil.throwErr("Card size mismatch: "+maxCardsInFinalList+" "+finalList.size()); }
			
		} else {
			
			// If there will be at least one non-aging fighting card in the finalList..
			
			if(containsHighestFightCardEquals0 && lhsAndRhsCards.size() > 0) {
				Card highestValue = lhsAndRhsCards.get(lhsAndRhsCards.size()-1);
				int value = highestValue.getFightingValue();
				
				if(isDoubled.get(highestValue.getPhysicalCardId()) == Boolean.TRUE) {
					value *=2 ;
				}
				
				if(value > 0) {
					highestFightValueToAdd = value;
				}
			}
			
			// Add the highest value cards to the final list, until we have half

			while(finalList.size() < maxCardsInFinalList) {
				finalList.add(lhsAndRhsCards.remove(lhsAndRhsCards.size()-1));
			}
			// It is possible to have 
			
			if(FridayUtil.RUNTIME_CHECK && maxCardsInFinalList != finalList.size()) { FridayUtil.throwErr("Card size mismatch: "+maxCardsInFinalList+" "+finalList.size()); }
			
		}
		
	
		int result = 0;
		for(Card c : finalList) {
			
			int value = c.getFightingValue();
			if(isDoubled.get(c.getPhysicalCardId()) == Boolean.TRUE) {
				value *= 2;
			}
			
			result -= value;
		}
		
		result += highestFightValueToAdd;
		
		result += getPirateCardInfo().getHazardPoints();
		
		return result;
	}
	
	public final static class CalculateRemainingHazardValues_HalfFaceUpFighting_Comparator implements Comparator<Card> {

		private final MapCards isDoubled;
		
		public CalculateRemainingHazardValues_HalfFaceUpFighting_Comparator(final MapCards isDoubled) {
			this.isDoubled = isDoubled;
		}
		
		@Override
		public int compare(Card o1, Card o2) {
			
			int o1Val = o1.getFightingValue();
			if(isDoubled.get(o1.getPhysicalCardId()) == Boolean.TRUE) { o1Val *=2 ; }
			int o2Val = o2.getFightingValue();
			if(isDoubled.get(o2.getPhysicalCardId()) == Boolean.TRUE) { o2Val *=2 ; }

			return o1Val - o2Val;
		}
		
	}
	
	public int calculateRemainingHazardValue() {

		// Special case: If fighting the half face up pirate, then use a different calculation.
		if(activePirateCard.getAbility() == PirateCardAbility.ONLY_HALF_OF_FACE_UP_FIGHTING_CARDS_COUNT) {
			return calculateRemainingHazardValue_halfFaceUpFighting();
		}
		
		boolean eachFaceUpFightingCardIsPlus1 = activePirateCard.getAbility() == PirateCardAbility.EACH_FACE_UP_FIGHTING_CARD_COUNTS_PLUS1_FIGHTING_POINT;
		
		int currHazardVal = 0;
		
		MapCards isDoubled = lhsOrRhsFightingCardDoubled;

		boolean containsHighestFightCardEquals0 = false;
		Card highestCard = null;

		for (Card c : getLhsFightCards().getAsList()) {
			if(c.getAbility() == Ability.HIGHEST_CARD_IS_0) {
				containsHighestFightCardEquals0 = true;
			}

			int fightingVal = c.getFightingValue();

			if(isDoubled.get(c.getPhysicalCardId()) == Boolean.TRUE) {
				fightingVal *= 2;
			}

			if(highestCard == null || c.getFightingValue() > highestCard.getFightingValue()) {
				highestCard = c;
			}
			
			if(eachFaceUpFightingCardIsPlus1) {
				currHazardVal--;
			}
			
			currHazardVal -= fightingVal;
		}

		for(Card c : getRhsFightCards().getAsList()) {
			if(c.getAbility() == Ability.HIGHEST_CARD_IS_0) {
				containsHighestFightCardEquals0 = true;
			}

			int fightingVal = c.getFightingValue();

			if(isDoubled.get(c.getPhysicalCardId()) == Boolean.TRUE) {
				fightingVal *= 2;
			}

			if(highestCard == null || c.getFightingValue() > highestCard.getFightingValue()) {
				highestCard = c;
			}

			if(eachFaceUpFightingCardIsPlus1) {
				currHazardVal--;
			}

			currHazardVal -= fightingVal;

		}

		currHazardVal += getPirateCardInfo().getHazardPoints();

		if(containsHighestFightCardEquals0 && highestCard != null && highestCard.getFightingValue() > 0) {
			
			currHazardVal += highestCard.getFightingValue(); // Yes this is correct as '+=' 
		}

		return currHazardVal;
	}
	
	public boolean calculateCanDrawXFightingCards(int x) {
		return yourFightingCards.size() + discardFightCards.size() >= x;
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
	
	private final static boolean OUT_ENABLED = false;

	@SuppressWarnings("unused")
	private static void out(PirateGameState gs) {
		if (!OUT_ENABLED) {
			return;
		}

//		System.out.println();
	}

	private static void out(String str, PirateGameState gs) {
		if (!OUT_ENABLED) {
			return;
		}

//		System.out.println();
//		System.out.println(ansi().fgBrightGreen().a("* ").reset()+str);
	}

	// ------------------------------------------------------------------------
	

	public final AbilityObject getAbilityObject() {
		return abilityObject;
	}
	
	public final static ImmutableGrowableListCards defaultLhsFightCards() {
		return new ImmutableGrowableListCards(6);
	}
	
	public final static ImmutableGrowableListCards defaultRhsFightCards() {
		return new ImmutableGrowableListCards(6);
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

	public PirateCard getActivePirateCard() {
		return activePirateCard;
	}
	
	public final PirateCard[] getActivePirates() {
		return slowGameState.getActivePirates();
	}

	public final int getLifePoints() {
		return lifePoints;
	}

	public final PirateState getState() {
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

	public PirateCardInfo getPirateCardInfo() {
		return pirateCardInfo;
	}
	
}
