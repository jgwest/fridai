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

import static org.fusesource.jansi.Ansi.ansi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.fridai.Card.Difficulty;
import com.fridai.GameState.State;
import com.fridai.PirateGameState.PirateState;
import com.fridai.actions.Action;
import com.fridai.actions.ActionResponse;
import com.fridai.actions.DestroyCardsWithPaidLifeAction;
import com.fridai.actions.DestroyCardsWithPaidLifeActionResponse;
import com.fridai.actions.DrawAFreeCardActionResponse;
import com.fridai.actions.EndDrawFreeCardPhaseActionResponse;
import com.fridai.actions.EndMultistageAbilityActionResponse;
import com.fridai.actions.EndPirateRoundActionResponse;
import com.fridai.actions.EndSacrificeLifePhaseActionResponse;
import com.fridai.actions.FightOrDiscardHazardCardActionResponse;
import com.fridai.actions.SacrificeLifeToDrawActionResponse;
import com.fridai.actions.SelectAPirateAction;
import com.fridai.actions.SelectAPirateActionResponse;
import com.fridai.actions.SelectFromTwoHazardCardsActionResponse;
import com.fridai.actions.UseCardAbilityActionResponse;
import com.fridai.actions.UseCardAbilitySortAction;
import com.fridai.actions.UseCardAbilitySortActionResponse;
import com.fridai.actions.UseCardAbilityWithTargetAction;
import com.fridai.actions.UseCardAbilityWithTargetActionResponse;
import com.fridai.actions.UseCopyAbilityAction;
import com.fridai.actions.Action.ActionType;
import com.fridai.ui.ansi.AnsiCards;
import com.fridai.ui.ansi.AnsiGrid;
import com.fridai.ui.ansi.AnsiGridPanel;
import com.fridai.util.FridayUtil;

/** Utility and methods used to implement the keyboard-driven ANSI game UI. */
public class MainUI {

	public static void printUIState(GameStateContainer container, Action actionBeingTaken) {

		if(container.isGameState()) {
			printUIStateNonPirate(container, actionBeingTaken);
			return;
		} else {
			printUIStatePirate(container, actionBeingTaken);
		}

	}
	
	public static void printUIStatePirate(GameStateContainer container, Action actionBeingTaken) {
		
		PirateGameState gs = container.getPirateGameState();
		
		if(gs.getState() == PirateState.SELECT_A_PIRATE) {

			System.out.println("Select a pirate to fight:");
			
			// Sort top 2 cards descending by free cards
			List<PirateCard> cardsInUI = new ArrayList<>();
			{
				PirateCard[] pirates = gs.getSlowGameState().getActivePirates(); 
				
				for(int x = 0; x < pirates.length; x++) {
					cardsInUI.add(pirates[x]);
				}
			}
			
			for(int x = 0; x < cardsInUI.size(); x++) {
				PirateCard c = cardsInUI.get(x);
	
				String str = "hazard value: "+ c.getFreeCards()+"  free cards: "+c.getFreeCards()+"  ability: "+c.getAbility().name(); 
				System.out.println(str);
				
			}
			
			System.out.println();
			
			System.out.println("Life points: "+gs.getLifePoints());
			System.out.println("Fighting stack remaining: "+gs.getYourFightingCards().size());

		} else {
			outputStatePirate(gs);
		}
	}
	
	public static void printUIStateNonPirate(GameStateContainer container, Action actionBeingTaken) {
		
		GameState gs = container.getGameState();
		
		if(gs.getState() == State.SELECT_A_HAZARD_CARD) {

			if(actionBeingTaken.getType() == ActionType.SELECT_FROM_TWO_HAZARD_CARDS) {
				System.out.println("Select a hazard, the remaining hazard will be discarded:");
				
				// Sort top 2 cards descending by free cards
				List<Card> cardsInUI = new ArrayList<>();
				{
					for(int x = 0; x < 2; x++) {
						cardsInUI.add(gs.getHazardCards().get(x));
					}
					Collections.sort(cardsInUI, (a,b) -> { 	
						return b.getFightingValue() - a.getFreeCards();
					});
				}
				
				AnsiGrid grid = new AnsiGrid();
				for(int x = 0; x < 2; x++) {
					Card c = cardsInUI.get(x);

					AnsiGridPanel panel = grid.createNewPanel();
					List<String> lines = new ArrayList<>();
					
					lines = AnsiCards.asHazardInList(c, gs, x);
					
					panel.addLinesToPanel(lines);
					
				}
				
				// Add status
				{
					AnsiGridPanel panel = grid.createNewPanel();
					panel.addLineToPanel("");
					panel.addLineToPanel("");
					panel.addLineToPanel("Phase #: "+gs.getPhaseNumber());
					panel.addLineToPanel("Life points: "+gs.getLifePoints());
					panel.addLineToPanel("");
					panel.addLineToPanel("Fighting stack remaining: "+gs.getYourFightingCards().size());
					panel.addLineToPanel("Hazard stack remaining: "+gs.getHazardCards().size());
				}
				
				System.out.println(grid.render(10));
				
			} else {
				
				System.out.println("There is one hazard card remaining, do you want to fight or discard:");
				
				AnsiGrid grid = new AnsiGrid();

				Card c = gs.getHazardCards().get(0);
				{
					AnsiGridPanel panel = grid.createNewPanel();
					List<String> lines = new ArrayList<>();
					
					lines = AnsiCards.asHazardInList(c, gs, null);
					
					panel.addLinesToPanel(lines);
				}
					
				
				// Add status
				{
					AnsiGridPanel panel = grid.createNewPanel();
					panel.addLineToPanel("");
					panel.addLineToPanel("");
					panel.addLineToPanel("Phase #: "+gs.getPhaseNumber());
					panel.addLineToPanel("Life points: "+gs.getLifePoints());
					panel.addLineToPanel("");
					panel.addLineToPanel("Fighting stack remaining: "+gs.getYourFightingCards().size());
					panel.addLineToPanel("Hazard stack remaining: "+gs.getHazardCards().size());
				}
				
				System.out.println(grid.render(10));
			}
			
			
		} else if(gs.getState() == State.PAY_LIFE_POINTS_ON_HAZARD_MISS) {
			
			System.out.println("-------------------------------");			
			outputState(gs);

			final int remainingHazard = gs.calculateRemainingHazardValue();

			System.out.println("Remaining Hazard: "+remainingHazard);
			
		} else if(gs.getAbilityObject() != null && gs.getAbilityObject().getActiveCard().getAbility() == Ability.SORT_3_CARDS 
				&& gs.getAbilityObject().getStage() == 1) {

			
		} else {
			System.out.println("-------------------------------");			
			outputState(gs);
			System.out.println();
			
			
		}
		
	}
	
	
	
