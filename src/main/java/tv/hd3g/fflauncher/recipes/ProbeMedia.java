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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.fflauncher.FFLogLevel;
import tv.hd3g.fflauncher.FFprobe;
import tv.hd3g.fflauncher.FFprobe.FFPrintFormat;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.tool.RunningTool;
import tv.hd3g.processlauncher.tool.ToolRunner;

public class ProbeMedia extends Recipe {

	private static Logger log = LogManager.getLogger();

	private final ScheduledExecutorService maxExecTimeScheduler;

	public ProbeMedia(final ToolRunner toolRun, final ScheduledExecutorService maxExecTimeScheduler) {
		super(toolRun, "ffprobe");
		this.maxExecTimeScheduler = Objects.requireNonNull(maxExecTimeScheduler, "\"maxExecTimeScheduler\" can't to be null");
	}

	public ProbeMedia(final ToolRunner toolRun, final String execName, final ScheduledExecutorService maxExecTimeScheduler) {
		super(toolRun, execName);
		this.maxExecTimeScheduler = Objects.requireNonNull(maxExecTimeScheduler, "\"maxExecTimeScheduler\" can't to be null");
	}

	private FFprobe internal() throws IOException {
		final Parameters parameters = new Parameters();
		final FFprobe ffprobe = new FFprobe(execName, parameters);

		ffprobe.setPrintFormat(FFPrintFormat.xml).setShowStreams().setShowFormat().setShowChapters().isHidebanner();
		ffprobe.setMaxExecTimeScheduler(maxExecTimeScheduler);
		ffprobe.setLogLevel(FFLogLevel.error, false, false);
		ffprobe.setFilterForLinesEventsToDisplay(l -> {
			return l.isStdErr() && ffprobe.filterOutErrorLines().test(l.getLine());
		});

		return ffprobe;
	}

	public static class InvalidFFprobeReturn extends RuntimeException {
		private InvalidFFprobeReturn(final String source, final String actualPayload, final IOException origin) {
			super("Can't analyst " + source + ". Raw ffprobe return: \"" + actualPayload + "\"", origin);
		}
	}

	private CompletableFuture<FFprobeJAXB> execute(final FFprobe ffprobe, final String source) throws IOException {
		return toolRun.execute(ffprobe)
		        .thenApplyAsync(RunningTool::checkExecutionGetText, executor)
		        .thenApplyAsync(textRetention -> {
			        final var stdOut = textRetention.getStdout(false, System.lineSeparator());
			        try {
				        return new FFprobeJAXB(stdOut, warn -> log.warn(warn));
			        } catch (final IOException e) {
				        log.error("Raw ffprobe return: \"{}\"", stdOut);
				        throw new InvalidFFprobeReturn(source, e);
			        }
		        }, executor);
	}

	/**
	 * Stateless
	 * Get streams, format and chapters.
	 * Can throw an InvalidExecution in CompletableFuture, with stderr embedded.
	 * @see FFprobe to get cool FfprobeType parsers
	 */
	public CompletableFuture<FFprobeJAXB> doAnalysing(final String source) throws IOException {
		final FFprobe ffprobe = internal();
		ffprobe.addSimpleInputSource(source);
		return execute(ffprobe, source);
	}

	/**
	 * Stateless
	 * Get streams, format and chapters.
	 * Can throw an InvalidExecution in CompletableFuture, with stderr embedded.
	 * @see FFprobe to get cool FfprobeType parsers
	 */
	public CompletableFuture<FFprobeJAXB> doAnalysing(final File source) throws IOException {
		final FFprobe ffprobe = internal();
		ffprobe.addSimpleInputSource(source);
		return execute(ffprobe, source.getPath());
	}

}
