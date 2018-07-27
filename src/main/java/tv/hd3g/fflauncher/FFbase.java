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
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tv.hd3g.execprocess.CommandLineProcessor;
import tv.hd3g.execprocess.CommandLineProcessor.CommandLine;
import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecutableFinder;

abstract class FFbase extends ConversionTool {
	
	private FFAbout about;
	
	public FFAbout getAbout() {
		synchronized (this) {
			if (about == null) {
				try {
					about = new FFAbout(exec_finder, new CommandLineProcessor().createCommandLine(executable.getPath()));
				} catch (FileNotFoundException e) {
					throw new RuntimeException("Can't init About", e);
				}
			}
		}
		return about;
	}
	
	protected void applyExecProcessCatcher(ExecProcessText exec_process) {
		if (exec_process.getEnvironmentVar("AV_LOG_FORCE_COLOR") == null) {
			exec_process.setEnvironmentVarIfNotFound("AV_LOG_FORCE_NOCOLOR", "1");
		}
		super.applyExecProcessCatcher(exec_process);
	}
	
	public FFbase(ExecutableFinder exec_finder, CommandLine command_line) throws FileNotFoundException {
		super(exec_finder, command_line);
		
	}
	
	/**
	 * Add like -loglevel repeat+level+verbose
	 */
	public FFbase addLogLevel(FFLogLevel level, boolean repeat, boolean display_level) {
		command_line.ifHasNotParameter(() -> {
			StringBuilder sb = new StringBuilder();
			if (repeat) {
				sb.append("repeat+");
			}
			if (display_level) {
				sb.append("level+");
			}
			sb.append(level.name());
			command_line.prependParameters("-loglevel", sb.toString());
		}, "-loglevel", "-v");
		
		return this;
	}
	
	public boolean isLogLevelSet() {
		return command_line.hasParameters("-loglevel", "-v");
	}
	
	public FFbase setHidebanner() {
		command_line.ifHasNotParameter(() -> {
			command_line.prependParameters("-hide_banner");
		}, "-hide_banner");
		return this;
	}
	
	public boolean isHidebanner() {
		return command_line.hasParameters("-hide_banner");
	}
	
	public FFbase setOverwriteOutputFiles() {
		command_line.ifHasNotParameter(() -> {
			command_line.prependParameters("-y");
		}, "-y");
		return this;
	}
	
	public boolean isOverwriteOutputFiles() {
		return command_line.hasParameters("-y");
	}
	
	public FFbase setNeverOverwriteOutputFiles() {
		command_line.ifHasNotParameter(() -> {
			command_line.prependParameters("-n");
		}, "-n");
		return this;
	}
	
	public boolean isNeverOverwriteOutputFiles() {
		return command_line.hasParameters("-n");
	}
	
	/**
	 * Define cmd var name like <%IN_AUTOMATIC_n%> with "n" the # of setted sources.
	 * Add -i parameter
	 */
	public FFbase addSimpleInputSource(String source_name, String... source_options) {
		if (source_name == null) {
			throw new NullPointerException("\"source_name\" can't to be null");
		}
		
		Stream<String> s_source_options = Stream.empty();
		if (source_options != null) {
			s_source_options = Arrays.stream(source_options);
		}
		
		String varname = command_line.addVariable("IN_AUTOMATIC_" + input_sources.size());
		addInputSource(source_name, varname, Stream.concat(s_source_options, Stream.of("-i")).collect(Collectors.toList()), Collections.emptyList());
		
		return this;
	}
	
}