	static ActionResponse handlePirateOptions(List<Action> actions, GameStateContainer container) { 
		
		if(actions.size() == 0) { return null; }
		
		PirateGameState gs = container.getPirateGameState();
		
		if(gs.getState() == PirateState.SELECT_A_PIRATE && actions.size() == gs.getActivePirates().length) {
			
			PirateCard[] cards = gs.getActivePirates();
			
			if(cards.length >= 2) { 
				
				System.out.println();
				System.out.println("Select a pirate: ");
				
				for(int x = 0; x < actions.size(); x++) {
					
					SelectAPirateAction sapa = (SelectAPirateAction) actions.get(x);
					
					System.out.println( (x+1)+") "+AnsiCards.simplePirateText(sapa.getCard()));
					
				}
				
				System.out.print("> ");
				int choice = readValidNumericInput(1, 2);
				
				return new SelectAPirateActionResponse(actions.get(choice-1));
				
				
			} else if(cards.length == 1) {
				
				System.out.println("Selecting other pirate: "+AnsiCards.simplePirateText(cards[0]));
				
				return new SelectAPirateActionResponse(actions.get(0));
				
			} else { FridayUtil.throwErr("Unexpected number of cards"); return null; } 
			
		} else if(gs.getAbilityObject() != null && gs.getAbilityObject().getActiveCard().getAbility() == Ability.SORT_3_CARDS 
				&& gs.getAbilityObject().getStage() == 1) {
			List<Card> cardsToSort = new ArrayList<>(gs.getAbilityObject().getDrawnSortCards());
			
			boolean continueLoop = true;
			
			do {
				System.out.println();
				System.out.println("-------------------------------");
				System.out.println(" Move card to top: ");
				for(int x = 0; x < cardsToSort.size(); x++) {
					System.out.println((x+1)+") "+AnsiCards.asFightingCard(cardsToSort.get(x), (GameState)null));
				}
				System.out.println("4) Done.");
				System.out.print("> ");
				int index = readValidNumericInput(1, 4);	
				
				if(index == 4) {
					continueLoop = false;
				} else {
					Card newTop = cardsToSort.remove(index-1);
					cardsToSort.add(0, newTop);
				}
				
			} while(continueLoop);
			
			int indexToDiscard = Integer.MIN_VALUE;
			continueLoop = true;
			do {
				System.out.println();
				System.out.println("-------------------------------");
				for(int x = 0; x < cardsToSort.size(); x++) {
					System.out.println((x+1)+") "+AnsiCards.asFightingCard(cardsToSort.get(x), (GameState)null));
				}
				System.out.print("Select a card to discard, or 9 for none> ");
				int index = readValidNumericInput(1, 9);	
				
				if(index == 9) {
					continueLoop = false;
				} else if(index >= 1 && index <= 3){
					indexToDiscard = index-1;
					continueLoop = false;
				}
				
			} while(continueLoop);
			
			Card toDiscard = null;
			if(indexToDiscard != Integer.MIN_VALUE) {
				 toDiscard = cardsToSort.get(indexToDiscard);
				 final Card toDiscardFinal = toDiscard;
				 cardsToSort = cardsToSort.stream().filter( e -> e.getPhysicalCardId() != toDiscardFinal.getPhysicalCardId()).collect(Collectors.toList());
			}
			
			UseCardAbilitySortAction matchingAction = null;
			// Find the matching action
			actions: for(Action a : actions) {
				if(a.getType() == ActionType.USE_CARD_ABILITY_SORT) {
					UseCardAbilitySortAction ucasa = (UseCardAbilitySortAction) a;
					
					if(ucasa.getDiscard() == null && toDiscard != null) { continue; }
					if(ucasa.getDiscard() != null && toDiscard == null) { continue; }
					
					if(ucasa.getDiscard() != null && ucasa.getDiscard().getPhysicalCardId() != toDiscard.getPhysicalCardId()) { continue; }
					
					if(ucasa.getSortOrder().length != cardsToSort.size()) { continue; }
					
					for(int x = 0; x < cardsToSort.size(); x++) {
						if(ucasa.getSortOrder()[x].getPhysicalCardId() != cardsToSort.get(x).getPhysicalCardId()) {
							continue actions;
						}
						
					}
					matchingAction = ucasa;
					break actions;
				}
			}
			
			if(matchingAction == null) {
				throwErr("Could not find matching action for "+cardsToSort+" "+toDiscard);
			}
			System.out.println();
			System.out.println("Matching action: "+matchingAction);
			
			return convertActionToResponse(matchingAction, null);
			
			
		} else {
			System.out.println("-------------------------------");			
			outputStatePirate(gs);
			System.out.println();
			
			Map<Integer /* ability ordinal*/, Boolean> copyAbilitiesSeen = new HashMap<>();
			
			Map<Integer /* ability ordinal*/, Boolean> abilitiesSeen = new HashMap<>();
			
			List<Action> tweakedActions = new ArrayList<>(actions);
			for(Iterator<Action> tweakedIt = tweakedActions.iterator(); tweakedIt.hasNext();) {
				Action a = tweakedIt.next();
				
				if(a.getType() == ActionType.USE_COPY_ABILITY) {
					UseCopyAbilityAction ucaa = (UseCopyAbilityAction)a;
					if(ucaa.getContainedAction().getType() == ActionType.USE_CARD_ABILITY_WITH_TARGET) {
						UseCardAbilityWithTargetAction ucawta = (UseCardAbilityWithTargetAction) ucaa.getContainedAction();
						
						Ability ab = ucawta.getCard().getAbility();
						
						if(ab == Ability.BELOW_THE_PILE_1x || ab == Ability.DESTROY_1x || ab == Ability.EXCHANGE_X1 
								|| ab == Ability.EXCHANGE_X2 || ab == Ability.DOUBLE_1x) {
							copyAbilitiesSeen.put(ucawta.getCard().getAbility().ordinal(), true);
							tweakedIt.remove();
						}
					}
				}
				
				if(a.getType() == ActionType.USE_CARD_ABILITY_WITH_TARGET) {
					UseCardAbilityWithTargetAction ucawta = (UseCardAbilityWithTargetAction) a;
					Ability ab = ucawta.getCard().getAbility();
					if(ab == Ability.DESTROY_1x || ab == Ability.BELOW_THE_PILE_1x || ab == Ability.EXCHANGE_X1 
							|| ab == Ability.EXCHANGE_X2 || ab == Ability.DOUBLE_1x) {
						tweakedIt.remove();
						abilitiesSeen.put(ab.ordinal(), true);
					}
				}
			}

			int x = 0; 
			for(x = 0; x < tweakedActions.size(); x++) {
				Action a = tweakedActions.get(x);
				System.out.println((x+1)+") "+a);
			}
			
			int copyBtpOption = Integer.MIN_VALUE;
			int copyDestroyOption = Integer.MIN_VALUE;
			int copyExchangeOption1x = Integer.MIN_VALUE;
			int copyExchangeOption2x = Integer.MIN_VALUE;
			int copyDoubleOption = Integer.MIN_VALUE;
			
			int btpOption = Integer.MIN_VALUE;
			int destroyOption = Integer.MIN_VALUE;
			int exchangeOption1x = Integer.MIN_VALUE;
			int exchangeOption2x = Integer.MIN_VALUE;
			int doubleOption = Integer.MIN_VALUE;
			
			if(abilitiesSeen.get(Ability.DESTROY_1x.ordinal()) != null) {
				System.out.println((x+1)+") Destroy a card");
				destroyOption = x+1;
				x++;
			}
			
			if(abilitiesSeen.get(Ability.BELOW_THE_PILE_1x.ordinal()) != null) {
				System.out.println((x+1)+") Below the pile a card");
				btpOption = x+1;
				x++;
			}

			if(abilitiesSeen.get(Ability.EXCHANGE_X1.ordinal()) != null) {
				System.out.println((x+1)+") Exchange x1 a card");
				exchangeOption1x = x+1;
				x++;
			}

			if(abilitiesSeen.get(Ability.EXCHANGE_X2.ordinal()) != null) {
				System.out.println((x+1)+") Exchange x2 a card");
				exchangeOption2x = x+1;
				x++;
			}
			
			if(abilitiesSeen.get(Ability.DOUBLE_1x.ordinal()) != null) {
				System.out.println((x+1)+") Double x1 a card");
				doubleOption = x+1;
				x++;
			}			
			
			if(copyAbilitiesSeen.get(Ability.BELOW_THE_PILE_1x.ordinal()) != null) {
				System.out.println((x+1)+") Copy: Below the pile a card");
				copyBtpOption = x+1;
				x++;
			}

			if(copyAbilitiesSeen.get(Ability.DESTROY_1x.ordinal()) != null) {
				System.out.println((x+1)+") Copy: Destroy a card");
				copyDestroyOption = x+1;
				x++;
			}
			
			if(copyAbilitiesSeen.get(Ability.DOUBLE_1x.ordinal()) != null) {
				System.out.println((x+1)+") Copy: Double a card");
				copyDoubleOption = x+1;
				x++;
			}
			
			if(copyAbilitiesSeen.get(Ability.EXCHANGE_X1.ordinal()) != null) {
				System.out.println((x+1)+") Copy: Exchange x1 a card");
				copyExchangeOption1x = x+1;
				x++;
			}

			if(copyAbilitiesSeen.get(Ability.EXCHANGE_X2.ordinal()) != null) {
				System.out.println((x+1)+") Copy: Exchange x2 a card");
				copyExchangeOption2x = x+1;
				x++;
			}


			System.out.print("> ");
			int index = readValidNumericInput(1, x);
			
			if(index == destroyOption) {
				return convertActionToResponse(useTargettedActionMenu("Card to destroy:", Ability.DESTROY_1x, actions, null, gs), null);
			}
			
			if(index == btpOption) {
				return convertActionToResponse(btpMenu(actions, null, gs), null);
			}
			
			if(index == exchangeOption1x) {
				return convertActionToResponse(exchangeMenu(Ability.EXCHANGE_X1, actions, null, gs), null);
			}
			
			if(index == exchangeOption2x) {
				return convertActionToResponse(exchangeMenu(Ability.EXCHANGE_X2, actions, null, gs), null);
			}

			if(index == doubleOption) {
				return convertActionToResponse(useTargettedActionMenu("Select a card to double:", Ability.DOUBLE_1x, actions, null, gs), null);
			}
			
			// Copy actions
			if(index == copyDestroyOption || index == copyBtpOption || index == copyExchangeOption1x 
					|| index == copyExchangeOption2x || index == copyDoubleOption) {
				
				// Extract the contained actions into a list 
				List<Action> copiedActions = actions.stream().filter(e -> e.getType() == ActionType.USE_COPY_ABILITY)
						.map( e-> ((UseCopyAbilityAction)e).getContainedAction() ).collect(Collectors.toList());
				
				Action innerAction;
				if(index == copyBtpOption) {
					innerAction = btpMenu(copiedActions, null, gs);
				} else if(index == copyDestroyOption) {
					innerAction = useTargettedActionMenu("Card to destroy:", Ability.DESTROY_1x, copiedActions, null, gs);
				} else if(index == copyExchangeOption1x) {
					innerAction = exchangeMenu(Ability.EXCHANGE_X1, copiedActions, null, gs);
				} else if(index == copyExchangeOption2x) {
					innerAction = exchangeMenu(Ability.EXCHANGE_X2, copiedActions, null, gs);
				} else if(index == copyDoubleOption) {
					innerAction = useTargettedActionMenu("Select a card to double:", Ability.DOUBLE_1x, copiedActions, null, gs);
				} else {
					throwErr("Unrecognized inner action");
					return null; // unreachable, added to satisfy the compiler.
				}
				
				Action copyAction = actions.stream().filter(e -> e.getType() == ActionType.USE_COPY_ABILITY && 
						((UseCopyAbilityAction)e).getContainedAction() == innerAction ).findFirst().orElse(null);
				
				if(copyAction == null) {
					throwErr("Could not find btp action in copy action list: "+innerAction);
				}
				
				return convertActionToResponse(copyAction, null);
				
			}
			
			index--;
			
			Action a = tweakedActions.get(index);
			
			return convertActionToResponse(a, null);

		} 		
	}
	
