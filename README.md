Fridai is an AI and game engine for the single-player board/card game Friday by Friedemann Friese. 

In Friedemann Friese's Friday, you are Robinson, drawing fighting resources, facing hazards, building your fighting card deck, and ultimately fighting pirates to win the game. During the game you deploy a set of card abilities to draw additional cards, defeat hazards, and improve the quality of your deck, with the final verson of your deck used to face off against 2 randomly drawn pirate cards.

The original Friedemann Friese's Friday is available as a physical board/card game, on the Apple App store, and the Google Play store.


## Introduction to Fridai

Fridai is an AI that plays Friday, and a game engine that simulates the rules of the game by allowing the game state to manipulated by the AI (I refer to the AI as 'player' from here on) in ways that are consistent with the game rules.

The *Game state* describes what is currently going on in the game:
- Which fighting cards are remaining in the fighting card stack (which the player may draw when battling hazards)
- Which hazard cards are remaining in the hazard card stack
- The number of life points of the player
- During the hazard phase, what cards have already been drawn to the left-hand side or right-hand side of the hazard
- Which pirates the player will face in the final round
- And so on...

Each game state is fully contained within either the `GameState` or `PirateGameState` objects. 

Each turn, the game engine will look at the current game state and generate one or more Action objects (eg `DrawAFreeCardAction`, `UseCardAbilityAction`, `EndSacrificeLifePhaseAction`) . These action objects fully encompass all possible 'moves' the player can make that turn (eg from that game state). The AI will select one of those moves (by determining which is the best, using a tree search with a custom evaluation metric), and return that to the game engine as an `ActionResponse`. That response will be processed and the game state will be mutated appropriately, as if the player took that action.

The core game loop, from the perspective of the game engine, looks like this:
`GameState => List of 1 or more Actions => An action response based on a single of those actions => new GameState => (...)`

The "rules" of the game are thus actually just a set of valid state transitions between game states.

## AI - Tree algorithm

As noted above, the Fridai AI will select the best move by searching the universe of all moves using an A* tree search algortihm. The metric used by A* is the game state evaluation metric, which returns a score based on an arbitrary measure of 'how well the player is doing'.  Thus, branches of the tree that are better for the player are explored first.

Each turn, the tree search algorithm works as follows:
- Add the current game state to a priority queue:
  - The priority queue is sorted by highest game state score, descending, using the game state evaluation metric.
- While the priority queue is not empty:
  - Poll a game state from the queue
  - Look at the available actions from the game state ('what can we do from here?')
  - Filter out any actions that are known to be fruitless (a quick heuristic to prevent exploring unproductive tree branches)
  - Perform each of the actions, each giving us a new game state (corresponding to the state of the world after the action was completed.)
  - Calculate the score for each of the game states resulting from each of the above actions. 
  - Add to the queue the [action, score, and game state]-tuple for the game states resulting from each of the above actions.


## AI Game state evaluation metric - Option 1 -  Life points only

While searching the game state tree, we need a metric to determine how a player is doing. 

One option is to only use life points as the evaluation metric. A player begins the game with 20 life points, and may have at most 22. The issue with using life points as the sole evaluation metric, is that the algorithm will tend seek to preserve life at the expense of fighting card deck quality.

Fundamental to winning a round of the card/board game Friday, is knowing how to best spend your early life points to improve the your chance of winning later in the game. If you spend too many life points early on, your fighting card deck will be much higher quality, but you may die before reaching the final round. On the other hand, if you don't spend enough life points early on, you might reach the final round, but, your fightng card deck will not be of sufficient quality to actually beat the final round.

So a fundamental strategy of playing Friday is knowning how best to invest your life points for maximum fighting card quality return. Thus, when the AI was only focused on life points, it would tend to avoid investing life early in the game, at the expense of deck quality (and life) later in the game.

## AI Game state evaluation metric - Option 2 -  Fighting card stack quality only

Another option is to use the fighting card stack quality as the evaluation metric. In Friday, 'Hazard' cards are the card you are currenting fighting, and 'Fighting' cards are the card you fight those hazards with.

Both hazard cards and fighting cards have varying power levels, both in terms of their actualy strength (from -2 to +5), and in terms of their abilities (draw cards, gain life, copy an ability of another card, etc).

