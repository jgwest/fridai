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

package com.fridai.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.fridai.Ability;
import com.fridai.Card;
import com.fridai.GameState;
import com.fridai.GameState.State;
import com.fridai.Main.DebugTreeEntry;
import com.fridai.Main.TreeEntry;
import com.fridai.PirateCard;
import com.fridai.PirateCard.PirateCardAbility;
import com.fridai.SlowGameState;

/** Various utility methods only used for debugging. */
public class DebugUtil {

	public static GameState setActivePiratesByAbility(PirateCardAbility[] pirateAbilities, GameState gs) {

		if(pirateAbilities.length != 2) {
			FridayUtil.throwErr("Not enough pirate abilities");
		}
		
		if(pirateAbilities[0] == pirateAbilities[1]) {
			FridayUtil.throwErr("Cannot use matching pirates");
		}
		
		SlowGameState oldGameState = gs.getSlowGameState();
		
		PirateCard[] newPirateCards = new PirateCard[2];  
		int x = 0;
		for(PirateCardAbility pca : pirateAbilities) {
			boolean match = false;
			for(PirateCard card : FridayUtil.ALL_CARDS.getPirateCards()) {
				if(card.getAbility() == pca) {
					newPirateCards[x] = card;
					match = true;
					break;
				}
			}
			if(!match) { FridayUtil.throwErr("Unable to find pirate card with ability: "+pca); }
			x++;
		}
		
		SlowGameState newSlowGameState = new SlowGameState(oldGameState.getAgingCards(), oldGameState.getGameLevel(), 
				newPirateCards, oldGameState.getActiveRoundCards(), oldGameState.getPhaseNumber(), oldGameState.getWildCardPirate());
		
		
		GameState result = new GameState(gs.getState(), gs.getYourFightingCards(), gs.getHazardCards(), 
				gs.getDiscardHazards(), newSlowGameState, null, gs.getDiscardFightCards(), 
				gs.getLhsOrRhsFightingCardUsed(), gs.getLifePoints(), gs.getLhsFightCards(), gs.getRhsFightCards(), 
				gs.getLhsOrRhsFightingCardDoubled(), gs.getAbilityObject(), gs, null);

		
		
		return result;
	
	}
	
	public static GameState moveAbilityFromAgingToFrontFight(Ability ability, GameState gs) {

		Card match = null;
		
		List<Card> newAgingCards = gs.getAgingCards().asList();
		
		for(Iterator<Card> it = newAgingCards.iterator(); it.hasNext();) {
			
			Card c = it.next();
			
			if(c.getAbility() == ability) {
				match = c;
				it.remove();
				break;
			}
			
		}
		
		if(match == null) { FridayUtil.throwErr("Could not find aging card w/ ability: "+ability); }
		
		List<Card> yfc = gs.getYourFightingCards().asList();
		yfc.add(0, match);
		ListCards newYourFightingCards = new ListCards(yfc.toArray(new Card[yfc.size()]), 0);
		
		ListCards newAgingStack = new ListCards(newAgingCards.toArray(new Card[newAgingCards.size()]), 0);
		
		SlowGameState sgs = gs.getSlowGameState();
		
		SlowGameState newSgs = new SlowGameState(newAgingStack, sgs.getGameLevel(), sgs.getActivePirates(), 
				sgs.getActiveRoundCards(), sgs.getPhaseNumber(), sgs.getWildCardPirate());
		
		gs = new GameState(State.SELECT_A_HAZARD_CARD, newYourFightingCards, gs.getHazardCards(), 
				gs.getDiscardHazards(), newSgs, null, gs.getDiscardFightCards(), 
				null /* lhsOrRhsFightingCardUsed*/, gs.getLifePoints(), gs.getLhsFightCards(), gs.getRhsFightCards(), 
				gs.getLhsOrRhsFightingCardDoubled(), gs.getAbilityObject(), gs, null);
		
		return gs;
		
	}
	
	
	public static GameState decreasePhase(GameState gs) {
		
		
		SlowGameState old = gs.getSlowGameState();
		
		SlowGameState newSgs = new SlowGameState(old.getAgingCards(), old.getGameLevel(), old.getActivePirates(), 
				old.getActiveRoundCards(), old.getPhaseNumber()-1, old.getWildCardPirate());
		
		
		gs = new GameState(State.SELECT_A_HAZARD_CARD, gs.getYourFightingCards(), gs.getHazardCards(), 
				/* discard hazards */ new ImmutableGrowableListCards(15), newSgs, null, 
				/* fighting discard */new ImmutableGrowableListCards(60), 
				null /* lhsOrRhsFightingCardUsed*/, 20 /*life*/, gs.getLhsFightCards(), gs.getRhsFightCards(), 
				null /* lhsOrRhsFightingCardDoubled*/ , null /* ability object*/, gs, null);
			
		return gs;
	}
	
