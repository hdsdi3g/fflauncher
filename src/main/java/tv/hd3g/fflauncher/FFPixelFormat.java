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

public class FFPixelFormat {
	
	static List<FFPixelFormat> parsePixelsFormats(List<String> lines) {
		return lines.stream().map(line -> line.trim()).filter(line -> {
			return line.toLowerCase().startsWith("Pixel formats:".toLowerCase()) == false;
		}).filter(line -> {
			return line.contains("=") == false;
		}).filter(line -> {
			return line.toLowerCase().startsWith("FLAGS".toLowerCase()) == false;
		}).filter(line -> {
			return line.startsWith("-----") == false;
		}).map(line -> new FFPixelFormat(line)).collect(Collectors.toUnmodifiableList());
	}
	
	public final boolean supported_input;
	public final boolean supported_output;
	public final boolean hardware_accelerated;
	public final boolean paletted;
	public final boolean bitstream;
	public final int nb_components;
	public final int bits_per_pixel;
	public final String tag;
	
	FFPixelFormat(String line) {
		
		List<String> line_blocs = Arrays.stream(line.split(" ")).filter(lb -> lb.trim().equals("") == false).map(lb -> lb.trim()).collect(Collectors.toUnmodifiableList());
		
		if (line_blocs.size() != 4) {
			throw new RuntimeException("Can't parse line: \"" + line + "\"");
		}
		
		supported_input = line_blocs.get(0).contains("I");
		supported_output = line_blocs.get(0).contains("O");
		hardware_accelerated = line_blocs.get(0).contains("H");
		paletted = line_blocs.get(0).contains("P");
		bitstream = line_blocs.get(0).contains("B");
		tag = line_blocs.get(1);
		nb_components = Integer.parseInt(line_blocs.get(2));
		bits_per_pixel = Integer.parseInt(line_blocs.get(3));
	}
	
}
