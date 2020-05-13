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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2018
 *
 */
package tv.hd3g.fflauncher;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static tv.hd3g.fflauncher.Channel.BC;
import static tv.hd3g.fflauncher.Channel.BL;
import static tv.hd3g.fflauncher.Channel.BR;
import static tv.hd3g.fflauncher.Channel.DL;
import static tv.hd3g.fflauncher.Channel.DR;
import static tv.hd3g.fflauncher.Channel.FC;
import static tv.hd3g.fflauncher.Channel.FL;
import static tv.hd3g.fflauncher.Channel.FLC;
import static tv.hd3g.fflauncher.Channel.FR;
import static tv.hd3g.fflauncher.Channel.FRC;
import static tv.hd3g.fflauncher.Channel.LFE;
import static tv.hd3g.fflauncher.Channel.SL;
import static tv.hd3g.fflauncher.Channel.SR;
import static tv.hd3g.fflauncher.Channel.TBC;
import static tv.hd3g.fflauncher.Channel.TBL;
import static tv.hd3g.fflauncher.Channel.TBR;
import static tv.hd3g.fflauncher.Channel.TFC;
import static tv.hd3g.fflauncher.Channel.TFL;
import static tv.hd3g.fflauncher.Channel.TFR;
import static tv.hd3g.fflauncher.Channel.WL;
import static tv.hd3g.fflauncher.Channel.WR;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.Exec;
import tv.hd3g.processlauncher.InvalidExecution;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

/**
 * Threadsafe
 * Sync (blocking) during executions
 */
public class FFAbout {

	private static final Logger log = LogManager.getLogger();

	private final String execName;
	private final ExecutableFinder executableFinder;
	private final ScheduledExecutorService maxExecTimeScheduler;

	FFAbout(final String execName, final ExecutableFinder executableFinder,
	        final ScheduledExecutorService maxExecTimeScheduler) {
		this.execName = Objects.requireNonNull(execName, "\"execName\" can't to be null");
		this.executableFinder = Objects.requireNonNull(executableFinder, "\"executableFinder\" can't to be null");
		this.maxExecTimeScheduler = Objects.requireNonNull(maxExecTimeScheduler,
		        "\"maxExecTimeScheduler\" can't to be null");
	}

	private CapturedStdOutErrTextRetention internalRun(final String bulkParameters) {
		try {
			final FFbase referer = new FFbase(execName, new Parameters(bulkParameters));
			referer.setMaxExecTimeScheduler(maxExecTimeScheduler);
			return new Exec(referer, executableFinder).runWaitGetText(null);
		} catch (final InvalidExecution e) {
			if (log.isDebugEnabled()) {
				log.debug("Can't execute " + execName + ", it return: {}", e.getStdErr());
			}
			throw e;
		} catch (final IOException e) {
			throw new RuntimeException("Can't execute " + execName, e);
		}
	}

	private FFVersion version;
	private List<FFCodec> codecs;
	private List<FFFormat> formats;
	private List<FFDevice> devices;
	private Set<String> bit_stream_filters;
	private FFProtocols protocols;
	private List<FFFilter> filters;
	private List<FFPixelFormat> pixels_formats;
	private Set<String> hardware_acceleration_methods;

	public synchronized FFVersion getVersion() {
		if (version == null) {
			version = new FFVersion(internalRun("-loglevel quiet -version").getStdouterrLines(false).map(String::trim)
			        .collect(Collectors.toUnmodifiableList()));
		}
		return version;
	}

	/**
	 * -codecs show available codecs
	 */
	public synchronized List<FFCodec> getCodecs() {
		if (codecs == null) {
			codecs = FFCodec.parse(internalRun("-codecs").getStdoutLines(false).map(String::trim).collect(Collectors
			        .toUnmodifiableList()));
		}
		return codecs;
	}

	/**
	 * -formats show available formats
	 */
	public synchronized List<FFFormat> getFormats() {
		if (formats == null) {
			formats = FFFormat.parseFormats(internalRun("-formats").getStdoutLines(false).map(String::trim).collect(
			        Collectors.toUnmodifiableList()));
		}
		return formats;
	}

	/**
	 * -devices show available devices
	 */
	public synchronized List<FFDevice> getDevices() {
		if (devices == null) {
			devices = FFDevice.parseDevices(internalRun("-devices").getStdoutLines(false).map(String::trim).collect(
			        Collectors.toUnmodifiableList()));
		}
		return devices;
	}

	static Set<String> parseBSFS(final Stream<String> lines) {
		return lines.map(String::trim).filter(line -> (line.toLowerCase().startsWith("Bitstream filters:"
		        .toLowerCase()) == false)).collect(Collectors.toSet());
	}

