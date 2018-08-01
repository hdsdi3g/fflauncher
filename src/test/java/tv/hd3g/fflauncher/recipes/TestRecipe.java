/*
 * This file is part of fflauncher.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2018
 * 
*/
package tv.hd3g.fflauncher.recipes;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import junit.framework.TestCase;
import tv.hd3g.execprocess.ExecutableFinder;

public class TestRecipe extends TestCase {
	
	public void test() {
		
		Recipe r = new Recipe(new ExecutableFinder(), "java") {
			protected String getDefaultExecName() {
				return "NOPE NOPE";
			}
		};
		
		assertEquals(r.getExecName(), "java");
		assertEquals(r.getDefaultExecName(), "NOPE NOPE");
		assertEquals(ForkJoinPool.commonPool(), r.getExecutionExecutor());
		assertEquals(ForkJoinPool.commonPool(), r.getPostProcessExecutor());
		
		Executor e1 = Executors.newCachedThreadPool();
		Executor e2 = Executors.newCachedThreadPool();
		
		assertFalse(e1.equals(e2));
		
		r.setExecutionExecutor(e1);
		r.setPostProcessExecutor(e2);
		
		assertEquals(e1, r.getExecutionExecutor());
		assertEquals(e2, r.getPostProcessExecutor());
	}
	
}
