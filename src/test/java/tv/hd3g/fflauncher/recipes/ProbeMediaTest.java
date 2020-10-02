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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2018
 *
 */
package tv.hd3g.fflauncher.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.awt.Point;
import java.io.File;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import tv.hd3g.fflauncher.enums.OutputFilePresencePolicy;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.tool.ToolRunner;

class ProbeMediaTest {

	final ToolRunner run;

	ProbeMediaTest() {
		run = new ToolRunner(new ExecutableFinder());
	}

	@Test
	void test() throws Exception {
		final var gvf = new GenerateVideoFile(run);

		final var tDir = System.getProperty("java.io.tmpdir");
		final var test_file_to_create = new File(tDir + File.separator + "smptebars-" + System.nanoTime() + ".mkv");
		final var ffmpeg = gvf.generateBarsAnd1k(test_file_to_create, 1, new Point(768, 432))
		        .getExecutableToolSource();

		ffmpeg.checkDestinations();
		final var outputFiles = ffmpeg.getOutputFiles(OutputFilePresencePolicy.ALL);
		assertEquals(test_file_to_create, outputFiles.get(0));
		assertEquals(1, outputFiles.size());
		assertNotSame(0, test_file_to_create.length());

		final var probe = new ProbeMedia(run, Executors.newSingleThreadScheduledExecutor());
		final var result = probe.doAnalysing(test_file_to_create);

		assertEquals(1, result.getFormat().getDuration().intValue());
		assertEquals(432, result.getVideoStreams().findFirst().get().getHeight().intValue());

		ffmpeg.cleanUpOutputFiles(true, false);
	}

}
