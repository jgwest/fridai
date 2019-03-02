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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fridai.Ability;
import com.fridai.AbilityObject;
import com.fridai.Card;
import com.fridai.GameState;
import com.fridai.GameStateContainer;
import com.fridai.Main;
import com.fridai.PirateCard;
import com.fridai.PirateGameState;
import com.fridai.actions.Action;
import com.fridai.actions.Action.ActionType;
import com.fridai.actions.UseCardAbilityWithTargetAction;

/** This class contains global utility functions used throughout the application, as well as various constants 
 * used to control program behaviour for debugging purposes. */
public class FridayUtil {

	
	// User-tweakable constants:
	
	public static int QUEUE_TO_PROCESS = 200000;
	
	/** Whether or not to allow life points > 22, for debugging purposes */
	public static final boolean ALLOW_LARGE_LIFE_POINTS = false; 
	
	public static final boolean RUNTIME_CHECK = false;

	// Logged to this file: randomSeed, date, life points, phase score
	public static File RESULT_LOG = null;
	
	public static File RUN_LOG = null;
	
	public static File ON_ERROR_STATE_OUTPUT_PATH = null;
	
	// --------------------------------------------
	
	public static final String CRLF = System.getProperty("line.separator");
	
	public static AllCards ALL_CARDS;

	
	private static final Map<Ability, Integer> skillMapping = Collections.unmodifiableMap(getSkillMapping());
	
	private static final FightingCardComparator fightingCardComparator = new FightingCardComparator();

	
	public static List<Action> getActionsByAbility(List<Action> actions, ActionType typeParam, Ability abilityParam) {
		
		if(typeParam != ActionType.USE_CARD_ABILITY_WITH_TARGET) {
			FridayUtil.throwErr("Unsupported action type: "+typeParam);
		}
		
		List<Action> resultActions = actions.stream().filter(a -> {
			
			if(a.getType() == typeParam) {
				UseCardAbilityWithTargetAction ucawta = (UseCardAbilityWithTargetAction) a;
				Ability ab = ucawta.getCard().getAbility(); 
				if(ab == abilityParam) {
					return true;
				}
			}
			
			return false;
				
		}).collect(Collectors.toList());

		return resultActions;
	}
		
	public static void throwErr(String str) throws RuntimeException {
		throw new RuntimeException(str);
	}
	
	private static final Comparator<Card> comparatorByTrait = new Comparator<Card>() {

		@Override
		public int compare(Card o1, Card o2) {
			return o1.getTraitId() - o2.getTraitId();
		}
	};
 
	public final static ThreadLocal<RandomAdvanced> UNIVERAL_RANDOM = new ThreadLocal<>();
	
	public static void initializeRandomSeed(long l, long iterations) {
		UNIVERAL_RANDOM.set(new RandomAdvanced(l, iterations));
	}
	

	public final static void shuffleCollection(List<?> list) {
//		Collections.shuffle(list, ThreadLocalRandom.current());
		Collections.shuffle(list, UNIVERAL_RANDOM.get());
	}
	
	public final static void sortFightingCardsWorstToBest(List<Card> cards) {
		
		Collections.sort(cards, fightingCardComparator);
		
	}
	
	public final static int calculateFightingCardValue_simpleNew(Card one) {
		int abilityOne = 0;
		if(one.getAbility() != null) {
			abilityOne = skillMapping.get(one.getAbility());
		}
		
		return abilityOne + one.getFightingValue();
		
	}
	
