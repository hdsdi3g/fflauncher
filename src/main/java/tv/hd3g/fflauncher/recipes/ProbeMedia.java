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

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ffmpeg.ffprobe.FfprobeType;
import org.xml.sax.SAXException;

import tv.hd3g.execprocess.CommandLineProcessor;
import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.fflauncher.FFprobe;
import tv.hd3g.fflauncher.FFprobe.FFPrintFormat;

public class ProbeMedia extends Recipe {
	private static Logger log = LogManager.getLogger();
	
	public ProbeMedia() {
		super();
	}
	
	public ProbeMedia(ExecutableFinder exec_finder, String exec_name) {
		super(exec_finder, exec_name);
	}
	
	/**
	 * Get streams, format and chapters.
	 */
	CompletableFuture<FfprobeType> doAnalysing(String source) throws IOException {
		FFprobe ffprobe = new FFprobe(getExecFinder(), new CommandLineProcessor().createEmptyCommandLine(getExecName()));
		
		ffprobe.setPrintFormat(FFPrintFormat.xml).setShowStreams().setShowFormat().setShowChapters().isHidebanner();
		ffprobe.addSimpleInputSource(source);
		
		ExecProcessText exec = ffprobe.createExecWithLimitedExecutionTime();
		log.info("Queue \"" + source + "\" ffprobe analysing");
		
		return exec.start(getExecutionExecutor()).waitForEnd(getPostProcessExecutor()).thenApplyAsync(result -> {
			try {
				return FFprobe.fromXML(result.checkExecution().getStdout(false, System.lineSeparator()));
			} catch (JAXBException | ParserConfigurationException | SAXException | IOException e) {
				throw new RuntimeException("Can't analyst " + source, e);
			}
		}, getPostProcessExecutor());
	}
	
	protected String getDefaultExecName() {
		return "ffprobe";
	}
	
}
