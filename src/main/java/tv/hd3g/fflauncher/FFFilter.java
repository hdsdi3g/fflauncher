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

public class FFFilter {

	static List<FFFilter> parseFilters(final List<String> lines) {
		return lines.stream()
		        .map(String::trim)
		        .filter(line -> (line.toLowerCase().startsWith("Filters:".toLowerCase()) == false))
		        .filter(line -> (line.startsWith("---") == false))
		        .filter(line -> (line.indexOf('=') == -1))
		        .map(FFFilter::new)
		        .collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Like "aeval"
	 */
	public final String tag;

	/**
	 * Like "Filter audio signal according to a specified expression."
	 */
	public final String long_name;

	public final boolean timeline_support;
	public final boolean slice_threading;
	public final boolean command_support;

	public enum ConnectorType {
		AUDIO,
		VIDEO,
		/**
		 * Dynamic number and/or type
		 */
		DYNAMIC,
		SOURCE_SINK;
	}

	public final ConnectorType source_connector;
	public final ConnectorType dest_connector;

	public final int source_connectors_count;
	public final int dest_connectors_count;

	FFFilter(final String line) {

		final List<String> line_blocs = Arrays.stream(line.split(" ")).filter(lb -> lb.trim().equals("") == false).map(
		        String::trim).collect(Collectors.toUnmodifiableList());

		if (line_blocs.size() < 4) {
			throw new RuntimeException("Can't parse line: \"" + line + "\"");
		}

		tag = line_blocs.get(1);
		long_name = line_blocs.stream().filter(lb -> lb.trim().equals("") == false).skip(3).collect(Collectors.joining(
		        " "));

		timeline_support = line_blocs.get(0).contains("T");
		slice_threading = line_blocs.get(0).contains("S");
		command_support = line_blocs.get(0).contains("C");

		final String filter_graph = line_blocs.get(2);

		final int pos = filter_graph.indexOf("->");
		final String s_source_connector = filter_graph.substring(0, pos);
		final String s_dest_connector = filter_graph.substring(pos + "->".length());

		if (s_source_connector.contains("A")) {
			source_connector = ConnectorType.AUDIO;
		} else if (s_source_connector.contains("V")) {
			source_connector = ConnectorType.VIDEO;
		} else if (s_source_connector.contains("N")) {
			source_connector = ConnectorType.DYNAMIC;
		} else if (s_source_connector.contains("|")) {
			source_connector = ConnectorType.SOURCE_SINK;
		} else {
			throw new RuntimeException("Invalid line : \"" + line + "\", invalid filter_graph source_connector");
		}

		if (s_dest_connector.contains("A")) {
			dest_connector = ConnectorType.AUDIO;
		} else if (s_dest_connector.contains("V")) {
			dest_connector = ConnectorType.VIDEO;
		} else if (s_dest_connector.contains("N")) {
			dest_connector = ConnectorType.DYNAMIC;
		} else if (s_dest_connector.contains("|")) {
			dest_connector = ConnectorType.SOURCE_SINK;
		} else {
			throw new RuntimeException("Invalid line : \"" + line + "\", invalid filter_graph source_connector");
		}

		source_connectors_count = s_source_connector.length();
		dest_connectors_count = s_dest_connector.length();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append(long_name);
		sb.append(" [");
		sb.append(tag);
		sb.append("] ");

		sb.append(source_connector.toString().toLowerCase());

		if (source_connectors_count > 1) {
			sb.append(" (");
			sb.append(source_connectors_count);
			sb.append(")");
		}

		sb.append(" -> ");
		sb.append(dest_connector.toString().toLowerCase());

		if (dest_connectors_count > 1) {
			sb.append(" (");
			sb.append(dest_connectors_count);
			sb.append(")");
		}

		if (timeline_support) {
			sb.append(" <timeline support>");
		}
		if (slice_threading) {
			sb.append(" <slice threading>");
		}
		if (command_support) {
			sb.append(" <command support>");
		}

		return sb.toString();
	}

}
