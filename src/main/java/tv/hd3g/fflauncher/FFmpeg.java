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
package tv.hd3g.fflauncher;

import java.io.FileNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.CommandLineProcessor.CommandLine;
import tv.hd3g.execprocess.ExecutableFinder;

public class FFmpeg extends FFbase {
	
	private static final Logger log = LogManager.getLogger();
	
	public FFmpeg(ExecutableFinder exec_finder, CommandLine command_line) throws FileNotFoundException {
		super(exec_finder, command_line);
	}
	
	// overwrite = new Option("-y", "overwrite output files").createSwitch();
	// ignore_unknown = new Option("-ignore_unknown", "Ignore unknown stream types").createSwitch();
	// loglevel = new Option("-loglevel", "set logging level").setAlternateParams("-v").prepareEmptyValue();
	// stats = new Option("-stats", "print progress report during encoding").createSwitch();
	
	// TODO implements ffmpeg
	// check output file(s) if exists and not empty
}
