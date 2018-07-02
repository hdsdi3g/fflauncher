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
package tv.hd3g.fflauncher.exec;

import java.util.concurrent.Executor;

class StdOutErrCallback {
	
	private StdOutErrObserver observer;
	private Executor executor;
	
	StdOutErrCallback(StdOutErrObserver observer, Executor executor) {
		this.observer = observer;
		if (observer == null) {
			throw new NullPointerException("\"observer\" can't to be null");
		}
		this.executor = executor;
		if (executor == null) {
			throw new NullPointerException("\"executor\" can't to be null");
		}
	}
	
	/**
	 * Async
	 */
	void onStdout(ExecProcessTextResult source, String line) {
		executor.execute(() -> {
			observer.onStdout(source, line);
		});
	}
	
	/**
	 * Async
	 */
	void onStderr(ExecProcessTextResult source, String line) {
		executor.execute(() -> {
			observer.onStderr(source, line);
		});
	}
	
}
