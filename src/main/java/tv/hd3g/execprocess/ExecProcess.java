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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ExecProcess {
	
	// private static Logger log = Logger.getLogger(ExecProcess.class);
	// T O D O ProcessBuilder.startPipeline(builders) :: external laucher
	
	protected final ArrayList<String> params;
	protected final File executable;
	protected final LinkedHashMap<String, String> environment;
	protected final ArrayList<EndExecutionCallback> end_exec_callback_list;
	
	protected boolean exec_code_must_be_zero;
	protected File working_directory;
	protected ScheduledExecutorService max_exec_time_scheduler;
	protected long max_exec_time = Long.MAX_VALUE;
	protected Consumer<ProcessBuilder> alter_process_builder;
	
	/**
	 * @param executable can be a simple file or an exact full path
	 */
	public ExecProcess(String executable, ExecutableFinder exec_finder) throws IOException {
		params = new ArrayList<>();
		this.executable = exec_finder.get(executable);
		environment = new LinkedHashMap<>();
		end_exec_callback_list = new ArrayList<>(1);
		setup(exec_finder.getFullPathToString());
	}
	
	public ExecProcess(File executable) throws IOException {
		if (executable.isFile() == false | executable.exists() == false) {
			throw new FileNotFoundException("Can't found " + executable);
		} else if (executable.canExecute() == false) {
			throw new IOException("Can't execute " + executable);
		}
		
		params = new ArrayList<>();
		this.executable = executable;
		environment = new LinkedHashMap<>();
		end_exec_callback_list = new ArrayList<>(1);
		setup(System.getenv("PATH"));
	}
	
	private void setup(String path) throws IOException {
		environment.putAll(System.getenv());
		if (environment.containsKey("LANG") == false) {
			environment.put("LANG", Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry() + "." + Charset.forName("UTF-8"));
		}
		environment.put("PATH", path);
		exec_code_must_be_zero = true;
		setWorkingDirectory(new File(System.getProperty("java.io.tmpdir", "")));
	}
	
	public ExecProcess setParams(Collection<String> params) {
		if (params == null) {
			throw new NullPointerException("\"params\" can't to be null");
		}
		this.params.clear();
		
		this.params.addAll(params.stream().filter(p -> p != null).collect(Collectors.toList()));
		return this;
	}
	
	public ExecProcess setParams(String... params) {
		if (params == null) {
			throw new NullPointerException("\"params\" can't to be null");
		}
		this.params.clear();
		return addParams(params);
	}
	
	/**
	 * @return never null
	 */
	public ArrayList<String> getParams() {
		return params;
	}
	
	public ExecProcess addParams(String... params) {
		if (params == null) {
			throw new NullPointerException("\"params\" can't to be null");
		}
		
		this.params.addAll(Arrays.stream(params).filter(p -> {
			return p != null;
		}).collect(Collectors.toList()));
		
		return this;
	}
	
	/**
	 * @param params transform spaces in each param to new params: ["a b c", "d"] -> ["a", "b", "c", "d"]. It don't manage " or tabs.
	 */
	public ExecProcess addSpacedParams(String... params) {
		if (params == null) {
			throw new NullPointerException("\"params\" can't to be null");
		}
		
		this.params.addAll(Arrays.stream(params).filter(p -> {
			return p != null;
		}).flatMap(p -> {
			return Arrays.stream(p.split(" ")).filter(sub_p -> {
				return sub_p.equals("") == false && sub_p.equals(" ") == false;
			}).map(sub_p -> {
				return sub_p.trim();
			});
		}).collect(Collectors.toList()));
		
		return this;
	}
	
	/**
	 * @param params transform spaces in each param to new params: ["a b c", "d"] -> ["a", "b", "c", "d"]. It don't manage " or tabs.
	 */
	public ExecProcess setSpacedParams(String... params) {
		if (params == null) {
			throw new NullPointerException("\"params\" can't to be null");
		}
		
		this.params.clear();
		
		return addSpacedParams(params);
	}
	
	/*public Map<String, String> getEnvironment() {
		return environment;
	}*/
	
	/**
	 * @return null if not found
	 */
	public String getEnvironmentVar(String key) {
		return environment.get(key);
	}
	
	public ExecProcess setEnvironmentVar(String key, String value) {
		if (key.equalsIgnoreCase("path") && System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
			environment.put("PATH", value);
			environment.put("Path", value);
		} else {
			environment.put(key, value);
		}
		return this;
	}
	
	public ExecProcess setEnvironmentVarIfNotFound(String key, String value) {
		if (environment.containsKey(key)) {
			return this;
		}
		return setEnvironmentVar(key, value);
	}
	
	public void forEachEnvironmentVar(BiConsumer<String, String> action) {
		environment.forEach(action);
	}
	
	/**
	 * @return never null
	 */
	public File getWorkingDirectory() {
		return working_directory;
	}
	
	public ExecProcess setWorkingDirectory(File working_directory) throws IOException {
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
	
	public ExecProcess setMaxExecutionTime(long max_exec_time, TimeUnit unit, ScheduledExecutorService max_exec_time_scheduler) {
		if (max_exec_time == 0) {
			return this;
		}
		if (max_exec_time_scheduler == null) {
			throw new NullPointerException("\"max_exec_time_scheduler\" can't to be null");
		}
		this.max_exec_time_scheduler = max_exec_time_scheduler;
		this.max_exec_time = unit.toMillis(max_exec_time);
		return this;
	}
	
	public long getMaxExecTime(TimeUnit unit) {
		return unit.convert(max_exec_time, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Default, yes.
	 */
	public ExecProcess setExecCodeMustBeZero(boolean exec_code_must_be_zero) {
		this.exec_code_must_be_zero = exec_code_must_be_zero;
		return this;
	}
	
	public boolean isExecCodeMustBeZero() {
		return exec_code_must_be_zero;
	}
	
	public ExecProcess addEndExecutionCallback(Consumer<ExecProcessResult> onEnd, Executor executor) {
		end_exec_callback_list.add(new EndExecutionCallback(onEnd, executor));
		return this;
	}
	
	public ExecProcess alterProcessBuilderBeforeStartIt(Consumer<ProcessBuilder> alter_process_builder) {
		this.alter_process_builder = alter_process_builder;
		return this;
	}
	
	/*public Consumer<ProcessBuilder> getAlterProcessBuilder() {
		return alter_process_builder;
	}*/
	
	/**
	 * Non-blocking
	 * Don't process here stdin/out/err
	 */
	public ExecProcessResult start(Executor executor) {
		return new ExecProcessResult(executable, params, environment, end_exec_callback_list, exec_code_must_be_zero, working_directory, max_exec_time_scheduler, max_exec_time, alter_process_builder).start(executor);
	}
	
	/**
	 * Non-blocking
	 * Don't process here stdin/out/err
	 */
	public ExecProcessResult start(ThreadFactory thread_factory) {
		return new ExecProcessResult(executable, params, environment, end_exec_callback_list, exec_code_must_be_zero, working_directory, max_exec_time_scheduler, max_exec_time, alter_process_builder).start(thread_factory);
	}
	
	/**
	 * @return new ProcessBuilder based on this configuration, without start the process.
	 */
	public ProcessBuilder makeProcessBuilder() {
		return new ExecProcessResult(executable, params, environment, end_exec_callback_list, exec_code_must_be_zero, working_directory, max_exec_time_scheduler, max_exec_time, alter_process_builder).makeProcessBuilder();
	}
	
	public String toString() {
		return executable.getName() + " " + getParams().stream().collect(Collectors.joining(" "));
	}
}
