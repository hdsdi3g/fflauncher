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
import java.io.PrintStream;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.processlauncher.ExecutionCallbacker;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.ProcesslauncherLifecycle;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.io.CapturedStdOutErrToPrintStream;
import tv.hd3g.processlauncher.io.LineEntry;
import tv.hd3g.processlauncher.tool.ExecutableTool;

public class ConversionTool implements ExecutableTool {
	private static Logger log = LogManager.getLogger();
	private static final Predicate<LineEntry> ignoreAllLinesEventsToDisplay = le -> false;

	protected final String execName;
	protected final ArrayList<ConversionToolParameterReference> input_sources;
	protected final ArrayList<ConversionToolParameterReference> output_expected_destinations;

	private final LinkedHashMap<String, String> parametersVariables;

	private File working_directory;
	private long max_exec_time_ms;
	private ScheduledExecutorService max_exec_time_scheduler;
	private boolean removeParamsIfNoVarToInject;
	protected final Parameters parameters;
	private boolean onErrorDeleteOutFiles;
	private boolean checkSourcesBeforeReady;
	private Optional<Predicate<LineEntry>> filterForLinesEventsToDisplay;

	public ConversionTool(final String execName) {
		this(execName, new Parameters());
	}

	protected ConversionTool(final String execName, final Parameters parameters) {
		this.execName = Objects.requireNonNull(execName, "\"execName\" can't to be null");
		this.parameters = Objects.requireNonNull(parameters, "\"parameters\" can't to be null");
		max_exec_time_ms = 5000;
		input_sources = new ArrayList<>();
		output_expected_destinations = new ArrayList<>();
		parametersVariables = new LinkedHashMap<>();
		checkSourcesBeforeReady = true;
		filterForLinesEventsToDisplay = Optional.of(ignoreAllLinesEventsToDisplay);
	}

	public boolean isRemoveParamsIfNoVarToInject() {
		return removeParamsIfNoVarToInject;
	}

	public ConversionTool setRemoveParamsIfNoVarToInject(final boolean remove_params_if_no_var_to_inject) {
		removeParamsIfNoVarToInject = remove_params_if_no_var_to_inject;
		return this;
	}

	/**
	 * You needs to provide a max_exec_time_scheduler
	 */
	public ConversionTool setMaxExecutionTimeForShortCommands(final long max_exec_time, final TimeUnit unit) {
		max_exec_time_ms = unit.toMillis(max_exec_time);
		return this;
	}

	/**
	 * Enable the execution time limitation
	 */
	public ConversionTool setMaxExecTimeScheduler(final ScheduledExecutorService maxExecTimeScheduler) {
		max_exec_time_scheduler = maxExecTimeScheduler;
		return this;
	}

	public long getMaxExecTime(final TimeUnit unit) {
		return unit.convert(max_exec_time_ms, TimeUnit.MILLISECONDS);
	}

	public ScheduledExecutorService getMaxExecTimeScheduler() {
		return max_exec_time_scheduler;
	}

	public ConversionTool setFilterForLinesEventsToDisplay(final Predicate<LineEntry> filterForLinesEventsToDisplay) {
		this.filterForLinesEventsToDisplay = Optional.ofNullable(filterForLinesEventsToDisplay);
		return this;
	}

	public Optional<Predicate<LineEntry>> getFilterForLinesEventsToDisplay() {
		return filterForLinesEventsToDisplay;
	}

	/**
	 * Set values for variables like &lt;%myvar%&gt; in the command line, do NOT set input/output references if they was set with addInputSource/addOutputDestination.
	 */
	public Map<String, String> getParametersVariables() {
		return parametersVariables;
	}

