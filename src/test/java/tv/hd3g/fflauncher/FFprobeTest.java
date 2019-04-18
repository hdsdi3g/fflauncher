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

import java.io.FileNotFoundException;

import junit.framework.TestCase;
import tv.hd3g.execprocess.DeprecatedCommandLineProcessor;
import tv.hd3g.fflauncher.FFprobe.FFPrintFormat;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

public class FFprobeTest extends TestCase {

	public void test() throws FileNotFoundException {
		final FFprobe p = new FFprobe(new ExecutableFinder(), new DeprecatedCommandLineProcessor().createEmptyCommandLine("ffprobe"));

		final int skip_base_cmdline = p.command_line.toString().length();

		assertFalse(p.isPretty());
		p.setPretty();
		assertEquals("-pretty", p.command_line.toString().substring(skip_base_cmdline));
		assertTrue(p.isPretty());

		p.command_line.clear();

		assertFalse(p.hasPrintFormat());
		p.setPrintFormat(FFPrintFormat.xml);
		assertEquals("-print_format xml", p.command_line.toString().substring(skip_base_cmdline));
		assertTrue(p.hasPrintFormat());

		p.command_line.clear();

		assertFalse(p.isShowFormat());
		p.setShowFormat();
		assertEquals("-show_format", p.command_line.toString().substring(skip_base_cmdline));
		assertTrue(p.isShowFormat());

		p.command_line.clear();

		assertFalse(p.isShowData());
		p.setShowData();
		assertEquals("-show_data", p.command_line.toString().substring(skip_base_cmdline));
		assertTrue(p.isShowData());

		p.command_line.clear();

		assertFalse(p.isShowError());
		p.setShowError();
		assertEquals("-show_error", p.command_line.toString().substring(skip_base_cmdline));
		assertTrue(p.isShowError());

		p.command_line.clear();

		assertFalse(p.isShowFrames());
		p.setShowFrames();
		assertEquals("-show_frames", p.command_line.toString().substring(skip_base_cmdline));
		assertTrue(p.isShowFrames());

		p.command_line.clear();

		assertFalse(p.isShowLog());
		p.setShowLog();
		assertEquals("-show_log", p.command_line.toString().substring(skip_base_cmdline));
		assertTrue(p.isShowLog());

		p.command_line.clear();

		assertFalse(p.isShowPackets());
		p.setShowPackets();
		assertEquals("-show_packets", p.command_line.toString().substring(skip_base_cmdline));
		assertTrue(p.isShowPackets());

		p.command_line.clear();

		assertFalse(p.isShowPrograms());
		p.setShowPrograms();
		assertEquals("-show_programs", p.command_line.toString().substring(skip_base_cmdline));
		assertTrue(p.isShowPrograms());

		p.command_line.clear();

		assertFalse(p.isShowStreams());
		p.setShowStreams();
		assertEquals("-show_streams", p.command_line.toString().substring(skip_base_cmdline));
		assertTrue(p.isShowStreams());

		p.command_line.clear();

		assertFalse(p.isShowChapters());
		p.setShowChapters();
		assertEquals("-show_chapters", p.command_line.toString().substring(skip_base_cmdline));
		assertTrue(p.isShowChapters());

		p.command_line.clear();
	}

}
