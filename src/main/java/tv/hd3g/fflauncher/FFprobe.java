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

import tv.hd3g.processlauncher.cmdline.Parameters;

public class FFprobe extends FFbase {

	public FFprobe(final String execName, final Parameters parameters) {
		super(execName, parameters);
	}

	/**
	 * -pretty prettify the format of displayed values, make it more human readable
	 */
	public FFprobe setPretty() {
		getInternalParameters().ifHasNotParameter(() -> {
			getInternalParameters().addParameters("-pretty");
		}, "-pretty");
		return this;
	}

	public boolean isPretty() {
		return getInternalParameters().hasParameters("-pretty");
	}

	public enum FFPrintFormat {
		_default {
			@Override
			public String toString() {
				return "default";
			}
		},
		compact, csv, flat, ini, json, xml;
	}

	/**
	 * -print_format format set the output printing format
	 */
	public FFprobe setPrintFormat(final FFPrintFormat print_format) {
		getInternalParameters().ifHasNotParameter(() -> {
			getInternalParameters().addParameters("-print_format", print_format.toString().toLowerCase());
		}, "-print_format", "-of");
		return this;
	}

	public boolean hasPrintFormat() {
		return getInternalParameters().hasParameters("-print_format", "-of");
	}

	/**
	 * -show_format show format/container info
	 * @return
	 */
	public FFprobe setShowFormat() {
		getInternalParameters().ifHasNotParameter(() -> {
			getInternalParameters().addParameters("-show_format");
		}, "-show_format");
		return this;
	}

	public boolean isShowFormat() {
		return getInternalParameters().hasParameters("-show_format");
	}

	/**
	 * -show_data show packets data
	 */
	public FFprobe setShowData() {
		getInternalParameters().ifHasNotParameter(() -> {
			getInternalParameters().addParameters("-show_data");
		}, "-show_data");
		return this;
	}

	public boolean isShowData() {
		return getInternalParameters().hasParameters("-show_data");
	}

	/**
	 * -show_error show probing error
	 */
	public FFprobe setShowError() {
		getInternalParameters().ifHasNotParameter(() -> {
			getInternalParameters().addParameters("-show_error");
		}, "-show_error");
		return this;
	}

	public boolean isShowError() {
		return getInternalParameters().hasParameters("-show_error");
	}

	/**
	 * -show_frames show frames info
	 */
	public FFprobe setShowFrames() {
		getInternalParameters().ifHasNotParameter(() -> {
			getInternalParameters().addParameters("-show_frames");
		}, "-show_frames");
		return this;
	}

	public boolean isShowFrames() {
		return getInternalParameters().hasParameters("-show_frames");
	}

	/**
	 * -show_log show log
	 */
	public FFprobe setShowLog() {
		getInternalParameters().ifHasNotParameter(() -> {
			getInternalParameters().addParameters("-show_log");
		}, "-show_log");
		return this;
	}

	public boolean isShowLog() {
		return getInternalParameters().hasParameters("-show_log");
	}

	/**
	 * -show_packets show packets info
	 */
	public FFprobe setShowPackets() {
		getInternalParameters().ifHasNotParameter(() -> {
			getInternalParameters().addParameters("-show_packets");
		}, "-show_packets");
		return this;
	}

	public boolean isShowPackets() {
		return getInternalParameters().hasParameters("-show_packets");
	}

	/**
	 * -show_programs show programs info
	 */
	public FFprobe setShowPrograms() {
		getInternalParameters().ifHasNotParameter(() -> {
			getInternalParameters().addParameters("-show_programs");
		}, "-show_programs");
		return this;
	}

	public boolean isShowPrograms() {
		return getInternalParameters().hasParameters("-show_programs");
	}

	/**
	 * -show_streams show streams info
	 */
	public FFprobe setShowStreams() {
		getInternalParameters().ifHasNotParameter(() -> {
			getInternalParameters().addParameters("-show_streams");
		}, "-show_streams");
		return this;
	}

	public boolean isShowStreams() {
		return getInternalParameters().hasParameters("-show_streams");
	}

	/**
	 * -show_chapters show chapters info
	 */
	public FFprobe setShowChapters() {
		getInternalParameters().ifHasNotParameter(() -> {
			getInternalParameters().addParameters("-show_chapters");
		}, "-show_chapters");
		return this;
	}

	public boolean isShowChapters() {
		return getInternalParameters().hasParameters("-show_chapters");
	}

}
