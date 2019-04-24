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
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import junit.framework.TestCase;
import tv.hd3g.fflauncher.FFmpeg;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.tool.ToolRunner;

public class GenerateVideoFileTest extends TestCase {

	public void test() throws InterruptedException, ExecutionException, IOException {
		final ToolRunner run = new ToolRunner(new ExecutableFinder(), 1);
		final GenerateVideoFile gvf = new GenerateVideoFile(run);

		final File test_file = File.createTempFile("smptebars", ".mkv");

		final FFmpeg ffmpeg = gvf.generateBarsAnd1k(test_file, 5, new Point(1280, 720)).get().getExecutableToolSource();

		assertTrue(test_file.exists());

		ffmpeg.checkDestinations();
		ffmpeg.cleanUpOutputFiles(true, true);
		assertFalse(test_file.exists());
	}

}
