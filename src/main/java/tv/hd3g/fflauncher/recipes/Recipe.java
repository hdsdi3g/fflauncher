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
package tv.hd3g.fflauncher.recipes;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.ExecutableFinder;

public abstract class Recipe {
	private static Logger log = LogManager.getLogger();
	
	private ExecutableFinder exec_finder;
	private String exec_name;
	private Executor execution_executor;
	private Executor post_process_executor;
	
	public Recipe(ExecutableFinder exec_finder, String exec_name) {
		this.exec_finder = exec_finder;
		if (exec_finder == null) {
			throw new NullPointerException("\"exec_finder\" can't to be null");
		}
		this.exec_name = exec_name;
		if (exec_name == null) {
			throw new NullPointerException("\"exec_name\" can't to be null");
		} else if (exec_name.isEmpty()) {
			throw new NullPointerException("\"exec_name\" can't to be empty");
		}
		execution_executor = ForkJoinPool.commonPool();
		post_process_executor = ForkJoinPool.commonPool();
		
		log.debug("Init recipe " + getClass().getSimpleName() + " with " + exec_name);
	}
	
	public Recipe() {
		exec_finder = new ExecutableFinder();
		exec_name = getDefaultExecName();
		if (exec_name == null) {
			throw new NullPointerException("\"exec_name\" can't to be null");
		} else if (exec_name.isEmpty()) {
			throw new NullPointerException("\"exec_name\" can't to be empty");
		}
		execution_executor = ForkJoinPool.commonPool();
		post_process_executor = ForkJoinPool.commonPool();
		
		log.debug("Init recipe " + getClass().getSimpleName() + " with " + exec_name + " (DefaultExecName)");
	}
	
	protected abstract String getDefaultExecName();
	
	public ExecutableFinder getExecFinder() {
		return exec_finder;
	}
	
	public Executor getExecutionExecutor() {
		return execution_executor;
	}
	
	public Recipe setExecutionExecutor(Executor execution_executor) {
		if (execution_executor == null) {
			throw new NullPointerException("\"execution_executor\" can't to be null");
		}
		this.execution_executor = execution_executor;
		log.debug("Set executor for recipe " + getClass().getSimpleName() + ": " + execution_executor.getClass().getSimpleName());
		return this;
	}
	
	public Executor getPostProcessExecutor() {
		return post_process_executor;
	}
	
	public Recipe setPostProcessExecutor(Executor post_process_executor) {
		if (post_process_executor == null) {
			throw new NullPointerException("\"post_process_executor\" can't to be null");
		}
		this.post_process_executor = post_process_executor;
		log.debug("Set executor for recipe " + getClass().getSimpleName() + ": " + post_process_executor.getClass().getSimpleName());
		return this;
	}
	
	public String getExecName() {
		return exec_name;
	}
	
}
