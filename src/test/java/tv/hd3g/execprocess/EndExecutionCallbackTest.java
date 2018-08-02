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
package tv.hd3g.execprocess;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import junit.framework.TestCase;

public class EndExecutionCallbackTest extends TestCase {
	
	public void test() {
		
		ExecProcessResult ref = new ExecProcessResult(new File(""), new ArrayList<>(), new HashMap<>(), new ArrayList<>(), false, null, null, 0, null, ForkJoinPool.commonPool());
		
		AtomicInteger t1_pass = new AtomicInteger(0);
		Consumer<ExecProcessResult> onEnd = t -> {
			if (ref == t) {
				t1_pass.incrementAndGet();
			} else {
				throw new RuntimeException("Invalid ref");
			}
		};
		
		AtomicInteger t2_pass = new AtomicInteger(0);
		Executor executor = command -> {
			command.run();
			t2_pass.incrementAndGet();
		};
		
		EndExecutionCallback<ExecProcessResult> eec = new EndExecutionCallback<>(onEnd, executor);
		eec.onEnd(ref);
		
		assertEquals(1, t1_pass.get());
		assertEquals(1, t2_pass.get());
	}
	
	public void test2() {
		
		ExecProcessTextResult ref = new ExecProcessTextResult(new File(""), new ArrayList<>(), new HashMap<>(), new ArrayList<>(), false, null, null, 0, null, ForkJoinPool.commonPool());
		
		AtomicInteger t1_pass = new AtomicInteger(0);
		Consumer<ExecProcessTextResult> onEnd = t -> {
			if (ref == t) {
				t1_pass.incrementAndGet();
			} else {
				throw new RuntimeException("Invalid ref");
			}
		};
		
		AtomicInteger t2_pass = new AtomicInteger(0);
		Executor executor = command -> {
			command.run();
			t2_pass.incrementAndGet();
		};
		
		EndExecutionCallback<ExecProcessTextResult> eec = new EndExecutionCallback<>(onEnd, executor);
		eec.onEnd(ref);
		
		assertEquals(1, t1_pass.get());
		assertEquals(1, t2_pass.get());
	}
	
}
