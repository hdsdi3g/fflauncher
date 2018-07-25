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
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tv.hd3g.execprocess.CommandLineProcessor.CommandLine;
import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecProcessTextResult;
import tv.hd3g.execprocess.ExecutableFinder;

abstract class FFbase extends ConversionTool {
	
	/**
	 * Get by ffmpeg -sample_fmts
	 */
	public static final Map<String, Integer> sample_formats;
	
	/**
	 * Get by ffmpeg -layouts
	 */
	public static final Map<String, List<Channel>> channel_layouts;
	
	static {
		HashMap<String, Integer> sf = new HashMap<>();
		sf.put("u8", 8);
		sf.put("s16", 16);
		sf.put("s32", 32);
		sf.put("flt", 32);
		sf.put("dbl", 64);
		sf.put("u8p", 8);
		sf.put("s16p", 16);
		sf.put("s32p", 32);
		sf.put("fltp", 32);
		sf.put("dblp", 64);
		sf.put("s64", 64);
		sf.put("s64p", 64);
		sample_formats = Collections.unmodifiableMap(sf);
		
		HashMap<String, List<Channel>> cl = new HashMap<>();
		cl.put("mono           ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FC)));
		cl.put("stereo         ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR)));
		cl.put("2.1            ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.LFE)));
		cl.put("3.0            ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC)));
		cl.put("3.0(back)      ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.BC)));
		cl.put("4.0            ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.BC)));
		cl.put("quad           ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.BL, Channel.BR)));
		cl.put("quad(side)     ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.SL, Channel.SR)));
		cl.put("3.1            ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.LFE)));
		cl.put("5.0            ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.BL, Channel.BR)));
		cl.put("5.0(side)      ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.SL, Channel.SR)));
		cl.put("4.1            ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.LFE, Channel.BC)));
		cl.put("5.1            ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.LFE, Channel.BL, Channel.BR)));
		cl.put("5.1(side)      ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.LFE, Channel.SL, Channel.SR)));
		cl.put("6.0            ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.BC, Channel.SL, Channel.SR)));
		cl.put("6.0(front)     ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FLC, Channel.FRC, Channel.SL, Channel.SR)));
		cl.put("hexagonal      ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.BL, Channel.BR, Channel.BC)));
		cl.put("6.1            ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.LFE, Channel.BC, Channel.SL, Channel.SR)));
		cl.put("6.1(back)      ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.LFE, Channel.BL, Channel.BR, Channel.BC)));
		cl.put("6.1(front)     ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.LFE, Channel.FLC, Channel.FRC, Channel.SL, Channel.SR)));
		cl.put("7.0            ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.BL, Channel.BR, Channel.SL, Channel.SR)));
		cl.put("7.0(front)     ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.FLC, Channel.FRC, Channel.SL, Channel.SR)));
		cl.put("7.1            ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.LFE, Channel.BL, Channel.BR, Channel.SL, Channel.SR)));
		cl.put("7.1(wide)      ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.LFE, Channel.BL, Channel.BR, Channel.FLC, Channel.FRC)));
		cl.put("7.1(wide-side) ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.LFE, Channel.FLC, Channel.FRC, Channel.SL, Channel.SR)));
		cl.put("octagonal      ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.BL, Channel.BR, Channel.BC, Channel.SL, Channel.SR)));
		cl.put("hexadecagonal  ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.FL, Channel.FR, Channel.FC, Channel.BL, Channel.BR, Channel.BC, Channel.SL, Channel.SR, Channel.TFL, Channel.TFC, Channel.TFR, Channel.TBL, Channel.TBC, Channel.TBR, Channel.WL, Channel.WR)));
		cl.put("downmix        ".trim(), Collections.unmodifiableList(Arrays.asList(Channel.DL, Channel.DR)));
		channel_layouts = Collections.unmodifiableMap(cl);
	}
	
	static Set<String> parseHWAccelerationMethods(Stream<String> lines) {
		return lines.map(l -> l.trim()).filter(line -> {
			return line.toLowerCase().startsWith("Hardware acceleration methods:".toLowerCase()) == false;
		}).collect(Collectors.toSet());
	}
	
	static Set<String> parseBSFS(Stream<String> lines) {
		return lines.map(l -> l.trim()).filter(line -> {
			return line.toLowerCase().startsWith("Bitstream filters:".toLowerCase()) == false;
		}).collect(Collectors.toSet());
	}
	
	public final About about = new About();
	/*
	#-sources device     list sources of the input device
	#-sinks device       list sinks of the output device
	*/
	
	public class About {
		
		private About() {
		}
		
		public FFVersion getVersion() throws IOException {
			ExecProcessTextResult result = checkExecution(createExec(true).addBulkParameters("-loglevel quiet -version").run());
			
			return new FFVersion(result.getStdouterrLines(false).map(l -> l.trim()).collect(Collectors.toList()));
		}
		
		/**
		 * -codecs show available codecs
		 */
		public List<FFCodec> getCodecs() throws IOException {
			ExecProcessTextResult result = checkExecution(createExec(true).addBulkParameters("-codecs").run());
			
			return FFCodec.parse(result.getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toList()));
		}
		
		/**
		 * -formats show available formats
		 */
		public List<FFFormat> getFormats() throws IOException {
			ExecProcessTextResult result = checkExecution(createExec(true).addBulkParameters("-formats").run());
			
			return FFFormat.parseFormats(result.getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toList()));
		}
		
		/**
		 * -devices show available devices
		 */
		public List<FFDevice> getDevices() throws IOException {
			ExecProcessTextResult result = checkExecution(createExec(true).addBulkParameters("-devices").run());
			
			return FFDevice.parseDevices(result.getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toList()));
		}
		
		/**
		 * -bsfs show available bit stream filters
		 */
		public Set<String> getBitStreamFilters() throws IOException {
			ExecProcessTextResult result = checkExecution(createExec(true).addBulkParameters("-bsfs").run());
			
			return parseBSFS(result.getStdoutLines(false).map(l -> l.trim()));
		}
		
		/**
		 * -protocols show available protocols
		 */
		public FFProtocols getProtocols() throws IOException {
			ExecProcessTextResult result = checkExecution(createExec(true).addBulkParameters("-protocols").run());
			
			return new FFProtocols(result.getStdouterrLines(false).map(l -> l.trim()).collect(Collectors.toList()));
		}
		
		/**
		 * -filters show available filters
		 */
		public List<FFFilter> getFilters() throws IOException {
			ExecProcessTextResult result = checkExecution(createExec(true).addBulkParameters("-filters").run());
			
			return FFFilter.parseFilters(result.getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toList()));
		}
		
		/**
		 * -pix_fmts show available pixel formats
		 */
		public List<FFPixelFormat> getPixelFormats() throws IOException {
			ExecProcessTextResult result = checkExecution(createExec(true).addBulkParameters("-pix_fmts").run());
			
			return FFPixelFormat.parsePixelsFormats(result.getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toList()));
		}
		
		/**
		 * -hwaccels show available HW acceleration methods
		 */
		public Set<String> getAvailableHWAccelerationMethods() throws IOException {
			ExecProcessTextResult result = checkExecution(createExec(true).addBulkParameters("-pix_fmts").run());
			
			return parseHWAccelerationMethods(result.getStdoutLines(false).map(l -> l.trim()));
		}
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
