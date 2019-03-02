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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.fusesource.jansi.AnsiConsole;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fridai.Card.Difficulty;
import com.fridai.Card.Type;
import com.fridai.GameState.State;
import com.fridai.PirateGameState.PirateState;
import com.fridai.actions.Action;
import com.fridai.actions.Action.ActionType;
import com.fridai.actions.ActionResponse;
import com.fridai.actions.DestroyCardsWithPaidLifeAction;
import com.fridai.actions.DestroyCardsWithPaidLifeActionResponse;
import com.fridai.actions.DrawAFreeCardAction;
import com.fridai.actions.DrawAFreeCardActionResponse;
import com.fridai.actions.EndDrawFreeCardPhaseAction;
import com.fridai.actions.EndDrawFreeCardPhaseActionResponse;
import com.fridai.actions.EndMultistageAbilityAction;
import com.fridai.actions.EndMultistageAbilityActionResponse;
import com.fridai.actions.EndPirateRoundAction;
import com.fridai.actions.EndPirateRoundActionResponse;
import com.fridai.actions.EndSacrificeLifePhaseAction;
import com.fridai.actions.EndSacrificeLifePhaseActionResponse;
import com.fridai.actions.FightOrDiscardHazardCardAction;
import com.fridai.actions.FightOrDiscardHazardCardActionResponse;
import com.fridai.actions.SacrificeLifeToDrawAction;
import com.fridai.actions.SacrificeLifeToDrawActionResponse;
import com.fridai.actions.SelectAPirateAction;
import com.fridai.actions.SelectAPirateActionResponse;
import com.fridai.actions.SelectFromTwoHazardCardsAction;
import com.fridai.actions.SelectFromTwoHazardCardsActionResponse;
import com.fridai.actions.UseCardAbilityAction;
import com.fridai.actions.UseCardAbilityActionResponse;
import com.fridai.actions.UseCardAbilitySortAction;
import com.fridai.actions.UseCardAbilitySortActionResponse;
import com.fridai.actions.UseCardAbilityWithTargetAction;
import com.fridai.actions.UseCardAbilityWithTargetActionResponse;
import com.fridai.actions.UseCopyAbilityAction;
import com.fridai.actions.UseCopyAbilityActionResponse;
import com.fridai.json.JsonGameStatePersistence;
import com.fridai.json.JsonPersistenceUtil;
import com.fridai.util.AllCards;
import com.fridai.util.BenchmarkEntries;
import com.fridai.util.ComboUtil;
import com.fridai.util.DebugUtil;
import com.fridai.util.FridayUtil;
import com.fridai.util.ImmutableGrowableListCards;
import com.fridai.util.ListCards;


public class Main {
	
	public static final boolean LOG_OUT = false;

	public static void main(String[] args) throws IOException {

		// Uncomment this if running in Eclipse w/ ANSI colour console plugin:
		// System.setProperty("jansi.passthrough", "true");
		AnsiConsole.systemInstall();

		// Note: You can change the value of `LOG_OUT` to true, to see the moves that
		// the AI is making. If you do this, you should set the # of threads to 1.
		
		// Note: See user-tweakable constants in FridayUtil.
		
		if(args.length == 0) {
			// Zero args: run the keyboard drive game mode
			gameUI(args);
		} else {
			// Otherwise kick off the AI with the specified parameters
			gameAI(args);
		}

		// Uncomment and comment out the above to debug a specific failure game state.
		// debugSpecificGameState();		
	}
	
	private static void gameUI(String[] args) throws IOException {

		InputStream fridayGameTxtInputStream = FridayUtil.readFridayGameDataFile();
		
		if(fridayGameTxtInputStream == null) {
			FridayUtil.throwErr("Unable to find Friday data file.");
			return;
		}
		
		CardReader cr = new CardReader(fridayGameTxtInputStream);
		
		AllCards ac = new AllCards(cr.getAgingCards(), cr.getPirateCards(), cr.getFightingCards(), cr.getHazardCards());
		
		FridayUtil.ALL_CARDS = ac;
		
		FridayUtil.initializeRandomSeed((new Random()).nextLong(), 0);
		 
		GameStateContainer gs = new GameStateContainer(initializeGameState(FridayUtil.ALL_CARDS));

		devMain(gs, true);
	}
	
	
	private static void gameAI(String args[]) throws IOException { 
		if(args.length != 5) {
			System.err.println("(time to run in minutes) (hyper param value) (perf output file) (num thread) (result output file)");
			return;
		}
		
		InputStream fridayGameTxtInputStream = FridayUtil.readFridayGameDataFile();
		
		if(fridayGameTxtInputStream == null) {
			FridayUtil.throwErr("Unable to find Friday data file.");
			return;
		}
		
		int timeToRunInMinutes = Integer.parseInt(args[0]);
		int hyperParamValue = Integer.parseInt(args[1]); 
		File perfOutputFile = new File(args[2]);
		int numThreads = Integer.parseInt(args[3]);
		File resultOutputFile = new File(args[4]);
		
		FridayUtil.QUEUE_TO_PROCESS = hyperParamValue;
		FridayUtil.ON_ERROR_STATE_OUTPUT_PATH = new File(resultOutputFile.getPath()+".state.json");
		FridayUtil.RUN_LOG = new File(resultOutputFile.getPath()+".run-log.txt");
		FridayUtil.RESULT_LOG = resultOutputFile;
		
		CardReader cr = new CardReader(fridayGameTxtInputStream);
		
		AllCards ac = new AllCards(cr.getAgingCards(), cr.getPirateCards(), cr.getFightingCards(), cr.getHazardCards());
		
		FridayUtil.ALL_CARDS = ac;
		
		// Note: Change this to a fixed value (eg 0) to deterministically run a fixed set of games.
		long initialSeed = new Random().nextLong();
		
		for(int x = 0; x < numThreads; x++) {
			StateRunnerThread trt = new StateRunnerThread(initialSeed+x, numThreads, "Runner thread");
			trt.start();
		}

		long expireTimeInNanos;
		if(timeToRunInMinutes > 0) {
			expireTimeInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeToRunInMinutes, TimeUnit.MINUTES);
		} else {
			expireTimeInNanos = Long.MAX_VALUE;
		}
		
		while(System.nanoTime() < expireTimeInNanos) {
			
			try { Thread.sleep(12 * 1000); } catch (InterruptedException e) { e.printStackTrace(); }
			
			BenchmarkEntries.getInstance().printThroughput();
			
		}

		System.out.println("Final throughput:");
		BenchmarkEntries.getInstance().printThroughput();

		BenchmarkEntries.getInstance().writeAverageThroughputEntryToFile(perfOutputFile);

