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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
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
import tv.hd3g.execprocess.ExecProcessTextResult;
import tv.hd3g.execprocess.ExecutableFinder;

public class ConversionTool {
	private static Logger log = LogManager.getLogger();
	
	protected final File executable;
	protected long max_exec_time_ms;
	protected ScheduledExecutorService max_exec_time_scheduler;
	private Consumer<ExecProcessText> exec_process_catcher;
	
	protected final CommandLine command_line;
	protected final ArrayList<ParameterReference> input_sources;
	protected final ArrayList<ParameterReference> output_expected_destinations;
	
	/**
	 * Set values for variables like <%myvar%> in the command line, do NOT set input/output references if they was set with addInputSource/addOutputDestination.
	 */
	public final LinkedHashMap<String, String> parameters_variables;
	private boolean remove_params_if_no_var_to_inject;
	
	public ConversionTool(ExecutableFinder exec_finder, CommandLine command_line) throws FileNotFoundException {
		this.command_line = command_line;
		if (command_line == null) {
			throw new NullPointerException("\"command_line\" can't to be null");
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
		
		max_exec_time_ms = 5000;
		
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
	public boolean isRemove_params_if_no_var_to_inject() {
		return remove_params_if_no_var_to_inject;
	}
	
	/**
	 * @param remove_params_if_no_var_to_inject false by default
	 */
	public ConversionTool setRemove_params_if_no_var_to_inject(boolean remove_params_if_no_var_to_inject) {
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
		this.max_exec_time_scheduler = max_exec_time_scheduler;
		if (max_exec_time_scheduler == null) {
			throw new NullPointerException("\"max_exec_time_scheduler\" can't to be null");
		}
		return this;
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
	
	/**
	 * @return result
	 */
	protected ExecProcessTextResult checkExecution(ExecProcessTextResult result) throws IOException {
		if (result.isCorrectlyDone() == false) {
			throw new IOException("Can't execute correcly " + result.getCommandline() + ", " + result.getEndStatus() + " [" + result.getExitCode() + "] \"" + result.getStderr(false, " ") + "\"");
		}
		return result;
	}
	
	class ParameterReference {
		final String ressource;
		final String var_name_in_command_line;
		final ArrayList<String> parameters_before_ref;
		final ArrayList<String> parameters_after_ref;
		
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
				this.parameters_before_ref = new ArrayList<>(1);
			} else {
				this.parameters_before_ref = new ArrayList<>(parameters_before_ref);
			}
			if (parameters_after_ref == null) {
				this.parameters_after_ref = new ArrayList<>(1);
			} else {
				this.parameters_after_ref = new ArrayList<>(parameters_after_ref);
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
			return addInputSource(source, var_name_in_command_line, Arrays.stream(parameters_before_input_source).filter(p -> p != null).collect(Collectors.toList()), Collections.emptyList());
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
			return addOutputDestination(destination, var_name_in_command_line, Arrays.stream(parameters_before_output_destination).filter(p -> p != null).collect(Collectors.toList()), Collections.emptyList());
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
		
		Stream.concat(input_sources.stream(), output_expected_destinations.stream()).forEach(param_ref -> {
			String var_name = param_ref.var_name_in_command_line;
			boolean done = command_line.injectParamsAroundVariable(var_name, param_ref.parameters_before_ref, param_ref.parameters_after_ref);
			
			if (done) {
				if (all_vars_to_inject.containsKey(var_name)) {
					throw new RuntimeException("Variable collision: \"" + var_name + "\" was already set to \"" + all_vars_to_inject.get(var_name) + "\" in " + command_line);
				}
				all_vars_to_inject.put(var_name, param_ref.ressource);
			} else {
				onMissingInputOutputVar(var_name, param_ref.ressource);
			}
		});
		
		return command_line.process(all_vars_to_inject, remove_params_if_no_var_to_inject);
	}
	
	/**
	 * @param short_command_limited_execution_time controlled by setMaxExecutionTimeForShortCommands()
	 * @param working_directory can be null
	 */
	public ExecProcessText createExec(boolean short_command_limited_execution_time, File working_directory) throws IOException {
		ExecProcessText exec_process = new ExecProcessText(executable);
		
		if (working_directory != null) {
			exec_process.setWorkingDirectory(working_directory);
		} else if (short_command_limited_execution_time) {
			exec_process.setMaxExecutionTime(max_exec_time_ms, TimeUnit.MILLISECONDS, max_exec_time_scheduler);
		}
		
		exec_process.importParametersFrom(createProcessedCommandLine());
		applyExecProcessCatcher(exec_process);
		
		return exec_process;
	}
	
	public ExecProcessText createExec(boolean limited_execution_time) throws IOException {
		return createExec(limited_execution_time, null);
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
		return input_sources.stream().map(getRessourceFromParameterReference).collect(Collectors.toList());
	}
	
	/**
	 * @return never null, can be empty
	 */
	public List<String> getDeclaredDestinations() {
		return output_expected_destinations.stream().map(getRessourceFromParameterReference).collect(Collectors.toList());
	}
	
}
