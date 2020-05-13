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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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

import tv.hd3g.processlauncher.CapturedStdOutErrToPrintStream;
import tv.hd3g.processlauncher.ExecutionCallbacker;
import tv.hd3g.processlauncher.LineEntry;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.ProcesslauncherLifecycle;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.tool.ExecutableTool;

public class ConversionTool implements ExecutableTool {
	private static Logger log = LogManager.getLogger();
	private static final Predicate<LineEntry> ignoreAllLinesEventsToDisplay = le -> false;

	protected final String execName;
	protected final List<ConversionToolParameterReference> inputSources;
	protected final List<ConversionToolParameterReference> outputExpectedDestinations;

	private final LinkedHashMap<String, String> parametersVariables;

	private File workingDirectory;
	private long maxExecTimeMs;
	private ScheduledExecutorService maxExecTimeScheduler;
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
		maxExecTimeMs = 5000;
		inputSources = new ArrayList<>();
		outputExpectedDestinations = new ArrayList<>();
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
	 * You needs to provide a maxExecTimeScheduler
	 */
	public ConversionTool setMaxExecutionTimeForShortCommands(final long max_exec_time, final TimeUnit unit) {
		maxExecTimeMs = unit.toMillis(max_exec_time);
		return this;
	}

	/**
	 * Enable the execution time limitation
	 */
	public ConversionTool setMaxExecTimeScheduler(final ScheduledExecutorService maxExecTimeScheduler) {
		this.maxExecTimeScheduler = maxExecTimeScheduler;
		return this;
	}

	public long getMaxExecTime(final TimeUnit unit) {
		return unit.convert(maxExecTimeMs, TimeUnit.MILLISECONDS);
	}