	private static Map<Ability, Integer> getSkillMapping() {
		
		Map<Ability, Integer> map = new HashMap<>();
		
		map.put(Ability.PHASE_MINUS_1, 3);
		map.put(Ability.LIFE_ADD_2, 3);
		map.put(Ability.DESTROY_1x, 3);
		map.put(Ability.DOUBLE_1x, 3);
		map.put(Ability.CARDS_DRAW_2, 3);
		map.put(Ability.EXCHANGE_X2, 3);

		map.put(Ability.COPY_1x, 2);
		map.put(Ability.CARDS_DRAW_1, 2);
		map.put(Ability.EXCHANGE_X1, 2);
		map.put(Ability.LIFE_ADD_1, 2);

		map.put(Ability.SORT_3_CARDS, 1);
		map.put(Ability.BELOW_THE_PILE_1x, 1);

		map.put(Ability.DOT_DOT_DOT, 0); 

		map.put(Ability.LIFE_MINUS_1, -1);
		map.put(Ability.LIFE_MINUS_2, -2);
		map.put(Ability.STOP, -3);
		map.put(Ability.HIGHEST_CARD_IS_0, -3);

		return map;
		
	}

	public final static boolean areCardListsEqualByTrait(List<Card> one, List<Card> two) {
		if(one.size() != two.size()) { return false; }

		Collections.sort(one, comparatorByTrait);
		Collections.sort(two, comparatorByTrait);
		
		for(int x = 0; x < one.size(); x++) {
			Card cardOne = one.get(x);
			Card cardTwo = two.get(x);
			
			if(cardOne.getTraitId() != cardTwo.getTraitId()) { return false; }
			
		}
		
		return true;
		
	}

