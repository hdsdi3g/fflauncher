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
import java.util.List;
import java.util.concurrent.Executors;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.fflauncher.FFmpeg;
import tv.hd3g.fflauncher.OutputFilePresencePolicy;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.tool.ToolRunner;

public class ProbeMediaTest extends TestCase {

	private final ToolRunner run;

	public ProbeMediaTest() {
		run = new ToolRunner(new ExecutableFinder(), 1);
	}

	public void test() throws Exception {
		final GenerateVideoFile gvf = new GenerateVideoFile(run);

		final String tDir = System.getProperty("java.io.tmpdir");
		final File test_file_to_create = new File(tDir + File.separator + "smptebars-" + System.nanoTime() + ".mkv");
		final FFmpeg ffmpeg = gvf.generateBarsAnd1k(test_file_to_create, 1, new Point(768, 432)).get().getExecutableToolSource();

		ffmpeg.checkDestinations();
		final List<File> outputFiles = ffmpeg.getOutputFiles(OutputFilePresencePolicy.ALL);
		Assert.assertEquals(test_file_to_create, outputFiles.get(0));
		Assert.assertEquals(1, outputFiles.size());
		Assert.assertNotSame(0, test_file_to_create.length());

		final ProbeMedia probe = new ProbeMedia(run, Executors.newSingleThreadScheduledExecutor());
		final FFprobeJAXB result = probe.doAnalysing(test_file_to_create).get();

		Assert.assertEquals(1, result.getFormat().getDuration().intValue());
		Assert.assertEquals(432, result.getVideoStreams().findFirst().get().getHeight().intValue());

		ffmpeg.cleanUpOutputFiles(true, false);
	}

}