	public static GameState moveFromHazardStackToFightStack(Ability ability, GameState gs) {
		
		Card myCard = null;
		List<Card> hazardCardList = new ArrayList<Card>(gs.getHazardCards().asList());
		for(Iterator<Card> it = hazardCardList.iterator(); it.hasNext(); ) {
			Card c = it.next();
			
			if(c.getAbility() == ability) {
				myCard = c ;
				it.remove();
				break;
			}
		}
		
		if(myCard == null) {
			FridayUtil.throwErr("Card with ability not found: "+ability);
		}

		ListCards newHazardCards = new ListCards(hazardCardList.toArray(new Card[hazardCardList.size()]), 0);
		
		List<Card> fightCardList = new ArrayList<>(gs.getYourFightingCards().asList());
		fightCardList.add(0, myCard);
		ListCards newYourFightingCards = new ListCards(fightCardList.toArray(new Card[fightCardList.size()]), 0);
		
		gs = new GameState(State.SELECT_A_HAZARD_CARD, newYourFightingCards, newHazardCards, 
				/* discard hazards */ new ImmutableGrowableListCards(15), gs.getSlowGameState(), null, /* fighting discard */new ImmutableGrowableListCards(60), 
				null /* lhsOrRhsFightingCardUsed*/, 20 /*life*/, GameState.defaultLhsFightCards(), GameState.defaultRhsFightCards(), 
				null /* lhsOrRhsFightingCardDoubled*/ , null /* ability object*/, gs, null);
			
		return gs;
	}

	
	public static GameState onlyOneHazardOnPile(GameState gs) {
		ImmutableGrowableListCards newDiscardHazards = gs.getDiscardHazards();

		List<Card> cards = gs.getHazardCards().asList();
		while(cards.size() > 1) {
			Card c = cards.remove(0);
			newDiscardHazards = newDiscardHazards.mutateAdd(c);
		}
		ListCards newHazardCards = new ListCards(cards.toArray(new Card[cards.size()]), 0);

		gs = new GameState(gs.getState(), gs.getYourFightingCards(), newHazardCards, 
				newDiscardHazards, gs.getSlowGameState(), gs.getActiveHazardCard(), gs.getDiscardFightCards(), 
				gs.getLhsOrRhsFightingCardUsed(), gs.getLifePoints(), gs.getLhsFightCards(), gs.getRhsFightCards(), 
				gs.getLhsOrRhsFightingCardDoubled(), gs.getAbilityObject(), gs, null);

		return gs;
	
	}
	

	public static void printTreeEntry(TreeEntry te, int depth) {
		
		if(te.getChildren().size() ==  0) { return; }
		
		String str = "";
		while(str.length() < depth*2) {
			str += " ";
		}
		
		str = "["+depth+"] "+te; 
		
		System.out.println(str);
		
		for(TreeEntry child : te.getChildren()) {
			
			printTreeEntry(child, depth+1);
			
		}
		
	}

	public static void calculateTransitiveChildren(Collection<TreeEntry> rootActionToTreeEntry) {
		
		HashMap<TreeEntry, Long> depthMap = new HashMap<>();
		
		HashMap<Long, List<TreeEntry>> entriesAtDepthMap = new HashMap<>();
		
		HashMap<TreeEntry, TreeEntry> childToParentMap = new HashMap<>();
		
		long deepestTreeEntry = -1;
		
		{
			final Queue<TreeEntry> remainingEntries = new LinkedList<TreeEntry>();
			remainingEntries.addAll(rootActionToTreeEntry);
			for(TreeEntry rootEntry : rootActionToTreeEntry) {
				depthMap.put(rootEntry, 0l);
			}
			
			int processed = 0;
			
			while(remainingEntries.size() > 0) {
				
				TreeEntry curr = remainingEntries.poll();
				processed++;
				
				Long depth = depthMap.get(curr);
				if(depth == null) { FridayUtil.throwErr("Depth not found for: "+curr); }

				if(depth > deepestTreeEntry) {
					deepestTreeEntry = depth;
				}
				
				List<TreeEntry> edList = entriesAtDepthMap.get(depth);
				if(edList == null) {
					edList = new ArrayList<>();
					entriesAtDepthMap.put(depth, edList);
				}
				edList.add(curr);				
				
				if(curr.getChildren() != null) {
					
					if(processed % 1000 == 0) {
						System.out.println("ctc: "+remainingEntries.size()+" "+curr.getChildren().size()+" "+processed);
					}
				
					curr.getChildren().forEach( child -> {
						childToParentMap.put(child, curr);
						
						depthMap.put(child, depth+1);
						remainingEntries.offer(child);
					});
				
				}
			}
		}
		
		
		
		{
			long currDepth = deepestTreeEntry;
			
			while(currDepth >= 0) {
				
				List<TreeEntry> list = entriesAtDepthMap.get(currDepth);
				
				for(TreeEntry te : list) {
					
					TreeEntry parent = childToParentMap.get(te);
					
					if(parent == null) { continue; }
					DebugTreeEntry parentDte = parent.getOrCreateDebugEntry();
					DebugTreeEntry teDte = te.getOrCreateDebugEntry();
					
					long newValue = parentDte.getDebug_transitive_children() + teDte.getDebug_transitive_children() + 1;
					
					parentDte.setDebug_transitive_children(newValue);
					
				}
			
				currDepth--;
			}
			
		}
				
	}
	
}
