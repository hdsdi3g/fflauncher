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
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

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

public class ProbeMedia {
	private static Logger log = LogManager.getLogger();
	
	private final ExecutableFinder exec_finder;
	private final String exec_name;
	private final Executor executor;
	
	public ProbeMedia(ExecutableFinder exec_finder, String exec_name, Executor executor) {
		this.exec_finder = exec_finder;
		if (exec_finder == null) {
			throw new NullPointerException("\"exec_finder\" can't to be null");
		}
		this.exec_name = exec_name;
		if (exec_name == null) {
			throw new NullPointerException("\"exec_name\" can't to be null");
		}
		this.executor = executor;
		if (executor == null) {
			throw new NullPointerException("\"executor\" can't to be null");
		}
	}
	
	/**
	 * Use ffprobe as exec_name
	 */
	public ProbeMedia(ExecutableFinder exec_finder, Executor executor) {
		this(exec_finder, "ffprobe", executor);
	}
	
	/**
	 * ForkJoinPool as executor
	 */
	public ProbeMedia(ExecutableFinder exec_finder, String exec_name) {
		this(exec_finder, exec_name, ForkJoinPool.commonPool());
	}
	
	/**
	 * Use ffprobe as exec_name
	 */
	public ProbeMedia(Executor executor) {
		this(new ExecutableFinder(), "ffprobe", executor);
	}
	
	/**
	 * Use ffprobe as exec_name and ForkJoinPool as executor
	 */
	public ProbeMedia() {
		this(new ExecutableFinder(), "ffprobe", ForkJoinPool.commonPool());
	}
	
	/**
	 * Get streams, format and chapters.
	 */
	CompletableFuture<FfprobeType> doAnalysing(String source) throws IOException {
		FFprobe ffprobe = new FFprobe(exec_finder, new CommandLineProcessor().createEmptyCommandLine(exec_name));
		
		ffprobe.setPrintFormat(FFPrintFormat.xml).setShowStreams().setShowFormat().setShowChapters().isHidebanner();
		ffprobe.addSimpleInputSource(source);
		
		ExecProcessText exec = ffprobe.createExec(true);
		log.info("Queue \"" + source + "\" ffprobe analysing");
		
		return exec.start(executor).waitForEnd(executor).thenApplyAsync(result -> {
			try {
				return FFprobe.fromXML(result.getStdout(false, System.lineSeparator()));
			} catch (JAXBException | ParserConfigurationException | SAXException | IOException e) {
				throw new RuntimeException("Can't analyst " + source, e);
			}
		}, executor);
	}
	
}
