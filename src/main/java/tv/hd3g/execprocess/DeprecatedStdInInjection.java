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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Deprecated
public class DeprecatedStdInInjection extends OutputStream {

	private static final Logger log = LogManager.getLogger();
	public static final String LINESEPARATOR = System.getProperty("line.separator");

	private final OutputStream std_in;

	DeprecatedStdInInjection(final OutputStream std_in) {
		this.std_in = std_in;
	}

	@Override
	public void flush() throws IOException {
		std_in.flush();
	}

	@Override
	public void close() throws IOException {
		std_in.close();
	}

	@Override
	public void write(final int b) throws IOException {
		std_in.write(b);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		std_in.write(b, off, len);
	}

	/**
	 * Send text + new line + flush
	 */
	public DeprecatedStdInInjection println(final String text) throws IOException {
		println(text, Charset.defaultCharset());
		return this;
	}

	/**
	 * Send text + new line + flush
	 */
	public DeprecatedStdInInjection println(final String text, final Charset charset) throws IOException {
		if (log.isTraceEnabled()) {
			log.trace("Println: \"" + text + "\"");
		}
		write(text.getBytes(charset));
		write(LINESEPARATOR.getBytes(charset));
		flush();
		return this;
	}

}
