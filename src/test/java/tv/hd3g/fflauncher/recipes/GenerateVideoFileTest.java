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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import junit.framework.TestCase;

public class GenerateVideoFileTest extends TestCase {
	
	public void test() throws InterruptedException, ExecutionException, IOException {
		GenerateVideoFile gvf = new GenerateVideoFile();
		
		File test_file = File.createTempFile("smptebars", ".mkv");
		
		gvf.generate(test_file.getPath()).get();
		
		assertTrue(test_file.exists());
		assertTrue(test_file.delete());
	}
}