	static ActionResponse handleOptions(List<Action> actions, GameStateContainer container) {
		
		if(!container.isGameState()) {
			return handlePirateOptions(actions, container);
		}
		
		GameState gs = container.getGameState();
		
		System.out.println();
		
		if(actions.size() == 1) {
			return convertActionToResponse(actions.get(0), gs);
			
		} else if(gs.getState() == State.SELECT_A_HAZARD_CARD) {

			if(actions.get(0).getType() == ActionType.SELECT_FROM_TWO_HAZARD_CARDS) {
				System.out.println("Select a hazard, the remaining hazard will be discarded:");
				
				// Sort top 2 cards descending by free cards
				List<Card> cardsInUI = new ArrayList<>();
				{
					for(int x = 0; x < 2; x++) {
						cardsInUI.add(gs.getHazardCards().get(x));
					}
					Collections.sort(cardsInUI, (a,b) -> { 	
						return b.getFightingValue() - a.getFreeCards();
					});
				}
				
				AnsiGrid grid = new AnsiGrid();
				for(int x = 0; x < 2; x++) {
					Card c = cardsInUI.get(x);

					AnsiGridPanel panel = grid.createNewPanel();
					List<String> lines = new ArrayList<>();
					
					lines = AnsiCards.asHazardInList(c, gs, x);
					
					panel.addLinesToPanel(lines);
					
				}
				
				// Add status
				{
					AnsiGridPanel panel = grid.createNewPanel();
					panel.addLineToPanel("");
					panel.addLineToPanel("");
					panel.addLineToPanel("Phase #: "+gs.getPhaseNumber());
					panel.addLineToPanel("Life points: "+gs.getLifePoints());
					panel.addLineToPanel("");
					panel.addLineToPanel("Fighting stack remaining: "+gs.getYourFightingCards().size());
					panel.addLineToPanel("Hazard stack remaining: "+gs.getHazardCards().size());
				}
				
				System.out.println(grid.render(10));
				
				System.out.print("> ");
				int choice = readValidNumericInput(1, 2);
				
				int indexIntoHazardDeck;
				{
					Card toMatch = cardsInUI.get(choice-1);
					for(indexIntoHazardDeck = 0; indexIntoHazardDeck < 2; indexIntoHazardDeck++) {
						Card fromDeck = gs.getHazardCards().get(indexIntoHazardDeck);
						if(fromDeck == toMatch) {
							break;
						}
					}
				}
				
				
				if(indexIntoHazardDeck == 0) {
					return SelectFromTwoHazardCardsActionResponse.INSTANCE_ZERO;
				} else {
					return SelectFromTwoHazardCardsActionResponse.INSTANCE_ONE;
				}
				
			} else {
				
				System.out.println("There is one hazard card remaining, do you want to fight or discard:");
				
				AnsiGrid grid = new AnsiGrid();

				Card c = gs.getHazardCards().get(0);
				{
					AnsiGridPanel panel = grid.createNewPanel();
					List<String> lines = new ArrayList<>();
					
					lines = AnsiCards.asHazardInList(c, gs, null);
					
					panel.addLinesToPanel(lines);
				}
					
				
				// Add status
				{
					AnsiGridPanel panel = grid.createNewPanel();
					panel.addLineToPanel("");
					panel.addLineToPanel("");
					panel.addLineToPanel("Phase #: "+gs.getPhaseNumber());
					panel.addLineToPanel("Life points: "+gs.getLifePoints());
					panel.addLineToPanel("");
					panel.addLineToPanel("Fighting stack remaining: "+gs.getYourFightingCards().size());
					panel.addLineToPanel("Hazard stack remaining: "+gs.getHazardCards().size());
				}
				
				System.out.println(grid.render(10));
				
				System.out.println("1) Fight");
				System.out.println("2) Discard");
				
				
				System.out.print("> ");
				int choice = readValidNumericInput(1, 2);

				if(choice == 1) {
					return FightOrDiscardHazardCardActionResponse.INSTANCE_FIGHT;
				} else {
					return FightOrDiscardHazardCardActionResponse.INSTANCE_DISCARD;
				}			
			}
			
			
		} else if(gs.getState() == State.PAY_LIFE_POINTS_ON_HAZARD_MISS) {
			
			System.out.println("-------------------------------");			
			outputState(gs);

			final int remainingHazard = gs.calculateRemainingHazardValue();
			
			List<Card> cardsAvailableToDestroy = new ArrayList<Card>();
			cardsAvailableToDestroy.addAll(gs.getRhsFightCards().getAsList());
			cardsAvailableToDestroy.addAll(gs.getLhsFightCards().getAsList());
			
			FridayUtil.sortFightingCardsWorstToBest(cardsAvailableToDestroy);
			
			boolean toDestroy[] = new boolean[cardsAvailableToDestroy.size()];
			for(int x = 0; x < toDestroy.length; x++) { toDestroy[x] = false; }
			
			boolean continueLoop = true;
			while(continueLoop) {
				
				// Find an action that exactly matches what the user has selected
				int totalPointsRequiredForSelectedCards = 0;
				Action matchingAction;
				{
					List<Card> selectedCards = new ArrayList<>();
					for(int x = 0; x < toDestroy.length; x++) {
						if(toDestroy[x]) {
							
							Card c = cardsAvailableToDestroy.get(x);
							selectedCards.add(c); 
							
							if(c.getDifficulty() == null || c.getDifficulty() == Difficulty.NONE) {
								totalPointsRequiredForSelectedCards++;
							} else {
								totalPointsRequiredForSelectedCards+=2;
							}
						}
					}
					
					matchingAction = findMatchingDestroyCardsAction(actions, selectedCards, gs);					
				}
				
				System.out.println("Destruction points remaining: "+(remainingHazard- totalPointsRequiredForSelectedCards));
				System.out.println();
				System.out.println("Cards to destroy:");
				
				int x = 0;
				for(; x < cardsAvailableToDestroy.size(); x++) {
					
					boolean selected = toDestroy[x];
					Card c = cardsAvailableToDestroy.get(x);
					System.out.println((selected? "* ": "  ")+(x+1)+") "+AnsiCards.asFightingCard(c, gs));
				}
				
				System.out.println( "  "+(x+1)+") Done: "+matchingAction);
				
				int option = readValidNumericInput(1, cardsAvailableToDestroy.size()+1);
				
				
				if(option == x+1) {
					if(matchingAction != null) {
						return new DestroyCardsWithPaidLifeActionResponse(matchingAction);
					} else {
						System.out.println("No matching action.");
					}
				} else {
					toDestroy[option-1] = !toDestroy[option-1];	
				}

				System.out.println();
			}
			
			return null;
			
		} else if(gs.getAbilityObject() != null && gs.getAbilityObject().getActiveCard().getAbility() == Ability.SORT_3_CARDS 
				&& gs.getAbilityObject().getStage() == 1) {
			List<Card> cardsToSort = new ArrayList<>(gs.getAbilityObject().getDrawnSortCards());
			
			boolean continueLoop = true;
			
			do {
				System.out.println();
				System.out.println("-------------------------------");
				System.out.println(" Move card to top: ");
				for(int x = 0; x < cardsToSort.size(); x++) {
					System.out.println((x+1)+") "+AnsiCards.asFightingCard(cardsToSort.get(x), (GameState)null));
				}
				System.out.println("4) Done.");
				System.out.print("> ");
				int index = readValidNumericInput(1, 4);	
				
				if(index == 4) {
					continueLoop = false;
				} else {
					Card newTop = cardsToSort.remove(index-1);
					cardsToSort.add(0, newTop);
				}
				
			} while(continueLoop);
			
			int indexToDiscard = Integer.MIN_VALUE;
			continueLoop = true;
			do {
				System.out.println();
				System.out.println("-------------------------------");
				for(int x = 0; x < cardsToSort.size(); x++) {
					System.out.println((x+1)+") "+AnsiCards.asFightingCard(cardsToSort.get(x), (GameState)null));
				}
				System.out.print("Select a card to discard, or 9 for none> ");
				int index = readValidNumericInput(1, 9);	
				
				if(index == 9) {
					continueLoop = false;
				} else if(index >= 1 && index <= 3){
					indexToDiscard = index-1;
					continueLoop = false;
				}
				
			} while(continueLoop);
			
			Card toDiscard = null;
			if(indexToDiscard != Integer.MIN_VALUE) {
				 toDiscard = cardsToSort.get(indexToDiscard);
				 final Card toDiscardFinal = toDiscard;
				 cardsToSort = cardsToSort.stream().filter( e -> e.getPhysicalCardId() != toDiscardFinal.getPhysicalCardId()).collect(Collectors.toList());
			}
			
			UseCardAbilitySortAction matchingAction = null;
			// Find the matching action
			actions: for(Action a : actions) {
				if(a.getType() == ActionType.USE_CARD_ABILITY_SORT) {
					UseCardAbilitySortAction ucasa = (UseCardAbilitySortAction) a;
					
					if(ucasa.getDiscard() == null && toDiscard != null) { continue; }
					if(ucasa.getDiscard() != null && toDiscard == null) { continue; }
					
					if(ucasa.getDiscard() != null && ucasa.getDiscard().getPhysicalCardId() != toDiscard.getPhysicalCardId()) { continue; }
					
					if(ucasa.getSortOrder().length != cardsToSort.size()) { continue; }
					
					for(int x = 0; x < cardsToSort.size(); x++) {
						if(ucasa.getSortOrder()[x].getPhysicalCardId() != cardsToSort.get(x).getPhysicalCardId()) {
							continue actions;
						}
						
					}
					matchingAction = ucasa;
					break actions;
				}
			}
			
			if(matchingAction == null) {
				throwErr("Could not find matching action for "+cardsToSort+" "+toDiscard);
			}
			System.out.println();
			System.out.println("Matching action: "+matchingAction);
			
			return convertActionToResponse(matchingAction, gs);
			
			
		} else {
			System.out.println("-------------------------------");			
			outputState(gs);
			System.out.println();
			
			Map<Integer /* ability ordinal*/, Boolean> copyAbilitiesSeen = new HashMap<>();
			
			Map<Integer /* ability ordinal*/, Boolean> abilitiesSeen = new HashMap<>();
			
			List<Action> tweakedActions = new ArrayList<>(actions);
			for(Iterator<Action> tweakedIt = tweakedActions.iterator(); tweakedIt.hasNext();) {
				Action a = tweakedIt.next();
				
				if(a.getType() == ActionType.USE_COPY_ABILITY) {
					UseCopyAbilityAction ucaa = (UseCopyAbilityAction)a;
					if(ucaa.getContainedAction().getType() == ActionType.USE_CARD_ABILITY_WITH_TARGET) {
						UseCardAbilityWithTargetAction ucawta = (UseCardAbilityWithTargetAction) ucaa.getContainedAction();
						
						Ability ab = ucawta.getCard().getAbility();
						
						if(ab == Ability.BELOW_THE_PILE_1x || ab == Ability.DESTROY_1x || ab == Ability.EXCHANGE_X1 
								|| ab == Ability.EXCHANGE_X2 || ab == Ability.DOUBLE_1x) {
							copyAbilitiesSeen.put(ucawta.getCard().getAbility().ordinal(), true);
							tweakedIt.remove();
						}
					}
				}
				
				if(a.getType() == ActionType.USE_CARD_ABILITY_WITH_TARGET) {
					UseCardAbilityWithTargetAction ucawta = (UseCardAbilityWithTargetAction) a;
					Ability ab = ucawta.getCard().getAbility();
					if(ab == Ability.DESTROY_1x || ab == Ability.BELOW_THE_PILE_1x || ab == Ability.EXCHANGE_X1 
							|| ab == Ability.EXCHANGE_X2 || ab == Ability.DOUBLE_1x) {
						tweakedIt.remove();
						abilitiesSeen.put(ab.ordinal(), true);
					}
				}
			}

			int x = 0; 
			for(x = 0; x < tweakedActions.size(); x++) {
				Action a = tweakedActions.get(x);
				System.out.println((x+1)+") "+a);
			}
			
			int copyBtpOption = Integer.MIN_VALUE;
			int copyDestroyOption = Integer.MIN_VALUE;
			int copyExchangeOption1x = Integer.MIN_VALUE;
			int copyExchangeOption2x = Integer.MIN_VALUE;
			int copyDoubleOption = Integer.MIN_VALUE;
			
			int btpOption = Integer.MIN_VALUE;
			int destroyOption = Integer.MIN_VALUE;
			int exchangeOption1x = Integer.MIN_VALUE;
			int exchangeOption2x = Integer.MIN_VALUE;
			int doubleOption = Integer.MIN_VALUE;
			
			if(abilitiesSeen.get(Ability.DESTROY_1x.ordinal()) != null) {
				System.out.println((x+1)+") Destroy a card");
				destroyOption = x+1;
				x++;
			}
			
			if(abilitiesSeen.get(Ability.BELOW_THE_PILE_1x.ordinal()) != null) {
				System.out.println((x+1)+") Below the pile a card");
				btpOption = x+1;
				x++;
			}

			if(abilitiesSeen.get(Ability.EXCHANGE_X1.ordinal()) != null) {
				System.out.println((x+1)+") Exchange x1 a card");
				exchangeOption1x = x+1;
				x++;
			}

			if(abilitiesSeen.get(Ability.EXCHANGE_X2.ordinal()) != null) {
				System.out.println((x+1)+") Exchange x2 a card");
				exchangeOption2x = x+1;
				x++;
			}
			
			if(abilitiesSeen.get(Ability.DOUBLE_1x.ordinal()) != null) {
				System.out.println((x+1)+") Double x1 a card");
				doubleOption = x+1;
				x++;
			}			
			
			if(copyAbilitiesSeen.get(Ability.BELOW_THE_PILE_1x.ordinal()) != null) {
				System.out.println((x+1)+") Copy: Below the pile a card");
				copyBtpOption = x+1;
				x++;
			}

			if(copyAbilitiesSeen.get(Ability.DESTROY_1x.ordinal()) != null) {
				System.out.println((x+1)+") Copy: Destroy a card");
				copyDestroyOption = x+1;
				x++;
			}
			
			if(copyAbilitiesSeen.get(Ability.DOUBLE_1x.ordinal()) != null) {
				System.out.println((x+1)+") Copy: Double a card");
				copyDoubleOption = x+1;
				x++;
			}

			
			if(copyAbilitiesSeen.get(Ability.EXCHANGE_X1.ordinal()) != null) {
				System.out.println((x+1)+") Copy: Exchange x1 a card");
				copyExchangeOption1x = x+1;
				x++;
			}

			if(copyAbilitiesSeen.get(Ability.EXCHANGE_X2.ordinal()) != null) {
				System.out.println((x+1)+") Copy: Exchange x2 a card");
				copyExchangeOption2x = x+1;
				x++;
			}


			System.out.print("> ");
			int index = readValidNumericInput(1, x);
			
			if(index == destroyOption) {
				return convertActionToResponse(useTargettedActionMenu("Card to destroy:", Ability.DESTROY_1x, actions, gs, null), gs);				
			}
			
			if(index == btpOption) {
				return convertActionToResponse(btpMenu(actions, gs, null), gs);
			}
			
			if(index == exchangeOption1x) {
				return convertActionToResponse(exchangeMenu(Ability.EXCHANGE_X1, actions, gs, null), gs);
			}
			
			if(index == exchangeOption2x) {
				return convertActionToResponse(exchangeMenu(Ability.EXCHANGE_X2, actions, gs, null), gs);
			}

			if(index == doubleOption) {
				return convertActionToResponse(useTargettedActionMenu("Select a card to double:", Ability.DOUBLE_1x, actions, gs, null), gs);
			}
			
			// Copy actions
			if(index == copyDestroyOption || index == copyBtpOption || index == copyExchangeOption1x 
					|| index == copyExchangeOption2x || index == copyDoubleOption) {
				
				// Extract the contained actions into a list 
				List<Action> copiedActions = actions.stream().filter(e -> e.getType() == ActionType.USE_COPY_ABILITY)
						.map( e-> ((UseCopyAbilityAction)e).getContainedAction() ).collect(Collectors.toList());
				
				Action innerAction;
				if(index == copyBtpOption) {
					innerAction = btpMenu(copiedActions, gs, null);
				} else if(index == copyDestroyOption) {
					innerAction = useTargettedActionMenu("Card to destroy:", Ability.DESTROY_1x, copiedActions, gs, null);
				} else if(index == copyExchangeOption1x) {
					innerAction = exchangeMenu(Ability.EXCHANGE_X1, copiedActions, gs, null);
				} else if(index == copyExchangeOption2x) {
					innerAction = exchangeMenu(Ability.EXCHANGE_X2, copiedActions, gs, null);
				} else if(index == copyDoubleOption) {
					innerAction = useTargettedActionMenu("Select a card to double:", Ability.DOUBLE_1x, copiedActions, gs, null);
				} else {
					throwErr("Unrecognized inner action");
					return null; // unreachable, added to satisfy the compiler.
				}
				
				Action copyAction = actions.stream().filter(e -> e.getType() == ActionType.USE_COPY_ABILITY && 
						((UseCopyAbilityAction)e).getContainedAction() == innerAction ).findFirst().orElse(null);
				
				if(copyAction == null) {
					throwErr("Could not find btp action in copy action list: "+innerAction);
				}
				
				return convertActionToResponse(copyAction, gs);
				
			}
			
			index--;
			
			Action a = tweakedActions.get(index);
			
			return convertActionToResponse(a, gs);

		}
		
	}
	
