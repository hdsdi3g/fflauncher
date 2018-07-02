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

@FunctionalInterface
public interface InteractiveExecProcessHandler {
	
	/**
	 * @return text to send to process
	 * @see CaptureOutStreamsBehavior
	 * @see StdInInjection
	 */
	public String onText(ExecProcessTextResult source, String line, boolean is_std_err);
	
	public default String onStdout(ExecProcessTextResult source, String line) {
		return onText(source, line, false);
	}
	
	public default String onStderr(ExecProcessTextResult source, String line) {
		return onText(source, line, true);
	}
	
	/**
	 * @return true if send to process via stdin "text" + [\r\n|\n] + flush()
	 *         Else false send "text" + flush()
	 */
	public default boolean addNewLineAtTheEndOfInjectedText() {
		return false;
	}
}