		// All work is complete, so manually terminate the process.
		System.exit(0);
		
	}
	
	/** Use this method to debug a specific point in a time for a specific game state. Use the seed that the
	 * game was started with, and the number of rand iterations that was printed when the failure state JSON
	 * was output. */
	@SuppressWarnings("unused")
	private static void debugSpecificGameState() throws IOException {
		
		final File currStateFile = new File("fail-state.json"); // change me
		final int SEED_TO_DEBUG = 173; // change me
		final int NUMBER_OF_RAND_ITERATIONS = 0; // change me
		
		InputStream fridayGameTxtInputStream = FridayUtil.readFridayGameDataFile();
		
		if(fridayGameTxtInputStream == null) { FridayUtil.throwErr("Unable to find Friday data file."); return; }
		
		CardReader cr = new CardReader(fridayGameTxtInputStream);
		
		AllCards ac = new AllCards(cr.getAgingCards(), cr.getPirateCards(), cr.getFightingCards(), cr.getHazardCards());
		
		FridayUtil.ALL_CARDS = ac;
		
		FridayUtil.initializeRandomSeed(SEED_TO_DEBUG, NUMBER_OF_RAND_ITERATIONS);
		JsonGameStatePersistence jgsp = new ObjectMapper().readValue(currStateFile, JsonGameStatePersistence.class);
		
		GameStateContainer gs = JsonPersistenceUtil.fromJson(jgsp);
		devMain(gs, false);

	}
	
	
	/** When the main game loop is processing game rounds, the various active game states are stored
	 * in instances of this class. The TreeEntry class is used apply score and depth to each individual
	 * game state, as well as the keep track of children of a given parent game state.*/
	private static final class QueueEntry {
		final GameStateContainer gs;
		final TreeEntry te;
		
		public QueueEntry(GameStateContainer gs, TreeEntry te) {
			this.gs = gs;
			this.te = te;
		}
		
	}
	
	
	/** This is used only for debugging; it is used to create a game state graph by keeping track of the 
	 * additional graph data which is not used outside of debugging, including the action that was taken
	 * to arrive at the given game state, and the result. */
	public static final class DebugTreeEntry {
		private Action debug_preAction = null;
		private GameStateContainer debug_postContainer = null;
		private long debug_transitive_children = 0;
		private long debugId;

		// TODO: LOWER - This should be thread local
		private static long nextTreeEntryDebugId = 0;
		
		public DebugTreeEntry() {
			debugId = nextTreeEntryDebugId++;
		}
		
		public void setDebug_preAction(Action debug_preAction) {
			this.debug_preAction = debug_preAction;
		}
		
		public Action getDebug_preAction() {
			return debug_preAction;
		}
		
		public GameStateContainer getDebug_postContainer() {
			return debug_postContainer;
		}
		
		public void setDebug_postContainer(GameStateContainer debug_postContainer) {
			this.debug_postContainer = debug_postContainer;
		}
		
		public long getDebug_transitive_children() {
			return debug_transitive_children;
		}
		
		public void setDebug_transitive_children(long debug_transitive_children) {
			this.debug_transitive_children = debug_transitive_children;
		}
		
		public long getDebugId() {
			return debugId;
		}

	}
	
	/** Keep track of the metadata and children of a QueueEntry used by the game loop's main queue. 
	 * Score represents inherent "winability" of a game state, and the depth and children 
	 * values represent the tree relationships with other game states. */
	public static final class TreeEntry {
		private int score;
		private final int depth;
		private final List<TreeEntry> children = new ArrayList<>();
		
		private DebugTreeEntry debugEntry;
		
		public TreeEntry(int score, int depth) {
			this.score = score;
			this.depth = depth;
		}
		
		public List<TreeEntry> getChildren() {
			return children; // TODO: LOWER - To reduce memory usage, should this be null until it is first used?
		}
				
		@SuppressWarnings("unused")
		@Override
		public String toString() {
			
			String result = "score: "+score+" depth:"+depth+" children: "+children.size();

			if(FridayUtil.RUNTIME_CHECK && this.debugEntry != null) {
				result += " transitive children: "+debugEntry.debug_transitive_children;				
			}
			
			return result;

		}
		
		public DebugTreeEntry getOrCreateDebugEntry() {
			if(debugEntry == null) {
				this.debugEntry = new DebugTreeEntry();
			}
			return this.debugEntry;
		}
		
		@SuppressWarnings("unused")
		@Override
		public int hashCode() {
			if(!FridayUtil.RUNTIME_CHECK) { return super.hashCode(); } // Fast path for non-debug
			
			if(this.debugEntry == null) {
				return super.hashCode();
			} else {
				return (int) this.debugEntry.debugId;
			}
			
			
		}
		
		@SuppressWarnings("unused")
		@Override
		public boolean equals(Object obj) {
			if(!FridayUtil.RUNTIME_CHECK) { return super.equals(obj); } // Fast path for non-debug 
			
			if(this.debugEntry == null) {
				return super.equals(obj);
			} else {
				TreeEntry other = (TreeEntry)obj;
				if(other.debugEntry != null) {
					return other.debugEntry.debugId == this.debugEntry.debugId;
				} else {
					return false;
				}
			}
		}
		
	}
	
	
	/** This method removes low-value actions that are unlikely to bare fruit, so that the 
	 * tree logic does search it. */
	private static void filterActions(List<Action> actions, GameState gs) {
		if(gs.getState() == State.PAY_LIFE_POINTS_ON_HAZARD_MISS) {

			DestroyCardsWithPaidLifeAction bestAction = null;
			int bestDestructionValue = Integer.MAX_VALUE; 
			
			for(Action a : actions) {
				DestroyCardsWithPaidLifeAction dcwpla = (DestroyCardsWithPaidLifeAction)a;
				
				int destructionValue = 0; 
				for(Card c : dcwpla.getCardsToDestroy()) {
					destructionValue += c.getRatingSimple();
				}
				
				if(destructionValue <= 0 && destructionValue < bestDestructionValue) {
					bestAction = dcwpla;
					bestDestructionValue = destructionValue;
				}
			}
			
			// Remove all but the best
			if(bestAction != null) {
				for(Iterator<Action> it = actions.iterator(); it.hasNext(); ) {			
					Action a = it.next();
					if(a != bestAction) { it.remove(); }
				}
			}
		}
	}
	
	
	/** Apply a "how winnable is this" score (larger is better) to a given game state. */
	private static final int score(GameStateContainer container) {
		
		if(!container.isGameState()) {
			PirateGameState gs = container.getPirateGameState();

			int allFightingCardsValue = 0;
			int count = 0;

			for(int x = 0; x < gs.getDiscardFightCards().size(); x++) {
				Card c = gs.getDiscardFightCards().get(x);
				allFightingCardsValue += c.getRatingSimple();
				count++;
			}
			
			for(int x = 0; x < gs.getLhsFightCards().size(); x++) {
				Card c = gs.getLhsFightCards().get(x);
				allFightingCardsValue += c.getRatingSimple();
				count++;
			}

			for(int x = 0; x < gs.getRhsFightCards().size(); x++) {
				Card c = gs.getRhsFightCards().get(x);
				allFightingCardsValue += c.getRatingSimple();
				count++;
			}

			
			for(int x = 0; x < gs.getYourFightingCards().size(); x++) {
				Card c = gs.getYourFightingCards().get(x);
				allFightingCardsValue += c.getRatingSimple();
				count++;
			}
			
			return gs.getLifePoints()* 10000000 + ((1000*allFightingCardsValue)/count);

		} else {
			GameState gs = container.getGameState();
			
			int allFightingCardsValue = 0;
			int count = 0;

			for(int x = 0; x < gs.getDiscardFightCards().size(); x++) {
				Card c = gs.getDiscardFightCards().get(x);
				allFightingCardsValue += c.getRatingSimple();
				count++;
			}
			
			for(int x = 0; x < gs.getLhsFightCards().size(); x++) {
				Card c = gs.getLhsFightCards().get(x);
				allFightingCardsValue += c.getRatingSimple();
				count++;
			}

			for(int x = 0; x < gs.getRhsFightCards().size(); x++) {
				Card c = gs.getRhsFightCards().get(x);
				allFightingCardsValue += c.getRatingSimple();
				count++;
			}

			
			for(int x = 0; x < gs.getYourFightingCards().size(); x++) {
				Card c = gs.getYourFightingCards().get(x);
				allFightingCardsValue += c.getRatingSimple();
				count++;
			}
			
			return gs.getLifePoints()* 10000000 + ((1000*allFightingCardsValue)/count);
			
		}
		
	}	

	/** Score all of the available actions of gsParam */
	@SuppressWarnings("unused")
	public static Map<Action, Integer> evaluateBestAction(GameStateContainer gsParam) {
		
		boolean debug_createGameStateGraph = false;
		
		Comparator<QueueEntry> scoreComparator = (a, b)-> { return b.te.score - a.te.score; };
	
		PriorityQueue<QueueEntry> queue = new PriorityQueue<>(256 *1024, scoreComparator );

		HashMap<Action, TreeEntry> rootActionToTreeEntry = new HashMap<>();
		
		List<Action> rootActions = calculateAvailableActions(gsParam);
		for(Action action : rootActions) {
			ActionResponse r = convertActionToResponse(action);

			WinnableGameStateContainer wgsc = doAction(r, gsParam);
			
			TreeEntry te;
			if(wgsc.isWin()) { // Have we won?
				te = new TreeEntry(Integer.MAX_VALUE, 0 /* depth*/);
			} else {
				GameStateContainer ngs = wgsc.getGsc();
				te = new TreeEntry(score(ngs), 0 /* depth*/);
				queue.offer(new QueueEntry(ngs, te));	
			}
			
			rootActionToTreeEntry.put(action, te);
			
		}

		int currDeepestTree = 0;
		
		long processed = 0;
		
		long startTimeInNanos = System.nanoTime();
		
		long maxProcessed = FridayUtil.QUEUE_TO_PROCESS;
		
		while(queue.size() > 0) {
					
			QueueEntry curr = queue.poll();
			
			List<Action> actions = calculateAvailableActions(curr.gs);
			
			if(actions == null || actions.size() == 0) { continue; }
			
			if(curr.gs.isGameState()) {
				filterActions(actions, curr.gs.getGameState());
			}
			
			
			// Uncomment this line to verify the JSON persistence correctness of every game state that this
			// method processes.			
			// debugVerifyJsonPersistenceCorrectness(curr.gs);
		
			for(Action action : actions) {
				ActionResponse r = convertActionToResponse(action);

				WinnableGameStateContainer wgsc = doAction(r, curr.gs);

				if(curr.te.depth > currDeepestTree) {
					currDeepestTree = curr.te.depth;
				}
				
				if(FridayUtil.RUNTIME_CHECK && !wgsc.isWin()) {
					// Sanity check that we can't get an infinite loop by returning the same state pre- and post-
					if(curr.gs.isGameState() && wgsc.getGsc().isGameState()) {
						
						if(curr.gs.getGameState() == wgsc.getGsc().getGameState()) {
							FridayUtil.throwErr("Input and output should not match.");
						}
						
					}
					
					if(!curr.gs.isGameState() && !wgsc.getGsc().isGameState()) {
						
						if(curr.gs.getPirateGameState() == wgsc.getGsc().getPirateGameState()) {
							FridayUtil.throwErr("Input and output should not match.");
						}
						
					}
				}
				
				TreeEntry newTreeEntry;
				if(wgsc.isWin()) {
					newTreeEntry = new TreeEntry(Integer.MAX_VALUE, curr.te.depth+1);					
				} else {
					GameStateContainer newGameState = wgsc.getGsc();
					newTreeEntry = new TreeEntry(score(newGameState), curr.te.depth+1);
					queue.offer(new QueueEntry(newGameState, newTreeEntry));
				}
				
				if(debug_createGameStateGraph) {
					DebugTreeEntry dte = newTreeEntry.getOrCreateDebugEntry();
					dte.setDebug_preAction(action);
					dte.setDebug_postContainer(wgsc.getGsc());
				}
								
				curr.te.getChildren().add(newTreeEntry);
				
			}
			
			processed++;
			
			if(processed > maxProcessed) {
				break;
			}

			// Uncomment this block to output the # of processed game states per second:
			// if(processed % 100000 == 0) {				
			//	double seconds = ((double)TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTimeInNanos, TimeUnit.NANOSECONDS))/1000d;
			//	System.out.println(processed+" "+(int)(processed/seconds)); 
			// }

		} // end while()
		
		long completeTime = System.nanoTime();
		
		BenchmarkEntries.getInstance().addIterations(processed, completeTime - startTimeInNanos);
		
		double seconds = ((double)TimeUnit.MILLISECONDS.convert(completeTime - startTimeInNanos, TimeUnit.NANOSECONDS))/1000d;
		
		if(LOG_OUT) {
			System.out.println();
			NumberFormat nf = NumberFormat.getInstance();
			System.out.println("Total processed:"+nf.format(processed)+"  "+nf.format((int)(processed/seconds))+" per second");
		}
		
		Map<Action, Integer> result = new HashMap<>(); 
		
		int deepestTree = Integer.MIN_VALUE; // LOG_OUT only 
		
		for(Map.Entry<Action, TreeEntry> e : rootActionToTreeEntry.entrySet()) { 
			applyFinalScore(e.getValue());
			result.put(e.getKey(), e.getValue().score);
			
			if(LOG_OUT) {
				int depth = getDeepestTreeEntry(e.getValue());
				if(depth > deepestTree) {
					deepestTree = depth;
				}
			}
		}
		
		if(LOG_OUT) {
			System.out.println("Deepest tree: "+deepestTree);
		}
		
		return result;
	}

	/**
	 * The score of a tree is the best score of a child, if a child exists, otherwise it is the life points 
	 * for the game state.
	 */
	private static void applyFinalScore(TreeEntry root) {

		HashMap<TreeEntry, TreeEntry> childToParent = new HashMap<>();
		
		List<TreeEntry> leafNodes = new ArrayList<>();
		{
			Deque<TreeEntry> stack = new ArrayDeque<TreeEntry>();
			stack.offer(root);
			while(stack.size() > 0) {
				TreeEntry curr = stack.pop();
				
				if(curr.getChildren().size() == 0) {
					leafNodes.add(curr);
				} else {
					
					// Reset the score of all non-leaf nodes to lowest value.
					curr.score = Integer.MIN_VALUE;
					
					for(TreeEntry child : curr.getChildren()) {
						stack.offer(child);
						childToParent.put(child, curr);
					}
				}
			}
		}
				
		Deque<TreeEntry> queue = new ArrayDeque<TreeEntry>(leafNodes);
		
		while(queue.size() > 0) {
			TreeEntry curr = queue.poll();
			
			TreeEntry parent = childToParent.get(curr);
			if(parent == null) { continue; }
			
			if(curr.score > parent.score) {
				parent.score = curr.score;
				queue.offer(parent);
			}
		}			
	}
	
	
	private static void applyFinalScoreRecursive(TreeEntry root) {
		
		int bestScore = Integer.MIN_VALUE;
		
		for(TreeEntry child : root.getChildren()) {
			
			applyFinalScore(child);
			
			if(child.score > bestScore) {
				bestScore = child.score;
			}
		}
		
		if(bestScore != Integer.MIN_VALUE) {
			root.score = bestScore;
		} else {
			// In this scenario, where the root has no children, the score is the existing score 
			// that is already in the tree root (eg life points).
		}
			
	}

	

	/**
	 * The score of a tree is the best score of a child, if a child exists, otherwise it is the life points for the game state.
	 */
	private static int getDeepestTreeEntry(TreeEntry root) {
		
		// TODO: Test getDeepestTreeEntryNew as a replacement for this.
		
		int bestScore = Integer.MIN_VALUE;
		
		if(root.getChildren().size() == 0) {
			return root.depth;
		}
		
		for(TreeEntry child : root.getChildren()) {
		
			int childDepth = getDeepestTreeEntry(child);
			if(childDepth > bestScore) {
				bestScore = childDepth;
			}
		}
		
		return bestScore;
	}

	@SuppressWarnings("unused")
	private static int getDeepestTreeEntryNew(TreeEntry root) {
		
		Deque<TreeEntry> stack = new ArrayDeque<TreeEntry>();
		
		stack.offer(root);
		
		int overallBestScore = Integer.MIN_VALUE;
		
		
		while(stack.size() > 0) {
			
			TreeEntry curr = stack.poll();
			
			int currBestScore = Integer.MIN_VALUE;
			if(curr.getChildren().size() == 0) {
				currBestScore = root.depth;
			} else {
				curr.getChildren().forEach( e -> { stack.offer(e); });
			}
			
			
			if(currBestScore > overallBestScore) {
				overallBestScore = currBestScore;
			}
			
		}

		return overallBestScore;
		
		
//		for(TreeEntry child : root.getChildren()) {
//		
//			int childDepth = getDeepestTreeEntry(child);
//			if(childDepth > bestScore) {
//				bestScore = childDepth;
//			}
//		}
//		
//		return bestScore;
	}

		
	private static DevMainResult devMain(GameStateContainer gs, boolean userUI) throws IOException {
		
		boolean gameRunning = true;

		long mostRecentlyProcessedRandomIterations = 0;
		GameStateContainer mostRecentlyProcessed = null; 

		List<GameStateContainer> previousGameStates = new ArrayList<>();
		
		// Note: You can alter the game state at this point for debugging purposes, for example:
		// gs = DebugUtil.decreasePhase(gs);
		// gs = DebugUtil.moveFromHazardStackToFightStack(Ability.DOUBLE_1x, gs);		
		// gs = DebugUtil.moveAbilityFromAgingToFrontFight(Ability.LIFE_MINUS_2, gs);
		// gs = new GameStateContainer(DebugUtil.onlyOneHazardOnPile(gs.getGameState()));		
		// gs = new GameStateContainer(DebugUtil.setActivePiratesByAbility(new PirateCardAbility[] {PirateCardAbility.FIGHT_AGAINST_ALL_REMAINING_HAZARD_CARDS, PirateCardAbility.DOT_DOT_DOT}, gs.getGameState()));
		
		try {
			
			while(gameRunning) {
	
				mostRecentlyProcessed = gs;
				mostRecentlyProcessedRandomIterations = FridayUtil.UNIVERAL_RANDOM.get().getNumbersGenerated();
				
				List<Action> availableActions = calculateAvailableActions(gs);
				if(availableActions.size() == 0) {
					// Game over: player has lost.
					System.out.println("You lose!");
					return new DevMainResult(false, gs); 
				
				}

				// Detect if we are actively in a action cycle, by looking for duplicate game states.
				detectAndAmeliorateCycle(previousGameStates, gs, availableActions);
								
				// In the non-user case, we only need to evaluate best action if there is more than one action.
				Action actionBeingTaken = availableActions.get(0); // This should not be null.
				if(availableActions.size() > 1 && !userUI) {
					Map<Action, Integer> m = evaluateBestAction(gs);
					actionBeingTaken = null;
					Integer actionScore =  null;
	
					if(LOG_OUT) {
						System.out.println("Best actions: ");
					}
					// Find the best action
					for(Map.Entry<Action, Integer> e : m.entrySet()) {
						if(LOG_OUT) {
							System.out.println(e.getKey()+" "+e.getValue());
						}
						if(actionBeingTaken == null || actionScore < e.getValue()) {
							actionBeingTaken = e.getKey();
							actionScore = e.getValue();
						}
					}					
				}
				
				ActionResponse r;
				if(userUI) {
					// Trigger GC while we wait for user input
					new Thread( () -> { System.gc(); }).start();					
					r = MainUI.handleOptions(availableActions, gs);
				} else {
					if(LOG_OUT) {
						MainUI.printUIState(gs, actionBeingTaken);
						System.out.println("Taking action: "+actionBeingTaken.prettyPrint());
					}
					r = convertActionToResponse(actionBeingTaken);
				}
				
				WinnableGameStateContainer wgs = doAction(r, gs);
				if(wgs.isWin()) {
					System.out.println("You win!");
					// On win, return the game state just before the win (because there is no win game state)
					if(gs.isGameState()) {
						return new DevMainResult(true, new GameStateContainer(gs.getGameState()));
					} else {
						return new DevMainResult(true, new GameStateContainer(gs.getPirateGameState()));
					}
					
				} else {
					gs = wgs.getGsc();
					
					previousGameStates.add(gs);
					while(previousGameStates.size() > 10) { previousGameStates.remove(0); }

				}
												
			}
			
		} catch(Throwable e) {
			// On error, log the failed game state to file for postmortem.
			if(mostRecentlyProcessed != null) {
				
				ObjectMapper om = new ObjectMapper();

				FileWriter fw = new FileWriter(FridayUtil.ON_ERROR_STATE_OUTPUT_PATH);
				
				JsonGameStatePersistence jgsp;
				if(mostRecentlyProcessed.getGameState() != null) {
					jgsp = JsonPersistenceUtil.toJson(mostRecentlyProcessed.getGameState());	
				} else {
					jgsp = JsonPersistenceUtil.toJson(mostRecentlyProcessed.getPirateGameState());
				}
				
				om.writeValue(fw, jgsp);
				fw.close();				
			}
			
			String MSG = "Random seed is: "+FridayUtil.UNIVERAL_RANDOM.get().getRandomAdvancedSeed()+" iterations: "+mostRecentlyProcessedRandomIterations;
			FridayUtil.logToFile(MSG, FridayUtil.RUN_LOG);
			System.err.println(MSG);
			throw e;
		}
		
		return null;
	}
	
	/** Wrapper method to perform the action specified in the action response, on the given game state, and return the new
	 * resulting game state. 
	 *
	 * Most of the code in this method is around responding to the special case of destroying a STOP card in
	 * the SACRIFICE_LIFE_TO_DRAW phase, which according to the rules will take you back to drawing free cards.
	 * 
	 **/
	public static WinnableGameStateContainer doAction(ActionResponse r, final GameStateContainer gsParam)  {
		
		boolean containsStop = false;
		
		if(gsParam.isGameState()) {
			
			GameState gsProper = gsParam.getGameState();
			containsStop = gsProper.getLhsFightCards().findCardByAbility(Ability.STOP) != null 
					&& gsProper.getState() == State.SACRIFICE_LIFE_TO_DRAW;
		} else {
			
			PirateGameState gsPirate = gsParam.getPirateGameState();
			containsStop = gsPirate.getLhsFightCards().findCardByAbility(Ability.STOP) != null 
					&& gsPirate.getState() == PirateState.SACRIFICE_LIFE_TO_DRAW;			
		}
		
		WinnableGameStateContainer result = handleResponse(r, gsParam);
		
		if(result.isWin()) { return result; }
		
		// If we are in the SACRIFICE_LIFE_TO_DRAW, and we just destroyed/moved a STOP CARD, then go
		// back to DRAW_FREE_CARDS.
		if(result.getGsc().isGameState()) {
			
			GameState responseGs = result.getGsc().getGameState();
			
			if(responseGs.getState() == State.SACRIFICE_LIFE_TO_DRAW && containsStop) {
				boolean postContainsStop =  responseGs.getLhsFightCards().findCardByAbility(Ability.STOP) != null &&
						responseGs.getState() == State.SACRIFICE_LIFE_TO_DRAW;

				// Switch to draw free cards if the STOP card was destroyed/moved
				// (but only if there are still fighting cards left in fight/discard stack)
				if(!postContainsStop && responseGs.calculateCanDrawXFightingCards(1)) {
					
					GameState resultGs = new GameState(State.DRAW_FREE_CARDS, responseGs.getYourFightingCards(), responseGs.getHazardCards(), 
							responseGs.getDiscardHazards(), responseGs.getSlowGameState(), responseGs.getActiveHazardCard(), 
							responseGs.getDiscardFightCards(), responseGs.getLhsOrRhsFightingCardUsed(), responseGs.getLifePoints(), 
							responseGs.getLhsFightCards(), responseGs.getRhsFightCards(),  responseGs.getLhsOrRhsFightingCardDoubled(), 
							responseGs.getAbilityObject(), responseGs, null);
					
					result = new WinnableGameStateContainer(new GameStateContainer(resultGs));
					
				}
				
			}		
		} else {
			PirateGameState responseGs = result.getGsc().getPirateGameState();
			
			if(responseGs.getState() == PirateState.SACRIFICE_LIFE_TO_DRAW && containsStop) {
				boolean postContainsStop =  responseGs.getLhsFightCards().findCardByAbility(Ability.STOP) != null &&
						responseGs.getState() == PirateState.SACRIFICE_LIFE_TO_DRAW;

				// Switch to draw free cards if the STOP card was destroyed/moved
				// (but only if there are still fighting cards left in fight/discard stack)
				if(!postContainsStop && responseGs.calculateCanDrawXFightingCards(1)) {
					
					PirateGameState resultPgs = new PirateGameState(PirateState.DRAW_FREE_CARDS, 
							responseGs.getYourFightingCards(), responseGs.getSlowGameState(),
							responseGs.getActivePirateCard(), responseGs.getPirateCardInfo(), 
							responseGs.getDiscardFightCards(),  responseGs.getLhsOrRhsFightingCardUsed(), 
							responseGs.getLifePoints(), responseGs.getLhsFightCards(), 
							responseGs.getRhsFightCards(), responseGs.getLhsOrRhsFightingCardDoubled(),
							responseGs.getAbilityObject(), responseGs, null /* runtime object*/);
										
					result = new WinnableGameStateContainer(new GameStateContainer(resultPgs));
						
				}
				
			}		
			
		}
	
		return result;
	}
	
	
	/** Convert an Action object to its corresponding response. */
	private static ActionResponse convertActionToResponse(Action a) {	
		
		if(a.getType() == ActionType.DESTROY_CARDS_WITH_PAID_LIFE) {
			DestroyCardsWithPaidLifeActionResponse dcwplar = new DestroyCardsWithPaidLifeActionResponse(a);
			return dcwplar;
			
		} else if(a.getType() == ActionType.DRAW_A_FREE_CARD) {
			return DrawAFreeCardActionResponse.INSTANCE; 
			
		} else if(a.getType() == ActionType.END_DRAW_FREE_CARDS_PHASE) {
			return EndDrawFreeCardPhaseActionResponse.INSTANCE;
			
		} else if(a.getType() == ActionType.END_MULTISTAGE_ABILITY_ACTION) {
			return EndMultistageAbilityActionResponse.INSTANCE;
			
		} else if(a.getType() == ActionType.END_SACRIFICE_LIFE_PHASE) {
			return EndSacrificeLifePhaseActionResponse.INSTANCE;
			
		} else if(a.getType() == ActionType.FIGHT_OR_DISCARD_HAZARD_CARD) {
			
			if(((FightOrDiscardHazardCardAction)a).isFight()) {
				return FightOrDiscardHazardCardActionResponse.INSTANCE_FIGHT;
			} else {
				return FightOrDiscardHazardCardActionResponse.INSTANCE_DISCARD;
			}
			
		} else if(a.getType() == ActionType.SACRIFICE_LIFE_TO_DRAW) {
			return SacrificeLifeToDrawActionResponse.INSTANCE;
			
		} else if(a.getType() == ActionType.SELECT_FROM_TWO_HAZARD_CARDS) {

			if(((SelectFromTwoHazardCardsAction)a).getIndex() == 0) {
				return SelectFromTwoHazardCardsActionResponse.INSTANCE_ZERO;
			} else {
				return SelectFromTwoHazardCardsActionResponse.INSTANCE_ONE;
			}
		} else if(a.getType() == ActionType.USE_CARD_ABILITY) {
			return new UseCardAbilityActionResponse(a);
			
		} else if(a.getType() == ActionType.USE_CARD_ABILITY_SORT) {
			return new UseCardAbilitySortActionResponse(a);
			
		} else if(a.getType() == ActionType.USE_CARD_ABILITY_WITH_TARGET) {
			return new UseCardAbilityWithTargetActionResponse(a);
			
		} else if(a.getType() == ActionType.USE_COPY_ABILITY) {
			return new UseCopyAbilityActionResponse((UseCopyAbilityAction) a);
			
		} else if(a.getType() == ActionType.SELECT_A_PIRATE) {
			return new SelectAPirateActionResponse(a);
			
		} else if(a.getType() == ActionType.END_PIRATE_ROUND) {
			return EndPirateRoundActionResponse.INSTANCE;
			
		} else {
			FridayUtil.throwErr("Can't find: "+a.getType().name());
			return null;
		}
			
	}
	
	/** Wrapper to call either the non-pirate or pirate version of handleResponse(...). */
	private static WinnableGameStateContainer handleResponse(final ActionResponse ar, GameStateContainer gs) {

		if(gs.isGameState()) {
			return new WinnableGameStateContainer(handleResponseNonPirate(ar, gs.getGameState()));
		
		} else {
			return handleResponsePirate(ar, gs.getPirateGameState());
		}
		
	}
	
	/** Perform the action specified in the action response, on the given game state, and return the new
	 * resulting game state. */
	private static WinnableGameStateContainer handleResponsePirate(final ActionResponse ar, PirateGameState gs) {
		Action action = ar.getAction();
		ActionType type = action.getType();

		boolean isCopiedAbility = false;
		
		// In the copy case, unwrap the inner action and just process it as if it were a normal action,
		// but don't flag the action's card as used		
		if(type == ActionType.USE_COPY_ABILITY) {
			isCopiedAbility = true;
			action = ((UseCopyAbilityAction)action).getContainedAction();
			type = action.getType();
			gs = gs.sacrificeLifeToDraw_flagCopyAbilityAsUsed(((UseCopyAbilityAction)ar.getAction()));
		}
	
		if(gs.getState() == PirateState.SELECT_A_PIRATE) {
			
			SelectAPirateActionResponse sapar = (SelectAPirateActionResponse)ar;

			gs = gs.selectAPirate_selectPirate( ((SelectAPirateAction)sapar.getAction()).getCard());
			return new WinnableGameStateContainer(new GameStateContainer(gs));
			
		}
		
		if(gs.getState() == PirateState.DRAW_FREE_CARDS) {
			
			if(type == ActionType.DRAW_A_FREE_CARD) {
				return new WinnableGameStateContainer(new GameStateContainer(gs.drawFreeCards_drawACard()));
				
			} else if(type == ActionType.END_DRAW_FREE_CARDS_PHASE) {
				return new WinnableGameStateContainer(new GameStateContainer(gs.drawFreeCards_endDrawFreeCardsPhase()));
				
			} else {
				FridayUtil.throwErr("Unrecognized response "+ar);
			}
			
		}
		
		if(gs.getState() == PirateState.SACRIFICE_LIFE_TO_DRAW) {
		
			if(type == ActionType.SACRIFICE_LIFE_TO_DRAW) {
				return new WinnableGameStateContainer(new GameStateContainer(gs.sacrificeLifeToDraw_drawACard()));
				
			} else if(type == ActionType.END_PIRATE_ROUND) {
				PirateGameState pgs = gs.sacrificeLifeToDraw_endPirateRound();
				if(pgs == null) {
					WinnableGameStateContainer hr = new WinnableGameStateContainer(null);
					hr.setWin(true);
					return hr;
				}
				return new WinnableGameStateContainer(new GameStateContainer(pgs));
				
			} else if(type == ActionType.END_SACRIFICE_LIFE_PHASE) {
				FridayUtil.throwErr("Not implemented.");
				
			} else if(type == ActionType.USE_CARD_ABILITY) {
				return new WinnableGameStateContainer(new GameStateContainer(gs.sacrificeLifeToDraw_useCardAbility((UseCardAbilityAction) action, isCopiedAbility)));
			
			}  else if(type == ActionType.USE_CARD_ABILITY_WITH_TARGET) {
				return new WinnableGameStateContainer(new GameStateContainer(gs.sacrificeLifeToDraw_useCardAbilityWithTarget(
						(UseCardAbilityWithTargetAction) action, isCopiedAbility)));
				
			} else if(type == ActionType.END_MULTISTAGE_ABILITY_ACTION) {
				return new WinnableGameStateContainer(new GameStateContainer(gs.sacrificeLifeToDraw_endMultistageAbilityResponse(ar)));

			} else if(type == ActionType.USE_CARD_ABILITY_SORT) {
				return new WinnableGameStateContainer(new GameStateContainer(gs.sacrificeLifeToDraw_useCaseAbilitySort(ar)));
			} else {
				FridayUtil.throwErr("Unrecognized response "+ar);
			}
		}
		
		FridayUtil.throwErr("Unhandled reponse");
		return null;
	}
	
	/** Perform the action specified in the action response, on the given game state, and return the new
	 * resulting game state. */
	private static GameStateContainer handleResponseNonPirate(final ActionResponse ar, GameState gs) {
		
		Action action = ar.getAction();
		ActionType type = action.getType();

		boolean isCopiedAbility = false;
		
		// In the copy case, unwrap the inner action and just process it as if it were a normal action,
		// but don't flag the action's card as used		
		if(type == ActionType.USE_COPY_ABILITY) {
			isCopiedAbility = true;
			action = ((UseCopyAbilityAction)action).getContainedAction();
			type = action.getType();
			gs = gs.sacrificeLifeToDraw_flagCopyAbilityAsUsed(((UseCopyAbilityAction)ar.getAction()));
		}
		
		if(gs.getState() == State.SELECT_A_HAZARD_CARD) {
			
			if(type == ActionType.SELECT_FROM_TWO_HAZARD_CARDS) {

				SelectFromTwoHazardCardsActionResponse sr = (SelectFromTwoHazardCardsActionResponse) ar;
				return new GameStateContainer(gs.selectAHazardCard_selectFromTwoHazardCards(((SelectFromTwoHazardCardsAction)sr.getAction()).getIndex()));
				
				
			} else if(type == ActionType.FIGHT_OR_DISCARD_HAZARD_CARD) {
				
				FightOrDiscardHazardCardActionResponse fr = (FightOrDiscardHazardCardActionResponse) ar;
				return gs.selectAHazardCard_selectFromOneHazardCard( ((FightOrDiscardHazardCardAction)fr.getAction()).isFight()  );
			} else {
				FridayUtil.throwErr("Unrecognized response: "+type);
			}
			
		} else if(gs.getState() == State.DRAW_FREE_CARDS) {
			
			if(type == ActionType.DRAW_A_FREE_CARD) {
				
				return new GameStateContainer(gs.drawFreeCards_drawACard());
				
			} else if(type == ActionType.END_DRAW_FREE_CARDS_PHASE) {
				return new GameStateContainer(gs.drawFreeCards_endDrawFreeCardsPhase());
				
			} else {
				FridayUtil.throwErr("Unrecognized response "+ar);
			}
			
		} else if(gs.getState() == State.SACRIFICE_LIFE_TO_DRAW) {
			
			if(type == ActionType.SACRIFICE_LIFE_TO_DRAW) {
				return new GameStateContainer(gs.sacrificeLifeToDraw_drawACard());
				
			} else if(type == ActionType.END_SACRIFICE_LIFE_PHASE) {
				return gs.sacrificeLifeToDraw_endSacrificePhase();
				
			} else if(type == ActionType.USE_CARD_ABILITY) {
				return new GameStateContainer(gs.sacrificeLifeToDraw_useCardAbility((UseCardAbilityAction) action, isCopiedAbility));
			} else if(type == ActionType.USE_CARD_ABILITY_WITH_TARGET) {
				return new GameStateContainer(gs.sacrificeLifeToDraw_useCardAbilityWithTarget(
						(UseCardAbilityWithTargetAction) action, isCopiedAbility));
				
			} else if(type == ActionType.END_MULTISTAGE_ABILITY_ACTION) {
				return new GameStateContainer(gs.sacrificeLifeToDraw_endMultistageAbilityResponse(ar));
			} else if(type == ActionType.USE_CARD_ABILITY_SORT) {
				return new GameStateContainer(gs.sacrificeLifeToDraw_useCaseAbilitySort(ar));
			} else {
				FridayUtil.throwErr("Unrecognized response "+ar);
			}
			
		} else if(gs.getState() == State.PAY_LIFE_POINTS_ON_HAZARD_MISS) {
			
			DestroyCardsWithPaidLifeAction dca = (DestroyCardsWithPaidLifeAction) action;
			
			return gs.payLifePointsOnHazardMiss_destroyCards(dca.getCardsToDestroy());
		}
		
		
		FridayUtil.throwErr("Unhandled reponse");
		return null;
	}
		

	/** Wrapper around non-pirate and pirate calculateAvailableActions(...) */
	private static List<Action> calculateAvailableActions(GameStateContainer container) {
		
		if(container.isGameState()) {
			return calculateAvailableActionsNonPirate(container.getGameState());
			
		} else {
			return calculateAvailableActionsPirate(container.getPirateGameState());
		}
		
	}
	
	/** Look at what the player is able to do in the current pirate game state, returning a list of
	 * potential actions. */
	private static List<Action> calculateAvailableActionsPirate(PirateGameState gs) {
		if(gs.getLifePoints() < 0) {
			return Collections.emptyList();
		}

		List<Action> availableActions = new ArrayList<>();

		PirateState curr = gs.getState();
		
		if(curr == PirateState.SELECT_A_PIRATE) {
			
			PirateCard[] activePirates = gs.getActivePirates();
			for(int x = 0; x < activePirates.length; x++) {
				availableActions.add(new SelectAPirateAction(activePirates[x]));
			}
			
		} else if(curr == PirateState.DRAW_FREE_CARDS) {
			
			ImmutableGrowableListCards lhsList = gs.getLhsFightCards();
			Card stopCard = lhsList.findCardByAbility(Ability.STOP);

			// Can we draw more cards? Look at how many cards we have already drawn
			int cardsDrawn = gs.getLhsFightCards().size();
			if((cardsDrawn < gs.getPirateCardInfo().getFreeFightingCards() || cardsDrawn == 0) && stopCard == null) {
				DrawAFreeCardAction action = DrawAFreeCardAction.INSTANCE;
				availableActions.add(action);
			} 
						
			// Or we can just end the phase (but only if at least one card was drawn, as per rules)
			if(cardsDrawn > 0) {
				availableActions.add(EndDrawFreeCardPhaseAction.INSTANCE);
			}

			
		} else if(curr == PirateState.SACRIFICE_LIFE_TO_DRAW) {
						
			// If the user is inside a multistage ability, then don't allow them to exit the phase or sacrifice life
			if(gs.getAbilityObject() == null) {
			
				if(gs.getLifePoints() > 0 && (gs.getYourFightingCards().size() > 0 || gs.getDiscardFightCards().size() > 0) ) {
					SacrificeLifeToDrawAction sacrificeLife = SacrificeLifeToDrawAction.INSTANCE;
					availableActions.add(sacrificeLife);
				}
				
				// As soon as the hazard value drops to 0, ending the round is the only option
				if(gs.calculateRemainingHazardValue() <= 0 ) {
					availableActions.clear();
					availableActions.add(new EndPirateRoundAction());
					return availableActions;
				}
			}
			
			// Generate card abilities
			{
				List<Card> cardsOnBothSides = new ArrayList<>();
				cardsOnBothSides.addAll(gs.getLhsFightCards().getAsList());
				cardsOnBothSides.addAll(gs.getRhsFightCards().getAsList());

				// If we are in a multistage ability, then handle that first 
				if(gs.getAbilityObject() != null) {
					
					Card c = gs.getAbilityObject().getActiveCard();
					Ability a = c.getAbility();
					
					if(a == Ability.CARDS_DRAW_2) {
						if(gs.calculateCanDrawXFightingCards(1)) {
							availableActions.add(new UseCardAbilityAction(c));
						}
						availableActions.add(EndMultistageAbilityAction.INSTANCE);
					}
					
					if(a == Ability.EXCHANGE_X2) {
						if(gs.calculateCanDrawXFightingCards(1)) {
							for(Card d : cardsOnBothSides) {								
								if(d.getPhysicalCardId() == c.getPhysicalCardId() ) { continue; }
								availableActions.add(new UseCardAbilityWithTargetAction(c, d)); 
							}
						}
						availableActions.add(EndMultistageAbilityAction.INSTANCE);
					}
					
					if(a == Ability.SORT_3_CARDS) {
						AbilityObject ao = gs.getAbilityObject();
						if(ao.getStage() == 0 && ao.getNumberOfCardsDrawn()<=2) {
							
							if(gs.calculateCanDrawXFightingCards(1)) {
								availableActions.add(new UseCardAbilityAction(c)); 
							}
							availableActions.add(EndMultistageAbilityAction.INSTANCE);
						
						} else if(ao.getStage() == 1) {
							List<Card> cards = ao.getDrawnSortCards();
							
							if(cards.size() == 1) {
								
								availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(0)}, null));
								availableActions.add(new UseCardAbilitySortAction(new Card[] { }, cards.get(0)));
								
							} else if(cards.size() == 2) {

								// no discard
								availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(0), cards.get(1)}, null));
								availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(1), cards.get(0)}, null));									

								// discard
								availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(0)},  cards.get(1)));
								availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(1)}, cards.get(0)));									
							
							} else if(cards.size() == 3) {
								
								for(int[] perm : ComboUtil.PERMUTATIONS_OF_3) {

									// Non-discard case
									availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(perm[0]), cards.get(perm[1]), cards.get(perm[2])} , null));
									
									// Discard case
									availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(perm[0]), cards.get(perm[1])} , cards.get(perm[2])));
								}
								
							} else {
								FridayUtil.throwErr("Too many cards drawn - this shouldn't happen.");
							}
							
							
						} else {
							FridayUtil.throwErr("Stage value or cards drawn is invalid - this shouldn't happen. "+ao.getStage()+" "+ao.getNumberOfCardsDrawn());
						}
					}
					
				} else { // End multistage ability if
					
					availableActions.addAll(calculateIndividualActions(cardsOnBothSides, false, false, null, gs));
					
				} 
			}
			
		} else {
			FridayUtil.throwErr("Unrecognized state: "+curr);
		}
		
		return availableActions;
		
	}
	
	/** Look at what the player is able to do in the current non-pirate game state, returning a list of
	 * potential actions. */
	private static List<Action> calculateAvailableActionsNonPirate(GameState gs) {
		if(gs.getLifePoints() < 0) {
			return Collections.emptyList();
		}
		
		List<Action> availableActions = new ArrayList<>();
		
		State curr = gs.getState();
		
		if(curr == State.SELECT_A_HAZARD_CARD) {
			
			if(gs.getHazardCards().size() == 0) { FridayUtil.throwErr("Zero hazard cards. "); };
			
			if(gs.getHazardCards().size() >= 2) {
				// 1. Draw two hazard cards.
				availableActions.add(SelectFromTwoHazardCardsAction.INSTANCE_ZERO);
				availableActions.add(SelectFromTwoHazardCardsAction.INSTANCE_ONE);
				
			} else if(gs.getHazardCards().size() == 1) {
				// 2. Fight or discard
				availableActions.add(FightOrDiscardHazardCardAction.INSTANCE_FIGHT);
				availableActions.add(FightOrDiscardHazardCardAction.INSTANCE_DISCARD);
			}
			
		} else if(curr == State.DRAW_FREE_CARDS) {

			// Look at what the active hazard is
			Card activeHazard = gs.getActiveHazardCard();
			
			if(activeHazard == null) { FridayUtil.throwErr("Hazard card is null"); }

			ImmutableGrowableListCards lhsList = gs.getLhsFightCards();
			Card stopCard = lhsList.findCardByAbility(Ability.STOP);

			// Can we draw more cards? Look at how many cards we have already drawn
			int cardsDrawn = gs.getLhsFightCards().size();
			if((cardsDrawn < activeHazard.getFreeCards() || cardsDrawn == 0) && stopCard == null) {
				DrawAFreeCardAction action = DrawAFreeCardAction.INSTANCE;
				availableActions.add(action);
			} 
						
			// Or we can just end the phase (but only if at least one card was drawn)
			if(cardsDrawn > 0) {
				availableActions.add(EndDrawFreeCardPhaseAction.INSTANCE);
			}
			
		} else if(curr == State.SACRIFICE_LIFE_TO_DRAW) {

			// If the user is inside a multistage ability, then don't allow them to exit the phase or sacrifice life
			if(gs.getAbilityObject() == null) {
			
				if(gs.getLifePoints() > 0 && (gs.getYourFightingCards().size() > 0 || gs.getDiscardFightCards().size() > 0) ) {
					SacrificeLifeToDrawAction sacrificeLife = SacrificeLifeToDrawAction.INSTANCE;
					availableActions.add(sacrificeLife);
				}
				
				EndSacrificeLifePhaseAction endPhase = EndSacrificeLifePhaseAction.INSTANCE;
				availableActions.add(endPhase);
			}
			
			// Generate card abilities
			{
				List<Card> cardsOnBothSides = new ArrayList<>();
				cardsOnBothSides.addAll(gs.getLhsFightCards().getAsList());
				cardsOnBothSides.addAll(gs.getRhsFightCards().getAsList());

				// If we are in a multistage ability, then handle that first 
				if(gs.getAbilityObject() != null) {
					
					Card c = gs.getAbilityObject().getActiveCard();
					Ability a = c.getAbility();
					
					if(a == Ability.CARDS_DRAW_2) {
						if(gs.calculateCanDrawXFightingCards(1)) {
							availableActions.add(new UseCardAbilityAction(c));
						}
						availableActions.add(EndMultistageAbilityAction.INSTANCE);
					}
					
					if(a == Ability.EXCHANGE_X2) {
						if(gs.calculateCanDrawXFightingCards(1)) {
							for(Card d : cardsOnBothSides) {								
								if(d.getPhysicalCardId() == c.getPhysicalCardId() ) { continue; }
								availableActions.add(new UseCardAbilityWithTargetAction(c, d));
							}
						}
						availableActions.add(EndMultistageAbilityAction.INSTANCE);
					}
					
					if(a == Ability.SORT_3_CARDS) {
						AbilityObject ao = gs.getAbilityObject();
						if(ao.getStage() == 0 && ao.getNumberOfCardsDrawn()<=2) {
							if(gs.calculateCanDrawXFightingCards(1)) {
								availableActions.add(new UseCardAbilityAction(c)); 
							}
							availableActions.add(EndMultistageAbilityAction.INSTANCE);
						
						} else if(ao.getStage() == 1) {
							List<Card> cards = ao.getDrawnSortCards();
							
							if(cards.size() == 1) {
								
								availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(0)}, null));
								availableActions.add(new UseCardAbilitySortAction(new Card[] { }, cards.get(0)));
								
							} else if(cards.size() == 2) {

								// no discard
								availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(0), cards.get(1)}, null));
								availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(1), cards.get(0)}, null));									

								// discard
								availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(0)},  cards.get(1)));
								availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(1)}, cards.get(0)));									
							
							} else if(cards.size() == 3) {
								
								for(int[] perm : ComboUtil.PERMUTATIONS_OF_3) {

									// Non-discard case
									availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(perm[0]), cards.get(perm[1]), cards.get(perm[2])} , null));
									
									// Discard case
									availableActions.add(new UseCardAbilitySortAction(new Card[] { cards.get(perm[0]), cards.get(perm[1])} , cards.get(perm[2])));
								}
								
							} else {
								FridayUtil.throwErr("Too many cards drawn - this shouldn't happen.");
							}
							
							
						} else {
							FridayUtil.throwErr("Stage value or cards drawn is invalid - this shouldn't happen. "+ao.getStage()+" "+ao.getNumberOfCardsDrawn());
						}
					}
					
				} else { // End multistage ability if
					
					availableActions.addAll(calculateIndividualActions(cardsOnBothSides, false, false, gs, null));
					
				} 
			}
		
		} else if(curr == State.PAY_LIFE_POINTS_ON_HAZARD_MISS) {

			final int lifeLost = gs.calculateRemainingHazardValue();

			List<Card> availableToBeDestroyed = new ArrayList<Card>();
			{
				// Add the lhs and rhs to the list; these are card that are available to be destroyed.				
				availableToBeDestroyed.addAll(gs.getLhsFightCards().getAsList());
				availableToBeDestroyed.addAll(gs.getRhsFightCards().getAsList());

				// Sort by worst to best, ascending
				FridayUtil.sortFightingCardsWorstToBest(availableToBeDestroyed);

				// Remove all the cards, except the ones we plan to destroy
				int currLifeLost = lifeLost;
				for(Iterator<Card> it = availableToBeDestroyed.iterator(); it.hasNext();) {
					Card currCard = it.next();
					
					if(currLifeLost <= 0) {
						it.remove();
					} else {
						// Only destroy a card if we have the life paid to do so
						int costToDestroy = currCard.getType() == Type.AGING ? 2 : 1;						
						if(currLifeLost >= costToDestroy) {
							currLifeLost -= costToDestroy;
						} else {
							it.remove();
						}
					}
					
				}
				
				// Destroy at most MAX_COMBO_COUNT cards 
				if(availableToBeDestroyed.size() > ComboUtil.MAX_COMBO_COUNT) {
					for(int index = availableToBeDestroyed.size()-1; index >= ComboUtil.MAX_COMBO_COUNT; index--) {
						availableToBeDestroyed.remove(index);
					}					
				}
			}
			
			// Add every combination of cards to destroy
			if(availableToBeDestroyed.size() > 0){
				List<List<Card>> cardsToDestroy = new ArrayList<>();			
				int[][] combos = ComboUtil.COMBOS[availableToBeDestroyed.size()]; // TODO: LOWER - This seems like a LOT of data to plow through
				
				for(int x = 0; x < combos.length; x++) {
					
					List<Card> cardCombo = new ArrayList<>();
					
					int[] combo = combos[x];
					
					for(int y = 0; y < combo.length; y++) {
						
						int pos = combo[y];
						cardCombo.add(availableToBeDestroyed.get(pos));
					}
					cardsToDestroy.add(cardCombo);
				}
				
				boolean[ /* indices of a cardToDestroy is true if they should be removed*/] removeCardAtPos = new boolean[cardsToDestroy.size()];
				for(int x = 0; x < removeCardAtPos.length; x++) { removeCardAtPos[x] = false; }
				
				for(int x = 0; x < cardsToDestroy.size(); x++) {
					List<Card> xList = cardsToDestroy.get(x);
					
					for(int y = x+1; y < cardsToDestroy.size(); y++) {
						List<Card> yList = cardsToDestroy.get(y);
						
						if(FridayUtil.areCardListsEqualByTrait(xList, yList)) {
							removeCardAtPos[y] = true;
						}
					}
				}
				
				// Remove any combinations that we have flagged as dupes
				for(int x = removeCardAtPos.length-1; x >= 0; x--) {
					if(removeCardAtPos[x]) {
						cardsToDestroy.remove(x);
					}
				}
				
				
				for(List<Card> ctd : cardsToDestroy) {
					DestroyCardsWithPaidLifeAction newAction = new DestroyCardsWithPaidLifeAction(ctd.toArray(new Card[ctd.size()]));
					availableActions.add(newAction);
				}
			} else {
				// We haven't paid enough to destroy any cards
				DestroyCardsWithPaidLifeAction newAction = new DestroyCardsWithPaidLifeAction(new Card[0]);
				availableActions.add(newAction);
			}
			
			// Destroy card
			// end phase
		
		} else {
			FridayUtil.throwErr("Unrecognized state: "+curr);
		}
		
		return availableActions;
		
	}
	
	/** Called by pirate and non-pirate version of calculateAvailableActions, for action logic that is shared
	 * between the two. Looks at a subset of the game state, and returns valid actions based on that.*/
	private static List<Action> calculateIndividualActions(List<Card> cardsOnBothSides, boolean ignoreAbilitiesUsed, 
			boolean calculateForCopyAction, GameState gsNew, PirateGameState pgsNew) {
		
		List<Action> availableActions = new ArrayList<>();
		
		// If we are not in a multistage ability, then process the cards as usual

		HashMap<Integer /* physical id*/, Boolean /* included in btp, true = yes, null = no */> belowThePileMap = new HashMap<>();	
		HashMap<Integer /* physical id*/, Boolean /* included in destroy, true = yes, null = no*/> destroyMap = new HashMap<>();
		HashMap<Integer /* physical id*/, Boolean /* included in exchange, true = yes, null = no */> exchangeX1Map = new HashMap<>();
		HashMap<Integer /* physical id*/, Boolean /* included in exchange, true = yes, null = no */> exchangeX2Map = new HashMap<>();
		HashMap<Integer /* physical id*/, Boolean /* included in exchange, true = yes, null = no */> doubleMap = new HashMap<>();
		
		for(Card c : cardsOnBothSides) {
			Ability a = c.getAbility();
			
			// Don't activate aging cards or empty abilities
			if(a == null || a == Ability.DOT_DOT_DOT || c.getDifficulty() != Difficulty.NONE ) { continue; }

			// Don't active used cards
			if( !ignoreAbilitiesUsed) {
				boolean isFcAbilityUsed = false;
				
				if(gsNew != null) {
					isFcAbilityUsed = gsNew.isFightingCardAbilityUsed(c);
				} else {
					isFcAbilityUsed = pgsNew.isFightingCardAbilityUsed(c);
				}
				
				if(isFcAbilityUsed) {
					continue;
				}				
			}
			
			if(a == Ability.LIFE_ADD_1 || a == Ability.LIFE_ADD_2 || a == Ability.CARDS_DRAW_1) {
				
				if(a != Ability.CARDS_DRAW_1) {
					availableActions.add(new UseCardAbilityAction(c));

				} else { // Ability.CARDS_DRAW_1
					if(canDrawXFightingCards(1, gsNew, pgsNew)) {
						availableActions.add(new UseCardAbilityAction(c));
					}
				}
				
			}

			if(calculateForCopyAction && a == Ability.PHASE_MINUS_1 && pgsNew == null) {
				// Phase-1 only available when copying, otherwise it is a passive ability
				// Likewise it is not available in pirate rounds
				availableActions.add(new UseCardAbilityAction(c));
			}
			
			if(a == Ability.COPY_1x && !calculateForCopyAction ) {
				
				List<Action> allAvailableActions = calculateIndividualActions(cardsOnBothSides, true, true, gsNew, pgsNew);
				
				for(Action actionToCopy :  allAvailableActions) {
					
					availableActions.add(new UseCopyAbilityAction(c, actionToCopy));
				}
				
			}
			
			if(a == Ability.DESTROY_1x) {
				// Thinking: you are only ever trying to accomplish the following with card removal:
				// - nuke an aging card (strategy: find the worst aging card)
				// - nuke a below average ability (strategy: find 1 or more cards with below avg abilities)
				// - nuke a below average fighting card value (strategy: find the worst fighting card value)
				// - nuke a weighted combination between the above two :P
				for(Card d : cardsOnBothSides) {
					
					// Can't destroy itself
					if(d.getPhysicalCardId() == c.getPhysicalCardId()) { continue; }
					
					boolean alreadyTargetedForDestruction = destroyMap.get(d.getPhysicalCardId()) != null ? true : false;
					
					if(!alreadyTargetedForDestruction) {
						availableActions.add(new UseCardAbilityWithTargetAction(c, d));
						destroyMap.put(d.getPhysicalCardId(), true);
					}
				}
			}
			
			if(a == Ability.DOUBLE_1x) {
				
				// Find the card with the highest fighting value, no point in doubling anything else
				Card highestFightingValue = null;
				for(Card d : cardsOnBothSides) {

					// Can't double yourself, as per rules, and can't double twice on same card
					if(gsNew != null) {
						if(d.getPhysicalCardId() == c.getPhysicalCardId() || gsNew.isFightingCardDoubled(d)) { continue; } 						
					} else {
						if(d.getPhysicalCardId() == c.getPhysicalCardId() || pgsNew.isFightingCardDoubled(d)) { continue; }
					}
					
					
					if(highestFightingValue == null) { highestFightingValue = d; }
					else if(d.getFightingValue() > highestFightingValue.getFightingValue()) {
						highestFightingValue = d;
					}							
				}
				
				// If we found it, and it's > 0
				if(highestFightingValue != null && highestFightingValue.getFightingValue() > 0) {
					boolean alreadyTargetedForDouble = doubleMap.get(highestFightingValue.getPhysicalCardId()) != null ? true : false;
					if(!alreadyTargetedForDouble) {
						availableActions.add(new UseCardAbilityWithTargetAction(c, highestFightingValue));
						doubleMap.put(highestFightingValue.getPhysicalCardId(), true);
					}
				}
			}
								
			if(a == Ability.CARDS_DRAW_2) {
				if(canDrawXFightingCards(1, gsNew, pgsNew)) {
					availableActions.add(new UseCardAbilityAction(c));
				}
			}
			
			if(a == Ability.BELOW_THE_PILE_1x) {
				
				// This map is only populated if there are no more fighting cards that may be drawn
				HashMap<Integer /*physical card id*/, Boolean> cardsOnLhs = null; 
				if(!canDrawXFightingCards(1, gsNew, pgsNew) ) {
					cardsOnLhs = new HashMap<>();
					ImmutableGrowableListCards lhsCards;
					if(gsNew != null) {
						lhsCards = gsNew.getLhsFightCards();
					} else {
						lhsCards = pgsNew.getLhsFightCards();
					}
					for(int x = 0; x < lhsCards.size(); x++) {
						cardsOnLhs.put( lhsCards.get(x).getPhysicalCardId(), true);
					}
				}
				
				for(Card d : cardsOnBothSides) {
					// Can't below the pile yourself, as per rules
					if(d.getPhysicalCardId() == c.getPhysicalCardId() ) { continue; } 
					
					boolean alreadyTargetedForBtp = belowThePileMap.get(d.getPhysicalCardId()) != null ? true : false;
					
					if(!alreadyTargetedForBtp) {
						// If no more cards can be drawn, then we can't BTP from the LHS
						if(cardsOnLhs == null || cardsOnLhs.get(d.getPhysicalCardId()) == null) {
							availableActions.add(new UseCardAbilityWithTargetAction(c, d));
						}
						
						belowThePileMap.put(d.getPhysicalCardId(), true); // We mark it true here, so that we don't do the above calculations again
					}
					
				}
				
			}
			
			if(a == Ability.SORT_3_CARDS) {
				
				if(canDrawXFightingCards(1, gsNew, pgsNew)) {
					availableActions.add(new UseCardAbilityAction(c));
				}			
			}

			if(a == Ability.EXCHANGE_X1) {
				if(canDrawXFightingCards(1, gsNew, pgsNew)) {
					for(Card d : cardsOnBothSides) {
						// Can't exchange yourself, as per rules
						if(d.getPhysicalCardId() == c.getPhysicalCardId() ) { continue; }
						
						boolean alreadyTargetedForExchanged = exchangeX1Map.get(d.getPhysicalCardId()) != null ? true : false;					
						
						if(!alreadyTargetedForExchanged) {
							availableActions.add(new UseCardAbilityWithTargetAction(c, d));
							exchangeX1Map.put(d.getPhysicalCardId(), true);
						}
					}
				}
			}

			if(a == Ability.EXCHANGE_X2) {
				if(canDrawXFightingCards(1, gsNew, pgsNew)) {
					for(Card d : cardsOnBothSides) {
						// Can't exchange yourself, as per rules
						if(d.getPhysicalCardId() == c.getPhysicalCardId() ) { continue; }
	
						boolean alreadyTargetedForExchanged = exchangeX2Map.get(d.getPhysicalCardId()) != null ? true : false;
	
						if(!alreadyTargetedForExchanged) {
							availableActions.add(new UseCardAbilityWithTargetAction(c, d));
							exchangeX2Map.put(d.getPhysicalCardId(), true);
						}
					}
				}
			}
		}
		
		return availableActions;
	}
	
	/** Wrapper method for non-pirate and pirate game states; are we currently able to draw the requested
	 * number of fighting cards from the fighting card stack. Takes either a game state or a pirate 
	 * game state, but not both. */
	private static boolean canDrawXFightingCards(int xFightingCards, GameState gs, PirateGameState pgs) { 
		boolean result = false;
		if(gs != null) {
			if(gs.calculateCanDrawXFightingCards(xFightingCards)) {
				result = true; 
			}
			
		} else {
			if(pgs.calculateCanDrawXFightingCards(xFightingCards)) {
				result = true; 
			}
		}
	
		return result;
	}
	
	
	/** Create an initial game state from the beginning of a new game. */
	private static GameState initializeGameState(AllCards cr) {

		// Fighting cards
		ListCards fightingCards;
		{
			List<Card> cards = new ArrayList<>();
			cards.addAll(cr.getFightingCards());
			FridayUtil.shuffleCollection(cards);
			fightingCards = new ListCards(cards.toArray(new Card[cards.size()]), 0);
		}

		// Hazard cards
		ListCards hazardCards;
		{
			
			List<Card> cards = new ArrayList<>();
			cards.addAll(cr.getHazardCards());
			FridayUtil.shuffleCollection(cards);
			
			hazardCards = new ListCards(cards.toArray(new Card[cards.size()]), 0);
		}


		// Aging cards
		ListCards agingCardsLc;
		{
			List<Card> normalAgingCards = cr.getAgingCards().stream().filter(e -> e.getDifficulty() == Difficulty.NORMAL
					&& e.getFightingValue() != -3).collect(Collectors.toList());
			
			List<Card> difficultAgingCards = cr.getAgingCards().stream().filter(e -> e.getDifficulty() == Difficulty.DIFFICULT).collect(Collectors.toList());

			if(normalAgingCards.size() != 7) { FridayUtil.throwErr("Invalid number of aging cards"); } 
			if(difficultAgingCards.size() != 3) { FridayUtil.throwErr("Invalid number of aging cards"); }
			
			FridayUtil.shuffleCollection(normalAgingCards);
			
			FridayUtil.shuffleCollection(difficultAgingCards);

			// Merge the normal and difficult cards into a single ListCards
			{
				List<Card> agingCardsList = new ArrayList<>();
				
				agingCardsList.addAll(normalAgingCards);
				agingCardsList.addAll(difficultAgingCards);
				Card[] agingCardArray = agingCardsList.toArray( new Card[agingCardsList.size()] );
				agingCardsLc = new ListCards(agingCardArray, 0);
			}
		}
		
		// Active Pirates
		PirateCard[] pirateCards;
		{
			List<PirateCard> pcList = new ArrayList<>();
			pcList.addAll(cr.getPirateCards());
			
			FridayUtil.shuffleCollection(pcList);
			while(pcList.size() != 2) {
				pcList.remove(0);
			}
			pirateCards = pcList.toArray(new PirateCard[pcList.size()] );
		}

		SlowGameState sgs = new SlowGameState(agingCardsLc, 1 /* game level */, pirateCards, null, 2 /*phase # */, null /* wild card pirate info */);
		
		GameState gs = new GameState(State.SELECT_A_HAZARD_CARD, fightingCards, hazardCards, 
				GameState.defaultDiscardHazards(), sgs, null, 
				GameState.defaultFightingDiscard(), null, 120 /*life*/, 
				GameState.defaultLhsFightCards(), GameState.defaultRhsFightCards(), null, null, 
				null /* prev game state*/, null /* nro */);
		
		return gs;
		
	}
	
	/** It possible to exploit the game rules to get into an infinite loop of game actions.
	 * For example: one can use a COPY action, two BELOW_THE_PILE actions, and a near empty fighting
	 * card stack, to continously cycle the deck without drawing any aging cards.
	 * 
	 * To avoid this, we detect when this occurs (by noticing that the current duplicate state
	 * is the same as a previous state), and look for the an action that will break the cycle. 
	 */
	private static void detectAndAmeliorateCycle(List<GameStateContainer> previousGameStates, GameStateContainer currState, List<Action> availableActions) {
		boolean dupeCycleDetected = false;
		for(int x = 0; x < previousGameStates.size()-1 /* ignore the last in the list*/; x++) { 
			GameStateContainer lastState = previousGameStates.get(x);
			
			if(FridayUtil.compareGameState(lastState, currState)) {							
				dupeCycleDetected = true;
			}
		}

		if(dupeCycleDetected) {
			System.out.println("Dupe detected.");
			if(currState.getGameState().getState() == State.SACRIFICE_LIFE_TO_DRAW) {

				// Look for an action that will end the phase, if it exists;
				Action exitAction = null;
				for(Action curr : availableActions) {
					if(curr.getType() == ActionType.END_SACRIFICE_LIFE_PHASE) {
						exitAction = curr;
						break;
					}
				}
				
				// If found, remove all other actions.
				if(exitAction != null) {
					availableActions.clear();
					availableActions.add(exitAction);
				}
			}  
			
		}
	}
	
	/** Debug only utility method to verify that converting a game state to JSON, and then back to
	 * a non-JSON game state, does not materially affect the contents of the game state. */
	@SuppressWarnings("unused")
	private static void debugVerifyJsonPersistenceCorrectness(GameStateContainer currGs) {
		if(!currGs.isGameState()) {
			
			// PirateGameState.validate(curr.gs.getPirateGameState(), null, null);
			
			JsonGameStatePersistence jgsp = JsonPersistenceUtil.toJson(currGs.getPirateGameState());
			
			PirateGameState newPgs = JsonPersistenceUtil.fromJson(jgsp).getPirateGameState();
			
			if(!FridayUtil.compareGameState(currGs.getPirateGameState(), newPgs)) {
				
				FridayUtil.throwErr("Does not match!");
			}
			
		}
		
		if(currGs.isGameState()) {
			
			// GameState.validate(curr.gs.getGameState(), null, null);
			JsonGameStatePersistence jgsp = JsonPersistenceUtil.toJson(currGs.getGameState());
			GameState newGs = JsonPersistenceUtil.fromJson(jgsp).getGameState();
			
			if(!FridayUtil.compareGameState(currGs.getGameState(), newGs)) {
			
				FridayUtil.throwErr("Game states don't match.");
			}

		}		
	}
	
	/** This class runs the game loop with a predefined pseudorandom seed (and given number of "uses" of the seed)
	 * in order to create a deterministic game environment which makes it easy to reproduce previous game
	 * behaviour during debugging. */
	public static class StateRunnerThread extends Thread {
		
		private final long initialSeed;
		private final int seedIncrement;
		private final String threadName;
		
		public StateRunnerThread(long initialSeed, int seedIncrement, String threadName) {
			this.initialSeed = initialSeed;
			this.seedIncrement = seedIncrement;
			this.threadName = threadName;
			this.setName(StateRunnerThread.class.getSimpleName()+" - increment: "+seedIncrement+" initial seed: "+initialSeed);
			this.setDaemon(false);
		}
		
		@Override
		public void run() {
			final SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss a");
			
			GameStateContainer gs;
			
			long randomSeed = initialSeed;
			
			while(true) {

				try {

					FridayUtil.logToFile("Starting on random seed "+randomSeed+" ["+seedIncrement+"]", FridayUtil.RUN_LOG);
					
					FridayUtil.initializeRandomSeed(randomSeed, 0);
				 
					gs = new GameStateContainer(initializeGameState(FridayUtil.ALL_CARDS));
			
					DevMainResult result = Main.devMain(gs, false);
					
					int phaseScore;
							
					if(result.isWon()) { 
						phaseScore = -2; 
					} else {
						phaseScore = GameStateContainerUtil.getPhase(result.getFinalGameState());
					}
					
					FridayUtil.logToFile(randomSeed
							+ "," + sdf.format(new Date())
							+ "," + GameStateContainerUtil.getLifePoints(result.getFinalGameState()) 
							+ "," + phaseScore,
							FridayUtil.RESULT_LOG);
					
					FridayUtil.logToFile("Passed on random seed "+randomSeed+" ["+seedIncrement+"]", FridayUtil.RUN_LOG);
				} catch(Throwable e) {
					try {
						FridayUtil.logToFile("Failed on random seed "+randomSeed+", no more actions: "+FridayUtil.convertExceptionToString(e), FridayUtil.RUN_LOG);
					} catch (IOException e2) { e2.printStackTrace(); try { Thread.sleep(10*1000); } catch (InterruptedException e1) { e1.printStackTrace(); } System.exit(0); }
					
					System.err.println("Failed on random seed "+randomSeed);
					e.printStackTrace();
					try { Thread.sleep(5000); } catch (InterruptedException e1) { e1.printStackTrace(); }
					System.exit(0);
					throw new RuntimeException(e);
				}
				
				randomSeed += seedIncrement;
				System.out.println(threadName+" now on random seed "+randomSeed);
			}

		}
		
	}
	
	/** Returned at the end of a game by devMain(); at this point the game is either won or 
	 * lost, and the final game state is returned as well.  */
	private static class DevMainResult {
		
		private final boolean won;
		
		private final GameStateContainer finalGameState;

		public DevMainResult(boolean won, GameStateContainer finalGameState) {
			this.won = won;
			this.finalGameState = finalGameState;
		}
		
		public boolean isWon() {
			return won;
		}
		
		public GameStateContainer getFinalGameState() {
			return finalGameState;
		}
		
		
	}
	
	/** Contains a game state (pirate or non), plus a flag on whether the game has been won. */
	public static class WinnableGameStateContainer {
		
		public boolean isWin = false;
		
		// may be null
		private final GameStateContainer gsc;
		
		public WinnableGameStateContainer(GameStateContainer gsc) {
			this.gsc = gsc;
		}
		
		public GameStateContainer getGsc() {
			return gsc;
		}
		
		public void setWin(boolean isWin) {
			this.isWin = isWin;
		}
		
		public boolean isWin() {
			return isWin;
		}
	}
}