	private static Action findMatchingDestroyCardsAction(List<Action> actions, List<Card> selectedCards, GameState gs) {
		next_action: for(Action action : actions) {
			
			if(action.getType() != ActionType.DESTROY_CARDS_WITH_PAID_LIFE) {
				continue;
			}
			
			DestroyCardsWithPaidLifeAction dcwpla = (DestroyCardsWithPaidLifeAction) action;
			
			List<Card> cardsInAction = new ArrayList<>();
			cardsInAction.addAll(Arrays.asList(dcwpla.getCardsToDestroy()));
			
			if(selectedCards.size() != cardsInAction.size()) { continue; }
			
			for(Card selectedCard : selectedCards) {
				
				boolean matchFound = false;
				for(Iterator<Card> it = cardsInAction.iterator(); it.hasNext();) {
					Card actionCard = it.next();
					if(actionCard.getTraitId() == selectedCard.getTraitId()) {
						matchFound = true;
						it.remove();
						break;
					}
					
				}
				
				if(!matchFound) {
					continue next_action;
				}
				
			}
			
			if(cardsInAction.size() == 0) {
				// All cards matched
				return action;
			} else {
				// This shouldn't happen
				FridayUtil.throwErr("All cards did not match");
			}
			
				
		}
		return null;
	}

