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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.CommandLineProcessor.CommandLine;
import tv.hd3g.execprocess.ExecProcessTextResult;
import tv.hd3g.execprocess.ExecutableFinder;

/**
 * Threadsafe
 */
public class FFAbout {
	
	private static Logger log = LogManager.getLogger();
	private final FFbase referer;
	
	FFAbout(ExecutableFinder exec_finder, CommandLine command_line) throws FileNotFoundException {
		referer = new FFbase(exec_finder, command_line);
	}
	
	/*
	#-sources device     list sources of the input device
	#-sinks device       list sinks of the output device
	*/
	
	private FFVersion version;
	private List<FFCodec> codecs;
	private List<FFFormat> formats;
	private List<FFDevice> devices;
	private Set<String> bit_stream_filters;
	private FFProtocols protocols;
	private List<FFFilter> filters;
	private List<FFPixelFormat> pixels_formats;
	private Set<String> hardware_acceleration_methods;
	
	public FFVersion getVersion() {
		synchronized (this) {
			if (version == null) {
				try {
					ExecProcessTextResult result = referer.createExecWithLimitedExecutionTime().addBulkParameters("-loglevel quiet -version").run().checkExecution();
					version = new FFVersion(result.getStdouterrLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
				} catch (IOException e) {
					throw new RuntimeException("Can't execute " + referer.executable.getName(), e);
				}
			}
		}
		return version;
	}
	
	/**
	 * -codecs show available codecs
	 */
	public List<FFCodec> getCodecs() {
		synchronized (this) {
			if (codecs == null) {
				try {
					ExecProcessTextResult result = referer.createExecWithLimitedExecutionTime().addBulkParameters("-codecs").run().checkExecution();
					codecs = FFCodec.parse(result.getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
				} catch (IOException e) {
					throw new RuntimeException("Can't execute " + referer.executable.getName(), e);
				}
			}
		}
		return codecs;
	}
	
	/**
	 * -formats show available formats
	 */
	public List<FFFormat> getFormats() {
		synchronized (this) {
			if (formats == null) {
				try {
					ExecProcessTextResult result = referer.createExecWithLimitedExecutionTime().addBulkParameters("-formats").run().checkExecution();
					formats = FFFormat.parseFormats(result.getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
				} catch (IOException e) {
					throw new RuntimeException("Can't execute " + referer.executable.getName(), e);
				}
			}
		}
		return formats;
	}
	
	/**
	 * -devices show available devices
	 */
	public List<FFDevice> getDevices() {
		synchronized (this) {
			if (devices == null) {
				try {
					ExecProcessTextResult result = referer.createExecWithLimitedExecutionTime().addBulkParameters("-devices").run().checkExecution();
					devices = FFDevice.parseDevices(result.getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
				} catch (IOException e) {
					throw new RuntimeException("Can't execute " + referer.executable.getName(), e);
				}
			}
		}
		return devices;
	}
	
	static Set<String> parseBSFS(Stream<String> lines) {
		return lines.map(l -> l.trim()).filter(line -> {
			return line.toLowerCase().startsWith("Bitstream filters:".toLowerCase()) == false;
		}).collect(Collectors.toSet());
	}
	
	/**
	 * -bsfs show available bit stream filters
	 */
	public Set<String> getBitStreamFilters() {
		synchronized (this) {
			if (bit_stream_filters == null) {
				try {
					ExecProcessTextResult result = referer.createExecWithLimitedExecutionTime().addBulkParameters("-bsfs").run().checkExecution();
					bit_stream_filters = parseBSFS(result.getStdoutLines(false).map(l -> l.trim()));
				} catch (IOException e) {
					throw new RuntimeException("Can't execute " + referer.executable.getName(), e);
				}
			}
		}
		return bit_stream_filters;
	}
	
	/**
	 * -protocols show available protocols
	 */
	public FFProtocols getProtocols() {
		synchronized (this) {
			if (protocols == null) {
				try {
					ExecProcessTextResult result = referer.createExecWithLimitedExecutionTime().addBulkParameters("-protocols").run().checkExecution();
					protocols = new FFProtocols(result.getStdouterrLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
				} catch (IOException e) {
					throw new RuntimeException("Can't execute " + referer.executable.getName(), e);
				}
			}
		}
		
		return protocols;
	}
	
	/**
	 * -filters show available filters
	 */
	public List<FFFilter> getFilters() {
		synchronized (this) {
			if (filters == null) {
				try {
					ExecProcessTextResult result = referer.createExecWithLimitedExecutionTime().addBulkParameters("-filters").run().checkExecution();
					filters = FFFilter.parseFilters(result.getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
				} catch (IOException e) {
					throw new RuntimeException("Can't execute " + referer.executable.getName(), e);
				}
			}
		}
		return filters;
	}
	
	/**
	 * -pix_fmts show available pixel formats
	 */
	public List<FFPixelFormat> getPixelFormats() {
		synchronized (this) {
			if (pixels_formats == null) {
				try {
					ExecProcessTextResult result = referer.createExecWithLimitedExecutionTime().addBulkParameters("-pix_fmts").run().checkExecution();
					pixels_formats = FFPixelFormat.parsePixelsFormats(result.getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
				} catch (IOException e) {
					throw new RuntimeException("Can't execute " + referer.executable.getName(), e);
				}
			}
		}
		return pixels_formats;
	}
	
	static Set<String> parseHWAccelerationMethods(Stream<String> lines) {
		return lines.map(l -> l.trim()).filter(line -> {
			return line.toLowerCase().startsWith("Hardware acceleration methods:".toLowerCase()) == false;
		}).collect(Collectors.toSet());
	}
	
	/**
	 * -hwaccels show available HW acceleration methods
	 */
	public Set<String> getAvailableHWAccelerationMethods() {
		synchronized (this) {
			if (hardware_acceleration_methods == null) {
				try {
					ExecProcessTextResult result = referer.createExecWithLimitedExecutionTime().addBulkParameters("-hwaccels").run().checkExecution();
					hardware_acceleration_methods = parseHWAccelerationMethods(result.getStdoutLines(false).map(l -> l.trim()));
				} catch (IOException e) {
					throw new RuntimeException("Can't execute " + referer.executable.getName(), e);
				}
			}
		}
		return hardware_acceleration_methods;
	}
	
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
	
	public boolean isCoderIsAvaliable(String codec_name) {
		return getCodecs().stream().anyMatch(codec -> {
			return codec.tag.equals(codec_name.toLowerCase()) & codec.encoding_supported == true;
		});
	}
	
	public boolean isDecoderIsAvaliable(String codec_name) {
		return getCodecs().stream().anyMatch(codec -> {
			return codec.tag.equals(codec_name.toLowerCase()) & codec.decoding_supported == true;
		});
	}
	
	public boolean isFromFormatIsAvaliable(String demuxer_name) {
		return getFormats().stream().anyMatch(format -> {
			return format.tag.equals(demuxer_name.toLowerCase()) & format.demuxing == true;
		});
	}
	
	public boolean isToFormatIsAvaliable(String muxer_name) {
		return getFormats().stream().anyMatch(format -> {
			return format.tag.equals(muxer_name.toLowerCase()) & format.muxing == true;
		});
	}
	
	public boolean isFilterIsAvaliable(String filter_name) {
		return getFilters().stream().anyMatch(filter -> {
			return filter.tag.equals(filter_name.toLowerCase());
		});
	}
	
}