	/**
	 * Add a parameters via an input reference, like:
	 * [parameters_before_input_source] {var_name_in_parameters replaced by source}
	 * For example, set source = "myfile", var_name_in_parameters = "IN", parameters_before_input_source = [-i],
	 * For an parameters = "exec -verbose &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -verbose -i myfile -send &lt;%OUT%&gt;"
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
	 * [parameters_before_input_source] {var_name_in_parameters replaced by source}
	 * For example, set source = "/myfile", var_name_in_parameters = "IN", parameters_before_input_source = [-i],
	 * For an parameters = "exec -verbose &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -verbose -i /myfile -send &lt;%OUT%&gt;"
	 */
	public ConversionTool addInputSource(final File source, final String var_name_in_parameters, final String... parameters_before_input_source) {
		if (parameters_before_input_source != null) {
			return addInputSource(source, var_name_in_parameters, Arrays.stream(parameters_before_input_source).filter(p -> p != null).collect(Collectors.toUnmodifiableList()), Collections.emptyList());
		}
		return addInputSource(source, var_name_in_parameters, Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Add a parameters via an input reference, like:
	 * [parameters_before_input_source] {var_name_in_parameters replaced by source} [parameters_after_input_source]
	 * For example, set source = "myfile", var_name_in_parameters = "IN", parameters_before_input_source = [-i], parameters_after_input_source = [-w],
	 * For an parameters = "exec -verbose &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -verbose -i myfile -w -send &lt;%OUT%&gt;"
	 * @param source can be another var name (mindfuck)
	 * @param parameters_before_input_source can be null, and can be another var name (mindfuck)
	 * @param parameters_after_input_source can be null, and can be another var name (mindfuck)
	 */
	public ConversionTool addInputSource(final String source, final String var_name_in_parameters, final Collection<String> parameters_before_input_source, final Collection<String> parameters_after_input_source) {
		input_sources.add(new ConversionToolParameterReference(source, var_name_in_parameters, parameters_before_input_source, parameters_after_input_source));
		return this;
	}

	/**
	 * Add a parameters via an input reference, like:
	 * [parameters_before_input_source] {var_name_in_parameters replaced by source} [parameters_after_input_source]
	 * For example, set source = "/myfile", var_name_in_parameters = "IN", parameters_before_input_source = [-i], parameters_after_input_source = [-w],
	 * For an parameters = "exec -verbose &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -verbose -i /myfile -w -send &lt;%OUT%&gt;"
	 * @param parameters_before_input_source can be null, and can be another var name (mindfuck)
	 * @param parameters_after_input_source can be null, and can be another var name (mindfuck)
	 */
	public ConversionTool addInputSource(final File source, final String var_name_in_parameters, final Collection<String> parameters_before_input_source, final Collection<String> parameters_after_input_source) {
		input_sources.add(new ConversionToolParameterReference(source, var_name_in_parameters, parameters_before_input_source, parameters_after_input_source));
		return this;
	}

	/**
	 * Add a parameters via an output reference, like:
	 * [parameters_before_output_destination] {var_name_in_parameters replaced by destination}
	 * For example, set destination = "myfile", var_name_in_parameters = "OUT", parameters_before_output_destination = [-o],
	 * For an parameters = "exec -verbose &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -verbose &lt;%IN%&gt; -send -o myfile"
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
	 * [parameters_before_output_destination] {var_name_in_parameters replaced by destination}
	 * For example, set destination = "myfile", var_name_in_parameters = "OUT", parameters_before_output_destination = [-o],
	 * For an parameters = "exec -verbose &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -verbose &lt;%IN%&gt; -send -o myfile"
	 */
	public ConversionTool addOutputDestination(final File destination, final String var_name_in_parameters, final String... parameters_before_output_destination) {
		if (parameters_before_output_destination != null) {
			return addOutputDestination(destination, var_name_in_parameters, Arrays.stream(parameters_before_output_destination).filter(p -> p != null).collect(Collectors.toUnmodifiableList()), Collections.emptyList());
		}
		return addOutputDestination(destination, var_name_in_parameters, Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Add a parameters via an output reference, like:
	 * [parameters_before_output_destination] {var_name_in_parameters replaced by destination} [parameters_after_output_destination]
	 * For example, set destination = "myfile", var_name_in_parameters = "OUT", parameters_before_output_destination = [-o], parameters_after_output_destination = [-w],
	 * For an parameters = "exec -verbose &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -verbose &lt;%IN%&gt; -send -o myfile -w"
	 * @param destination can be another var name (mindfuck)
	 * @param parameters_before_output_destination can be null, and can be another var name (mindfuck)
	 * @param parameters_after_output_destination can be null, and can be another var name (mindfuck)
	 */
	public ConversionTool addOutputDestination(final String destination, final String var_name_in_parameters, final Collection<String> parameters_before_output_destination, final Collection<String> parameters_after_output_destination) {
		output_expected_destinations.add(new ConversionToolParameterReference(destination, var_name_in_parameters, parameters_before_output_destination, parameters_after_output_destination));
		return this;
	}

	/**
	 * Add a parameters via an output reference, like:
	 * [parameters_before_output_destination] {var_name_in_parameters replaced by destination} [parameters_after_output_destination]
	 * For example, set destination = "myfile", var_name_in_parameters = "OUT", parameters_before_output_destination = [-o], parameters_after_output_destination = [-w],
	 * For an parameters = "exec -verbose &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -verbose &lt;%IN%&gt; -send -o myfile -w"
	 * @param parameters_before_output_destination can be null, and can be another var name (mindfuck)
	 * @param parameters_after_output_destination can be null, and can be another var name (mindfuck)
	 */
	public ConversionTool addOutputDestination(final File destination, final String var_name_in_parameters, final Collection<String> parameters_before_output_destination, final Collection<String> parameters_after_output_destination) {
		output_expected_destinations.add(new ConversionToolParameterReference(destination, var_name_in_parameters, parameters_before_output_destination, parameters_after_output_destination));
		return this;
	}

	protected void onMissingInputOutputVar(final String var_name, final String ressource) {
		log.warn("Missing I/O variable \"" + var_name + "\" in command line \"" + getInternalParameters() + "\". Ressource \"" + ressource + "\" will be ignored");
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

	public boolean isOnErrorDeleteOutFiles() {
		return onErrorDeleteOutFiles;
	}

	public ConversionTool setOnErrorDeleteOutFiles(final boolean onErrorDeleteOutFiles) {
		this.onErrorDeleteOutFiles = onErrorDeleteOutFiles;
		return this;
	}

	@Override
	public void beforeRun(final ProcesslauncherBuilder processBuilder) {
		if (max_exec_time_scheduler != null) {
			processBuilder.setExecutionTimeLimiter(max_exec_time_ms, TimeUnit.MILLISECONDS, max_exec_time_scheduler);
		}
		if (working_directory != null) {
			try {
				processBuilder.setWorkingDirectory(working_directory);
			} catch (final IOException e) {
			}
		}
		if (onErrorDeleteOutFiles) {
			/**
			 * If fail transcoding or shutdown hook, delete out files (optional)
			 */
			processBuilder.addExecutionCallbacker(new ExecutionCallbacker() {
				@Override
				public void onEndExecution(final ProcesslauncherLifecycle processlauncherLifecycle) {
					if (processlauncherLifecycle.isCorrectlyDone() == false) {
						log.warn("Error during execution of \"" + processlauncherLifecycle.toString() + "\", remove output files");
						cleanUpOutputFiles(true, true);
					}
				}
			});
		}

		filterForLinesEventsToDisplay.filter(ffletd -> ignoreAllLinesEventsToDisplay.equals(ffletd) == false).ifPresent(filter -> {
			final CapturedStdOutErrToPrintStream psOut = new CapturedStdOutErrToPrintStream(getStdOutPrintStreamToDisplayLinesEvents(), getStdErrPrintStreamToDisplayLinesEvents());
			psOut.setFilter(filter);
			processBuilder.getSetCaptureStandardOutputAsOutputText().getObservers().add(psOut);
		});
	}

	protected PrintStream getStdOutPrintStreamToDisplayLinesEvents() {
		return System.out;
	}

	protected PrintStream getStdErrPrintStreamToDisplayLinesEvents() {
		return System.err;
	}

	/**
	 * @return never null
	 */
	public Optional<String> getDeclaredSourceByVarName(final String var_name) {
		return input_sources.stream().filter(param_ref -> param_ref.isVarNameInParametersEquals(var_name)).map(ConversionToolParameterReference::getRessource).findFirst();
	}

	/**
	 * @return never null
	 */
	public Optional<String> getDeclaredDestinationByVarName(final String var_name) {
		return output_expected_destinations.stream().filter(param_ref -> param_ref.isVarNameInParametersEquals(var_name)).map(ConversionToolParameterReference::getRessource).findFirst();
	}

	/**
	 * @return never null, can be empty
	 */
	public List<String> getDeclaredSources() {
		return input_sources.stream().map(ConversionToolParameterReference::getRessource).collect(Collectors.toUnmodifiableList());
	}

	/**
	 * @return never null, can be empty
	 */
	public List<String> getDeclaredDestinations() {
		return output_expected_destinations.stream().map(ConversionToolParameterReference::getRessource).collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Define cmd var name like &lt;%OUT_AUTOMATIC_n%&gt; with "n" the # of setted destination.
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
	 * Define cmd var name like &lt;%OUT_AUTOMATIC_n%&gt; with "n" the # of setted destination.
	 * Add -i parameter
	 */
	public ConversionTool addSimpleOutputDestination(final File destination_file) {
		if (destination_file == null) {
			throw new NullPointerException("\"destination_file\" can't to be null");
		}

		final String varname = parameters.addVariable("OUT_AUTOMATIC_" + output_expected_destinations.size());
		addOutputDestination(destination_file, varname);
		return this;
	}

	/**
	 * Don't need to be executed before, only checks.
	 */
	public List<File> getOutputFiles(final OutputFilePresencePolicy filterPolicy) {
		return output_expected_destinations.stream().map(ConversionToolParameterReference::getRessource).flatMap(ressource -> {
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
		}).distinct().filter(filterPolicy.filter()).collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Don't need to be executed before.
	 * @param remove_all if false, remove only empty files.
	 */
	public ConversionTool cleanUpOutputFiles(final boolean remove_all, final boolean clean_output_directories) {
		getOutputFiles(OutputFilePresencePolicy.MUST_EXISTS).stream().filter(file -> {
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

	/**
	 * @return without variable injection
	 */
	public Parameters getInternalParameters() {
		return parameters;
	}

	/**
	 * True by default. Force to check read access for every files set in input.
	 * @return this
	 */
	public ConversionTool setCheckSourcesBeforeReady(final boolean checkSourcesBeforeReady) {
		this.checkSourcesBeforeReady = checkSourcesBeforeReady;
		return this;
	}

	/**
	 * @return true by default. Force to check read access for every files set in input.
	 */
	public boolean isCheckSourcesBeforeReady() {
		return checkSourcesBeforeReady;
	}

	/**
	 * Check read access for every files set in input.
	 * @throws RuntimeException
	 */
	public ConversionTool checkSources() {
		input_sources.forEach(s -> {
			try {
				s.checkOpenRessourceAsFile();
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException("Can't open file \"" + s + "\" for check reading", e);
			}
		});
		return this;
	}

	/**
	 * Check read access for every files set in output.
	 * @throws RuntimeException
	 */
	public ConversionTool checkDestinations() {
		output_expected_destinations.forEach(s -> {
			try {
				s.checkOpenRessourceAsFile();
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException("Can't open file \"" + s + "\" for check reading", e);
			}
		});
		return this;
	}

	/**
	 * @return a copy form internal parameters, with variable injection
	 */
	@Override
	public Parameters getReadyToRunParameters() {
		if (checkSourcesBeforeReady) {
			checkSources();
		}
		final HashMap<String, String> all_vars_to_inject = new HashMap<>(parametersVariables);

		final Parameters newer_parameters = parameters.clone();

		Stream.concat(input_sources.stream(), output_expected_destinations.stream()).forEach(param_ref -> {
			final String var_name = param_ref.getVarNameInParameters();

			final boolean done = newer_parameters.injectParamsAroundVariable(var_name, param_ref.getParametersListBeforeRef(), param_ref.getParametersListAfterRef());

			if (done) {
				if (all_vars_to_inject.containsKey(var_name)) {
					throw new RuntimeException("Variable collision: \"" + var_name + "\" was already set to \"" + all_vars_to_inject.get(var_name) + "\" in " + newer_parameters);
				}
				all_vars_to_inject.put(var_name, param_ref.getRessource());
			} else {
				onMissingInputOutputVar(var_name, param_ref.getRessource());
			}
		});

		return newer_parameters.injectVariables(all_vars_to_inject, removeParamsIfNoVarToInject);
	}

	@Override
	public String getExecutableName() {
		return execName;
	}

}