	private static ActionResponse convertActionToResponse(Action actionParam, GameState gsUNUSED) {
		
		ActionResponse response = null;
				
		if(actionParam.getType() == ActionType.DRAW_A_FREE_CARD) {
			response = DrawAFreeCardActionResponse.INSTANCE;	
		}
		
		if(actionParam.getType() == ActionType.END_DRAW_FREE_CARDS_PHASE) {
			response = EndDrawFreeCardPhaseActionResponse.INSTANCE;
		}
		
		if(actionParam.getType() == ActionType.SACRIFICE_LIFE_TO_DRAW) {
			response = SacrificeLifeToDrawActionResponse.INSTANCE;
		}
		
		if(actionParam.getType() == ActionType.END_SACRIFICE_LIFE_PHASE) {
			response = EndSacrificeLifePhaseActionResponse.INSTANCE;
		}
		
		if(actionParam.getType() == ActionType.DESTROY_CARDS_WITH_PAID_LIFE) {
			response = new DestroyCardsWithPaidLifeActionResponse(actionParam);
		}
		
		if(actionParam.getType() == ActionType.USE_CARD_ABILITY) {
			response = new UseCardAbilityActionResponse(actionParam);
		}
		
		if(actionParam.getType() == ActionType.END_MULTISTAGE_ABILITY_ACTION) {
			response = EndMultistageAbilityActionResponse.INSTANCE;
		}
		
		if(actionParam.getType() == ActionType.USE_CARD_ABILITY_SORT) {
			response = new UseCardAbilitySortActionResponse(actionParam);
		}
		
		if(actionParam.getType() == ActionType.USE_CARD_ABILITY_WITH_TARGET) {
			response = new UseCardAbilityWithTargetActionResponse(actionParam);
		}
		
		if(actionParam.getType() == ActionType.USE_COPY_ABILITY) {
			response = new UseCardAbilityActionResponse(actionParam);
		}
		
		if(actionParam.getType() == ActionType.END_PIRATE_ROUND) {
			response = EndPirateRoundActionResponse.INSTANCE;
		}
		
		if(response == null) {
			throwErr("Unhandled action: "+actionParam);
		}
		
		return response;
	}


	
	public static void outputState(GameState gs) {
		
		AnsiGrid grid = new AnsiGrid();
		
		// LHS
		{
			AnsiGridPanel agp = grid.createNewPanel();
			gs.getLhsFightCards().getAsList().forEach( e-> {
				String cStr = AnsiCards.asFightingCard(e, gs);
				agp.addLineToPanel(cStr);
			});
			
		}
		
		// Active hazard
		{
			grid.createNewPanel().addLinesToPanel(AnsiCards.asHazardInList(gs.getActiveHazardCard(), gs, null));
		}
		
		// RHS
		{
			AnsiGridPanel agp = grid.createNewPanel();
			gs.getRhsFightCards().getAsList().forEach( e-> {
				String cStr = AnsiCards.asFightingCard(e, gs);
				agp.addLineToPanel(cStr);
			});
			
			if(gs.getAbilityObject() != null) {
				AbilityObject ao = gs.getAbilityObject();
				if(ao.getActiveCard().getAbility() == Ability.SORT_3_CARDS) {
					
					agp.addLineToPanel("");
					agp.addLineToPanel("Sort cards:");
					
					ao.getDrawnSortCards().forEach(e -> {
						String cStr = AnsiCards.asFightingCard(e, gs);
						agp.addLineToPanel(cStr);
					});
					agp.addLineToPanel("");
					
				}
			}

		}
		
		
		System.out.println(grid.render(8));
		
		if(gs.getState() == State.DRAW_FREE_CARDS) {
			int remainingFreeCards = gs.getActiveHazardCard().getFreeCards() -  gs.getLhsFightCards().size();
			System.out.println("Remaining free fighting card draws: "+remainingFreeCards);
			
		}
		if(gs.getLhsFightCards().size() > 0)   {
			int remainingHazardValue = gs.calculateRemainingHazardValue();
			String value = "";
			if(remainingHazardValue > 0) {
				value = ansi().fgBrightRed().toString();
			} else if(remainingHazardValue == 0) {
				value = ansi().fgBrightDefault().toString();
			} else {
				value = ansi().fgBrightGreen().toString();
			}
			value += remainingHazardValue+ansi().reset().toString();
			
			System.out.println("Remaining hazard value: "+value);
		}
		
		System.out.println("Life points: "+gs.getLifePoints()+"   Phase #:"+gs.getPhaseNumber());
		
		System.out.println("Cards in fight stack: "+gs.getYourFightingCards().size()+"    Cards in hazard stack: "+gs.getHazardCards().size()+"   Cards in discard fight: "+gs.getDiscardFightCards().size());
		
	}
	
