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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import junit.framework.TestCase;
import tv.hd3g.execprocess.StdInInjection;

public class StdInInjectionTest extends TestCase {
	
	public void testInject() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		StdInInjection sii = new StdInInjection(baos);
		
		byte[] test = { 1, 2, 3 };
		sii.write(test);
		sii.close();
		byte[] result = baos.toByteArray();
		
		assertTrue(Arrays.equals(test, result));
		
		baos.reset();
		sii.println("test !", StandardCharsets.UTF_8);
		
		result = baos.toByteArray();
		
		assertTrue(Arrays.equals(("test !" + StdInInjection.LINESEPARATOR).getBytes(StandardCharsets.UTF_8), result));
	}
	
}
