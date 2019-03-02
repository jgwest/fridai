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
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** A singleton utility used to benchmark the game performance. */
public class BenchmarkEntries {
	
	private static final BenchmarkEntries instance = new BenchmarkEntries();
	
	private BenchmarkEntries() {

	}
	
	public static BenchmarkEntries getInstance() {
		return instance;
	}
	
	// -----------------------------

	private final Object lock = new Object();
	
	private final Map<Long, BTEntry> entryMap_synch_lock = new HashMap<>();

	
	public void writeLastThroughtputEntryToFile(File outputFile) throws IOException {
		FileWriter fw = new FileWriter(outputFile, true);  
		synchronized(lock) {

			List<BTPair> lastValues = new ArrayList<>();
			for(BTEntry be : entryMap_synch_lock.values()) {
				List<BTPair> l = be.individualEntries;
				if(l.size() > 0) { 
					lastValues.add(l.get(l.size()-1));
				}
			}
			
			double secondsTotal = 0;
			long processedTotal = 0;
			
			for(BTPair e : lastValues) {
				double seconds = ((double)TimeUnit.MILLISECONDS.convert(e.nanos, TimeUnit.NANOSECONDS))/1000d;

				long processed = e.iterations;
									
				secondsTotal += seconds;
				processedTotal += processed;
			}
	
			fw.write(secondsTotal+","+processedTotal+"\r\n");
			
		}			
		fw.close();
	}
	
	public void writeAverageThroughputEntryToFile(File outputFile) throws IOException {
		FileWriter fw = new FileWriter(outputFile, true);  
		synchronized(lock) {
			List<Map.Entry<Long, BTEntry>> list = new ArrayList<>();
			list.addAll(entryMap_synch_lock.entrySet());
			
			double totalSeconds = 0;
			int totalPerSecond = 0;
			
//			System.out.println("Current throughput: ");
			for(Map.Entry<Long, BTEntry> e : list) {
				
				double seconds = ((double)TimeUnit.MILLISECONDS.convert(e.getValue().total.nanos, TimeUnit.NANOSECONDS))/1000d;

				long processed = e.getValue().total.iterations;
				
				int perSecond = (int)(processed/seconds);
				totalPerSecond +=  perSecond;
				totalSeconds += seconds;
				
			}
			
//			System.out.println("Total per second: "+totalPerSecond);
			
			fw.write(totalSeconds+","+totalPerSecond+"\r\n");
			
		}
		
		fw.close();
		
	}
	
	public void printThroughput() {
		synchronized(lock) {
			List<Map.Entry<Long, BTEntry>> list = new ArrayList<>();
			list.addAll(entryMap_synch_lock.entrySet());
			
			Collections.sort(list, (a,b) -> { return (int)(a.getKey() - b.getKey()); } );
			
			int totalPerSecond = 0;
			
			System.out.println("Current throughput: ");
			for(Map.Entry<Long, BTEntry> e : list) {
				
				double seconds = ((double)TimeUnit.MILLISECONDS.convert(e.getValue().total.nanos, TimeUnit.NANOSECONDS))/1000d;

				long processed = e.getValue().total.iterations;
				
				int perSecond = (int)(processed/seconds);
				totalPerSecond +=  perSecond;
				
				NumberFormat nf = NumberFormat.getInstance();
				System.out.println("#"+e.getKey()+"  processed:"+nf.format(processed)+"  "+nf.format(perSecond)+" per second"); 

			}
			
			System.out.println("Total per second: "+totalPerSecond);
			
			
		}
	}
	
	public void addIterations(long iterations, long nanosElapsed) {

		synchronized(lock) {
			BTEntry entry = getEntry_synch_lock();

			entry.total.iterations += iterations;
			entry.total.nanos += nanosElapsed;
			
			entry.individualEntries.add(new BTPair(iterations, nanosElapsed));
		}
	}
	
	private BTEntry getEntry_synch_lock() {
		long threadId = Thread.currentThread().getId();
		BTEntry bte = entryMap_synch_lock.get((Long)threadId);
		if(bte == null) {
			bte = new BTEntry();
			entryMap_synch_lock.put((Long)threadId, bte);
		}
		
		return bte;
	}

	/** A per-thread store of the total number of completed iterations and elapsed nanoseconds, as well
	 * as a list of the individual components of that total. */
	public static class BTEntry {
		BTPair total = new BTPair(0, 0);
		List<BTPair> individualEntries = new ArrayList<>();
	}
	
	/** Store the number of iterations completed, and the number of nanoseconds elapsed during that time. */
	public static class BTPair {
		long iterations;
		long nanos;
		
		public BTPair(long iterations, long nanos) {
			this.iterations = iterations;
			this.nanos = nanos;
		}
		
	}
}
