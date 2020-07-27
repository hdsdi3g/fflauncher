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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

public class FFbase extends ConversionTool {

	private static final String P_LOGLEVEL = "-loglevel";
	private static final String P_HIDE_BANNER = "-hide_banner";
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

	@Override
	protected PrintStream getStdErrPrintStreamToDisplayLinesEvents() {
		return System.out;// NOSONAR
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
			sb.append(level);
			parameters.prependParameters(P_LOGLEVEL, sb.toString());
		}, P_LOGLEVEL, "-v");

		return this;
	}

	public boolean isLogLevelSet() {
		return parameters.hasParameters(P_LOGLEVEL, "-v");
	}

	public FFbase setHidebanner() {
		parameters.ifHasNotParameter(() -> parameters.prependParameters(P_HIDE_BANNER), P_HIDE_BANNER);
		return this;
	}

	public boolean isHidebanner() {
		return parameters.hasParameters(P_HIDE_BANNER);
	}

	public FFbase setOverwriteOutputFiles() {
		parameters.ifHasNotParameter(() -> parameters.prependParameters("-y"), "-y");
		return this;
	}

	public boolean isOverwriteOutputFiles() {
		return parameters.hasParameters("-y");
	}

	public FFbase setNeverOverwriteOutputFiles() {
		parameters.ifHasNotParameter(() -> parameters.prependParameters("-n"), "-n");
		return this;
	}

	public boolean isNeverOverwriteOutputFiles() {
		return parameters.hasParameters("-n");
	}

	/**
	 * Define cmd var name like &lt;%IN_AUTOMATIC_n%&gt; with "n" the # of setted sources.
	 * Add -i parameter
	 * Don't forget to call fixIOParametredVars() for add the new created var in current Parameters.
	 */
	public FFbase addSimpleInputSource(final String sourceName, final String... sourceOptions) {
		requireNonNull(sourceName, "\"sourceName\" can't to be null");

		if (sourceOptions == null) {
			return addSimpleInputSource(sourceName, Collections.emptyList());
		} else {
			return addSimpleInputSource(sourceName, Arrays.stream(sourceOptions).collect(Collectors
			        .toUnmodifiableList()));
		}
	}

	/**
	 * Define cmd var name like &lt;%IN_AUTOMATIC_n%&gt; with "n" the # of setted sources.
	 * Add -i parameter
	 * Don't forget to call fixIOParametredVars() for add the new created var in current Parameters.
	 */
	public FFbase addSimpleInputSource(final File file, final String... sourceOptions) {
		requireNonNull(file, "\"file\" can't to be null");

		if (sourceOptions == null) {
			return addSimpleInputSource(file, Collections.emptyList());
		} else {
			return addSimpleInputSource(file, Arrays.stream(sourceOptions).collect(Collectors.toUnmodifiableList()));
		}
	}

	/**
	 * Define cmd var name like &lt;%IN_AUTOMATIC_n%&gt; with "n" the # of setted sources.
	 * Add -i parameter
	 * Don't forget to call fixIOParametredVars() for add the new created var in current Parameters.
	 */
	public FFbase addSimpleInputSource(final String sourceName, final List<String> sourceOptions) {
		requireNonNull(sourceName, "\"sourceName\" can't to be null");
		requireNonNull(sourceOptions, "\"sourceOptions\" can't to be null");

		final var varname = parameters.tagVar("IN_AUTOMATIC_" + inputSources.size());
		addInputSource(sourceName, varname,
		        Stream.concat(sourceOptions.stream(), Stream.of("-i")).collect(toUnmodifiableList()));
		return this;
	}

	/**
	 * Define cmd var name like &lt;%IN_AUTOMATIC_n%&gt; with "n" the # of setted sources.
	 * Add -i parameter
	 * Don't forget to call fixIOParametredVars() for add the new created var in current Parameters.
	 */
	public FFbase addSimpleInputSource(final File file, final List<String> sourceOptions) {
		requireNonNull(file, "\"file\" can't to be null");
		requireNonNull(sourceOptions, "\"sourceOptions\" can't to be null");

		final var varname = parameters.tagVar("IN_AUTOMATIC_" + inputSources.size());
		addInputSource(file, varname,
		        Stream.concat(sourceOptions.stream(), Stream.of("-i")).collect(toUnmodifiableList()));
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

	private static final Predicate<String> filterOutErrorLines = rawL -> {
		final String l = rawL.trim();
		if (l.startsWith("[")) {
			return true;
		} else if (l.startsWith("ffmpeg version")
		           || l.startsWith("ffprobe version")
		           || l.startsWith("built with")
		           || l.startsWith("configuration:")
		           || l.startsWith("Press [q]")) {
			return false;
		} else if (l.startsWith("libavutil")
		           || l.startsWith("libavcodec")
		           || l.startsWith("libavformat")
		           || l.startsWith("libavdevice")
		           || l.startsWith("libavfilter")
		           || l.startsWith("libswscale")
		           || l.startsWith("libswresample")
		           || l.startsWith("libpostproc")) {
			return false;
		}
		return true;
	};

	@Override
	public Predicate<String> filterOutErrorLines() {
		return filterOutErrorLines;
	}
}
