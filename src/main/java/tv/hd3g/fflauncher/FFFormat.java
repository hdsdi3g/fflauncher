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

public class FFFormat {
	
	static List<FFFormat> parseFormats(List<String> lines) {
		return lines.stream().map(line -> line.trim()).filter(line -> {
			return line.toLowerCase().startsWith("File formats:".toLowerCase()) == false;
		}).filter(line -> {
			return line.toLowerCase().startsWith("D. = Demuxing supported".toLowerCase()) == false;
		}).filter(line -> {
			return line.toLowerCase().startsWith(".E = Muxing supported".toLowerCase()) == false;
		}).filter(line -> {
			return line.startsWith("--") == false;
		}).map(line -> new FFFormat(line)).collect(Collectors.toUnmodifiableList());
	}
	
	public final boolean demuxing;
	public final boolean muxing;
	
	/**
	 * Like "asf"
	 */
	public final String name;
	
	/**
	 * Like "mov, mp4, m4a, 3gp, 3g2, mj2"
	 */
	public final Set<String> alternate_tags;
	
	/**
	 * Like "ASF (Advanced / Active Streaming Format)"
	 */
	public final String long_name;
	
	FFFormat(String line) {
		
		List<String> line_blocs = Arrays.stream(line.split(" ")).filter(lb -> lb.trim().equals("") == false).map(lb -> lb.trim()).collect(Collectors.toUnmodifiableList());
		
		if (line_blocs.size() < 3) {
			throw new RuntimeException("Can't parse line: \"" + line + "\"");
		}
		
		demuxing = line_blocs.get(0).trim().contains("D");
		muxing = line_blocs.get(0).trim().contains("E");
		
		if (line_blocs.get(1).contains(",")) {
			name = Arrays.stream(line_blocs.get(1).trim().split(",")).findFirst().get();
			alternate_tags = Collections.unmodifiableSet(Arrays.stream(line_blocs.get(1).trim().split(",")).collect(Collectors.toSet()));
		} else {
			name = line_blocs.get(1);
			alternate_tags = Collections.singleton(name);
		}
		
		long_name = line_blocs.stream().filter(lb -> lb.trim().equals("") == false).skip(2).collect(Collectors.joining(" "));
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(long_name);
		sb.append(" [");
		sb.append(name);
		if (alternate_tags.size() > 1) {
			sb.append(", ");
			sb.append(alternate_tags.stream().filter(t -> t.equals(name) == false).collect(Collectors.joining(", ")));
		}
		sb.append("] ");
		
		if (muxing & demuxing) {
			sb.append("muxing and demuxing supported");
		} else if (muxing) {
			sb.append("muxing only supported");
		} else {
			sb.append("demuxing only supported");
		}
		
		return sb.toString();
	}
}
