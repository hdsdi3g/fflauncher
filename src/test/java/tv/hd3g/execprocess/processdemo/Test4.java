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
package tv.hd3g.execprocess.processdemo;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Test4 {
	
	public static final String std_out = "HELLO";
	public static final String std_err = "ERROR";
	
	public static void main(String[] args) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		
		executor.invokeAll(Arrays.asList(() -> {
			System.out.println(std_out);
			return null;
		}, () -> {
			System.err.println(std_err);
			return null;
		}), 100, TimeUnit.MILLISECONDS);
		
		System.exit(0);
	}
	
}
