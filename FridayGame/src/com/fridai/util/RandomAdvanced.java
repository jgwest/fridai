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

import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * This class extends Random (so as to be API compatible), but only supports the generation of int using 
 * nextInt(int bound). This class keeps track of which seed it was initialized, and how many random numbers have been
 * generated. 
 * 
 * While debugging failed game states, this allows us to deterministically reproduce the exact state of 
 * the random number generator at the time of a failure. This deterministic reproducibility property makes it easy
 * to reproduce the failing state, despite the use of (pseudo)randomness.
 * 
 * Otherwise it is a wrapper over an existing Random object, initialized by the RandomAdvanced(..) constructor.
 *    
 * Not thread safe, use it in a ThreadLocal. */
public class RandomAdvanced extends Random {

	private static final long serialVersionUID = 1L;

	private final Random innerRandom;

	private final long randomAdvancedSeed;

	private long numbersGenerated = 0;
	
	
	public RandomAdvanced(long seed, long iterations) {
		innerRandom = new Random(seed);
		this.randomAdvancedSeed = seed;
		
		// Move to the desired location in the internal Random stream. See class 
		// description for details.
		while(numbersGenerated < iterations) {
			innerRandom.nextInt();
			numbersGenerated++;
		}
	}

	@Override
	public DoubleStream doubles() {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound) {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public DoubleStream doubles(long streamSize) {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public IntStream ints() {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public IntStream ints(long streamSize) {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public LongStream longs() {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public LongStream longs(long streamSize) {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	protected int next(int bits) {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public boolean nextBoolean() {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public void nextBytes(byte[] bytes) {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public double nextDouble() {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public float nextFloat() {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public synchronized double nextGaussian() {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public int nextInt() {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public int nextInt(int bound) {
		numbersGenerated++;
		return innerRandom.nextInt(bound);
	}

	@Override
	public long nextLong() {
		throw new RuntimeException("Unsupported.");
	}

	@Override
	public synchronized void setSeed(long seed) {
		/* ignore */
	}
	
	
	public long getNumbersGenerated() {
		return numbersGenerated;
	}
	public long getRandomAdvancedSeed() {
		return randomAdvancedSeed;
	}

}
