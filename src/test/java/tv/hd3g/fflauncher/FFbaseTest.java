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
package tv.hd3g.fflauncher;

import junit.framework.TestCase;
import tv.hd3g.execprocess.ExecutableFinder;

public class FFbaseTest extends TestCase {
	
	public void test() throws Exception {
		FFbase b = new FFbase(new ExecutableFinder(), "ffmpeg");
		
		b.getCodecs();
		
		/*FFVersion version = b.getVersion();
		assertNotNull(version);
		assertNotNull(version.libavcodec_version);
		assertNotNull(version.raw_configuration);
		assertNotNull(version.configuration);
		
		assertFalse(version.libavcodec_version.isEmpty());
		assertFalse(version.raw_configuration.isEmpty());
		assertFalse(version.configuration.isEmpty());*/
		
	}
	
}
