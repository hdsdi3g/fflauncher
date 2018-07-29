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

import java.awt.Point;
import java.io.File;

import junit.framework.TestCase;
import tv.hd3g.fflauncher.FFmpeg;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

public class ProbeMediaTest extends TestCase {
	
	private FFmpeg ffmpeg;
	
	protected void setUp() throws Exception {
		GenerateVideoFile gvf = new GenerateVideoFile();
		File test_file = File.createTempFile("smptebars", ".mkv");
		ffmpeg = gvf.generateBarsAnd1k(test_file.getPath(), 1, new Point(768, 432)).get();
	}
	
	protected void tearDown() throws Exception {
		ffmpeg.cleanUpOutputFiles(true, false);
	}
	
	public void test() throws Exception {
		ProbeMedia probe = new ProbeMedia();
		
		File test_file = ffmpeg.getOutputFiles(true, true, true).stream().findFirst().get();
		
		FFprobeJAXB result = probe.doAnalysing(test_file.getAbsolutePath()).get();
		
		// result.getError();
		assertEquals(1, result.getFormat().getDuration().intValue());
		assertEquals(432, result.getVideoStreams().findFirst().get().getHeight().intValue());
		
		// System.out.println(new Gson().toJson(result));
	}
	
}
