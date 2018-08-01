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

import java.io.FileNotFoundException;
import java.util.stream.Collectors;

import junit.framework.TestCase;
import tv.hd3g.execprocess.CommandLineProcessor;
import tv.hd3g.execprocess.ExecutableFinder;

public class FFmpegTest extends TestCase {
	
	public void testSimpleOutputDestination() throws FileNotFoundException {
		FFmpeg ffmpeg = new FFmpeg(new ExecutableFinder(), new CommandLineProcessor().createEmptyCommandLine("ffmpeg"));
		ffmpeg.addSimpleOutputDestination("dest", "container");
		
		assertTrue(ffmpeg.createProcessedCommandLine().getParameters().stream().collect(Collectors.joining(" ")).endsWith("-f container dest"));
	}
	
	// TODO test NV tools
}
