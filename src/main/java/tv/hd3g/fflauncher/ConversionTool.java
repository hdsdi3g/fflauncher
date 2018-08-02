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
import java.util.Optional;
import java.util.concurrent.ExecutionException;
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

import tv.hd3g.execprocess.CommandLineProcessor.CommandLine;
import tv.hd3g.execprocess.CommandLineProcessor.CommandLine.ProcessedCommandLine;
import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.execprocess.ParametersUtility;

public class ConversionTool {
	private static Logger log = LogManager.getLogger();
	
	protected final File executable;
	protected long max_exec_time_ms = 5000;
	protected ScheduledExecutorService max_exec_time_scheduler;
	private Consumer<ExecProcessText> exec_process_catcher;
	
	protected final CommandLine command_line;
	protected final ArrayList<ParameterReference> input_sources;
	protected final ArrayList<ParameterReference> output_expected_destinations;
	private File working_directory;
	
	/**
	 * Set values for variables like <%myvar%> in the command line, do NOT set input/output references if they was set with addInputSource/addOutputDestination.
	 */
	public final LinkedHashMap<String, String> parameters_variables;
	private boolean remove_params_if_no_var_to_inject;
	
	protected final ExecutableFinder exec_finder;
	
	public ConversionTool(ExecutableFinder exec_finder, CommandLine command_line) throws FileNotFoundException {
		this.command_line = command_line;
		if (command_line == null) {
			throw new NullPointerException("\"command_line\" can't to be null");
		}
		this.exec_finder = exec_finder;
		if (exec_finder == null) {
			throw new NullPointerException("\"exec_finder\" can't to be null");
		}
		
		executable = exec_finder.get(command_line.getExecName());
		if (executable.exists() == false) {
			throw new FileNotFoundException("Can't found " + executable);
		} else if (executable.isFile() == false) {
			throw new FileNotFoundException("Not a regular file: " + executable);
		} else if (executable.canRead() == false) {
			throw new FileNotFoundException("Can't read " + executable);
		} else if (executable.canExecute() == false) {
			throw new FileNotFoundException("Can't execute " + executable);
		}
		log.debug("Use executable {}", executable.getPath());
		
		AtomicLong counter = new AtomicLong();
		
		max_exec_time_scheduler = new ScheduledThreadPoolExecutor(1, r -> {
			Thread t = new Thread(r);
			t.setName("ScheduledTask #" + counter.getAndIncrement() + " for " + getClass().getSimpleName());
			t.setDaemon(true);
			return t;
		}, (r, executor) -> log.error("Can't schedule task on {}", executor));
		
		input_sources = new ArrayList<>(1);
		output_expected_destinations = new ArrayList<>(1);
		
		parameters_variables = new LinkedHashMap<>(1);
		remove_params_if_no_var_to_inject = false;
	}
	
	/**
	 * @return false by default
	 */
	public boolean isRemoveParamsIfNoVarToInject() {
		return remove_params_if_no_var_to_inject;
	}
	
	/**
	 * @param remove_params_if_no_var_to_inject false by default
	 */
	public ConversionTool setRemoveParamsIfNoVarToInject(boolean remove_params_if_no_var_to_inject) {
		this.remove_params_if_no_var_to_inject = remove_params_if_no_var_to_inject;
		return this;
	}
	
	public File getExecutable() {
		return executable;
	}
	
	public ConversionTool setMaxExecutionTimeForShortCommands(long max_exec_time, TimeUnit unit) {
		max_exec_time_ms = unit.toMillis(max_exec_time);
		return this;
	}
	
	public ConversionTool setMaxExecTimeScheduler(ScheduledThreadPoolExecutor max_exec_time_scheduler) {
		this.max_exec_time_scheduler = max_exec_time_scheduler;
		return this;
	}
	
	public long getMaxExecTime(TimeUnit unit) {
		return unit.convert(max_exec_time_ms, TimeUnit.MILLISECONDS);
	}
	
	public ScheduledExecutorService getMaxExecTimeScheduler() {
		return max_exec_time_scheduler;
	}
	
	/**
	 * Can operate on process before execution.
	 */
	public ConversionTool setExecProcessCatcher(Consumer<ExecProcessText> new_instance_catcher) {
		exec_process_catcher = new_instance_catcher;
		if (new_instance_catcher == null) {
			throw new NullPointerException("\"new_instance_catcher\" can't to be null");
		}
		return this;
	}
	
	public Consumer<ExecProcessText> getExecProcessCatcher() {
		return exec_process_catcher;
	}
	
	protected void applyExecProcessCatcher(ExecProcessText exec_process) {
		if (exec_process_catcher != null) {
			exec_process_catcher.accept(exec_process);
		}
	}
	
	class ParameterReference {
		final String ressource;
		final String var_name_in_command_line;
		final ParametersUtility parameters_before_ref;
		final ParametersUtility parameters_after_ref;
		
