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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.ExecutableFinder;

public class FFmpeg extends FFbase {
	
	private static final Logger log = LogManager.getLogger();
	
	public FFmpeg(ExecutableFinder exec_finder, String exec_name) throws FileNotFoundException {
		super(exec_finder, exec_name);
	}
	
	public FFmpeg(File executable) throws FileNotFoundException {
		super(executable);
	}
	
	// TODO implements ffmpeg + ffprobe
}
