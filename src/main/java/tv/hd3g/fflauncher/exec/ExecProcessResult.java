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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

class ExecProcessResult {
	private static Logger log = Logger.getLogger(ExecProcessResult.class);
	
	private final File executable;
	
	/**
	 * unmodifiableList, with executable path.
	 */
	private final List<String> command_line;
	
	/**
	 * unmodifiableMap
	 */
	private final Map<String, String> environment;
	
	/**
	 * unmodifiableList
	 */
	private final List<EndExecutionCallback> end_exec_callback_list;
	
	private final boolean exec_code_must_be_zero;
	private final File working_directory;
	
	/**
	 * Can be null
	 */
	private ScheduledFuture<?> max_exec_time_stopper;
	private final long max_exec_time;
	private final ScheduledExecutorService max_exec_time_scheduler;
	
	/**
	 * Can be null
	 */
	private Consumer<ProcessBuilder> alter_process_builder;
	
	private volatile boolean process_was_killed;
	private volatile boolean process_was_stopped_because_too_long_time;
	private volatile boolean process_cant_start;
	
	protected Process process;
	private StdInInjection std_in_injection;
	
	ExecProcessResult(File executable, List<String> params, Map<String, String> environment, List<EndExecutionCallback> end_exec_callback_list, boolean exec_code_must_be_zero, File working_directory, ScheduledExecutorService max_exec_time_scheduler, long max_exec_time, Consumer<ProcessBuilder> alter_process_builder) {
		this.executable = executable;
		
		ArrayList<String> _cmd = new ArrayList<>(1 + params.size());
		_cmd.add(executable.getPath());
		_cmd.addAll(params);
		
		command_line = Collections.unmodifiableList(_cmd);
		this.environment = Collections.unmodifiableMap(new HashMap<>(environment));
		this.end_exec_callback_list = Collections.unmodifiableList(new ArrayList<>(end_exec_callback_list));
		this.exec_code_must_be_zero = exec_code_must_be_zero;
		this.working_directory = working_directory;
		this.max_exec_time_scheduler = max_exec_time_scheduler;
		this.max_exec_time = max_exec_time;
		
		process_was_killed = false;
		process_was_stopped_because_too_long_time = false;
		process_cant_start = false;
		
		this.alter_process_builder = alter_process_builder;
	}
	
	ExecProcessResult start(Executor executor) {
		executor.execute(getStart());
		return this;
	}
	
	ExecProcessResult start(ThreadFactory thread_factory) {
		thread_factory.newThread(getStart()).start();
		return this;
	}
	
	ProcessBuilder makeProcessBuilder() {
		ProcessBuilder process_builder = new ProcessBuilder(command_line);
		process_builder.environment().putAll(environment);
		
		if (working_directory != null) {
			process_builder.directory(working_directory);
		}
		
		if (alter_process_builder != null) {
			alter_process_builder.accept(process_builder);
		}
		
		return process_builder;
	}
	
	private Runnable getStart() {
		return () -> {
			try {
				process = makeProcessBuilder().start();
			} catch (IOException e) {
				log.error("Can't start process " + getCommandline(), e);
				process_cant_start = true;
				return;
			}
			
			if (log.isTraceEnabled()) {
				String env = "";
				if (environment.isEmpty() == false) {
					env = " ; with environment: " + environment;
				}
				String met = "";
				if (max_exec_time > 0) {
					met = " and max exec time: " + getMaxExecTime(TimeUnit.SECONDS) + " sec";
				}
				log.info("Start process #" + getPID() + " by " + getUserExec() + " " + getCommandline() + env + met);
			} else {
				log.info("Start process #" + getPID() + " " + getCommandline());
			}
			
			if (max_exec_time < Long.MAX_VALUE) {
				max_exec_time_stopper = max_exec_time_scheduler.schedule(() -> {
					process_was_stopped_because_too_long_time = true;
					killProcessTree();
				}, max_exec_time, TimeUnit.MILLISECONDS);
				
				process.onExit().thenRunAsync(() -> {
					max_exec_time_stopper.cancel(false);
				}, max_exec_time_scheduler);
			}
			
			process.onExit().thenRun(() -> end_exec_callback_list.forEach(observer -> {
				observer.onEnd(this);
			}));
			
			postStartupAction();
		};
	}
	
	/**
	 * Started in the right thread
	 */
	protected void postStartupAction() {
	}
	
	public String toString() {
		if (process == null) {
			return "Ready to exec " + getCommandline();
		} else if (process.isAlive()) {
			return "Process #" + getPID() + " " + getCommandline() + " ; since " + getUptime(TimeUnit.SECONDS) + " sec";
		} else {
			return "Exec " + getEndStatus() + " " + getCommandline();
		}
	}
	
	/**
	 * Blocking
	 */
	public ExecProcessResult waitForEnd() {
		while (process == null) {
			Thread.onSpinWait();
		}
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return this;
	}
	