		ParameterReference(String reference, String var_name_in_command_line, Collection<String> parameters_before_ref, Collection<String> parameters_after_ref) {
			ressource = reference;
			if (reference == null) {
				throw new NullPointerException("\"source\" can't to be null");
			}
			this.var_name_in_command_line = var_name_in_command_line;
			if (var_name_in_command_line == null) {
				throw new NullPointerException("\"var_name_in_command_line\" can't to be null");
			}
			if (parameters_before_ref == null) {
				this.parameters_before_ref = new ParametersUtility();
			} else {
				this.parameters_before_ref = new ParametersUtility(parameters_before_ref);
			}
			if (parameters_after_ref == null) {
				this.parameters_after_ref = new ParametersUtility();
			} else {
				this.parameters_after_ref = new ParametersUtility(parameters_after_ref);
			}
		}
	}
	
	/**
	 * Add a parameters via an input reference, like:
	 * [parameters_before_input_source] {var_name_in_command_line replaced by source}
	 * For example, set source = "myfile", var_name_in_command_line = "IN", parameters_before_input_source = [-i],
	 * For an command_line = "exec -verbose <%IN%> -send <%OUT>", you will get an updated command_line:
	 * "exec -verbose -i myfile -send <%OUT>"
	 * @param source can be another var name (mindfuck)
	 */
	public ConversionTool addInputSource(String source, String var_name_in_command_line, String... parameters_before_input_source) {
		if (parameters_before_input_source != null) {
			return addInputSource(source, var_name_in_command_line, Arrays.stream(parameters_before_input_source).filter(p -> p != null).collect(Collectors.toUnmodifiableList()), Collections.emptyList());
		}
		return addInputSource(source, var_name_in_command_line, Collections.emptyList(), Collections.emptyList());
	}
	
	/**
	 * Add a parameters via an input reference, like:
	 * [parameters_before_input_source] {var_name_in_command_line replaced by source} [parameters_after_input_source]
	 * For example, set source = "myfile", var_name_in_command_line = "IN", parameters_before_input_source = [-i], parameters_after_input_source = [-w],
	 * For an command_line = "exec -verbose <%IN%> -send <%OUT>", you will get an updated command_line:
	 * "exec -verbose -i myfile -w -send <%OUT>"
	 * @param source can be another var name (mindfuck)
	 * @param parameters_before_input_source can be null, and can be another var name (mindfuck)
	 * @param parameters_after_input_source can be null, and can be another var name (mindfuck)
	 */
	public ConversionTool addInputSource(String source, String var_name_in_command_line, Collection<String> parameters_before_input_source, Collection<String> parameters_after_input_source) {
		input_sources.add(new ParameterReference(source, var_name_in_command_line, parameters_before_input_source, parameters_after_input_source));
		return this;
	}
	
	/**
	 * Add a parameters via an output reference, like:
	 * [parameters_before_output_destination] {var_name_in_command_line replaced by destination}
	 * For example, set destination = "myfile", var_name_in_command_line = "OUT", parameters_before_output_destination = [-o],
	 * For an command_line = "exec -verbose <%IN%> -send <%OUT%>", you will get an updated command_line:
	 * "exec -verbose <%IN%> -send -o myfile"
	 * @param destination can be another var name (mindfuck)
	 */
	public ConversionTool addOutputDestination(String destination, String var_name_in_command_line, String... parameters_before_output_destination) {
		if (parameters_before_output_destination != null) {
			return addOutputDestination(destination, var_name_in_command_line, Arrays.stream(parameters_before_output_destination).filter(p -> p != null).collect(Collectors.toUnmodifiableList()), Collections.emptyList());
		}
		return addOutputDestination(destination, var_name_in_command_line, Collections.emptyList(), Collections.emptyList());
	}
	
	/**
	 * Add a parameters via an output reference, like:
	 * [parameters_before_output_destination] {var_name_in_command_line replaced by destination} [parameters_after_output_destination]
	 * For example, set destination = "myfile", var_name_in_command_line = "OUT", parameters_before_output_destination = [-o], parameters_after_output_destination = [-w],
	 * For an command_line = "exec -verbose <%IN%> -send <%OUT%>", you will get an updated command_line:
	 * "exec -verbose <%IN%> -send -o myfile -w"
	 * @param destination can be another var name (mindfuck)
	 * @param parameters_before_output_destination can be null, and can be another var name (mindfuck)
	 * @param parameters_after_output_destination can be null, and can be another var name (mindfuck)
	 */
	public ConversionTool addOutputDestination(String destination, String var_name_in_command_line, Collection<String> parameters_before_output_destination, Collection<String> parameters_after_output_destination) {
		output_expected_destinations.add(new ParameterReference(destination, var_name_in_command_line, parameters_before_output_destination, parameters_after_output_destination));
		return this;
	}
	
	protected void onMissingInputOutputVar(String var_name, String ressource) {
		log.warn("Missing I/O variable \"" + var_name + "\" in command line \"" + command_line.toString() + "\". Ressource \"" + ressource + "\" will be ignored");
	}
	
