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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.tool.ExecutableTool;

public class ConversionTool implements ExecutableTool {
	private static Logger log = LogManager.getLogger();

	private final String execName;
	protected final ArrayList<ParameterReference> input_sources;
	protected final ArrayList<ParameterReference> output_expected_destinations;

	/**
	 * Set values for variables like <%myvar%> in the command line, do NOT set input/output references if they was set with addInputSource/addOutputDestination.
	 */
	public final LinkedHashMap<String, String> parameters_variables;// TODO not public

	private File working_directory;
	protected long max_exec_time_ms = 5000;
	protected ScheduledExecutorService max_exec_time_scheduler;
	private Consumer<ProcesslauncherBuilder> exec_process_catcher;// TODO rename, ugly
	private boolean removeParamsIfNoVarToInject;
	private final Parameters parameters;

	public ConversionTool(final String execName) throws IOException {
		this.execName = Objects.requireNonNull(execName, "\"execName\" can't to be null");

		input_sources = new ArrayList<>();
		output_expected_destinations = new ArrayList<>();
		parameters_variables = new LinkedHashMap<>(1);
		parameters = new Parameters();

		final AtomicLong counter = new AtomicLong();
		max_exec_time_scheduler = new ScheduledThreadPoolExecutor(1, r -> {
			final Thread t = new Thread(r);
			t.setName("ScheduledTask #" + counter.getAndIncrement() + " for " + getClass().getSimpleName());
			t.setDaemon(true);
			return t;
		}, (r, executor) -> log.error("Can't schedule task on {}", executor));
	}

	public boolean isRemoveParamsIfNoVarToInject() {
		return removeParamsIfNoVarToInject;
	}

	public ConversionTool setRemoveParamsIfNoVarToInject(final boolean remove_params_if_no_var_to_inject) {
		removeParamsIfNoVarToInject = remove_params_if_no_var_to_inject;
		return this;
	}

	public ConversionTool setMaxExecutionTimeForShortCommands(final long max_exec_time, final TimeUnit unit) {
		max_exec_time_ms = unit.toMillis(max_exec_time);
		return this;
	}

	public ConversionTool setMaxExecTimeScheduler(final ScheduledThreadPoolExecutor max_exec_time_scheduler) {
		this.max_exec_time_scheduler = max_exec_time_scheduler;
		return this;
	}

	public long getMaxExecTime(final TimeUnit unit) {
		return unit.convert(max_exec_time_ms, TimeUnit.MILLISECONDS);
	}

	public ScheduledExecutorService getMaxExecTimeScheduler() {
		return max_exec_time_scheduler;
	}

	/**
	 * Can operate on process before execution.
	 */
	public ConversionTool setExecProcessCatcher(final Consumer<ProcesslauncherBuilder> new_instance_catcher) {// TODO rename
		exec_process_catcher = Objects.requireNonNull(new_instance_catcher, "\"new_instance_catcher\" can't to be null");
		return this;
	}

	public Consumer<ProcesslauncherBuilder> getExecProcessCatcher() {// TODO rename
		return exec_process_catcher;
	}

	protected void applyExecProcessCatcher(final ProcesslauncherBuilder exec_process) {// TODO rename
		if (exec_process_catcher != null) {
			exec_process_catcher.accept(exec_process);
		}
	}

	class ParameterReference {
		final String ressource;
		final String var_name_in_parameters;
		final Parameters parameters_before_ref;
		final Parameters parameters_after_ref;

		ParameterReference(final String reference, final String var_name_in_parameters, final Collection<String> parameters_before_ref, final Collection<String> parameters_after_ref) {
			ressource = reference;
			if (reference == null) {
				throw new NullPointerException("\"source\" can't to be null");
			}
			this.var_name_in_parameters = var_name_in_parameters;
			if (var_name_in_parameters == null) {
				throw new NullPointerException("\"var_name_in_parameters\" can't to be null");
			}
			if (parameters_before_ref == null) {
				this.parameters_before_ref = new Parameters();
			} else {
				this.parameters_before_ref = new Parameters(parameters_before_ref);
			}
			if (parameters_after_ref == null) {
				this.parameters_after_ref = new Parameters();
			} else {
				this.parameters_after_ref = new Parameters(parameters_after_ref);
			}
		}
	}

