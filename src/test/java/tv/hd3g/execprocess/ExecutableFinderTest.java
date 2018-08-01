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
import java.io.IOException;

import junit.framework.TestCase;

public class ExecutableFinderTest extends TestCase {
	
	public void testPreCheck() throws IOException {
		assertEquals("\\", "/".replaceAll("/", "\\\\"));
		assertEquals("/", "\\".replaceAll("\\\\", "/"));
	}
	
	public void test() throws IOException {
		ExecutableFinder ef = new ExecutableFinder();
		
		assertTrue(ef.getFullPath().contains(new File(System.getProperty("user.dir"))));
		
		File exec = ef.get("test-exec");
		if (File.separator.equals("/")) {
			assertEquals("test-exec", exec.getName());
		} else {
			assertEquals("test-exec.bat", exec.getName());
		}
	}
	
	public void testRegisterExecutable() throws IOException {
		ExecutableFinder ef = new ExecutableFinder();
		
		File element = ef.get("test-exec");
		
		ef = new ExecutableFinder();
		ef.registerExecutable("other-test", element);
		
		assertEquals(element.getPath(), ef.get("other-test").getPath());
		
		ef.get("java");
		ef.registerExecutable("java", element);
		assertEquals(element.getPath(), ef.get("java").getPath());
	}
}
