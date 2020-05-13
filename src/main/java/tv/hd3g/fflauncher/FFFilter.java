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
	public final String longName;

	public final boolean timelineSupport;
	public final boolean sliceThreading;
	public final boolean commandSupport;

	public enum ConnectorType {
		AUDIO,
		VIDEO,
		/**
		 * Dynamic number and/or type
		 */
		DYNAMIC,
		SOURCE_SINK;
	}

	public final ConnectorType sourceConnector;
	public final ConnectorType destConnector;

	public final int sourceConnectorsCount;
	public final int destConnectorsCount;

	FFFilter(final String line) {

		final List<String> lineBlocs = Arrays.stream(line.split(" "))
		        .filter(lb -> lb.trim().equals("") == false)
		        .map(String::trim)
		        .collect(Collectors.toUnmodifiableList());

		if (lineBlocs.size() < 4) {
			throw new UnknownFormatException("Can't parse line: \"" + line + "\"");
		}

		tag = lineBlocs.get(1);
		longName = lineBlocs.stream()
		        .filter(lb -> lb.trim().equals("") == false)
		        .skip(3)
		        .collect(Collectors.joining(" "));

		timelineSupport = lineBlocs.get(0).contains("T");
		sliceThreading = lineBlocs.get(0).contains("S");
		commandSupport = lineBlocs.get(0).contains("C");

		final String filter_graph = lineBlocs.get(2);

		final int pos = filter_graph.indexOf("->");
		final String s_source_connector = filter_graph.substring(0, pos);
		final String s_dest_connector = filter_graph.substring(pos + "->".length());

		if (s_source_connector.contains("A")) {
			sourceConnector = ConnectorType.AUDIO;
		} else if (s_source_connector.contains("V")) {
			sourceConnector = ConnectorType.VIDEO;
		} else if (s_source_connector.contains("N")) {
			sourceConnector = ConnectorType.DYNAMIC;
		} else if (s_source_connector.contains("|")) {
			sourceConnector = ConnectorType.SOURCE_SINK;
		} else {
			throw new UnknownFormatException("Invalid line : \"" + line + "\", invalid filter_graph sourceConnector");
		}

		if (s_dest_connector.contains("A")) {
			destConnector = ConnectorType.AUDIO;
		} else if (s_dest_connector.contains("V")) {
			destConnector = ConnectorType.VIDEO;
		} else if (s_dest_connector.contains("N")) {
			destConnector = ConnectorType.DYNAMIC;
		} else if (s_dest_connector.contains("|")) {
			destConnector = ConnectorType.SOURCE_SINK;
		} else {
			throw new UnknownFormatException("Invalid line : \"" + line + "\", invalid filter_graph sourceConnector");
		}

		sourceConnectorsCount = s_source_connector.length();
		destConnectorsCount = s_dest_connector.length();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append(longName);
		sb.append(" [");
		sb.append(tag);
		sb.append("] ");

		sb.append(sourceConnector.toString().toLowerCase());

		if (sourceConnectorsCount > 1) {
			sb.append(" (");
			sb.append(sourceConnectorsCount);
			sb.append(")");
		}

		sb.append(" -> ");
		sb.append(destConnector.toString().toLowerCase());

		if (destConnectorsCount > 1) {
			sb.append(" (");
			sb.append(destConnectorsCount);
			sb.append(")");
		}

		if (timelineSupport) {
			sb.append(" <timeline support>");
		}
		if (sliceThreading) {
			sb.append(" <slice threading>");
		}
		if (commandSupport) {
			sb.append(" <command support>");
		}

		return sb.toString();
	}

}
