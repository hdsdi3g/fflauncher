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
import java.util.List;
import java.util.stream.Collectors;

public class FFCodec {
	
	static List<FFCodec> parse(List<String> lines) {
		return lines.stream().map(line -> line.trim()).filter(line -> {
			return line.toLowerCase().startsWith("codecs:") == false;
		}).filter(line -> {
			return line.startsWith("-------") == false;
		}).filter(line -> {
			return line.indexOf("=") == -1;
		}).map(line -> new FFCodec(line)).collect(Collectors.toUnmodifiableList());
	}
	
	public enum CodecType {
		VIDEO, AUDIO, SUBTITLE, DATA;
	}
	
	public final boolean decoding_supported;
	public final boolean encoding_supported;
	public final CodecType type;
	public final boolean intra_frame_only;
	public final boolean lossy_compression;
	public final boolean lossless_compression;
	
	/**
	 * Like "dpx"
	 */
	public final String tag;
	
	/**
	 * Like "DPX (Digital Picture Exchange) image"
	 */
	public final String long_name;
	
	FFCodec(String line) {
		String[] line_blocs = line.split(" ");
		
		if (line_blocs.length < 3) {
			throw new RuntimeException("Can't parse line: \"" + line + "\"");
		}
		
		/**
		 * Parse "codec type zone"
		 */
		
		decoding_supported = line_blocs[0].charAt(0) == 'D';
		encoding_supported = line_blocs[0].charAt(1) == 'E';
		
		if (line_blocs[0].charAt(2) == 'V') {
			type = CodecType.VIDEO;
		} else if (line_blocs[0].charAt(2) == 'A') {
			type = CodecType.AUDIO;
		} else if (line_blocs[0].charAt(2) == 'S') {
			type = CodecType.SUBTITLE;
		} else if (line_blocs[0].charAt(2) == 'D') {
			type = CodecType.DATA;
		} else {
			throw new RuntimeException("Can't parse line: \"" + line + "\" (missing codec type)");
		}
		
		intra_frame_only = line_blocs[0].charAt(3) == 'I';
		lossy_compression = line_blocs[0].charAt(4) == 'L';
		lossless_compression = line_blocs[0].charAt(5) == 'S';
		
		if (line_blocs[0].substring(3).chars().noneMatch(i -> {
			return i == 'I' | i == 'L' | i == 'S' | i == '.';
		})) {
			throw new RuntimeException("Can't parse line: \"" + line + "\" (invalid ends for codec type)");
		}
		
		tag = line_blocs[1].trim();
		long_name = Arrays.stream(line_blocs).filter(lb -> lb.trim().equals("") == false).skip(2).collect(Collectors.joining(" "));
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(long_name);
		sb.append(" [");
		sb.append(tag);
		sb.append("] ");
		
		sb.append(type.toString().toLowerCase());
		
		if (decoding_supported & encoding_supported) {
			sb.append(" encoding and decoding supported");
		} else if (decoding_supported) {
			sb.append(" decoding only supported");
		} else {
			sb.append(" encoding only supported");
		}
		
		if (intra_frame_only) {
			sb.append(", intra frame-only codec");
		}
		if (lossy_compression) {
			sb.append(", lossy compression");
		}
		if (lossless_compression) {
			sb.append(", lossless compression");
		}
		
		return sb.toString();
	}
}
