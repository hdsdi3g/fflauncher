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

public enum FFLogLevel {
	/**
	 * Show nothing at all; be silent.
	 */
	quiet,

	/**
	 * Only show fatal errors which could lead the process to crash, such as an assertion failure. This is not currently used for anything.
	 */
	panic,

	/**
	 * Only show fatal errors. These are errors after which the process absolutely cannot continue.
	 */
	fatal,

	/**
	 * Show all errors, including ones which can be recovered from.
	 */
	error,

	/**
	 * Show all warnings and errors. Any message related to possibly incorrect or unexpected events will be shown.
	 */
	warning,

	/**
	 * Show informative messages during processing. This is in addition to warnings and errors. This is the default value.
	 */
	info,
	/**
	 * Same as info, except more verbose.
	 */
	verbose,

	/**
	 * Show everything, including debugging information.
	 */
	debug,

	trace
}
