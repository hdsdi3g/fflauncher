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
package tv.hd3g.fflauncher.recipes;

import java.util.Objects;
import java.util.concurrent.Executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.processlauncher.tool.ToolRunner;

public abstract class Recipe {
	private static Logger log = LogManager.getLogger();

	protected final ToolRunner toolRun;
	protected final String execName;
	protected Executor executor;// TODO keep ??

	public Recipe(final ToolRunner toolRun, final String execName) {
		this.toolRun = Objects.requireNonNull(toolRun, "\"toolRun\" can't to be null");
		this.execName = Objects.requireNonNull(execName, "\"execName\" can't to be null");
		if (execName.isEmpty()) {
			throw new NullPointerException("\"exec_name\" can't to be empty");
		}
		executor = Runnable::run;

		log.debug("Init recipe " + getClass().getSimpleName() + " with " + execName);
	}

	public String getExecName() {
		return execName;
	}

	public Recipe setPostProcessExecutor(final Executor executor) {
		this.executor = Objects.requireNonNull(executor, "\"executor\" can't to be null");
		log.debug("Set executor for recipe " + getClass().getSimpleName() + ": " + executor.getClass().getSimpleName());
		return this;
	}

}
