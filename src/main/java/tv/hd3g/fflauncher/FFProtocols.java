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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FFProtocols {

	/**
	 * Like async, bluray, httpproxy, unix...
	 */
	public final Set<String> input;

	/**
	 * Like gopher, md5, tee, sftp...
	 */
	public final Set<String> output;

	FFProtocols(final List<String> process_result) {
		input = process_result.stream()
		        .map(String::trim)
		        .filter(line -> (line.toLowerCase().startsWith("Input:".toLowerCase()) == false))
		        .takeWhile(line -> (line.toLowerCase().startsWith("Output:".toLowerCase()) == false))
		        .collect(Collectors.toSet());

		output = process_result.stream()
		        .map(String::trim)
		        .dropWhile(line -> (line.toLowerCase().startsWith("Output:".toLowerCase()) == false))
		        .filter(line -> (line.toLowerCase().startsWith("Output:".toLowerCase()) == false))
		        .collect(Collectors.toSet());
	}

}