	/**
	 * Blocking
	 */
	public ExecProcessResult waitForEnd(long timeout, TimeUnit unit) {
		while (process == null) {
			Thread.onSpinWait();
		}
		
		try {
			process.waitFor(timeout, unit);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return this;
	}
	
	private static String processHandleToString(ProcessHandle process_handle, boolean verbose) {
		if (verbose) {
			return process_handle.info().command().orElse("<?>") + " #" + process_handle.pid() + " by " + process_handle.info().user().orElse("<?>") + " since " + process_handle.info().totalCpuDuration().orElse(Duration.ZERO).getSeconds() + " sec";
		} else {
			return process_handle.info().commandLine().orElse("<?>") + " #" + process_handle.pid();
		}
	}
	
	private void killProcessTree() {
		if (isRunning() == false) {
			return;
		}
		
		List<ProcessHandle> cant_kill = process.descendants().filter(process_handle -> {
			return process_handle.isAlive();
		}).filter(process_handle -> {
			if (log.isDebugEnabled()) {
				log.info("Close manually process " + processHandleToString(process_handle, true));
			} else if (log.isInfoEnabled()) {
				log.info("Close manually process " + processHandleToString(process_handle, false));
			}
			return process_handle.destroy() == false;
		}).filter(process_handle -> {
			if (log.isDebugEnabled()) {
				log.info("Force to close process " + processHandleToString(process_handle, true));
			} else if (log.isInfoEnabled()) {
				log.info("Force to close process " + processHandleToString(process_handle, false));
			}
			return process_handle.destroyForcibly() == false;
		}).collect(Collectors.toList());
		
		if (process.isAlive()) {
			log.info("Close manually process " + processHandleToString(process.toHandle(), true));
			if (process.toHandle().destroy() == false) {
				log.info("Force to close process " + processHandleToString(process.toHandle(), true));
				if (process.toHandle().destroyForcibly() == false) {
					throw new RuntimeException("Can't close process " + processHandleToString(process.toHandle(), true));
				}
			}
		}
		
		if (cant_kill.isEmpty() == false) {
			cant_kill.forEach(process_handle -> {
				log.error("Can't force close process " + processHandleToString(process_handle, true));
			});
		}
	}
	
	/**
	 * Async
	 * @return CF of this
	 */
	public CompletableFuture<? extends ExecProcessResult> waitForEnd(Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			return waitForEnd();
		}, executor);
	}
	
	public ExecProcessResult kill() {
		if (isRunning() == false) {
			return this;
		}
		process_was_killed = true;
		killProcessTree();
		return this;
	}
	
	public boolean isRunning() {
		if (process == null) {
			return false;
		}
		return process.isAlive();
	}
	
	public boolean isCorrectlyDone() {
		if (process == null) {
			return false;
		} else if (process.isAlive()) {
			return false;
		} else if (exec_code_must_be_zero) {
			return process.exitValue() == 0;
		}
		return true;
	}
	
	public boolean isKilled() {
		return process_was_killed;
	}
	
	public boolean isTooLongTime() {
		return process_was_stopped_because_too_long_time;
	}
	
	public Process getProcess() {
		return process;
	}
	
	public EndStatus getEndStatus() {
		if (isRunning()) {
			return EndStatus.NOT_YET_DONE;
		} else if (process == null) {
			return EndStatus.NOT_YET_DONE;
		} else if (isCorrectlyDone()) {
			return EndStatus.CORRECTLY_DONE;
		} else if (isKilled()) {
			return EndStatus.KILLED;
		} else if (isTooLongTime()) {
			return EndStatus.TOO_LONG_EXECUTION_TIME;
		} else if (process_cant_start) {
			return EndStatus.CANT_START;
		}
		return EndStatus.DONE_WITH_ERROR;
	}
	
	/**
	 * @return -1 if not yet done
	 */
	public int getExitCode() {
		if (process == null) {
			return -1;
		}
		if (isRunning()) {
			return -1;
		}
		return process.exitValue();
	}
	
	/**
	 * @return -1 if not started
	 */
	public long getStartDate() {
		if (process == null) {
			return -1;
		}
		return process.info().startInstant().orElse(Instant.EPOCH).toEpochMilli();
	}
	
	/**
	 * @return -1 if not started
	 */
	public long getUptime(TimeUnit unit) {
		long start_date = getStartDate();
		if (start_date <= 0) {
			return -1;
		}
		return unit.convert(System.currentTimeMillis() - start_date, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * @return -1 if not started ; 0 if can't get result.
	 */
	public long getCPUDuration(TimeUnit unit) {
		if (process == null) {
			return -1;
		}
		return unit.convert(process.info().totalCpuDuration().orElse(Duration.ZERO).toMillis(), TimeUnit.MILLISECONDS);
	}
	
	/**
	 * @return null if not started of if it can't get result.
	 */
	public String getUserExec() {
		if (process == null) {
			return null;
		}
		return process.info().user().orElse(null);
	}
	
	/**
	 * @return -1 if not started.
	 */
	public long getPID() {
		if (process == null) {
			return -1;
		}
		try {
			return process.pid();
		} catch (UnsupportedOperationException e) {
			return -1;
		}
	}
	
	public long getMaxExecTime(TimeUnit unit) {
		if (max_exec_time_stopper != null) {
			return unit.convert(max_exec_time, TimeUnit.MILLISECONDS);
		}
		return 0;
	}
	
	public File getExecutable() {
		return executable;
	}
	
	/**
	 * @return with executable
	 */
	public String getCommandline() {
		return command_line.stream().collect(Collectors.joining(" "));
	}
	
	public File getWorkingDirectory() {
		return working_directory;
	}
	
	public Map<String, String> getEnvironment() {
		return environment;
	}
	
	public StdInInjection getStdInInjection() {
		if (std_in_injection == null) {
			synchronized (this) {
				std_in_injection = new StdInInjection(process.getOutputStream());
			}
		}
		return std_in_injection;
	}
	
}