	public static void outputStatePirate(PirateGameState gs) {
		
		AnsiGrid grid = new AnsiGrid();
		
		// LHS
		{
			AnsiGridPanel agp = grid.createNewPanel();
			gs.getLhsFightCards().getAsList().forEach( e-> {
				String cStr = AnsiCards.asPirateFightingCard(e, gs);
				agp.addLineToPanel(cStr);
			});
			
		}
		
		// Active pirate
		{
			grid.createNewPanel().addLinesToPanel(AnsiCards.simplePirateTextInList(gs.getActivePirateCard()) );
		}
		
		// RHS
		{
			AnsiGridPanel agp = grid.createNewPanel();
			gs.getRhsFightCards().getAsList().forEach( e-> {
				String cStr = AnsiCards.asPirateFightingCard(e, gs);
				agp.addLineToPanel(cStr);
			});
			
			if(gs.getAbilityObject() != null) {
				AbilityObject ao = gs.getAbilityObject();
				if(ao.getActiveCard().getAbility() == Ability.SORT_3_CARDS) {
					
					agp.addLineToPanel("");
					agp.addLineToPanel("Sort cards:");
					
					ao.getDrawnSortCards().forEach(e -> {
						String cStr = AnsiCards.asPirateFightingCard(e, gs);
						agp.addLineToPanel(cStr);
					});
					agp.addLineToPanel("");
					
				}
			}

		}
		
		
		System.out.println(grid.render(8));
		
		if(gs.getState() == PirateState.DRAW_FREE_CARDS) {
			int remainingFreeCards = gs.getActivePirateCard().getFreeCards() -  gs.getLhsFightCards().size();
			System.out.println("Remaining free fighting card draws: "+remainingFreeCards);
			
		}
		if(gs.getLhsFightCards().size() > 0)   {
			int remainingHazardValue = gs.calculateRemainingHazardValue();
			String value = "";
			if(remainingHazardValue > 0) {
				value = ansi().fgBrightRed().toString();
			} else if(remainingHazardValue == 0) {
				value = ansi().fgBrightDefault().toString();
			} else {
				value = ansi().fgBrightGreen().toString();
			}
			value += remainingHazardValue+ansi().reset().toString();
			
			System.out.println("Remaining hazard value: "+value);
		}
		
		System.out.println("Life points: "+gs.getLifePoints());
		
		System.out.println("Cards in fight stack: "+gs.getYourFightingCards().size()+"    Cards in discard fight: "+gs.getDiscardFightCards().size());
		
	}

	
	private static List<Card> getAllFightCards(GameState gs, PirateGameState pgs) {
		List<Card> result = new ArrayList<>();
		
		if(gs != null) {
			result.addAll(gs.getLhsFightCards().getAsList());
			result.addAll(gs.getRhsFightCards().getAsList());
			
		} else {
			result.addAll(pgs.getLhsFightCards().getAsList());
			result.addAll(pgs.getRhsFightCards().getAsList());
		}
		
		return result;
		
	}
	
