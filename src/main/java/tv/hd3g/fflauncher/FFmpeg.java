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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.CommandLineProcessor.CommandLine;
import tv.hd3g.execprocess.ExecutableFinder;

public class FFmpeg extends FFbase {
	
	private static final Logger log = LogManager.getLogger();
	
	public FFmpeg(ExecutableFinder exec_finder, CommandLine command_line) throws FileNotFoundException {
		super(exec_finder, command_line);
	}
	
	/**
	 * Define cmd var name like <%OUT_AUTOMATIC_n%> with "n" the # of setted destination.
	 * Add -i parameter
	 */
	public FFbase addSimpleOutputDestination(String destination_name, String destination_container) {
		if (destination_name == null) {
			throw new NullPointerException("\"destination_name\" can't to be null");
		} else if (destination_container == null) {
			throw new NullPointerException("\"destination_container\" can't to be null");
		}
		
		/*Stream<String> s_source_options = Stream.empty();
		if (source_options != null) {
			s_source_options = Arrays.stream(source_options);
		}*/
		
		String varname = command_line.addVariable("OUT_AUTOMATIC_" + output_expected_destinations.size());
		addOutputDestination(destination_name, varname, "-f", destination_container);
		return this;
	}
	
	/**
	 * Add "-movflags faststart"
	 * Please, put it a the end of command line, before output stream.
	 */
	public FFmpeg addFastStartMovMp4File() {
		command_line.addBulkParameters("-movflags faststart");
		return this;
	}
	
	/**
	 * Define cmd var name like <%OUT_AUTOMATIC_n%> with "n" the # of setted destination.
	 * Add -i parameter
	 */
	public FFmpeg addSimpleOutputDestination(String destination_name) {
		if (destination_name == null) {
			throw new NullPointerException("\"destination_name\" can't to be null");
		}
		
		/*Stream<String> s_source_options = Stream.empty();
		if (source_options != null) {
			s_source_options = Arrays.stream(source_options);
		}*/
		
		String varname = command_line.addVariable("OUT_AUTOMATIC_" + output_expected_destinations.size());
		addOutputDestination(destination_name, varname);
		return this;
	}
	
	/**
	 * Don't need to be executed before, only checks.
	 */
	public LinkedHashMap<File, Boolean> checkOutputfilesPresence() {
		output_expected_destinations.stream().filter(dest -> {
			return false;// XXX is file ?
		});
		
		return null; // XXX
	}
	
	// TODO check output file(s) if exists (only if file is expected) and not empty after exec
	// TODO what is ffmetadata in ffmpeg's formats ?
	
}
