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
package tv.hd3g.execprocess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import tv.hd3g.execprocess.DeprecatedCommandLineProcessor.DeprecatedCommandLine.ProcessedCommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Deprecated
public class ExecProcessText extends ExecProcess {

	private boolean keep_stdout;
	private boolean keep_stderr;

	private CaptureOutStreamsBehavior capture_streams_behavior;
	private DeprecatedInteractiveExecProcessHandler interactive_handler;
	private Executor interactive_handler_executor;
	private final ArrayList<DeprecatedStdOutErrCallback> stdouterr_callback_list;

	/**
	 * Keep stdout and stderr bu default.
	 */
	public ExecProcessText(final String executable, final ExecutableFinder exec_finder) throws IOException {
		super(executable, exec_finder);
		stdouterr_callback_list = new ArrayList<>();
		setup();
	}

	public ExecProcessText(final File executable) throws IOException {
		super(executable);
		stdouterr_callback_list = new ArrayList<>();
		setup();
	}

	/**
	 * @param cmd_line set exec_name and params
	 */
	public ExecProcessText(final ProcessedCommandLine cmd_line, final ExecutableFinder exec_finder) throws IOException {
		super(cmd_line, exec_finder);
		stdouterr_callback_list = new ArrayList<>();
		setup();
	}

	private void setup() {
		keep_stdout = true;
		keep_stderr = true;
		capture_streams_behavior = CaptureOutStreamsBehavior.BOTH_STDOUT_STDERR;
	}

	/**
	 * True by default.
	 * @param keep all resulted text during execution for get all at the end.
	 */
	public ExecProcessText setKeepStderr(final boolean keep_stderr) {
		this.keep_stderr = keep_stderr;
		return this;
	}

	/**
	 * True by default.
	 * @param keep all resulted text during execution for get all at the end.
	 */
	public ExecProcessText setKeepStdout(final boolean keep_stdout) {
		this.keep_stdout = keep_stdout;
		return this;
	}

	/**
	 * True by default.
	 */
	public boolean isKeepStderr() {
		return keep_stderr;
	}

	/**
	 * True by default.
	 */
	public boolean isKeepStdout() {
		return keep_stdout;
	}

	/**
	 * @param capture_streams_behavior both by default
	 */
	public ExecProcessText setCaptureOutStream(final CaptureOutStreamsBehavior capture_streams_behavior) {
		this.capture_streams_behavior = capture_streams_behavior;
		return this;
	}

	public CaptureOutStreamsBehavior getCaptureStreamsBehavior() {
		return capture_streams_behavior;
	}

	/**
	 * @param interactive_handler, use "(source, line, is_std_err) -> {return null;}"
	 */
	public ExecProcessText setInteractiveHandler(final DeprecatedInteractiveExecProcessHandler interactive_handler, final Executor executor) {
		if (interactive_handler == null) {
			throw new NullPointerException("\"interactive_handler\" can't to be null");
		}
		if (executor == null) {
			throw new NullPointerException("\"executor\" can't to be null");
		}
		this.interactive_handler = interactive_handler;
		interactive_handler_executor = executor;
		return this;
	}

	public ExecProcessText addStdOutErrObserver(final DeprecatedStdOutErrObserver stdouterr_observer, final Executor executor) {
		stdouterr_callback_list.add(new DeprecatedStdOutErrCallback(stdouterr_observer, executor));
		return this;
	}

	/**
	 * Non-blocking
	 */
	@Override
	public ExecProcessTextResult start(final Executor executor) {
		final ExecProcessTextResult r = new ExecProcessTextResult(executable, parameters, environment, end_exec_callback_list, exec_code_must_be_zero, working_directory, max_exec_time_scheduler, max_exec_time, alter_process_builder, executor);
		r.setup(capture_streams_behavior, keep_stdout, keep_stderr, interactive_handler, interactive_handler_executor, stdouterr_callback_list);
		return r.start();
	}

	/**
	 * Blocking, in this current Thread.
	 */
	@Override
	public ExecProcessTextResult run() {
		return (ExecProcessTextResult) super.run();
	}

	@Override
	public ExecProcessText setEnvironmentVar(final String key, final String value) {
		super.setEnvironmentVar(key, value);
		return this;
	}

	@Override
	public ExecProcessText setEnvironmentVarIfNotFound(final String key, final String value) {
		super.setEnvironmentVarIfNotFound(key, value);
		return this;
	}

	@Override
	public ExecProcessText setWorkingDirectory(final File working_directory) throws IOException {
		super.setWorkingDirectory(working_directory);
		return this;
	}

	@Override
	public ExecProcessText setMaxExecutionTime(final long max_exec_time, final TimeUnit unit, final ScheduledExecutorService max_exec_time_scheduler) {
		super.setMaxExecutionTime(max_exec_time, unit, max_exec_time_scheduler);
		return this;
	}

	/**
	 * Default, yes.
	 */
	@Override
	public ExecProcessText setExecCodeMustBeZero(final boolean exec_code_must_be_zero) {
		super.setExecCodeMustBeZero(exec_code_must_be_zero);
		return this;
	}

	@Override
	public <T extends ExecProcessResult> ExecProcessText addEndExecutionCallback(final Consumer<T> onEnd, final Executor executor) {
		super.addEndExecutionCallback(onEnd, executor);
		return this;
	}

	@Override
	public ExecProcessText alterProcessBuilderBeforeStartIt(final Consumer<ProcessBuilder> alter_process_builder) {
		super.alterProcessBuilderBeforeStartIt(alter_process_builder);
		return this;
	}

	@Override
	public ExecProcessText addParameters(final String... params) {
		super.addParameters(params);
		return this;
	}

	/**
	 * @param params transform spaces in each param to new parameters: "a b c d" -> ["a", "b", "c", "d"], and it manage " but not tabs.
	 */
	@Override
	public ExecProcessText addBulkParameters(final String params) {
		super.addBulkParameters(params);
		return this;
	}

}
