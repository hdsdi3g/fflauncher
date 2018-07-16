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
package tv.hd3g.fflauncher.exec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

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
	
	private void setup() {
		keep_stdout = true;
		keep_stderr = true;
		capture_streams_behavior = CaptureOutStreamsBehavior.BOTH_STDOUT_STDERR;
	}
	
	/**
	 * @param keep all resulted text during execution for get all at the end.
	 */
	public ExecProcess setKeepStderr(boolean keep_stderr) {
		this.keep_stderr = keep_stderr;
		return this;
	}
	
	/**
	 * @param keep all resulted text during execution for get all at the end.
	 */
	public ExecProcess setKeepStdout(boolean keep_stdout) {
		this.keep_stdout = keep_stdout;
		return this;
	}
	
	public boolean isKeepStderr() {
		return keep_stderr;
	}
	
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
	 * Non-blocking
	 */
	public ExecProcessTextResult start(ThreadFactory thread_factory) {
		ExecProcessTextResult r = new ExecProcessTextResult(executable, params, environment, end_exec_callback_list, exec_code_must_be_zero, working_directory, max_exec_time_scheduler, max_exec_time, alter_process_builder);
		r.setup(capture_streams_behavior, keep_stdout, keep_stderr, interactive_handler, interactive_handler_executor, stdouterr_callback_list);
		return r.start(thread_factory);
	}
	
}