As noted above, the key to winning a game of Friday is knowning how best to invest your life points for maximum fighting card quality return. The overall quality of a fighting card deck can be expressed as an average of the sum of the fighting card strenth and a ranking of the abilities of each of those fighting cards. With this quality metric, we can compare two game states like so: game state A has average fighting card stack quality of 4, while game state B has average fighting card stack quality of -1. The AI would then prefer to explore (and steer the game towards) game state A rather than game state B.

However, when using only fighting card stack quality as an evaluation metric, the AI tended to spend too much life early on, which would cause it to die before reaching the final round (albeit doing so with a high quality deck).


## AI Game state evaluation metric - Current strategy

The current evaluation metric strategy employed by the AI is a blend of the # of life points the player has, and the quality of the fighting card stack. See Main.score(...) for details. The life points of the player are the primary metric, with the fighting card stack quality used to break ties when the life points are otherwise equal.

More research is required to determine the best function for determining how “winnable” a game is. In addition, this score does not, but would benefit from, taking into account which pirate cards it will have to fight against.

Fighting pirate cards (a special type of hazard card) are the final round of the game. Once the pirates are beaten, the game is won. However, there are 10 pirates, but only 2 are used per game. The specific pirates that you fight are determined at the start of the game.

The strategy of the player should be to tailor their fighting card stack to best face the specific pirates they must fight in the final round.  For example: one of the pirate cards says you might fight all the remaining hazard cards. In this scenario, it would be beneficial for the AI to attempt to destroy the remaining hazard cards before the final round, even if it is at the expense of other metrics (life or card quality)


## Performance - Speed and implementation langauge

