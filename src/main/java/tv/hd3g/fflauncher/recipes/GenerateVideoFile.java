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
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.DeprecatedCommandLineProcessor;
import tv.hd3g.execprocess.DeprecatedCommandLineProcessor.DeprecatedCommandLine;
import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.fflauncher.FFmpeg;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

public class GenerateVideoFile extends Recipe {
	private static Logger log = LogManager.getLogger();

	public GenerateVideoFile() {
		super();
	}

	public GenerateVideoFile(final ExecutableFinder exec_finder, final String exec_name) {
		super(exec_finder, exec_name);
	}

	public CompletableFuture<FFmpeg> generateBarsAnd1k(final String destination, final int duration_in_sec, final Point resolution) throws IOException {
		final FFmpeg ffmpeg = new FFmpeg(getExecFinder(), new DeprecatedCommandLineProcessor().createEmptyCommandLine(getExecName()));

		if (ffmpeg.getAbout().isFromFormatIsAvaliable("lavfi") == false) {
			throw new IOException("This ffmpeg (" + ffmpeg.getExecutable() + ") can't handle \"lavfi\"");
		}

		ffmpeg.setOverwriteOutputFiles();
		ffmpeg.setOnErrorDeleteOutFiles(true);

		final DeprecatedCommandLine cmd = ffmpeg.getCommandLine();
		cmd.addBulkParameters("-f lavfi -i smptehdbars=duration=" + duration_in_sec + ":size=" + resolution.x + "x" + resolution.y + ":rate=25");
		cmd.addBulkParameters("-f lavfi -i sine=frequency=1000:sample_rate=48000:duration=" + duration_in_sec);

		if (ffmpeg.getAbout().isCoderIsAvaliable("h264")) {
			ffmpeg.addVideoCodecName("h264", -1).addCRF(1);
		} else {
			ffmpeg.addVideoCodecName("ffv1", -1);
		}

		if (ffmpeg.getAbout().isCoderIsAvaliable("aac")) {
			ffmpeg.addAudioCodecName("aac", -1);
		} else {
			ffmpeg.addAudioCodecName("opus", -1);
		}

		ffmpeg/*.addFastStartMovMp4File()*/.addSimpleOutputDestination(destination);

		final ExecProcessText exec = ffmpeg.createExec();
		log.info("Generate test file to \"" + destination + "\"");

		return exec.start(getExecutionExecutor()).waitForEnd().thenAcceptAsync(result -> {
			result.checkExecution();
		}, getPostProcessExecutor()).thenApplyAsync(_void -> ffmpeg, getPostProcessExecutor());
	}

	@Override
	protected String getDefaultExecName() {
		return "ffmpeg";
	}

}