	/**
	 * -bsfs show available bit stream filters
	 */
	public synchronized Set<String> getBitStreamFilters() {
		if (bit_stream_filters == null) {
			bit_stream_filters = parseBSFS(internalRun("-bsfs").getStdoutLines(false).map(String::trim));
		}
		return bit_stream_filters;
	}

	/**
	 * -protocols show available protocols
	 */
	public synchronized FFProtocols getProtocols() {
		if (protocols == null) {
			protocols = new FFProtocols(internalRun("-protocols").getStdouterrLines(false).map(String::trim).collect(
			        Collectors.toUnmodifiableList()));
		}

		return protocols;
	}

	/**
	 * -filters show available filters
	 */
	public synchronized List<FFFilter> getFilters() {
		if (filters == null) {
			filters = FFFilter.parseFilters(internalRun("-filters").getStdoutLines(false).map(String::trim).collect(
			        Collectors.toUnmodifiableList()));
		}
		return filters;
	}

	/**
	 * -pix_fmts show available pixel formats
	 */
	public synchronized List<FFPixelFormat> getPixelFormats() {
		if (pixels_formats == null) {
			pixels_formats = FFPixelFormat.parsePixelsFormats(internalRun("-pix_fmts").getStdoutLines(false).map(
			        String::trim).collect(Collectors.toUnmodifiableList()));
		}
		return pixels_formats;
	}

	static Set<String> parseHWAccelerationMethods(final Stream<String> lines) {
		return lines.map(String::trim).filter(line -> (line.toLowerCase().startsWith("Hardware acceleration methods:"
		        .toLowerCase()) == false)).collect(Collectors.toSet());
	}