	/**
	 * Add a parameters via an input reference, like:
	 * [parameters_before_input_source] {var_name_in_parameters replaced by source}
	 * For example, set source = "myfile", var_name_in_parameters = "IN", parameters_before_input_source = [-i],
	 * For an parameters = "exec -verbose <%IN%> -send <%OUT>", you will get an updated parameters:
	 * "exec -verbose -i myfile -send <%OUT>"
	 * @param source can be another var name (mindfuck)
	 */
	public ConversionTool addInputSource(final String source, final String var_name_in_parameters, final String... parameters_before_input_source) {
		if (parameters_before_input_source != null) {
			return addInputSource(source, var_name_in_parameters, Arrays.stream(parameters_before_input_source).filter(p -> p != null).collect(Collectors.toUnmodifiableList()), Collections.emptyList());
		}
		return addInputSource(source, var_name_in_parameters, Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Add a parameters via an input reference, like:
	 * [parameters_before_input_source] {var_name_in_parameters replaced by source} [parameters_after_input_source]
	 * For example, set source = "myfile", var_name_in_parameters = "IN", parameters_before_input_source = [-i], parameters_after_input_source = [-w],
	 * For an parameters = "exec -verbose <%IN%> -send <%OUT>", you will get an updated parameters:
	 * "exec -verbose -i myfile -w -send <%OUT>"
	 * @param source can be another var name (mindfuck)
	 * @param parameters_before_input_source can be null, and can be another var name (mindfuck)
	 * @param parameters_after_input_source can be null, and can be another var name (mindfuck)
	 */
	public ConversionTool addInputSource(final String source, final String var_name_in_parameters, final Collection<String> parameters_before_input_source, final Collection<String> parameters_after_input_source) {
		input_sources.add(new ParameterReference(source, var_name_in_parameters, parameters_before_input_source, parameters_after_input_source));
		return this;
	}

	/**
	 * Add a parameters via an output reference, like:
	 * [parameters_before_output_destination] {var_name_in_parameters replaced by destination}
	 * For example, set destination = "myfile", var_name_in_parameters = "OUT", parameters_before_output_destination = [-o],
	 * For an parameters = "exec -verbose <%IN%> -send <%OUT%>", you will get an updated parameters:
	 * "exec -verbose <%IN%> -send -o myfile"
	 * @param destination can be another var name (mindfuck)
	 */
	public ConversionTool addOutputDestination(final String destination, final String var_name_in_parameters, final String... parameters_before_output_destination) {
		if (parameters_before_output_destination != null) {
			return addOutputDestination(destination, var_name_in_parameters, Arrays.stream(parameters_before_output_destination).filter(p -> p != null).collect(Collectors.toUnmodifiableList()), Collections.emptyList());
		}
		return addOutputDestination(destination, var_name_in_parameters, Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Add a parameters via an output reference, like:
	 * [parameters_before_output_destination] {var_name_in_parameters replaced by destination} [parameters_after_output_destination]
	 * For example, set destination = "myfile", var_name_in_parameters = "OUT", parameters_before_output_destination = [-o], parameters_after_output_destination = [-w],
	 * For an parameters = "exec -verbose <%IN%> -send <%OUT%>", you will get an updated parameters:
	 * "exec -verbose <%IN%> -send -o myfile -w"
	 * @param destination can be another var name (mindfuck)
	 * @param parameters_before_output_destination can be null, and can be another var name (mindfuck)
	 * @param parameters_after_output_destination can be null, and can be another var name (mindfuck)
	 */
	public ConversionTool addOutputDestination(final String destination, final String var_name_in_parameters, final Collection<String> parameters_before_output_destination, final Collection<String> parameters_after_output_destination) {
		output_expected_destinations.add(new ParameterReference(destination, var_name_in_parameters, parameters_before_output_destination, parameters_after_output_destination));
		return this;
	}

	protected void onMissingInputOutputVar(final String var_name, final String ressource) {
		log.warn("Missing I/O variable \"" + var_name + "\" in command line \"" + getCommandLineParameters() + "\". Ressource \"" + ressource + "\" will be ignored");
	}

	public List<String> createProcessedCommandLine() throws IOException {
		final HashMap<String, String> all_vars_to_inject = new HashMap<>(parameters_variables);

		final Parameters newer_parameters = parameters.clone();
		final CommandLine newerCommandLine = new CommandLine(exec.getExecutableFile(), newer_parameters);// FIXME nope cmd line !!

		Stream.concat(input_sources.stream(), output_expected_destinations.stream()).forEach(param_ref -> {
			final String var_name = param_ref.var_name_in_parameters;

			final boolean done = newerCommandLine.injectParamsAroundVariable(var_name, param_ref.parameters_before_ref.getParameters(), param_ref.parameters_after_ref.getParameters());

			if (done) {
				if (all_vars_to_inject.containsKey(var_name)) {
					throw new RuntimeException("Variable collision: \"" + var_name + "\" was already set to \"" + all_vars_to_inject.get(var_name) + "\" in " + newer_parameters);
				}
				all_vars_to_inject.put(var_name, param_ref.ressource);
			} else {
				onMissingInputOutputVar(var_name, param_ref.ressource);
			}
		});

		return newerCommandLine.getParametersInjectVars(all_vars_to_inject, exec.isRemoveParamsIfNoVarToInject());
	}

	/**
	 * @return Can be null.
	 */
	public File getWorkingDirectory() {
		return working_directory;
	}

	public ConversionTool setWorkingDirectory(final File working_directory) throws IOException {
		if (working_directory == null) {
			throw new NullPointerException("\"working_directory\" can't to be null");
		} else if (working_directory.exists() == false) {
			throw new FileNotFoundException("\"" + working_directory.getPath() + "\" in filesytem");
		} else if (working_directory.canRead() == false) {
			throw new IOException("Can't read working_directory \"" + working_directory.getPath() + "\"");
		} else if (working_directory.isDirectory() == false) {
			throw new FileNotFoundException("\"" + working_directory.getPath() + "\" is not a directory");
		}
		this.working_directory = working_directory;
		return this;
	}

	private Executor on_error_delete_out_files_executor;

	public Executor getOnErrorDeleteOutFilesExecutor() {
		return on_error_delete_out_files_executor;
	}

	public boolean isOnErrorDeleteOutFilesExecutor() {
		return on_error_delete_out_files_executor != null;
	}

	public ConversionTool setOnErrorDeleteOutFiles(final Executor executor) {
		if (executor == null) {
			throw new NullPointerException("\"executor\" can't to be null");
		}
		on_error_delete_out_files_executor = executor;
		return this;
	}

	/*private ProcesslauncherBuilder createExec(final boolean short_command_limited_execution_time) throws IOException {
		final ProcesslauncherBuilder processlauncherBuilder = new ProcesslauncherBuilder(commandLine.getExecutable(), createProcessedCommandLine().stream().skip(1).collect(Collectors.toUnmodifiableList()));// TODO ugly as fuck

		if (short_command_limited_execution_time) {
			exec_process.setMaxExecutionTime(max_exec_time_ms, TimeUnit.MILLISECONDS, max_exec_time_scheduler);
		}

		if (working_directory != null) {
			exec_process.setWorkingDirectory(working_directory);
		}

		// TODO restore this applyExecProcessCatcher(exec_process);

		if (on_error_delete_out_files_executor != null) {
			exec_process.addEndExecutionCallback(r -> {
				**
				 * If fail transcoding or shutdown hook, delete out files (optional)
				 *
				try {
					if (r.isCorrectlyDone().get()) {
						return;
					}
				} catch (final InterruptedException e) {
					**
					 * Never start, never create files..
					 *
					return;
				} catch (final ExecutionException e) {
				}
				log.warn("Error during execution of \"" + exec_process.getExecutable().getName() + "\", remove output files");
				cleanUpOutputFiles(true, true);
			}, on_error_delete_out_files_executor);
		}

		return processlauncherBuilder;
	}*/

	/**
	 * Time controlled by setMaxExecutionTimeForShortCommands()
	 */
	/*public Processlauncher createExecWithLimitedExecutionTime() throws IOException {// TODO rename
		return createExec(true);
	}*/

	/*public Processlauncher createExec() throws IOException {// TODO rename
		return createExec(false);
	}*/

	private static final Function<ParameterReference, String> getRessourceFromParameterReference = param_ref -> param_ref.ressource;

	/**
	 * @return never null
	 */
	public Optional<String> getDeclaredSourceByVarName(final String var_name) {
		return input_sources.stream().filter(param_ref -> param_ref.var_name_in_parameters.equals(var_name)).map(getRessourceFromParameterReference).findFirst();
	}

	/**
	 * @return never null
	 */
	public Optional<String> getDeclaredDestinationByVarName(final String var_name) {
		return output_expected_destinations.stream().filter(param_ref -> param_ref.var_name_in_parameters.equals(var_name)).map(getRessourceFromParameterReference).findFirst();
	}

	/**
	 * @return never null, can be empty
	 */
	public List<String> getDeclaredSources() {
		return input_sources.stream().map(getRessourceFromParameterReference).collect(Collectors.toUnmodifiableList());
	}

	/**
	 * @return never null, can be empty
	 */
	public List<String> getDeclaredDestinations() {
		return output_expected_destinations.stream().map(getRessourceFromParameterReference).collect(Collectors.toUnmodifiableList());
	}

	public Parameters getParameters() {
		return parameters;
	}

	/**
	 * Define cmd var name like <%OUT_AUTOMATIC_n%> with "n" the # of setted destination.
	 * Add -i parameter
	 */
	public ConversionTool addSimpleOutputDestination(final String destination_name) {
		if (destination_name == null) {
			throw new NullPointerException("\"destination_name\" can't to be null");
		}

		final String varname = parameters.addVariable("OUT_AUTOMATIC_" + output_expected_destinations.size());
		addOutputDestination(destination_name, varname);
		return this;
	}

	/**
	 * Don't need to be executed before, only checks.
	 */
	public List<File> getOutputFiles(final boolean must_exists, final boolean must_be_a_regular_file, final boolean not_empty) {
		return output_expected_destinations.stream().map(dest -> dest.ressource).flatMap(ressource -> {
			try {
				final URL url = new URL(ressource);
				if (url.getProtocol().equals("file")) {
					return Stream.of(Paths.get(url.toURI()).toFile());
				}
			} catch (final MalformedURLException e) {
				/**
				 * Not an URL, maybe a file
				 */
				return Stream.of(new File(ressource));
			} catch (final URISyntaxException e) {
				/**
				 * It's an URL, but not a file
				 */
			}
			return Stream.empty();
		}).map(file -> {
			if (file.exists() == false & getWorkingDirectory() != null) {
				return new File(getWorkingDirectory().getAbsolutePath() + File.separator + file.getPath());
			}
			return file;
		}).distinct().filter(file -> {
			if (must_exists & file.exists() == false) {
				return false;
			} else if (must_be_a_regular_file & file.isFile() == false) {
				return false;
			} else if (not_empty & file.exists() & file.isFile() & file.length() == 0) {
				return false;
			}
			return true;
		}).collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Don't need to be executed before.
	 * @param remove_all if false, remove only empty files.
	 */
	public ConversionTool cleanUpOutputFiles(final boolean remove_all, final boolean clean_output_directories) {
		getOutputFiles(true, false, false).stream().filter(file -> {
			if (file.isFile()) {
				if (remove_all == false) {
					/**
					 * Remove only empty files
					 */
					if (file.length() > 0) {
						return false;
					}
				}
				return true;
			}
			/**
			 * It's a dir, remove dirs ?
			 */
			return clean_output_directories;
		}).filter(file -> {
			if (file.isFile()) {
				log.info("Delete file \"" + file + "\"");
				if (file.delete() == false) {
					throw new RuntimeException("Can't delete file \"" + file + "\"");
				}
				return false;
			}
			return true;
		}).map(dir -> dir.toPath()).flatMap(dir_path -> {
			try {
				return Files.walk(dir_path).sorted(Comparator.reverseOrder()).map(path -> path.toFile());
			} catch (final IOException e) {
				log.error("Can't access to " + dir_path, e);
				return Stream.empty();
			}
		}).forEach(file -> {
			log.info("Delete \"" + file + "\"");
			if (file.delete() == false) {
				throw new RuntimeException("Can't delete \"" + file + "\"");
			}
		});

		return this;
	}

	@Override
	public List<String> getCommandLineParameters() {
		return createProcessedCommandLine();
	}

	@Override
	public String getExecutableName() {
		return execName;
	}

}
