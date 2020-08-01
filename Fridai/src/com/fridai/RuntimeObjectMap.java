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

import java.util.WeakHashMap;

import com.fridai.util.FridayUtil;

/** If debugging is enabled, this class allows you to annotate instances of GameState and PirateGameState with
 * additional metadata. That metadata is stored in RuntimeObject. 
 * 
 * This class assumes that each thread of the application is running an independent game, and that the 
 * RuntimeObjectMap will not be shared between threads. This class thus uses ThreadLocal to keep track 
 * of the game state annotation map. */
public final class RuntimeObjectMap {

	private final static RuntimeObjectMap INSTANCE = new RuntimeObjectMap();
	
	private final ThreadLocal<WeakHashMap<GameState, RuntimeObject>> map = new ThreadLocal<WeakHashMap<GameState, RuntimeObject>>() {
		protected WeakHashMap<GameState,RuntimeObject> initialValue() {
			return new WeakHashMap<>();
		}; 
	};
	
	private final ThreadLocal<WeakHashMap<PirateGameState, RuntimeObject>> pirateMap = new ThreadLocal<WeakHashMap<PirateGameState, RuntimeObject>>() {
		protected WeakHashMap<PirateGameState,RuntimeObject> initialValue() {
			return new WeakHashMap<>();
		}; 
	};
	
	public final static RuntimeObjectMap getInstance() {
		if(!FridayUtil.RUNTIME_CHECK) { FridayUtil.throwErr("Runtime object map unsupported if runtime check is disabled.");}
		return INSTANCE;
	}
	
	private RuntimeObjectMap() {
	}
	

	public final void put(PirateGameState gs, RuntimeObject ro) {
		pirateMap.get().put(gs, ro);
	}
	
	public final RuntimeObject getByObject(Object o) {
		if(o instanceof PirateGameState) {
			return get((PirateGameState)o);
		} else {
			return get((GameState)o);
		}
	}
	
	public final RuntimeObject get(PirateGameState gs) {
		
		WeakHashMap<PirateGameState, RuntimeObject> mapEntry = pirateMap.get();
		
		RuntimeObject ro = mapEntry.get(gs);
		if(ro == null) {
			ro = new RuntimeObject();
			mapEntry.put(gs,  ro);
		}
		
		return ro;
	}
	
	public final void put(GameState gs, RuntimeObject ro) {
		map.get().put(gs, ro);
	}
	
	public final RuntimeObject get(GameState gs) {
		
		WeakHashMap<GameState, RuntimeObject> mapEntry = map.get();
		
		RuntimeObject ro = mapEntry.get(gs);
		if(ro == null) {
			ro = new RuntimeObject();
			mapEntry.put(gs,  ro);
		}
		
		return ro;
	}
	
}
