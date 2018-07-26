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
	}
	
	protected abstract String getDefaultExecName();
	
	public ExecutableFinder getExecFinder() {
		return exec_finder;
	}
	
	public Executor getExecutionExecutor() {
		return execution_executor;
	}
	
	public Recipe setExecutionExecutor(Executor execution_executor) {
		this.execution_executor = execution_executor;
		return this;
	}
	
	public Executor getPostProcessExecutor() {
		return post_process_executor;
	}
	
	public Recipe setPostProcessExecutor(Executor post_process_executor) {
		this.post_process_executor = post_process_executor;
		return this;
	}
	
	public String getExecName() {
		return exec_name;
	}
	
}
