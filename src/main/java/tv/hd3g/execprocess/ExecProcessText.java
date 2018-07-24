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
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import tv.hd3g.execprocess.CommandLineProcessor.CommandLine.ProcessedCommandLine;

public class ExecProcessText extends ExecProcess {
	
	private boolean keep_stdout;
	private boolean keep_stderr;
	
	private CaptureOutStreamsBehavior capture_streams_behavior;
	private InteractiveExecProcessHandler interactive_handler;
	private Executor interactive_handler_executor;
	private final ArrayList<StdOutErrCallback> stdouterr_callback_list;
	
	/**
	 * Keep stdout and stderr bu default.
	 */
	public ExecProcessText(String executable, ExecutableFinder exec_finder) throws IOException {
		super(executable, exec_finder);
		stdouterr_callback_list = new ArrayList<>();
		setup();
	}
	
	public ExecProcessText(File executable) throws IOException {
		super(executable);
		stdouterr_callback_list = new ArrayList<>();
		setup();
	}
	
	/**
	 * @param cmd_line set exec_name and params
	 */
	public ExecProcessText(ProcessedCommandLine cmd_line, ExecutableFinder exec_finder) throws IOException {
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
	public ExecProcessText setKeepStderr(boolean keep_stderr) {
		this.keep_stderr = keep_stderr;
		return this;
	}
	
	/**
	 * True by default.
	 * @param keep all resulted text during execution for get all at the end.
	 */
	public ExecProcessText setKeepStdout(boolean keep_stdout) {
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
	public ExecProcessText setCaptureOutStream(CaptureOutStreamsBehavior capture_streams_behavior) {
		this.capture_streams_behavior = capture_streams_behavior;
		return this;
	}
	
	public CaptureOutStreamsBehavior getCaptureStreamsBehavior() {
		return capture_streams_behavior;
	}
	
	public ExecProcessText setInteractiveHandler(InteractiveExecProcessHandler interactive_handler, Executor executor) {
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
	
	public ExecProcessText addStdOutErrObserver(StdOutErrObserver stdouterr_observer, Executor executor) {
		stdouterr_callback_list.add(new StdOutErrCallback(stdouterr_observer, executor));
		return this;
	}
	
	/**
	 * Non-blocking
	 */
	public ExecProcessTextResult start(Executor executor) {
		ExecProcessTextResult r = new ExecProcessTextResult(executable, params, environment, end_exec_callback_list, exec_code_must_be_zero, working_directory, max_exec_time_scheduler, max_exec_time, alter_process_builder);
		r.setup(capture_streams_behavior, keep_stdout, keep_stderr, interactive_handler, interactive_handler_executor, stdouterr_callback_list);
		return r.start(executor);
	}
	
	/**
	 * Blocking. It will call waitForEnd before return.
	 */
	public ExecProcessTextResult run() {
		return start(r -> r.run()).waitForEnd();
	}
	
	/**
	 * Non-blocking
	 */
	public ExecProcessTextResult start(ThreadFactory thread_factory) {
		ExecProcessTextResult r = new ExecProcessTextResult(executable, params, environment, end_exec_callback_list, exec_code_must_be_zero, working_directory, max_exec_time_scheduler, max_exec_time, alter_process_builder);
		r.setup(capture_streams_behavior, keep_stdout, keep_stderr, interactive_handler, interactive_handler_executor, stdouterr_callback_list);
		return r.start(thread_factory);
	}
	
	public ExecProcessText setParams(Collection<String> params) {
		super.setParams(params);
		return this;
	}
	
	public ExecProcessText setParams(String... params) {
		super.setParams(params);
		return this;
	}
	
	public ExecProcessText addParams(String... params) {
		super.addParams(params);
		return this;
	}
	
	/**
	 * @param params transform spaces in each param to new params: ["a b c", "d"] -> ["a", "b", "c", "d"]. It don't manage " or tabs.
	 */
	public ExecProcessText addSpacedParams(String... params) {
		super.addSpacedParams(params);
		return this;
	}
	
	/**
	 * @param params transform spaces in each param to new params: ["a b c", "d"] -> ["a", "b", "c", "d"]. It don't manage " or tabs.
	 */
	public ExecProcessText setSpacedParams(String... params) {
		super.setSpacedParams(params);
		return this;
	}
	
	public ExecProcessText setEnvironmentVar(String key, String value) {
		super.setEnvironmentVar(key, value);
		return this;
	}
	
	public ExecProcessText setEnvironmentVarIfNotFound(String key, String value) {
		super.setEnvironmentVarIfNotFound(key, value);
		return this;
	}
	
	public ExecProcessText setWorkingDirectory(File working_directory) throws IOException {
		super.setWorkingDirectory(working_directory);
		return this;
	}
	
	public ExecProcessText setMaxExecutionTime(long max_exec_time, TimeUnit unit, ScheduledExecutorService max_exec_time_scheduler) {
		super.setMaxExecutionTime(max_exec_time, unit, max_exec_time_scheduler);
		return this;
	}
	
	/**
	 * Default, yes.
	 */
	public ExecProcessText setExecCodeMustBeZero(boolean exec_code_must_be_zero) {
		super.setExecCodeMustBeZero(exec_code_must_be_zero);
		return this;
	}
	
	public ExecProcessText addEndExecutionCallback(Consumer<ExecProcessResult> onEnd, Executor executor) {
		super.addEndExecutionCallback(onEnd, executor);
		return this;
	}
	
	public ExecProcessText alterProcessBuilderBeforeStartIt(Consumer<ProcessBuilder> alter_process_builder) {
		super.alterProcessBuilderBeforeStartIt(alter_process_builder);
		return this;
	}
	
}