	/**
	 * -hwaccels show available HW acceleration methods
	 */
	public synchronized Set<String> getAvailableHWAccelerationMethods() {
		if (hardware_acceleration_methods == null) {
			hardware_acceleration_methods = parseHWAccelerationMethods(internalRun("-hwaccels").getStdoutLines(false)
			        .map(String::trim));
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
		final HashMap<String, Integer> sf = new HashMap<>();
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

		final HashMap<String, List<Channel>> cl = new HashMap<>();
		cl.put("mono           ".trim(), unmodifiableList(asList(FC)));
		cl.put("stereo         ".trim(), unmodifiableList(asList(FL, FR)));
		cl.put("2.1            ".trim(), unmodifiableList(asList(FL, FR, LFE)));
		cl.put("3.0            ".trim(), unmodifiableList(asList(FL, FR, FC)));
		cl.put("3.0(back)      ".trim(), unmodifiableList(asList(FL, FR, BC)));
		cl.put("4.0            ".trim(), unmodifiableList(asList(FL, FR, FC, BC)));
		cl.put("quad           ".trim(), unmodifiableList(asList(FL, FR, BL, BR)));
		cl.put("quad(side)     ".trim(), unmodifiableList(asList(FL, FR, SL, SR)));
		cl.put("3.1            ".trim(), unmodifiableList(asList(FL, FR, FC, LFE)));
		cl.put("5.0            ".trim(), unmodifiableList(asList(FL, FR, FC, BL, BR)));
		cl.put("5.0(side)      ".trim(), unmodifiableList(asList(FL, FR, FC, SL, SR)));
		cl.put("4.1            ".trim(), unmodifiableList(asList(FL, FR, FC, LFE, BC)));
		cl.put("5.1            ".trim(), unmodifiableList(asList(FL, FR, FC, LFE, BL, BR)));
		cl.put("5.1(side)      ".trim(), unmodifiableList(asList(FL, FR, FC, LFE, SL, SR)));
		cl.put("6.0            ".trim(), unmodifiableList(asList(FL, FR, FC, BC, SL, SR)));
		cl.put("6.0(front)     ".trim(), unmodifiableList(asList(FL, FR, FLC, FRC, SL, SR)));
		cl.put("hexagonal      ".trim(), unmodifiableList(asList(FL, FR, FC, BL, BR, BC)));
		cl.put("6.1            ".trim(), unmodifiableList(asList(FL, FR, FC, LFE, BC, SL, SR)));
		cl.put("6.1(back)      ".trim(), unmodifiableList(asList(FL, FR, FC, LFE, BL, BR, BC)));
		cl.put("6.1(front)     ".trim(), unmodifiableList(asList(FL, FR, LFE, FLC, FRC, SL, SR)));
		cl.put("7.0            ".trim(), unmodifiableList(asList(FL, FR, FC, BL, BR, SL, SR)));
		cl.put("7.0(front)     ".trim(), unmodifiableList(asList(FL, FR, FC, FLC, FRC, SL, SR)));
		cl.put("7.1            ".trim(), unmodifiableList(asList(FL, FR, FC, LFE, BL, BR, SL, SR)));
		cl.put("7.1(wide)      ".trim(), unmodifiableList(asList(FL, FR, FC, LFE, BL, BR, FLC, FRC)));
		cl.put("7.1(wide-side) ".trim(), unmodifiableList(asList(FL, FR, FC, LFE, FLC, FRC, SL, SR)));
		cl.put("octagonal      ".trim(), unmodifiableList(asList(FL, FR, FC, BL, BR, BC, SL, SR)));
		cl.put("hexadecagonal  ".trim(),
		        unmodifiableList(asList(FL, FR, FC, BL, BR, BC, SL, SR, TFL, TFC, TFR, TBL, TBC, TBR, WL, WR)));
		cl.put("downmix        ".trim(), unmodifiableList(asList(DL, DR)));
		channel_layouts = Collections.unmodifiableMap(cl);
	}

	public boolean isCoderIsAvaliable(final String codec_name) {
		return getCodecs().stream().anyMatch(codec -> (codec.name.equals(codec_name.toLowerCase())
		                                               & codec.encoding_supported == true));
	}

	public boolean isDecoderIsAvaliable(final String codec_name) {
		return getCodecs().stream().anyMatch(codec -> (codec.name.equals(codec_name.toLowerCase())
		                                               & codec.decoding_supported == true));
	}

	public boolean isFromFormatIsAvaliable(final String demuxer_name) {
		return getFormats().stream().anyMatch(format -> (format.name.equals(demuxer_name.toLowerCase())
		                                                 & format.demuxing == true));
	}

	public boolean isToFormatIsAvaliable(final String muxer_name) {
		return getFormats().stream().anyMatch(format -> (format.name.equals(muxer_name.toLowerCase())
		                                                 & format.muxing == true));
	}

	public boolean isFilterIsAvaliable(final String filter_name) {
		return getFilters().stream().anyMatch(filter -> filter.tag.equals(filter_name.toLowerCase()));
	}

	/**
	 * @param engine_name like libx264rgb or libxvid
	 *        ALL CODECS ARE NOT AVAILABLE FOR ALL GRAPHICS CARDS, EVEN IF FFMPEG SUPPORT IT HERE.
	 */
	public boolean isCoderEngineIsAvaliable(final String engine_name) {
		return getCodecs().stream().anyMatch(codec -> (codec.encoding_supported == true & codec.encoders.contains(
		        engine_name)));
	}

	/**
	 * @param engine_name like h264_cuvid or libopenjpeg
	 *        ALL CODECS ARE NOT AVAILABLE FOR ALL GRAPHICS CARDS, EVEN IF FFMPEG SUPPORT IT HERE.
	 */
	public boolean isDecoderEngineIsAvaliable(final String engine_name) {
		return getCodecs().stream().anyMatch(codec -> (codec.decoding_supported == true & codec.decoders.contains(
		        engine_name)));
	}

	/**
	 * ALL FUNCTIONS ARE NOT AVAILABLE FOR ALL GRAPHICS CARDS, EVEN IF FFMPEG SUPPORT IT HERE.
	 * @return true if configured and up for cuda, cuvid and nvenc
	 */
	public boolean isNVToolkitIsAvaliable() {
		if (getAvailableHWAccelerationMethods().contains("cuda") == false) {
			log.debug("(NVIDIA) Cuda is not available in hardware acceleration methods");
			return false;
		} else if (getAvailableHWAccelerationMethods().contains("cuvid") == false) {
			log.debug("(NVIDIA) Cuvid is not available in hardware acceleration methods");
			return false;
		}
		final List<String> all_nv_related_codecs = getCodecs().stream().filter(c -> c.decoders.isEmpty() == false
		                                                                            | c.encoders.isEmpty() == false)
		        .flatMap(c -> Stream.concat(c.decoders.stream(), c.encoders.stream())).distinct().filter(c -> c
		                .contains("nvenc") | c.contains("cuvid")).collect(Collectors.toList());

		if (all_nv_related_codecs.stream().noneMatch(c -> c.contains("nvenc"))) {
			log.debug("(NVIDIA) nvenc is not available in codec list");
			return false;
		} else if (all_nv_related_codecs.stream().noneMatch(c -> c.contains("cuvid"))) {
			log.debug("(NVIDIA) cuvid is not available in codec list");
			return false;
		}

		return true;
	}

	/**
	 * ALL FUNCTIONS ARE NOT AVAILABLE FOR ALL GRAPHICS CARDS, EVEN IF FFMPEG SUPPORT IT HERE.
	 * @return true if configured with NVIDIA Performance Primitives via libnpp
	 */
	public boolean isHardwareNVScalerFilterIsAvaliable() {
		return getVersion().configuration.contains("libnpp");
	}

}