	public static boolean listsEqual(List<Card> one, List<Card> two) {
		if(one == null && two == null) { return true; }
		if(one == null && two != null) { return false; }
		if(one != null && two == null) { return false; }
		
		if(one.size() != two.size()) { return false; }
		
		for(int x = 0; x < one.size(); x++) {
			Card oneThing = one.get(x);
			Card twoThing = two.get(x);
			
			if(!oneThing.equalsByContents(twoThing)) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean pirateListsEqual(List<PirateCard> one, List<PirateCard> two) {
		if(one == null && two == null) { return true; }
		if(one == null && two != null) { return false; }
		if(one != null && two == null) { return false; }
		
		if(one.size() != two.size()) { return false; }
		
		for(int x = 0; x < one.size(); x++) {
			PirateCard oneThing = one.get(x);
			PirateCard twoThing = two.get(x);
			
			if(!oneThing.equalsByContents(twoThing)) {
				return false;
			}
		}
		
		return true;
	}

	
	public static boolean objectEquals(Object one, Object two) {
		if(one == null && two == null) { return true; }
		if(one == null && two != null) { return false; }
		if(one != null && two == null) { return false; }
		
		if(!one.getClass().equals(two.getClass())) {
			return false;
		}
		
		return one.equals(two);
	}

	public static boolean isXorNull(Object one, Object two) {
		if(one == null && two != null ) { return true; }
		if(one != null && two == null ) { return true; }
		
		return false;
	}
	
	public static boolean bothAreNull(Object one, Object two) {
		if(one == null && two == null) { return true; }
		
		return false;
	}
	
	public static boolean areMapsEqual(MapCards one, MapCards two) {
		if(one.size() != two.size()) {
			return false;
		}
	
		for(int x = 0; x < one.size(); x++) {
			
			if(one.get(x) != two.get(x)) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean areMapsEqual(Map<?, ?> one, Map<?, ?> two) {
		if(one.size() != two.size()) {
			return false;
		}
		
		for(Map.Entry<?, ?> e : one.entrySet()) {
			
			Object value = two.get(e.getKey());
			
			if(!value.equals(e.getValue())) {
				return false;
			}
			
		}
		
		// This second foreach loop is unnecessary unless something very broken is going on
		for(Map.Entry<?, ?> e : two.entrySet()) {
			
			Object value = one.get(e.getKey());
			
			if(!value.equals(e.getValue())) {
				return false;
			}
			
		}
		
		return true;
	}
	
	private final static HashMap<String /* path */, Boolean /* is initialized*/ > logToFileInitalized_lock = new HashMap<>();

	/** Threadsafe */
	public static void logToFile(String str, File f) throws IOException {
		
		synchronized (logToFileInitalized_lock) {
			Boolean isInitialized = logToFileInitalized_lock.get(f.getPath());
			if(isInitialized ==  null) {
				f.delete();
				logToFileInitalized_lock.put(f.getPath(), true);
			}

			FileWriter fw = new FileWriter(f, true);
			fw.write(str+CRLF); 
			fw.close();
			
		}
		
	}
	
	public static InputStream readFridayGameDataFile() throws IOException {
		String path = System.getProperty("user.dir");
		
		
		InputStream fridayGameTxtInputStream = null;
		
		File devLocation =  new File(path, "resources/Friday-Game.txt");
		if(devLocation.exists()) {
			fridayGameTxtInputStream = new FileInputStream(devLocation);
		} else {

			File jarFile;
			try {
				jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
				
				if(!jarFile.getPath().endsWith(".jar")) { FridayUtil.throwErr("Unable to find JAR:" + jarFile );}
				
			} catch (URISyntaxException e1) {
				throw new RuntimeException(e1);
			}

			
			@SuppressWarnings("resource")
			ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile));

			ZipEntry ze;
			while (null != (ze = zis.getNextEntry())) {
				if(ze.getName().equals("Friday-Game.txt")) {
					fridayGameTxtInputStream = zis;
					break;
				}
			}

		}		
		

		return fridayGameTxtInputStream;
	}
	

	public static boolean compareGameState(GameStateContainer one, GameStateContainer two) {
		if(!FridayUtil.bothAreNull(one, two)) {
			
			if(FridayUtil.isXorNull(one, two)) {
				return false;
			} else {
				
				if(one.getGameState() != null && two.getGameState() == null) { return false; }
				if(one.getGameState() == null && two.getGameState() != null) { return false; }
				
				if(one.getGameState() != null) {
					return compareGameState(one.getGameState(), two.getGameState());
				} else {
					return compareGameState(one.getPirateGameState(), two.getPirateGameState());
				}				
			}
		}
		
		return true;
	}
	
	public static boolean compareGameState(GameState one, GameState two) {
		
		// Ability Object
		{
			AbilityObject aoOne = one.getAbilityObject();
			AbilityObject aoTwo = two.getAbilityObject();
			
			if(!FridayUtil.bothAreNull(aoOne, aoTwo)) {
				
				if(FridayUtil.isXorNull(aoOne, aoTwo)) {
					return false;
				} else if(!aoOne.equalsByContents(aoTwo)) {
					return false;
				}
								
			}
			
		}
		
		// Active Hazard Card
		{
			Card ahcOne = one.getActiveHazardCard();
			Card ahcTwo = two.getActiveHazardCard();
			
			if(!FridayUtil.bothAreNull(ahcOne, ahcTwo)) {
				
				if(FridayUtil.isXorNull(ahcOne, ahcTwo)) {
					return false;
				} else if(!ahcOne.equalsByContents(ahcTwo)) {
					return false;
				}
			}
		}
		
		// Discard fight
		if(!FridayUtil.listsEqual(one.getDiscardFightCards().getAsList(), two.getDiscardFightCards().getAsList())) {
			return false;
		}
		
		// Discard hazards
		if(!FridayUtil.listsEqual(one.getDiscardHazards().getAsList(), two.getDiscardHazards().getAsList())) {
			return false;
		}
		
		// Hazard cards
		if(!FridayUtil.listsEqual(one.getHazardCards().asList(), two.getHazardCards().asList())) {
			return false;
		}

		// Lhs fight
		if(!FridayUtil.listsEqual(one.getLhsFightCards().getAsList(), two.getLhsFightCards().getAsList())) {
			return false;
		}
		
		// Rhs fight
		if(!FridayUtil.listsEqual(one.getRhsFightCards().getAsList(), two.getRhsFightCards().getAsList())) {
			return false;
		}
		
		// Fighting cards doubled
		if(!FridayUtil.areMapsEqual(one.getLhsOrRhsFightingCardDoubled(), two.getLhsOrRhsFightingCardDoubled())) {
			return false;
		}
		
		// Fighting cards used
		if(!FridayUtil.areMapsEqual(one.getLhsOrRhsFightingCardUsed(), two.getLhsOrRhsFightingCardUsed())) {
			return false;
		}
		
		if(one.getLifePoints() != two.getLifePoints()) {
			return false;
		}
		
		if(one.getState() != two.getState()) {
			return false; 
		}
		
		if(!FridayUtil.listsEqual(one.getYourFightingCards().asList(), two.getYourFightingCards().asList())) {
			return false;
		}
		
		if(!one.getSlowGameState().equalByContents(two.getSlowGameState())) {
			return false;
		}
		
		return true;
	}
	
	public static boolean compareGameState(PirateGameState one, PirateGameState two) {
		
		// Ability Object
		{
			AbilityObject aoOne = one.getAbilityObject();
			AbilityObject aoTwo = two.getAbilityObject();
			
			if(!FridayUtil.bothAreNull(aoOne, aoTwo)) {
				
				if(FridayUtil.isXorNull(aoOne, aoTwo)) {
					return false;
				} else if(!aoOne.equalsByContents(aoTwo)) {
					return false;
				}
								
			}
			
		}
		
		// Active Hazard Card
		{
			PirateCard ahcOne = one.getActivePirateCard();
			PirateCard ahcTwo = two.getActivePirateCard();
			
			if(!FridayUtil.bothAreNull(ahcOne, ahcTwo)) {
				
				if(FridayUtil.isXorNull(ahcOne, ahcTwo)) {
					return false;
				} else if(!ahcOne.equalsByContents(ahcTwo)) {
					return false;
				}
			}
		}
		
		// Discard fight
		if(!FridayUtil.listsEqual(one.getDiscardFightCards().getAsList(), two.getDiscardFightCards().getAsList())) {
			return false;
		}
		
		// Lhs fight
		if(!FridayUtil.listsEqual(one.getLhsFightCards().getAsList(), two.getLhsFightCards().getAsList())) {
			return false;
		}
		
		// Rhs fight
		if(!FridayUtil.listsEqual(one.getRhsFightCards().getAsList(), two.getRhsFightCards().getAsList())) {
			return false;
		}
		
		// Fighting cards doubled
		if(!FridayUtil.areMapsEqual(one.getLhsOrRhsFightingCardDoubled(), two.getLhsOrRhsFightingCardDoubled())) {
			return false;
		}
		
		// Fighting cards used
		if(!FridayUtil.areMapsEqual(one.getLhsOrRhsFightingCardUsed(), two.getLhsOrRhsFightingCardUsed())) {
			return false;
		}
		
		if(one.getLifePoints() != two.getLifePoints()) {
			return false;
		}
		
		if(one.getState() != two.getState()) {
			return false; 
		}
		
		if(!FridayUtil.listsEqual(one.getYourFightingCards().asList(), two.getYourFightingCards().asList())) {
			return false;
		}
		
		if(!one.getSlowGameState().equalByContents(two.getSlowGameState())) {
			return false;
		}
		
		return true;
	}

	
	public static String convertExceptionToString(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return  sw.toString(); 
	}

	
	/** Sort cards by how "good" they are, using fighting value and an arbitrary mapping
	 * of skill ranking defined in this class.*/
	private static class FightingCardComparator implements Comparator<Card> {

		@Override
		public int compare(Card one, Card two) {
			
			int abilityOne = skillMapping.get(one.getAbility());
			int abilityTwo = skillMapping.get(two.getAbility());
			
			int skillDiff = abilityOne - abilityTwo;
			
			int fightingValueDiff = one.getFightingValue() - two.getFightingValue();

			return skillDiff + fightingValueDiff;
			
		}
		
	}
	
}
