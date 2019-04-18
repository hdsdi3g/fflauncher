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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Deprecated
public class ExecProcessTextResult extends ExecProcessResult {

	private static Logger log = LogManager.getLogger();

	private boolean keep_stdout;
	private boolean keep_stderr;
	private DeprecatedInteractiveExecProcessHandler interactive_handler;
	private Executor interactive_handler_executor;
	private CaptureOutStreamsBehavior capture_streams_behavior;

	// private StdTextParser stdout_parser;
	// private StdTextParser stderr_parser;
	private final LinkedBlockingQueue<LineEntry> lines;

	/**
	 * unmodifiableList
	 */
	private List<DeprecatedStdOutErrCallback> stdouterr_callback_list;

	ExecProcessTextResult(final File executable, final List<String> params, final Map<String, String> environment, final List<DeprecatedEndExecutionCallback<?>> observers, final boolean exec_code_must_be_zero, final File working_directory, final ScheduledExecutorService max_exec_time_scheduler, final long max_exec_time, final Consumer<ProcessBuilder> alter_process_builder, final Executor executor) {
		super(executable, params, environment, observers, exec_code_must_be_zero, working_directory, max_exec_time_scheduler, max_exec_time, alter_process_builder, executor);
		lines = new LinkedBlockingQueue<>();
	}

	ExecProcessTextResult setup(final CaptureOutStreamsBehavior capture_streams_behavior, final boolean keep_stdout, final boolean keep_stderr, final DeprecatedInteractiveExecProcessHandler interactive_handler, final Executor interactive_handler_executor, final ArrayList<DeprecatedStdOutErrCallback> stdouterr_callback_list) {
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
	@Override
	ExecProcessTextResult start() {
		super.start();
		return this;
	}

	/**
	 * Please keep override...
	 */
	@Override
	protected void postStartupAction(final Process process) {
		if (process.isAlive() == false) {
			return;
		}

		if (capture_streams_behavior.canCaptureStdout()) {
			/*stdout_parser =*/ new StdTextParser(process.getInputStream(), false, process.pid());
		}
		if (capture_streams_behavior.canCaptureStderr()) {
			/*stderr_parser = */new StdTextParser(process.getErrorStream(), true, process.pid());
		}
	}

	private class StdTextParser extends Thread {

		private final InputStream is;
		private final boolean std_err;

		public StdTextParser(final InputStream is, final boolean std_err, final long pid) {
			this.is = is;
			this.std_err = std_err;

			if (std_err) {
				setName("StdErr for " + getExecutable().getName() + "#" + pid);
			} else {
				setName("StdOut for " + getExecutable().getName() + "#" + pid);
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

		@Override
		public String toString() {
			if (std_err) {
				return "StdErr for " + getCommandline();
			} else {
				return "StdOut for " + getCommandline();
			}
		}

		@Override
		public void run() {
			try {
				final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				try {
					String line = "";
					while ((line = reader.readLine()) != null) {
						consumeStdOutErr(line, std_err);
					}
				} catch (final IOException ioe) {
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
				} catch (final Exception e) {
					log.error("Trouble during process " + toString(), e);
				} finally {
					reader.close();
				}
			} catch (final IOException ioe) {
				log.error("Trouble opening process streams: " + toString(), ioe);
			}
		}
	}

	private class LineEntry {
		final String line;
		final boolean std_err;

		private LineEntry(final String line, final boolean std_err) {
			this.line = line;
			this.std_err = std_err;
		}
	}

	private void consumeStdOutErr(final String line, final boolean std_err) {
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
			} catch (final Exception e) {
				log.warn("Can't callback by " + toString(), e);
			}
		});

		if (interactive_handler_executor != null && interactive_handler != null) {
			try {
				interactive_handler_executor.execute(() -> {
					try {
						final String out = interactive_handler.onText(this, line, std_err);
						if (getProcess().get().isAlive() == false) {
							return;
						}
						if (out != null) {
							try {
								getStdInInjection(Runnable::run).println(out);
							} catch (final IOException e) {
								log.error("Can't send some text to process", e);
							}
						}
					} catch (InterruptedException | ExecutionException e1) {
						throw new RuntimeException("Can't get process", e1);
					}
				});
			} catch (final Exception e) {
				log.warn("Can't callback by " + toString(), e);
			}
		}

	}

	/**
	 * Only set if setKeepStdout is set (false by default), else return empty stream.
	 */
	public Stream<String> getStdoutLines(final boolean keep_empty_lines) {
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
	public Stream<String> getStderrLines(final boolean keep_empty_lines) {
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
	public Stream<String> getStdouterrLines(final boolean keep_empty_lines) {
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
	public String getStdout(final boolean keep_empty_lines, final String new_line_separator) {
		return getStdoutLines(keep_empty_lines).collect(Collectors.joining(new_line_separator));
	}

	/**
	 * Only set if setKeepStdout is set (false by default), else return empty text.
	 * @param keep_empty_lines if set false, discard all empty trimed lines
	 * @param new_line_separator replace new line char by this
	 *        Use System.lineSeparator() if needed
	 */
	public String getStderr(final boolean keep_empty_lines, final String new_line_separator) {
		return getStderrLines(keep_empty_lines).collect(Collectors.joining(new_line_separator));
	}

	/**
	 * Only set if setKeepStdout is set (false by default), else return empty text.
	 * @param keep_empty_lines if set false, discard all empty trimed lines
	 * @param new_line_separator replace new line char by this
	 *        Use System.lineSeparator() if needed
	 */
	public String getStdouterr(final boolean keep_empty_lines, final String new_line_separator) {
		return getStdouterrLines(keep_empty_lines).collect(Collectors.joining(new_line_separator));
	}

	/**
	 * Blocking
	 */
	@Override
	public CompletableFuture<? extends ExecProcessTextResult> waitForEnd() {
		return super.waitForEnd().thenApply(_this -> this);
	}

	/**
	 * Blocking during the process ends
	 */
	@Override
	public ExecProcessTextResult checkExecution() {
		try {
			if (isCorrectlyDone().get() == false) {
				throw new RuntimeException("Can't execute correcly \"" + getCommandline() + "\", " + getEndStatus().get() + " [" + getExitCode().get() + "] \"" + getStderr(false, System.lineSeparator()) + "\"");
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Can't start process " + getCommandline());
		}
		return this;
	}

}