	private static String ansiAsFightingCard(Card card, GameState gs, PirateGameState pgs) {
		if(gs != null) {
			return AnsiCards.asFightingCard(card, gs);
		} else {
			return AnsiCards.asPirateFightingCard(card, pgs);
		}
	}
	
	private static Action useTargettedActionMenu(String menuText, final Ability ab, List<Action> actions, GameState gs, PirateGameState pgs) {
		
		List<Action> abilityActions = FridayUtil.getActionsByAbility(actions, ActionType.USE_CARD_ABILITY_WITH_TARGET,
				ab);

		List<Card> potentialTargets = getAllFightCards(gs, pgs);
		
		FridayUtil.sortFightingCardsWorstToBest(potentialTargets);
		if(ab == Ability.DOUBLE_1x) {
			Collections.reverse(potentialTargets); // best to worst
		}
		
		
		Map<Integer /* menu pos*/, Card> menuPosToCard = new HashMap<>();
		int nextMenuPos = 1;
		
		System.out.println("---------------------");
		
		System.out.println(menuText);
		
		for(Card potentialTarget : potentialTargets) {
			
			// If matching ability action for card
			if(abilityActions.stream().anyMatch(e-> ((UseCardAbilityWithTargetAction)e).getTarget().getPhysicalCardId() == potentialTarget.getPhysicalCardId() )) {
				System.out.println(nextMenuPos+") "+ansiAsFightingCard(potentialTarget, gs, pgs));
				menuPosToCard.put(nextMenuPos++, potentialTarget);
			}
			
		}
		System.out.print("> ");
		int menuOption = readValidNumericInput(1, nextMenuPos);

		Card cardToUseAbilityOn = menuPosToCard.get((Integer)menuOption);
		
		Action resultAction = abilityActions.stream().filter(e -> ((UseCardAbilityWithTargetAction) e).getTarget()
				.getPhysicalCardId() == cardToUseAbilityOn.getPhysicalCardId()).findFirst().orElse(null);
		
		
		return resultAction;
	}
	
