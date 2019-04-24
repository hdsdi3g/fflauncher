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
import java.util.concurrent.CompletableFuture;

import tv.hd3g.fflauncher.FFAbout;
import tv.hd3g.fflauncher.FFmpeg;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.tool.RunningTool;
import tv.hd3g.processlauncher.tool.ToolRunner;

public class GenerateVideoFile extends Recipe {

	public GenerateVideoFile(final ToolRunner toolRun) {
		super(toolRun, "ffmpeg");
	}

	public GenerateVideoFile(final ToolRunner toolRun, final String execName) {
		super(toolRun, execName);
	}

	/**
	 * Stateless
	 */
	private FFmpeg internal(final int duration_in_sec, final Point resolution) throws IOException {
		final Parameters parameters = new Parameters();
		final FFmpeg ffmpeg = new FFmpeg(execName, parameters);

		final FFAbout about = ffmpeg.getAbout(toolRun.getExecutableFinder());
		if (about.isFromFormatIsAvaliable("lavfi") == false) {
			throw new IOException("This ffmpeg (" + toolRun.getExecutableFinder().get(ffmpeg.getExecutableName()) + ") can't handle \"lavfi\"");
		}

		ffmpeg.setOverwriteOutputFiles();
		ffmpeg.setOnErrorDeleteOutFiles(true);

		parameters.addBulkParameters("-f lavfi -i smptehdbars=duration=" + duration_in_sec + ":size=" + resolution.x + "x" + resolution.y + ":rate=25");
		parameters.addBulkParameters("-f lavfi -i sine=frequency=1000:sample_rate=48000:duration=" + duration_in_sec);

		if (about.isCoderIsAvaliable("h264")) {
			ffmpeg.addVideoCodecName("h264", -1).addCRF(1);
		} else {
			ffmpeg.addVideoCodecName("ffv1", -1);
		}

		if (about.isCoderIsAvaliable("aac")) {
			ffmpeg.addAudioCodecName("aac", -1);
		} else {
			ffmpeg.addAudioCodecName("opus", -1);
		}

		// TODO set setOnTextOutEventExecutor(final Executor onTextOutEventExecutor) for logging

		return ffmpeg; /*.addFastStartMovMp4File()*/
	}

	/**
	 * Stateless
	 */
	public CompletableFuture<RunningTool<FFmpeg>> generateBarsAnd1k(final String destination, final int duration_in_sec, final Point resolution) throws IOException {
		final FFmpeg ffmpeg = internal(duration_in_sec, resolution);
		ffmpeg.addSimpleOutputDestination(destination);
		return toolRun.execute(ffmpeg);
	}

	/**
	 * Stateless
	 */
	public CompletableFuture<RunningTool<FFmpeg>> generateBarsAnd1k(final File destination, final int duration_in_sec, final Point resolution) throws IOException {
		final FFmpeg ffmpeg = internal(duration_in_sec, resolution);
		ffmpeg.addSimpleOutputDestination(destination);
		return toolRun.execute(ffmpeg);
	}

}
