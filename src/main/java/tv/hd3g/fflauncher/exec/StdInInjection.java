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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;

public class StdInInjection extends OutputStream {
	
	private static Logger log = Logger.getLogger(StdInInjection.class);
	public static final String LINESEPARATOR = System.getProperty("line.separator");
	
	private final OutputStream std_in;
	
	StdInInjection(OutputStream std_in) {
		this.std_in = std_in;
	}
	
	public void flush() throws IOException {
		std_in.flush();
	}
	
	public void close() throws IOException {
		std_in.close();
	}
	
	public void write(int b) throws IOException {
		std_in.write(b);
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		std_in.write(b, off, len);
	}
	
	/**
	 * Send text + new line + flush
	 */
	public StdInInjection println(String text) throws IOException {
		println(text, Charset.defaultCharset());
		return this;
	}
	
	/**
	 * Send text + new line + flush
	 */
	public StdInInjection println(String text, Charset charset) throws IOException {
		if (log.isTraceEnabled()) {
			log.trace("Println: \"" + text + "\"");
		}
		write(text.getBytes(charset));
		write(LINESEPARATOR.getBytes(charset));
		flush();
		return this;
	}
	
}