	private static Action exchangeMenu(Ability ability, List<Action> actions, GameState gs, PirateGameState pgs) {
		
		List<Action> exchangeActions = FridayUtil.getActionsByAbility(actions, ActionType.USE_CARD_ABILITY_WITH_TARGET,
				ability);
		
		List<Card> potentialTargets = getAllFightCards(gs, pgs);
		
		Map<Integer /* menu pos*/, Card> menuPosToCard = new HashMap<>();
		int nextMenuPos = 1;
		
		System.out.println("---------------------");
		
		System.out.println("Card to exchange: ");
		
		for(Card potentialTarget : potentialTargets) {
			
			// If matching exchange action for card
			if(exchangeActions.stream().anyMatch(e-> ((UseCardAbilityWithTargetAction)e).getTarget().getPhysicalCardId() == potentialTarget.getPhysicalCardId() )) {
				System.out.println(nextMenuPos+") "+ansiAsFightingCard(potentialTarget, gs, pgs));
				menuPosToCard.put(nextMenuPos++, potentialTarget);
			}
			
		}
		System.out.print("> ");
		int menuOption = readValidNumericInput(1, nextMenuPos);

		Card cardToExchange = menuPosToCard.get((Integer)menuOption);
		
		Action exchangeAction = exchangeActions.stream().filter(e -> ((UseCardAbilityWithTargetAction) e).getTarget()
				.getPhysicalCardId() == cardToExchange.getPhysicalCardId()).findFirst().orElse(null);
		
		
		return exchangeAction;
	}

	
	private static Action btpMenu(List<Action> actions, GameState gs2, PirateGameState pgs) {
		
		List<Action> btpActions = FridayUtil.getActionsByAbility(actions, ActionType.USE_CARD_ABILITY_WITH_TARGET,
				Ability.BELOW_THE_PILE_1x);

		HashMap<Integer /* physical id*/, Boolean /* on LHS, true if yes, null if no */> onLhs = new HashMap<>();
		
		List<Card> potentialTargets = getAllFightCards(gs2, pgs);
		
		if(gs2 != null) {
			gs2.getLhsFightCards().getAsList().stream().forEach( e -> { onLhs.put(e.getPhysicalCardId(), true); });	
		} else {
			pgs.getLhsFightCards().getAsList().stream().forEach( e -> { onLhs.put(e.getPhysicalCardId(), true); });
		}
				
		Map<Integer /* menu pos*/, Card> menuPosToCard = new HashMap<>();
		int nextMenuPos = 1;
		
		System.out.println("---------------------");
		
		System.out.println("Card to move below the pile: ");
		
		for(Card potentialTarget : potentialTargets) {
			
			// If matching btp action for card
			if(btpActions.stream().anyMatch(e-> ((UseCardAbilityWithTargetAction)e).getTarget().getPhysicalCardId() == potentialTarget.getPhysicalCardId() )) {
				boolean isPotentialTargetOnLhs = onLhs.get(potentialTarget.getPhysicalCardId()) != null;
				
				System.out.println(nextMenuPos+") "+ansiAsFightingCard(potentialTarget, gs2, pgs)+( isPotentialTargetOnLhs ? " [LHS]" : "" ));
				menuPosToCard.put(nextMenuPos++, potentialTarget);
			}
			
		}
		System.out.print("> ");
		int menuOption = readValidNumericInput(1, nextMenuPos);

		Card cardToBury = menuPosToCard.get((Integer)menuOption);
		
		Action buryAction = btpActions.stream().filter(e -> ((UseCardAbilityWithTargetAction) e).getTarget()
				.getPhysicalCardId() == cardToBury.getPhysicalCardId()).findFirst().orElse(null);
		
		
		return buryAction;
	}

	// --------------------------------------------------------------------------------
	
	private static int readValidNumericInput(int minVal, int maxVal) {
		
		Integer result = null;
		while(result == null) {
			result = readNumericInput(minVal, maxVal).orElse(null);
		}
		
		return result;
		
	}
	
	private static Optional<Integer> readNumericInput(int minVal, int maxVal) {
		String str = readInput().trim();
		try {
			int val = Integer.parseInt(str);

			if(!(val >= minVal && val <= maxVal)) {
				System.err.println("Invalid option: ["+str+"]");
				return Optional.empty();
			}
			
			return Optional.of(val);

		} catch(NumberFormatException nfe) {
			return Optional.empty();
		}
	}
	
	public static String readInput() {
		@SuppressWarnings("resource")
		Scanner s = new Scanner(System.in);
		String line = s.nextLine();
		return line;
	}

	public static void throwErr(String str) {
		FridayUtil.throwErr(str);
	}

}
