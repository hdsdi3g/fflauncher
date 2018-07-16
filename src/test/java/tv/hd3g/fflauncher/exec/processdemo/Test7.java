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
package tv.hd3g.fflauncher.exec.processdemo;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Test7 {
	
	public static final String[] std_out = { "Lorem ipsum dolor sit amet, consectetur adipiscing elit.", "Praesent vitae erat sed justo efficitur aliquam eu eu erat.", "Proin ut elit ultrices neque ullamcorper vehicula.", "", "Sed quis varius diam. Proin sodales mi sodales, fringilla est a, sagittis nisl." };
	public static final String[] std_err = { "Vivamus ac rhoncus mi. Aliquam cursus consequat tellus eget vulputate.", "", "Donec varius odio orci, quis vehicula urna pulvinar quis.", "Maecenas consequat neque nulla, ut tempus eros aliquam vitae.", "Sed a velit scelerisque, hendrerit lectus id, suscipit mi.", "Morbi a venenatis nulla." };
	
	public static void main(String[] args) throws InterruptedException {
		if (args[0].equals("1")) {
			Arrays.spliterator(std_out).forEachRemaining(t -> System.out.println(t));
			Arrays.spliterator(std_err).forEachRemaining(t -> System.err.println(t));
		} else {
			ExecutorService executor = Executors.newFixedThreadPool(2);
			
			executor.invokeAll(Arrays.asList(() -> {
				Arrays.spliterator(std_out).forEachRemaining(t -> System.out.println(t));
				return null;
			}, () -> {
				Arrays.spliterator(std_err).forEachRemaining(t -> System.err.println(t));
				return null;
			}), 200, TimeUnit.MILLISECONDS);
			
			System.exit(0);
		}
	}
	
}