	public ScheduledExecutorService getMaxExecTimeScheduler() {
		return maxExecTimeScheduler;
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
	 * [parametersBeforeInputSource] {varNameInParameters replaced by source}
	 * For example, set source = "myfile", varNameInParameters = "IN", parametersBeforeInputSource = [-i],
	 * For an parameters = "exec -VERBOSE &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -VERBOSE -i myfile -send &lt;%OUT%&gt;"
	 * @param source can be another var name (mindfuck)
	 */
	public ConversionTool addInputSource(final String source,
	                                     final String varNameInParameters,
	                                     final String... parametersBeforeInputSource) {
		if (parametersBeforeInputSource != null) {
			return addInputSource(source, varNameInParameters,
			        Arrays.stream(parametersBeforeInputSource)
			                .filter(Objects::nonNull)
			                .collect(Collectors.toUnmodifiableList()), Collections.emptyList());
		}
		return addInputSource(source, varNameInParameters, Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Add a parameters via an input reference, like:
	 * [parametersBeforeInputSource] {varNameInParameters replaced by source}
	 * For example, set source = "/myfile", varNameInParameters = "IN", parametersBeforeInputSource = [-i],
	 * For an parameters = "exec -VERBOSE &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -VERBOSE -i /myfile -send &lt;%OUT%&gt;"
	 */
	public ConversionTool addInputSource(final File source,
	                                     final String varNameInParameters,
	                                     final String... parametersBeforeInputSource) {
		if (parametersBeforeInputSource != null) {
			return addInputSource(source, varNameInParameters,
			        Arrays.stream(parametersBeforeInputSource)
			                .filter(Objects::nonNull)
			                .collect(Collectors.toUnmodifiableList()), Collections.emptyList());
		}
		return addInputSource(source, varNameInParameters, Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Add a parameters via an input reference, like:
	 * [parametersBeforeInputSource] {varNameInParameters replaced by source} [parametersAfterInputSource]
	 * For example, set source = "myfile", varNameInParameters = "IN", parametersBeforeInputSource = [-i], parametersAfterInputSource = [-w],
	 * For an parameters = "exec -VERBOSE &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -VERBOSE -i myfile -w -send &lt;%OUT%&gt;"
	 * @param source can be another var name (mindfuck)
	 * @param parametersBeforeInputSource can be null, and can be another var name (mindfuck)
	 * @param parametersAfterInputSource can be null, and can be another var name (mindfuck)
	 */
	public ConversionTool addInputSource(final String source,
	                                     final String varNameInParameters,
	                                     final Collection<String> parametersBeforeInputSource,
	                                     final Collection<String> parametersAfterInputSource) {
		inputSources.add(new ConversionToolParameterReference(source, varNameInParameters,
		        parametersBeforeInputSource, parametersAfterInputSource));
		return this;
	}

	/**
	 * Add a parameters via an input reference, like:
	 * [parametersBeforeInputSource] {varNameInParameters replaced by source} [parametersAfterInputSource]
	 * For example, set source = "/myfile", varNameInParameters = "IN", parametersBeforeInputSource = [-i], parametersAfterInputSource = [-w],
	 * For an parameters = "exec -VERBOSE &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -VERBOSE -i /myfile -w -send &lt;%OUT%&gt;"
	 * @param parametersBeforeInputSource can be null, and can be another var name (mindfuck)
	 * @param parametersAfterInputSource can be null, and can be another var name (mindfuck)
	 */
	public ConversionTool addInputSource(final File source,
	                                     final String varNameInParameters,
	                                     final Collection<String> parametersBeforeInputSource,
	                                     final Collection<String> parametersAfterInputSource) {
		inputSources.add(new ConversionToolParameterReference(source, varNameInParameters,
		        parametersBeforeInputSource, parametersAfterInputSource));
		return this;
	}

	/**
	 * Add a parameters via an output reference, like:
	 * [parametersBeforeOutputDestination] {varNameInParameters replaced by destination}
	 * For example, set destination = "myfile", varNameInParameters = "OUT", parametersBeforeOutputDestination = [-o],
	 * For an parameters = "exec -VERBOSE &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -VERBOSE &lt;%IN%&gt; -send -o myfile"
	 * @param destination can be another var name (mindfuck)
	 */
	public ConversionTool addOutputDestination(final String destination,
	                                           final String varNameInParameters,
	                                           final String... parametersBeforeOutputDestination) {
		if (parametersBeforeOutputDestination != null) {
			return addOutputDestination(destination, varNameInParameters,
			        Arrays.stream(parametersBeforeOutputDestination)
			                .filter(Objects::nonNull)
			                .collect(Collectors.toUnmodifiableList()), Collections.emptyList());
		}
		return addOutputDestination(destination, varNameInParameters,
		        Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Add a parameters via an output reference, like:
	 * [parametersBeforeOutputDestination] {varNameInParameters replaced by destination}
	 * For example, set destination = "myfile", varNameInParameters = "OUT", parametersBeforeOutputDestination = [-o],
	 * For an parameters = "exec -VERBOSE &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -VERBOSE &lt;%IN%&gt; -send -o myfile"
	 */
	public ConversionTool addOutputDestination(final File destination,
	                                           final String varNameInParameters,
	                                           final String... parametersBeforeOutputDestination) {
		if (parametersBeforeOutputDestination != null) {
			return addOutputDestination(destination, varNameInParameters,
			        Arrays.stream(parametersBeforeOutputDestination)
			                .filter(Objects::nonNull)
			                .collect(Collectors.toUnmodifiableList()), Collections.emptyList());
		}
		return addOutputDestination(destination, varNameInParameters,
		        Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Add a parameters via an output reference, like:
	 * [parametersBeforeOutputDestination] {varNameInParameters replaced by destination} [parametersAfterOutputDestination]
	 * For example, set destination = "myfile", varNameInParameters = "OUT", parametersBeforeOutputDestination = [-o], parametersAfterOutputDestination = [-w],
	 * For an parameters = "exec -VERBOSE &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -VERBOSE &lt;%IN%&gt; -send -o myfile -w"
	 * @param destination can be another var name (mindfuck)
	 * @param parametersBeforeOutputDestination can be null, and can be another var name (mindfuck)
	 * @param parametersAfterOutputDestination can be null, and can be another var name (mindfuck)
	 */
	public ConversionTool addOutputDestination(final String destination,
	                                           final String varNameInParameters,
	                                           final Collection<String> parametersBeforeOutputDestination,
	                                           final Collection<String> parametersAfterOutputDestination) {
		outputExpectedDestinations.add(new ConversionToolParameterReference(destination, varNameInParameters,
		        parametersBeforeOutputDestination, parametersAfterOutputDestination));
		return this;
	}

	/**
	 * Add a parameters via an output reference, like:
	 * [parametersBeforeOutputDestination] {varNameInParameters replaced by destination} [parametersAfterOutputDestination]
	 * For example, set destination = "myfile", varNameInParameters = "OUT", parametersBeforeOutputDestination = [-o], parametersAfterOutputDestination = [-w],
	 * For an parameters = "exec -VERBOSE &lt;%IN%&gt; -send &lt;%OUT%&gt;", you will get an updated parameters:
	 * "exec -VERBOSE &lt;%IN%&gt; -send -o myfile -w"
	 * @param parametersBeforeOutputDestination can be null, and can be another var name (mindfuck)
	 * @param parametersAfterOutputDestination can be null, and can be another var name (mindfuck)
	 */
	public ConversionTool addOutputDestination(final File destination,
	                                           final String varNameInParameters,
	                                           final Collection<String> parametersBeforeOutputDestination,
	                                           final Collection<String> parametersAfterOutputDestination) {
		outputExpectedDestinations.add(new ConversionToolParameterReference(destination, varNameInParameters,
		        parametersBeforeOutputDestination, parametersAfterOutputDestination));
		return this;
	}

	protected void onMissingInputOutputVar(final String var_name, final String ressource) {
		log.warn("Missing I/O variable \"{}\" in command line \"{}\". Ressource \"{}\" will be ignored",
		        var_name, getInternalParameters(), ressource);
	}

	/**
	 * @return Can be null.
	 */
	public File getWorkingDirectory() {
		return workingDirectory;
	}

	public ConversionTool setWorkingDirectory(final File workingDirectory) throws IOException {
		if (workingDirectory == null) {
			throw new NullPointerException("\"workingDirectory\" can't to be null");
		} else if (workingDirectory.exists() == false) {
			throw new FileNotFoundException("\"" + workingDirectory.getPath() + "\" in filesytem");
		} else if (workingDirectory.canRead() == false) {
			throw new IOException("Can't read workingDirectory \"" + workingDirectory.getPath() + "\"");
		} else if (workingDirectory.isDirectory() == false) {
			throw new FileNotFoundException("\"" + workingDirectory.getPath() + "\" is not a directory");
		}
		this.workingDirectory = workingDirectory;
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
		if (maxExecTimeScheduler != null) {
			processBuilder.setExecutionTimeLimiter(maxExecTimeMs, TimeUnit.MILLISECONDS, maxExecTimeScheduler);
		}
		if (workingDirectory != null) {
			try {
				processBuilder.setWorkingDirectory(workingDirectory);
			} catch (final IOException e) {// NOSONAR
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
						log.warn("Error during execution of \"{}\", remove output files", processlauncherLifecycle);
						cleanUpOutputFiles(true, true);
					}
				}
			});
		}

		filterForLinesEventsToDisplay
		        .filter(ffletd -> ignoreAllLinesEventsToDisplay.equals(ffletd) == false)
		        .ifPresent(
		                filter -> {
			                final CapturedStdOutErrToPrintStream psOut = new CapturedStdOutErrToPrintStream(
			                        getStdOutPrintStreamToDisplayLinesEvents(),
			                        getStdErrPrintStreamToDisplayLinesEvents());
			                psOut.setFilter(filter);
			                processBuilder.getSetCaptureStandardOutputAsOutputText().addObserver(psOut);
		                });
	}

	protected PrintStream getStdOutPrintStreamToDisplayLinesEvents() {
		return System.out;// NOSONAR
	}

	protected PrintStream getStdErrPrintStreamToDisplayLinesEvents() {
		return System.err;// NOSONAR
	}

	/**
	 * @return never null
	 */
	public Optional<String> getDeclaredSourceByVarName(final String var_name) {
		return inputSources.stream().filter(paramRef -> paramRef.isVarNameInParametersEquals(var_name)).map(
		        ConversionToolParameterReference::getRessource).findFirst();
	}

	/**
	 * @return never null
	 */
	public Optional<String> getDeclaredDestinationByVarName(final String var_name) {
		return outputExpectedDestinations.stream().filter(paramRef -> paramRef.isVarNameInParametersEquals(
		        var_name)).map(ConversionToolParameterReference::getRessource).findFirst();
	}

	/**
	 * @return never null, can be empty
	 */
	public List<String> getDeclaredSources() {
		return inputSources.stream().map(ConversionToolParameterReference::getRessource).collect(Collectors
		        .toUnmodifiableList());
	}

	/**
	 * @return never null, can be empty
	 */
	public List<String> getDeclaredDestinations() {
		return outputExpectedDestinations.stream().map(ConversionToolParameterReference::getRessource).collect(
		        Collectors.toUnmodifiableList());
	}

	/**
	 * Define cmd var name like &lt;%OUT_AUTOMATIC_n%&gt; with "n" the # of setted destination.
	 * Add -i parameter
	 */
	public ConversionTool addSimpleOutputDestination(final String destination_name) {
		if (destination_name == null) {
			throw new NullPointerException("\"destination_name\" can't to be null");
		}

		final String varname = parameters.addVariable("OUT_AUTOMATIC_" + outputExpectedDestinations.size());
		addOutputDestination(destination_name, varname);
		return this;
	}

	/**
	 * Define cmd var name like &lt;%OUT_AUTOMATIC_n%&gt; with "n" the # of setted destination.
	 * Add -i parameter
	 */
	public ConversionTool addSimpleOutputDestination(final File destinationFile) {
		if (destinationFile == null) {
			throw new NullPointerException("\"destinationFile\" can't to be null");
		}

		final String varname = parameters.addVariable("OUT_AUTOMATIC_" + outputExpectedDestinations.size());
		addOutputDestination(destinationFile, varname);
		return this;
	}

	/**
	 * Don't need to be executed before, only checks.
	 */
	public List<File> getOutputFiles(final OutputFilePresencePolicy filterPolicy) {
		return outputExpectedDestinations.stream().map(ConversionToolParameterReference::getRessource).flatMap(
		        ressource -> {
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
			        if (file.exists() == false && getWorkingDirectory() != null) {
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
			if (file.isFile() == false) {
				/**
				 * It's a dir, remove dirs ?
				 */
				return clean_output_directories;
			}
			/**
			 * Remove only empty files
			 */
			return (remove_all == false && file.length() > 0) == false;
		}).filter(file -> {
			if (file.isFile()) {
				log.info("Delete file \"{}\"", file);
				if (file.delete() == false) {
					throw new RuntimeException("Can't delete file \"" + file + "\"");
				}
				return false;
			}
			return true;
		}).map(File::toPath).flatMap(dirPath -> {
			try {
				return Files.walk(dirPath).sorted(Comparator.reverseOrder()).map(Path::toFile);
			} catch (final IOException e) {
				log.error("Can't access to {}", dirPath, e);
				return Stream.empty();
			}
		}).forEach(file -> {
			log.info("Delete \"{}\"", file);
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
		inputSources.forEach(s -> {
			try {
				s.checkOpenRessourceAsFile();
			} catch (IOException | InterruptedException e) {// NOSONAR
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
		outputExpectedDestinations.forEach(s -> {
			try {
				s.checkOpenRessourceAsFile();
			} catch (IOException | InterruptedException e) {// NOSONAR
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
		final HashMap<String, String> allVarsToInject = new HashMap<>(parametersVariables);

		final Parameters newerParameters = parameters.duplicate();

		Stream.concat(inputSources.stream(), outputExpectedDestinations.stream()).forEach(paramRef -> {
			final String var_name = paramRef.getVarNameInParameters();

			final boolean done = newerParameters.injectParamsAroundVariable(var_name, paramRef
			        .getParametersListBeforeRef(), paramRef.getParametersListAfterRef());

			if (done) {
				if (allVarsToInject.containsKey(var_name)) {
					throw new RuntimeException("Variable collision: \"" + var_name + "\" was already set to \""
					                           + allVarsToInject.get(var_name) + "\" in " + newerParameters);
				}
				allVarsToInject.put(var_name, paramRef.getRessource());
			} else {
				onMissingInputOutputVar(var_name, paramRef.getRessource());
			}
		});

		return newerParameters.injectVariables(allVarsToInject, removeParamsIfNoVarToInject);
	}

	@Override
	public String getExecutableName() {
		return execName;
	}

}
