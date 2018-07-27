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

import tv.hd3g.execprocess.CommandLineProcessor;
import tv.hd3g.execprocess.CommandLineProcessor.CommandLine;
import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.fflauncher.FFmpeg;

public class GenerateVideoFile extends Recipe {
	private static Logger log = LogManager.getLogger();
	
	public GenerateVideoFile() {
		super();
	}
	
	public GenerateVideoFile(ExecutableFinder exec_finder, String exec_name) {
		super(exec_finder, exec_name);
	}
	
	public CompletableFuture<Void> generate(String destination) throws IOException {
		FFmpeg ffmpeg = new FFmpeg(getExecFinder(), new CommandLineProcessor().createEmptyCommandLine(getExecName()));
		
		// ffmpeg.about.getFilters()
		
		ffmpeg.setOverwriteOutputFiles();
		
		CommandLine cmd = ffmpeg.getCommandLine();
		cmd.addBulkParameters("-f lavfi -i smptebars=duration=5:size=1920x1080:rate=25");
		cmd.addParameters("-vf", "drawtext=\"fontsize=15:timecode='00\\:00\\:00\\:00':rate=25:fontsize=72:fontcolor='white':boxcolor=0x000000AA:box=1:x=1920/2:y=800\"");
		
		cmd.addBulkParameters("-codec:v ffv1"); // TODO add audio -codec:a opus
		
		ffmpeg.addSimpleOutputDestination(destination);
		
		ExecProcessText exec = ffmpeg.createExec();
		log.info("Generate test file to \"" + destination + "\"");
		
		return exec.start(getExecutionExecutor()).waitForEnd().thenAcceptAsync(result -> {
			result.checkExecution();
		}, getPostProcessExecutor());
	}
	
	protected String getDefaultExecName() {
		return "ffmpeg";
	}
	
}
