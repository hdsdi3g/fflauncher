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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExecProcessResult {
	private static final Logger log = LogManager.getLogger();
	
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
	private final List<EndExecutionCallback<?>> end_exec_callback_list;
	
	private final boolean exec_code_must_be_zero;
	private final File working_directory;
	private final Executor executor;
	
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
	private CompletableFuture<Process> cf_startup_process_exec;
	
	private CompletableFuture<StdInInjection> cf_std_in_injection;
	private final Thread shutdown_hook;
	
	ExecProcessResult(File executable, List<String> params, Map<String, String> environment, List<EndExecutionCallback<?>> end_exec_callback_list, boolean exec_code_must_be_zero, File working_directory, ScheduledExecutorService max_exec_time_scheduler, long max_exec_time, Consumer<ProcessBuilder> alter_process_builder, Executor executor) {
		this.executable = executable;
		this.executor = executor;
		
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
		
		this.alter_process_builder = alter_process_builder;
		
		cf_startup_process_exec = CompletableFuture.failedFuture(new NullPointerException("Process is not yet pending to start..."));
		
		shutdown_hook = new Thread(() -> {
			if (cf_startup_process_exec.isDone()) {
				try {
					log.warn("Try to kill " + toString());
					killProcessTree(cf_startup_process_exec.get());
				} catch (InterruptedException | ExecutionException e) {
				}
			} else {
				log.warn("Cancel " + toString());
				cf_startup_process_exec.cancel(true);
			}
		});
		shutdown_hook.setDaemon(false);
		shutdown_hook.setPriority(Thread.MAX_PRIORITY);
		shutdown_hook.setName("ShutdownHook for " + executable.getName());
	}
	
	synchronized ExecProcessResult start() {
		cf_startup_process_exec = CompletableFuture.supplyAsync(() -> {
			synchronized (this) {
				Process process;
				try {
					process = makeProcessBuilder().start();
				} catch (IOException e) {
					throw new RuntimeException("Can't start process " + getCommandline(), e);
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
					log.info("Start process #" + process.pid() + " by " + process.info().user().orElse("(?)") + " " + getCommandline() + env + met);
				} else {
					log.info("Start process #" + process.pid() + " " + getCommandline());
				}
				
				if (max_exec_time < Long.MAX_VALUE) {
					max_exec_time_stopper = max_exec_time_scheduler.schedule(() -> {
						process_was_stopped_because_too_long_time = true;
						killProcessTree(process);
					}, max_exec_time, TimeUnit.MILLISECONDS);
					
					process.onExit().thenRunAsync(() -> {
						max_exec_time_stopper.cancel(false);
					}, max_exec_time_scheduler);
				}
				
				process.onExit().thenRun(() -> {
					Runtime.getRuntime().removeShutdownHook(shutdown_hook);
					end_exec_callback_list.forEach(observer -> {
						observer.onEnd(this);
					});
				});
				
				postStartupAction(process);
				
				Runtime.getRuntime().addShutdownHook(shutdown_hook);
				
				return process;
			}
		}, executor);
		
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
	
	/**
	 * Started in the right thread
	 */
	protected void postStartupAction(Process process) {
	}
	
	public String toString() {
		if (cf_startup_process_exec.isCompletedExceptionally()) {
			return "Can't start to exec " + getCommandline();
		}
		
		Process process = cf_startup_process_exec.getNow(null);
		
		if (process == null) {
			return "Ready to exec " + getCommandline();
		} else if (process.isAlive()) {
			return "Process #" + getPID() + " " + getCommandline() + " ; since " + getUptime(TimeUnit.SECONDS) + " sec";
		} else {
			return "Exec " + getEndStatus() + " " + getCommandline();
		}
	}
	
	private static String processHandleToString(ProcessHandle process_handle, boolean verbose) {
		if (verbose) {
			return process_handle.info().command().orElse("<?>") + " #" + process_handle.pid() + " by " + process_handle.info().user().orElse("<?>") + " since " + process_handle.info().totalCpuDuration().orElse(Duration.ZERO).getSeconds() + " sec";
		} else {
			return process_handle.info().commandLine().orElse("<?>") + " #" + process_handle.pid();
		}
	}
	
	/**
	 * Blocking
	 */
	private void killProcessTree(Process process) {
		log.debug("Internal kill " + toString());
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
		}).collect(Collectors.toUnmodifiableList());
		
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
			throw new RuntimeException("Can't close process " + toString() + " for PID " + cant_kill.stream().map(p -> p.pid()).map(pid -> String.valueOf(pid)).collect(Collectors.joining(", ")));
		}
	}
	
	/**
	 * Async, don't checks if running is ok.
	 * @return CF of this
	 */
	public CompletableFuture<? extends ExecProcessResult> waitForEnd() {
		return cf_startup_process_exec.thenApplyAsync(process -> {
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return this;
		}, executor);
	}
	
	/**
	 * Async, don't checks if running is ok.
	 * @return CF of this
	 */
	public CompletableFuture<? extends ExecProcessResult> waitForEnd(long timeout, TimeUnit unit) {
		return cf_startup_process_exec.thenApplyAsync(process -> {
			try {
				process.waitFor(timeout, unit);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return this;
		}, executor);
	}
	
	public CompletableFuture<? extends ExecProcessResult> kill(Executor executor) {
		return cf_startup_process_exec.thenApplyAsync(process -> {
			process_was_killed = true;
			return process;
		}, executor).thenApplyAsync(process -> {
			killProcessTree(process);
			return this;
		}, executor);
		
	}
	
	public CompletableFuture<Boolean> isRunning() {
		return cf_startup_process_exec.thenApplyAsync(process -> {
			return process.isAlive();
		}, executor);
	}
	
	public CompletableFuture<Boolean> isCorrectlyDone() {
		return cf_startup_process_exec.thenApplyAsync(process -> {
			if (process.isAlive()) {
				return false;
			} else if (exec_code_must_be_zero) {
				return process.exitValue() == 0;
			}
			return true;
		}, executor);
		
	}
	
	public boolean isKilled() {
		return process_was_killed;
	}
	
	public boolean isTooLongTime() {
		return process_was_stopped_because_too_long_time;
	}
	
	public CompletableFuture<Process> getProcess() {
		return cf_startup_process_exec;
	}
	
	public CompletableFuture<EndStatus> getEndStatus() {
		return cf_startup_process_exec.thenApplyAsync(process -> {
			if (process.isAlive()) {
				return EndStatus.NOT_YET_DONE;
			} else if (process_was_killed) {
				return EndStatus.KILLED;
			} else if (process_was_stopped_because_too_long_time) {
				return EndStatus.TOO_LONG_EXECUTION_TIME;
			} else if (exec_code_must_be_zero && process.exitValue() != 0) {
				return EndStatus.DONE_WITH_ERROR;
			}
			return EndStatus.CORRECTLY_DONE;
		}, executor);
	}
	
	public CompletableFuture<Integer> getExitCode() {
		return cf_startup_process_exec.thenApplyAsync(process -> {
			return process.exitValue();
		}, executor);
	}
	
	public CompletableFuture<Long> getStartDate() {
		return cf_startup_process_exec.thenApplyAsync(process -> {
			return process.info().startInstant().orElse(Instant.EPOCH).toEpochMilli();
		}, executor);
	}
	
	public CompletableFuture<Long> getUptime(TimeUnit unit) {
		return getStartDate().thenApplyAsync(start_date -> {
			if (start_date <= 0) {
				return -1l;
			}
			return unit.convert(System.currentTimeMillis() - start_date, TimeUnit.MILLISECONDS);
		}, executor);
	}
	
	public CompletableFuture<Long> getCPUDuration(TimeUnit unit) {
		return cf_startup_process_exec.thenApplyAsync(process -> {
			return unit.convert(process.info().totalCpuDuration().orElse(Duration.ZERO).toMillis(), TimeUnit.MILLISECONDS);
		}, executor);
	}
	
	/**
	 * on Windows, return like "HOST_or_DOMAIN"\"username"
	 */
	public CompletableFuture<Optional<String>> getUserExec() {
		return cf_startup_process_exec.thenApplyAsync(process -> {
			return process.info().user();
		}, executor);
	}
	
	public CompletableFuture<Long> getPID() {
		return cf_startup_process_exec.thenApplyAsync(process -> {
			try {
				return process.pid();
			} catch (UnsupportedOperationException e) {
				return -1l;
			}
		}, executor);
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
	
	/**
	 * Blocking during the process really starts
	 */
	public StdInInjection getStdInInjection(Executor executor) {
		if (cf_std_in_injection == null) {
			synchronized (this) {
				cf_std_in_injection = cf_startup_process_exec.thenApplyAsync(process -> {
					return new StdInInjection(process.getOutputStream());
				}, executor);
			}
		}
		
		try {
			return cf_std_in_injection.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Can't get std_in_injection", e);
		}
	}
	
	/**
	 * Blocking during the process ends
	 */
	public ExecProcessResult checkExecution() {
		try {
			if (isCorrectlyDone().get() == false) {
				throw new RuntimeException("Can't execute correcly " + getCommandline() + ", " + getEndStatus().get() + " [" + getExitCode().get() + "]");
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Can't start process " + getCommandline());
		}
		return this;
	}
	
}
