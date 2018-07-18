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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FFVersion {
	// private static Logger log = LogManager.getLogger();
	
	// private final FFbase base;
	
	/**
	 * Like "4.0 Copyright (c) 2000-2018 the FFmpeg developers"
	 */
	public final String header_version;
	
	/**
	 * Like "gcc 6.3.0 (Debian 6.3.0-18+deb9u1) 20170516" or "gcc 7.3.0 (GCC)"
	 */
	public final String built_with;
	
	/**
	 * unmodifiableSet, like "yasm, gpl, version3, nonfree, libmp3lame, libbluray..."
	 */
	public final Set<String> configuration;
	
	/**
	 * Like "--as=yasm --enable-gpl --enable-version3 --enable-nonfree --enable-libmp3lame" ...
	 */
	public final String raw_configuration;
	
	/**
	 * Like "56. 14.100 / 56. 14.100"
	 */
	public final String libavutil_version;
	/**
	 * Like "56. 14.100 / 56. 14.100"
	 */
	public final String libavcodec_version;
	/**
	 * Like "56. 14.100 / 56. 14.100"
	 */
	public final String libavformat_version;
	/**
	 * Like "56. 14.100 / 56. 14.100"
	 */
	public final String libavdevice_version;
	/**
	 * Like "56. 14.100 / 56. 14.100"
	 */
	public final String libavfilter_version;
	/**
	 * Like "56. 14.100 / 56. 14.100"
	 */
	public final String libswscale_version;
	/**
	 * Like "56. 14.100 / 56. 14.100"
	 */
	public final String libswresample_version;
	/**
	 * Like "56. 14.100 / 56. 14.100"
	 */
	public final String libpostproc_version;
	
	FFVersion(List<String> process_result) {
		/*this.base = base;
		if (base == null) {
			throw new NullPointerException("\"base\" can't to be null");
		}*/
		
		header_version = process_result.stream().filter(l -> {
			return l.startsWith("ffmpeg version ");
		}).findFirst().orElse("ffmpeg version ?").substring("ffmpeg version ".length()).trim();
		
		built_with = process_result.stream().filter(l -> {
			return l.startsWith("built with ");
		}).findFirst().orElse("built with ?").substring("built with ".length()).trim();
		
		raw_configuration = process_result.stream().filter(l -> {
			return l.startsWith("configuration:");
		}).findFirst().orElse("configuration:").substring("configuration:".length()).trim();
		
		configuration = Collections.unmodifiableSet(Arrays.stream(raw_configuration.split(" ")).map(c -> {
			if (c.startsWith("--enable-")) {
				return c.substring("--enable-".length()).trim();
			} else if (c.startsWith("--as=")) {
				return c.substring("--as=".length()).trim();
			}
			return c.trim();
		}).distinct().collect(Collectors.toSet()));
		
		libavutil_version = extractLibavVersion("libavutil", process_result);
		libavcodec_version = extractLibavVersion("libavcodec", process_result);
		libavformat_version = extractLibavVersion("libavformat", process_result);
		libavdevice_version = extractLibavVersion("libavdevice", process_result);
		libavfilter_version = extractLibavVersion("libavfilter", process_result);
		libswscale_version = extractLibavVersion("libswscale", process_result);
		libswresample_version = extractLibavVersion("libswresample", process_result);
		libpostproc_version = extractLibavVersion("libpostproc", process_result);
	}
	
	/**
	 * @return header_version
	 */
	public String toString() {
		return header_version;
	}
	
	private static String extractLibavVersion(String key, List<String> lines) {
		
		String line = lines.stream().filter(l -> {
			return l.startsWith(key);
		}).findFirst().orElse(key + "      ?.?.?");
		
		/**
		 * libavutil 56. 14.100 / 56. 14.100
		 */
		return line.substring(key.length()).trim();
	}
	
}
