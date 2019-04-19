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

import java.io.IOException;
import java.util.Arrays;
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

import tv.hd3g.processlauncher.Exec;
import tv.hd3g.processlauncher.InvalidExecution;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextRetention;

/**
 * Threadsafe
 * Sync (blocking) during executions
 */
public class FFAbout {

	private static final Logger log = LogManager.getLogger();

	private final String execName;
	private final ExecutableFinder executableFinder;
	private final ScheduledExecutorService maxExecTimeScheduler;

	FFAbout(final String execName, final ExecutableFinder executableFinder, final ScheduledExecutorService maxExecTimeScheduler) throws IOException {
		this.execName = Objects.requireNonNull(execName, "\"execName\" can't to be null");
		this.executableFinder = Objects.requireNonNull(executableFinder, "\"executableFinder\" can't to be null");
		this.maxExecTimeScheduler = Objects.requireNonNull(maxExecTimeScheduler, "\"maxExecTimeScheduler\" can't to be null");
	}

	/*
	#-sources device     list sources of the input device
	#-sinks device       list sinks of the output device
	*/

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
			version = new FFVersion(internalRun("-loglevel quiet -version").getStdouterrLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
		}
		return version;
	}

	/**
	 * -codecs show available codecs
	 */
	public synchronized List<FFCodec> getCodecs() {
		if (codecs == null) {
			codecs = FFCodec.parse(internalRun("-codecs").getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
		}
		return codecs;
	}

	/**
	 * -formats show available formats
	 */
	public synchronized List<FFFormat> getFormats() {
		if (formats == null) {
			formats = FFFormat.parseFormats(internalRun("-formats").getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
		}
		return formats;
	}

	/**
	 * -devices show available devices
	 */
	public synchronized List<FFDevice> getDevices() {
		if (devices == null) {
			devices = FFDevice.parseDevices(internalRun("-devices").getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
		}
		return devices;
	}

	static Set<String> parseBSFS(final Stream<String> lines) {
		return lines.map(l -> l.trim()).filter(line -> {
			return line.toLowerCase().startsWith("Bitstream filters:".toLowerCase()) == false;
		}).collect(Collectors.toSet());
	}

	/**
	 * -bsfs show available bit stream filters
	 */
	public synchronized Set<String> getBitStreamFilters() {
		if (bit_stream_filters == null) {
			bit_stream_filters = parseBSFS(internalRun("-bsfs").getStdoutLines(false).map(l -> l.trim()));
		}
		return bit_stream_filters;
	}

	/**
	 * -protocols show available protocols
	 */
	public synchronized FFProtocols getProtocols() {
		if (protocols == null) {
			protocols = new FFProtocols(internalRun("-protocols").getStdouterrLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
		}

		return protocols;
	}

	/**
	 * -filters show available filters
	 */
	public synchronized List<FFFilter> getFilters() {
		if (filters == null) {
			filters = FFFilter.parseFilters(internalRun("-filters").getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
		}
		return filters;
	}

	/**
	 * -pix_fmts show available pixel formats
	 */
	public synchronized List<FFPixelFormat> getPixelFormats() {
		if (pixels_formats == null) {
			pixels_formats = FFPixelFormat.parsePixelsFormats(internalRun("-pix_fmts").getStdoutLines(false).map(l -> l.trim()).collect(Collectors.toUnmodifiableList()));
		}
		return pixels_formats;
	}

	static Set<String> parseHWAccelerationMethods(final Stream<String> lines) {
		return lines.map(l -> l.trim()).filter(line -> {
			return line.toLowerCase().startsWith("Hardware acceleration methods:".toLowerCase()) == false;
		}).collect(Collectors.toSet());
	}

	/**
	 * -hwaccels show available HW acceleration methods
	 */
	public synchronized Set<String> getAvailableHWAccelerationMethods() {
		if (hardware_acceleration_methods == null) {
			hardware_acceleration_methods = parseHWAccelerationMethods(internalRun("-hwaccels").getStdoutLines(false).map(l -> l.trim()));
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

	public boolean isCoderIsAvaliable(final String codec_name) {
		return getCodecs().stream().anyMatch(codec -> {
			return codec.name.equals(codec_name.toLowerCase()) & codec.encoding_supported == true;
		});
	}

	public boolean isDecoderIsAvaliable(final String codec_name) {
		return getCodecs().stream().anyMatch(codec -> {
			return codec.name.equals(codec_name.toLowerCase()) & codec.decoding_supported == true;
		});
	}

	public boolean isFromFormatIsAvaliable(final String demuxer_name) {
		return getFormats().stream().anyMatch(format -> {
			return format.name.equals(demuxer_name.toLowerCase()) & format.demuxing == true;
		});
	}

	public boolean isToFormatIsAvaliable(final String muxer_name) {
		return getFormats().stream().anyMatch(format -> {
			return format.name.equals(muxer_name.toLowerCase()) & format.muxing == true;
		});
	}

	public boolean isFilterIsAvaliable(final String filter_name) {
		return getFilters().stream().anyMatch(filter -> {
			return filter.tag.equals(filter_name.toLowerCase());
		});
	}

	/**
	 * @param engine_name like libx264rgb or libxvid
	 *        ALL CODECS ARE NOT AVAILABLE FOR ALL GRAPHICS CARDS, EVEN IF FFMPEG SUPPORT IT HERE.
	 */
	public boolean isCoderEngineIsAvaliable(final String engine_name) {
		return getCodecs().stream().anyMatch(codec -> {
			return codec.encoding_supported == true & codec.encoders.contains(engine_name);
		});
	}

	/**
	 * @param engine_name like h264_cuvid or libopenjpeg
	 *        ALL CODECS ARE NOT AVAILABLE FOR ALL GRAPHICS CARDS, EVEN IF FFMPEG SUPPORT IT HERE.
	 */
	public boolean isDecoderEngineIsAvaliable(final String engine_name) {
		return getCodecs().stream().anyMatch(codec -> {
			return codec.decoding_supported == true & codec.decoders.contains(engine_name);
		});
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
		final List<String> all_nv_related_codecs = getCodecs().stream().filter(c -> c.decoders.isEmpty() == false | c.encoders.isEmpty() == false).flatMap(c -> {
			return Stream.concat(c.decoders.stream(), c.encoders.stream());
		}).distinct().filter(c -> c.contains("nvenc") | c.contains("cuvid")).collect(Collectors.toList());

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