	public ProcessedCommandLine createProcessedCommandLine() {
		final HashMap<String, String> all_vars_to_inject = new HashMap<>(parameters_variables);
		
		CommandLine newer_command_line = command_line.clone();
		
		Stream.concat(input_sources.stream(), output_expected_destinations.stream()).forEach(param_ref -> {
			String var_name = param_ref.var_name_in_command_line;
			
			boolean done = newer_command_line.injectParamsAroundVariable(var_name, param_ref.parameters_before_ref.getParameters(), param_ref.parameters_after_ref.getParameters());
			
			if (done) {
				if (all_vars_to_inject.containsKey(var_name)) {
					throw new RuntimeException("Variable collision: \"" + var_name + "\" was already set to \"" + all_vars_to_inject.get(var_name) + "\" in " + newer_command_line);
				}
				all_vars_to_inject.put(var_name, param_ref.ressource);
			} else {
				onMissingInputOutputVar(var_name, param_ref.ressource);
			}
		});
		
		return newer_command_line.process(all_vars_to_inject, remove_params_if_no_var_to_inject);
	}
	
	/**
	 * @return Can be null.
	 */
	public File getWorkingDirectory() {
		return working_directory;
	}
	
	public ConversionTool setWorkingDirectory(File working_directory) throws IOException {
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
	
	public ConversionTool setOnErrorDeleteOutFiles(Executor executor) {
		if (executor == null) {
			throw new NullPointerException("\"executor\" can't to be null");
		}
		on_error_delete_out_files_executor = executor;
		return this;
	}
	
	private ExecProcessText createExec(boolean short_command_limited_execution_time) throws IOException {
		ExecProcessText exec_process = new ExecProcessText(executable);
		
		if (short_command_limited_execution_time) {
			exec_process.setMaxExecutionTime(max_exec_time_ms, TimeUnit.MILLISECONDS, max_exec_time_scheduler);
		}
		
		if (working_directory != null) {
			exec_process.setWorkingDirectory(working_directory);
		}
		
		exec_process.importParametersFrom(createProcessedCommandLine());
		applyExecProcessCatcher(exec_process);
		
		if (on_error_delete_out_files_executor != null) {
			exec_process.addEndExecutionCallback(r -> {
				/**
				 * If fail transcoding or shutdown hook, delete out files (optional)
				 */
				try {
					if (r.isCorrectlyDone().get()) {
						return;
					}
				} catch (InterruptedException e) {
					/**
					 * Never start, never create files..
					 */
					return;
				} catch (ExecutionException e) {
				}
				log.warn("Error during execution of \"" + exec_process.getExecutable().getName() + "\", remove output files");
				cleanUpOutputFiles(true, true);
			}, on_error_delete_out_files_executor);
		}
		
		return exec_process;
	}
	
	/**
	 * Time controlled by setMaxExecutionTimeForShortCommands()
	 */
	public ExecProcessText createExecWithLimitedExecutionTime() throws IOException {
		return createExec(true);
	}
	
	public ExecProcessText createExec() throws IOException {
		return createExec(false);
	}
	
	private static final Function<ParameterReference, String> getRessourceFromParameterReference = param_ref -> param_ref.ressource;
	
	/**
	 * @return never null
	 */
	public Optional<String> getDeclaredSourceByVarName(String var_name) {
		return input_sources.stream().filter(param_ref -> param_ref.var_name_in_command_line.equals(var_name)).map(getRessourceFromParameterReference).findFirst();
	}
	
	/**
	 * @return never null
	 */
	public Optional<String> getDeclaredDestinationByVarName(String var_name) {
		return output_expected_destinations.stream().filter(param_ref -> param_ref.var_name_in_command_line.equals(var_name)).map(getRessourceFromParameterReference).findFirst();
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
	
	/**
	 * @return current command_line, with raw variables.
	 */
	public CommandLine getCommandLine() {
		return command_line;
	}
	
	/**
	 * Define cmd var name like <%OUT_AUTOMATIC_n%> with "n" the # of setted destination.
	 * Add -i parameter
	 */
	public ConversionTool addSimpleOutputDestination(String destination_name) {
		if (destination_name == null) {
			throw new NullPointerException("\"destination_name\" can't to be null");
		}
		
		/*Stream<String> s_source_options = Stream.empty();
		if (source_options != null) {
			s_source_options = Arrays.stream(source_options);
		}*/
		
		String varname = command_line.addVariable("OUT_AUTOMATIC_" + output_expected_destinations.size());
		addOutputDestination(destination_name, varname);
		return this;
	}
	
	/**
	 * Don't need to be executed before, only checks.
	 */
	public List<File> getOutputFiles(boolean must_exists, boolean must_be_a_regular_file, boolean not_empty) {
		return output_expected_destinations.stream().map(dest -> dest.ressource).flatMap(ressource -> {
			try {
				URL url = new URL(ressource);
				if (url.getProtocol().equals("file")) {
					return Stream.of(Paths.get(url.toURI()).toFile());
				}
			} catch (MalformedURLException e) {
				/**
				 * Not an URL, maybe a file
				 */
				return Stream.of(new File(ressource));
			} catch (URISyntaxException e) {
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
	public ConversionTool cleanUpOutputFiles(boolean remove_all, boolean clean_output_directories) {
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
			} catch (IOException e) {
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
	
}
