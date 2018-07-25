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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExecProcessTextResult extends ExecProcessResult {
	
	private static Logger log = LogManager.getLogger();
	
	private boolean keep_stdout;
	private boolean keep_stderr;
	private InteractiveExecProcessHandler interactive_handler;
	private Executor interactive_handler_executor;
	private CaptureOutStreamsBehavior capture_streams_behavior;
	
	// private StdTextParser stdout_parser;
	// private StdTextParser stderr_parser;
	private final LinkedBlockingQueue<LineEntry> lines;
	
	/**
	 * unmodifiableList
	 */
	private List<StdOutErrCallback> stdouterr_callback_list;
	
	ExecProcessTextResult(File executable, List<String> params, Map<String, String> environment, List<EndExecutionCallback> observers, boolean exec_code_must_be_zero, File working_directory, ScheduledExecutorService max_exec_time_scheduler, long max_exec_time, Consumer<ProcessBuilder> alter_process_builder) {
		super(executable, params, environment, observers, exec_code_must_be_zero, working_directory, max_exec_time_scheduler, max_exec_time, alter_process_builder);
		lines = new LinkedBlockingQueue<>();
	}
	
	ExecProcessTextResult setup(CaptureOutStreamsBehavior capture_streams_behavior, boolean keep_stdout, boolean keep_stderr, InteractiveExecProcessHandler interactive_handler, Executor interactive_handler_executor, ArrayList<StdOutErrCallback> stdouterr_callback_list) {
		this.capture_streams_behavior = capture_streams_behavior;
		this.keep_stdout = keep_stdout;
		this.keep_stderr = keep_stderr;
		this.interactive_handler = interactive_handler;
		this.interactive_handler_executor = interactive_handler_executor;
		this.stdouterr_callback_list = Collections.unmodifiableList(new ArrayList<>(stdouterr_callback_list));
		return this;
	}
	
	/**
	 * Async !
	 */
	ExecProcessTextResult start(Executor executor) {
		super.start(executor);
		return this;
	}
	
	/**
	 * Async !
	 */
	ExecProcessTextResult start(ThreadFactory thread_factory) {
		super.start(thread_factory);
		return this;
	}
	
	protected void postStartupAction() {
		if (process == null) {
			return;
		}
		if (process.isAlive() == false) {
			return;
		}
		
		if (capture_streams_behavior.canCaptureStdout()) {
			/*stdout_parser =*/ new StdTextParser(process.getInputStream(), false);
		}
		if (capture_streams_behavior.canCaptureStderr()) {
			/*stderr_parser = */new StdTextParser(process.getErrorStream(), true);
		}
	}
	
	private class StdTextParser extends Thread {
		
		private final InputStream is;
		private final boolean std_err;
		
		public StdTextParser(InputStream is, boolean std_err) {
			this.is = is;
			this.std_err = std_err;
			
			if (std_err) {
				setName("StdErr for " + getExecutable().getName() + "#" + getPID());
			} else {
				setName("StdOut for " + getExecutable().getName() + "#" + getPID());
			}
			
			setPriority(MIN_PRIORITY);
			setDaemon(false);
			
			setUncaughtExceptionHandler((t, e) -> {
				if (std_err) {
					log.error("Can't process stderr stream for " + getCommandline(), e);
				} else {
					log.error("Can't process stdout stream for " + getCommandline(), e);
				}
			});
			
			start();
		}
		
		public String toString() {
			if (std_err) {
				return "StdErr for " + getCommandline();
			} else {
				return "StdOut for " + getCommandline();
			}
		}
		
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				try {
					String line = "";
					while ((line = reader.readLine()) != null) {
						consumeStdOutErr(line, std_err);
					}
				} catch (IOException ioe) {
					if (ioe.getMessage().equalsIgnoreCase("Bad file descriptor")) {
						if (log.isTraceEnabled()) {
							log.trace("Bad file descriptor, " + toString());
						}
					} else if (ioe.getMessage().equalsIgnoreCase("Stream closed")) {
						if (log.isTraceEnabled()) {
							log.trace("Stream closed, " + toString());
						}
					} else {
						throw ioe;
					}
				} catch (Exception e) {
					log.error("Trouble during process " + toString(), e);
				} finally {
					reader.close();
				}
			} catch (IOException ioe) {
				log.error("Trouble opening process streams: " + toString(), ioe);
			}
		}
	}
	
	private class LineEntry {
		final String line;
		final boolean std_err;
		
		private LineEntry(String line, boolean std_err) {
			this.line = line;
			this.std_err = std_err;
		}
	}
	
	private void consumeStdOutErr(String line, boolean std_err) {
		if (std_err) {
			if (keep_stderr) {
				lines.add(new LineEntry(line, std_err));
			}
		} else {
			if (keep_stdout) {
				lines.add(new LineEntry(line, std_err));
			}
		}
		
		stdouterr_callback_list.forEach(callback -> {
			try {
				if (std_err) {
					callback.onStderr(this, line);
				} else {
					callback.onStdout(this, line);
				}
			} catch (Exception e) {
				log.warn("Can't callback by " + toString(), e);
			}
		});
		
		if (interactive_handler_executor != null && interactive_handler != null) {
			try {
				interactive_handler_executor.execute(() -> {
					String out = interactive_handler.onText(this, line, std_err);
					if (process.isAlive() == false) {
						return;
					}
					if (out != null) {
						try {
							getStdInInjection().println(out);
						} catch (IOException e) {
							log.error("Can't send some text to process", e);
						}
					}
				});
			} catch (Exception e) {
				log.warn("Can't callback by " + toString(), e);
			}
		}
		
	}
	
	/**
	 * Only set if setKeepStdout is set (false by default), else return empty stream.
	 */
	public Stream<String> getStdoutLines(boolean keep_empty_lines) {
		return lines.stream().filter(le -> {
			if (keep_empty_lines) {
				return true;
			}
			return le.line.equals("") == false;
		}).filter(le -> {
			return le.std_err == false;
		}).map(le -> {
			return le.line;
		});
	}
	
	/**
	 * Only set if setKeepStdout is set (false by default), else return empty stream.
	 * @param keep_empty_lines if set false, discard all empty trimed lines
	 */
	public Stream<String> getStderrLines(boolean keep_empty_lines) {
		return lines.stream().filter(le -> {
			if (keep_empty_lines) {
				return true;
			}
			return le.line.equals("") == false;
		}).filter(le -> {
			return le.std_err;
		}).map(le -> {
			return le.line;
		});
	}
	
	/**
	 * Only set if setKeepStdout is set (false by default), else return empty stream.
	 * @param keep_empty_lines if set false, discard all empty trimed lines
	 */
	public Stream<String> getStdouterrLines(boolean keep_empty_lines) {
		return lines.stream().filter(le -> {
			if (keep_empty_lines) {
				return true;
			}
			return le.line.equals("") == false;
		}).map(le -> {
			return le.line;
		});
	}
	
	/**
	 * Only set if setKeepStdout is set (false by default), else return empty text.
	 * @param keep_empty_lines if set false, discard all empty trimed lines
	 * @param new_line_separator replace new line char by this
	 *        Use System.lineSeparator() if needed
	 */
	public String getStdout(boolean keep_empty_lines, String new_line_separator) {
		return getStdoutLines(keep_empty_lines).collect(Collectors.joining(new_line_separator));
	}
	
	/**
	 * Only set if setKeepStdout is set (false by default), else return empty text.
	 * @param keep_empty_lines if set false, discard all empty trimed lines
	 * @param new_line_separator replace new line char by this
	 *        Use System.lineSeparator() if needed
	 */
	public String getStderr(boolean keep_empty_lines, String new_line_separator) {
		return getStderrLines(keep_empty_lines).collect(Collectors.joining(new_line_separator));
	}
	
	/**
	 * Only set if setKeepStdout is set (false by default), else return empty text.
	 * @param keep_empty_lines if set false, discard all empty trimed lines
	 * @param new_line_separator replace new line char by this
	 *        Use System.lineSeparator() if needed
	 */
	public String getStdouterr(boolean keep_empty_lines, String new_line_separator) {
		return getStdouterrLines(keep_empty_lines).collect(Collectors.joining(new_line_separator));
	}
	
	/**
	 * Blocking
	 */
	public ExecProcessTextResult waitForEnd() {
		super.waitForEnd();
		return this;
	}
	
	/**
	 * Blocking
	 */
	public ExecProcessTextResult waitForEnd(long timeout, TimeUnit unit) {
		super.waitForEnd(timeout, unit);
		return this;
	}
	
	/**
	 * Async
	 * @return CF of this
	 */
	public CompletableFuture<ExecProcessTextResult> waitForEnd(Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			return waitForEnd();
		}, executor);
	}
	
}
