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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FFPixelFormat {

	static List<FFPixelFormat> parsePixelsFormats(final List<String> lines) {
		return lines.stream()
		        .map(String::trim)
		        .filter(line -> (line.toLowerCase().startsWith("Pixel formats:".toLowerCase()) == false))
		        .filter(line -> (line.contains("=") == false))
		        .filter(line -> (line.toLowerCase().startsWith("FLAGS".toLowerCase()) == false))
		        .filter(line -> (line.startsWith("-----") == false))
		        .map(FFPixelFormat::new)
		        .collect(Collectors.toUnmodifiableList());
	}

	public final boolean supportedInput;
	public final boolean supportedOutput;
	public final boolean hardwareAccelerated;
	public final boolean paletted;
	public final boolean bitstream;
	public final int nbComponents;
	public final int bitsPerPixel;
	public final String tag;

	FFPixelFormat(final String line) {

		final List<String> lineBlocs = Arrays.stream(line.split(" ")).filter(lb -> lb.trim().equals("") == false).map(
		        String::trim).collect(Collectors.toUnmodifiableList());

		if (lineBlocs.size() != 4) {
			throw new RuntimeException("Can't parse line: \"" + line + "\"");
		}

		supportedInput = lineBlocs.get(0).contains("I");
		supportedOutput = lineBlocs.get(0).contains("O");
		hardwareAccelerated = lineBlocs.get(0).contains("H");
		paletted = lineBlocs.get(0).contains("P");
		bitstream = lineBlocs.get(0).contains("B");
		tag = lineBlocs.get(1);
		nbComponents = Integer.parseInt(lineBlocs.get(2));
		bitsPerPixel = Integer.parseInt(lineBlocs.get(3));
	}

}
