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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

public class FFbase extends ConversionTool {

	private FFAbout about;

	public FFbase(final String execName, final Parameters parameters) {
		super(execName, parameters);
	}

	@Override
	public void beforeRun(final ProcesslauncherBuilder processBuilder) {
		super.beforeRun(processBuilder);
		if (processBuilder.getEnvironmentVar("AV_LOG_FORCE_COLOR") == null) {
			processBuilder.setEnvironmentVarIfNotFound("AV_LOG_FORCE_NOCOLOR", "1");
		}
	}

	/**
	 * Add like -loglevel repeat+level+verbose
	 */
	public FFbase setLogLevel(final FFLogLevel level, final boolean repeat, final boolean display_level) {
		parameters.ifHasNotParameter(() -> {
			final StringBuilder sb = new StringBuilder();
			if (repeat) {
				sb.append("repeat+");
			}
			if (display_level) {
				sb.append("level+");
			}
			sb.append(level.name());
			parameters.prependParameters("-loglevel", sb.toString());
		}, "-loglevel", "-v");

		return this;
	}

	public boolean isLogLevelSet() {
		return parameters.hasParameters("-loglevel", "-v");
	}

	public FFbase setHidebanner() {
		parameters.ifHasNotParameter(() -> {
			parameters.prependParameters("-hide_banner");
		}, "-hide_banner");
		return this;
	}

	public boolean isHidebanner() {
		return parameters.hasParameters("-hide_banner");
	}

	public FFbase setOverwriteOutputFiles() {
		parameters.ifHasNotParameter(() -> {
			parameters.prependParameters("-y");
		}, "-y");
		return this;
	}

	public boolean isOverwriteOutputFiles() {
		return parameters.hasParameters("-y");
	}

	public FFbase setNeverOverwriteOutputFiles() {
		parameters.ifHasNotParameter(() -> {
			parameters.prependParameters("-n");
		}, "-n");
		return this;
	}

	public boolean isNeverOverwriteOutputFiles() {
		return parameters.hasParameters("-n");
	}

	/**
	 * Define cmd var name like <%IN_AUTOMATIC_n%> with "n" the # of setted sources.
	 * Add -i parameter
	 */
	public FFbase addSimpleInputSource(final String source_name, final String... source_options) {
		if (source_name == null) {
			throw new NullPointerException("\"source_name\" can't to be null");
		}

		if (source_options == null) {
			return addSimpleInputSource(source_name, Collections.emptyList());
		} else {
			return addSimpleInputSource(source_name, Arrays.stream(source_options).collect(Collectors.toUnmodifiableList()));
		}
	}

	/**
	 * Define cmd var name like <%IN_AUTOMATIC_n%> with "n" the # of setted sources.
	 * Add -i parameter
	 */
	public FFbase addSimpleInputSource(final String source_name, final List<String> source_options) {
		if (source_name == null) {
			throw new NullPointerException("\"source_name\" can't to be null");
		} else if (source_options == null) {
			throw new NullPointerException("\"source_options\" can't to be null");
		}

		final String varname = parameters.addVariable("IN_AUTOMATIC_" + input_sources.size());
		addInputSource(source_name, varname, Stream.concat(source_options.stream(), Stream.of("-i")).collect(Collectors.toUnmodifiableList()), Collections.emptyList());

		return this;
	}

	public synchronized FFAbout getAbout(final ExecutableFinder executableFinder) {
		if (about == null) {
			final ScheduledExecutorService maxExecTimeScheduler = getMaxExecTimeScheduler();
			if (maxExecTimeScheduler == null) {
				about = new FFAbout(execName, executableFinder, Executors.newSingleThreadScheduledExecutor());
			} else {
				about = new FFAbout(execName, executableFinder, maxExecTimeScheduler);
			}
		}
		return about;
	}

}
