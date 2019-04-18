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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.DeprecatedCommandLineProcessor;
import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.fflauncher.FFprobe;
import tv.hd3g.fflauncher.FFprobe.FFPrintFormat;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

public class ProbeMedia extends Recipe {
	private static Logger log = LogManager.getLogger();

	public ProbeMedia() {
		super();
	}

	public ProbeMedia(final ExecutableFinder exec_finder, final String exec_name) {
		super(exec_finder, exec_name);
	}

	/**
	 * Get streams, format and chapters.
	 * @see FFprobe to get cool FfprobeType parsers
	 */
	public CompletableFuture<FFprobeJAXB> doAnalysing(final String source) throws IOException {
		final FFprobe ffprobe = new FFprobe(getExecFinder(), new DeprecatedCommandLineProcessor().createEmptyCommandLine(getExecName()));

		ffprobe.setPrintFormat(FFPrintFormat.xml).setShowStreams().setShowFormat().setShowChapters().isHidebanner();
		ffprobe.addSimpleInputSource(source);

		final ExecProcessText exec = ffprobe.createExecWithLimitedExecutionTime();
		log.info("Queue \"" + source + "\" ffprobe analysing");

		return exec.start(getExecutionExecutor()).waitForEnd().thenApplyAsync(result -> {
			try {
				return new FFprobeJAXB(result.checkExecution().getStdout(false, System.lineSeparator()), warn -> log.warn(warn));
			} catch (final IOException e) {
				throw new RuntimeException("Can't analyst " + source, e);
			}
		}, getPostProcessExecutor());
	}

	@Override
	protected String getDefaultExecName() {
		return "ffprobe";
	}

}