This type of AI problem is refered to as [embarassingly parallell](https://en.wikipedia.org/wiki/Embarrassingly_parallel), which means it is simple to distribute the work into parallel tasks across multiple CPU cores. In this situation, the main area of complexity is ensuring that shared thread data is not incorrectly shared between threads: ThreadLocal is used to efficiently ensure data is available only on the thread using it. There are very few synchronized statements in the code, and these are only in the infrequently called benchmark and logging code.

The current AI code is quite fast: it is able to search ~550,000 game states per core, per second, on a [7th-generation Intel Core i5-7600](https://en.wikipedia.org/wiki/List_of_Intel_Core_i5_microprocessors#Kaby_Lake_microarchitecture_(7th_generation)). The performance of the search scales linearly with the number of CPU cores assigned.

Java was used as the implementation language for its strong blend of developer productivity and application performance.  (The Rust language was also considered, which would also be a great fit, and in fact I have prototyped porting of some of the Fridai code to Rust, but at present only the Java AI code is available here.)

While I did use Java, I only used the subset of language features which are known to be best optimizable by the JIT compiler. Thus, for code on the performance "hot" path, I wrote code with these restrictions in mind:
- static methods only 
- simple static non-abstract final classes only (akin to C/Rust structs)
- very limited OOP
This means that while I did write this project in Java, the code itself looks a bit like Go, Rust, or C (and unlike traditional OOP Java or C++).

In order to verify that my application was spending its CPU time in the expected areas (eg no unexpected performance bottlenecks), and ensuring that memory was not being leaked/wasted in unexpected areas, I used the [Eclipse Memory Analyzer tool](https://www.eclipse.org/mat/)  for Java heap profiling analysis, and [VisualVM](https://visualvm.github.io/) was used for sample-based method execution profiling.

Thanks to the above programming techniques, and intelligent optimization algorithms such as escape-analysis in the JVM itself, the overall garbage collection CPU overhead of the application is quite low, less than 8% with a properly sized heap. For this reason, I have not needed to explore strategies to reduce GC overhead, and am pleased with the use of Java as implementation language overall.

## Performance Optimizations - Specialized-collections and faux immutable abstractions

A major performance optimization used in Fridai is the use of specialized List/Map classes for code that is on the performance hot path. 

The goal with these optimizations is to reduce heap usage and CPU usage (though usually CPU usage is reduced as a by-product of heap reduction, by reducing need for GC and/or object allocation).

There are three major data structure patterns used in the code:
- `ImmutableGrowableListCards`: An ordered list of cards, that is only ever added to, or iterated through, never removed from. 
	- Used in game: hazard discard pile, fight card discard pile, lefthand-side fight cards during hazard phases, righthand-side fight cards during hazard phase, active round cards
	- Exposes an immutable API

- `ListCards`: An ordered list of cards that is only ever removed from, or iterated through, never added to.
	- Used in game: fighting card stack, hazard card stack, aging cards
	- Exposes an immutable API

- `MapCards`: Map of card -> boolean, for whether a particular card was used. 
	- lhsOrRhsFightingCardUsed, lhsOrRhsFightingCardDoubled
	- Exposes an immutable API


These data structures do not require all of the Java Collections' `Map` or `List` operations, and likewise they do not need to be threadsafe, they either add or remove (but not both), and expose an immmutable API. For these reasons we can optimize them for improved performance for this specific use cases.

A key optimization used for these classes/patterns is best described in contrast to traditional Java collections: with a normal Java collection, such as `ArrayList` or `HashMap`, each object instance maintains its own independent collection data (usually an array). So if I create a new ArrayList, the backing array of that ArrayList will only be used by that ArrayList object, and never shared by other ArrayList instances.

In contrast, when I create an ImmutableGrowableListCards, and add a Card to it, a new ImmutableGrowableListCards object is created and returned with that new card added to the new list. However, both instances of ImmutableGrowablieListCards share the same backing array.

You can see this when comparing the APIs of these two

Adding to a standard ArrayList:
```
	Card c = new Card( ... );
	ArrayList<Card> cardList = new ArrayList<>();
	cardList.add(c);
	// Thus, the original list now contains c.
```

Adding to an ImmutableGrowableListCards:
```
	Card c = new Card( ... );
	ImmutableGrowableListCards cardList = new ImmutableGrowableListCards(16 /* max size, before increasing*/);
	ImmutableGrowableListCards newList = cardList.mutateAdd(c);
	// Thus, the original list 'cardList' object is unchanged, but a 'newList' list was created that includes card c.
```

Behind the scenes, `cardList` and `newList` actually share the same backing array, which reduces heap usage. This sharing of data is only possible if the interface that is exposed by these collections is itself immutable, which is the case here. In a similar way, instances of `ListCards` share the same backing array, with the remove() operation only requiring the incrementing of a single index value in the new ListCards object that is created.

Finally, MapCards simulates a `Map<Card, Boolean>` map, but is much more memory efficient as it is implemented as a `boolean[]` array with only 64 elements. This is possible because there are less-than 64 cards in the game, and each card has a unique 'physical card id' field we can use to easily map from Card to an integer (with that integer used as the array index). 

For more information, see the class descriptions of `ListCards`, `ImmutableGrowableListCards` and `MapCards`.

In all cases, these performance improvements were carefully benchmarked to ensure they were truely improving performance, while still being hidden behind the existing List/Map abstractions so as not to introduce complexity to the calling code.


## Future performance improvements

While when it comes to performance I have already picked most of the 'low-hanging fruit', there are still additional areas to explore:
- `ImmutableGrowableListCards` and `ListCards` could be further improved; in both cases, bulk add is less efficient then it could be.
- Find a way to identify game states that we have already processed, so as not to process them twice:
  - Need a quick way to tag the state of a game state: `(fighting card stack id, lhs stack id, rhs stack id, hazard stack id, etc)`
  - Each time a `ListCards` or `ImmutableGrowableListCards` was modified, its id would change, allowed us to update the tag.
  - lossy vs lossless tag (think it needs to be lossless, unfortunately)
- No plans to alter the application GC strategy using object pools, etc; GC overhead is already very low due to existing design.
- I may be able to further reduce the heap size of `TreeEntry` by replacing the empty `ArrayList` with a null.
- Improve action filtering to handle more cases that are obviously not worth spending any time processing. 
- Processing which combinations of cards to destroy is definitely a focus area.
- The priority queue implementation would likely be more efficient if I were feeding it keys that fit into less 'buckets'.
- A-star search algorithm should favour branches that are closer to the top of the tree.

It is worth noting that performance improvements also improve the overall quality of the AI: the less time it takes for the AI to search the branches of the tree, the more time it has to explore additional branches. 
- Improve `Main.score(...)`: the current score algorithm will not necessarily make efficient trades of life for card destruction. 
  - For example, it cannot always distinguish between paying 1 life to destroy a strong hazard, versus not paying 1 life to allow it to be shuffled back into the deck.
  - The idea here is the spending efficiency of life, as it relates to card destruction. Metric is to maximize `(hazard value divided by life)`.

## To build and run the AI/game engine

To build and the game:
```
git clone "https://github.com/jgwest/fridai"
cd fridai/FridayGame
mvn clean package
java -jar target/Fridai-1.0.0.jar

To run the AI:
java -jar target/Fridai-1.0.0.jar (time to run in minutes) (# of nodes to search per round) (perf output file) (num cpu cores to use) (result output file)
```

