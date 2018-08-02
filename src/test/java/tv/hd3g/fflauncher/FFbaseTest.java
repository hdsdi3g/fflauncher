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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import junit.framework.TestCase;
import tv.hd3g.execprocess.CommandLineProcessor;
import tv.hd3g.execprocess.CommandLineProcessor.CommandLine;
import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.fflauncher.FFCodec.CodecType;
import tv.hd3g.fflauncher.FFFilter.ConnectorType;

public class FFbaseTest extends TestCase {
	
	private static class FFbaseImpl extends FFbase {
		
		public FFbaseImpl(ExecutableFinder exec_finder, CommandLine command_line) throws FileNotFoundException {
			super(exec_finder, command_line);
		}
		
	}
	
	public void testBase() throws Exception {
		FFbaseImpl b = new FFbaseImpl(new ExecutableFinder(), new CommandLineProcessor().createEmptyCommandLine("ffmpeg"));
		
		assertNotNull(b.getAbout().getVersion());
		assertFalse(b.getAbout().getCodecs().isEmpty());
		assertFalse(b.getAbout().getFormats().isEmpty());
		assertFalse(b.getAbout().getDevices().isEmpty());
		assertFalse(b.getAbout().getBitStreamFilters().isEmpty());
		assertNotNull(b.getAbout().getProtocols());
		assertFalse(b.getAbout().getFilters().isEmpty());
		assertFalse(b.getAbout().getPixelFormats().isEmpty());
		
		assertTrue(b.getAbout().isCoderIsAvaliable("ffv1"));
		assertFalse(b.getAbout().isCoderIsAvaliable("nonono"));
		assertTrue(b.getAbout().isDecoderIsAvaliable("rl2"));
		assertFalse(b.getAbout().isDecoderIsAvaliable("nonono"));
		assertTrue(b.getAbout().isFilterIsAvaliable("color"));
		assertFalse(b.getAbout().isFilterIsAvaliable("nonono"));
		assertTrue(b.getAbout().isToFormatIsAvaliable("wav"));
		assertFalse(b.getAbout().isToFormatIsAvaliable("nonono"));
	}
	
	public void testNVPresence() throws Exception {
		FFbaseImpl b = new FFbaseImpl(new ExecutableFinder(), new CommandLineProcessor().createEmptyCommandLine("ffmpeg"));
		
		if (System.getProperty("ffmpeg.test.nvidia", "").equals("1")) {
			assertTrue("Can't found NV lib like cuda, cuvid and nvenc", b.getAbout().isNVToolkitIsAvaliable());
		}
		if (System.getProperty("ffmpeg.test.libnpp", "").equals("1")) {
			assertTrue("Can't found libnpp", b.getAbout().isHardwareNVScalerFilterIsAvaliable());
		}
	}
	
	/**
	 * @return unmodifiableList
	 */
	List<String> readLinesFromResource(String resource) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resource)))) {
			
			ArrayList<String> lines = new ArrayList<>();
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
			return Collections.unmodifiableList(lines);
		} catch (IOException e) {
			throw new RuntimeException("Can't get resource " + resource, e);
		}
	}
	
	public void testVersion() {
		FFVersion v = new FFVersion(readLinesFromResource("test-version.txt"));
		
		assertEquals("3.3.3 Copyright (c) 2000-2017 the FFmpeg developers", v.header_version);
		assertEquals("gcc 4.9.2 (Debian 4.9.2-10)", v.built_with);
		
		Arrays.stream("gpl version3 nonfree yasm libmp3lame libbluray libopenjpeg libtheora libvorbis libtwolame libvpx libxvid libgsm libopencore-amrnb libopencore-amrwb libopus librtmp libschroedinger libsmbclient libspeex libssh libvo-amrwbenc libwavpack libwebp libzvbi libx264 libx265 libsmbclient libssh".split(" ")).forEach(cf -> {
			assertTrue("Missing " + cf, v.configuration.contains(cf));
		});
		
		assertEquals("--enable-gpl --enable-version3 --enable-nonfree --as=yasm --enable-libmp3lame --enable-libbluray --enable-libopenjpeg --enable-libtheora --enable-libvorbis --enable-libtwolame --enable-libvpx --enable-libxvid --enable-libgsm --enable-libopencore-amrnb --enable-libopencore-amrwb --enable-libopus --enable-librtmp --enable-libschroedinger --enable-libsmbclient --enable-libspeex --enable-libssh --enable-libvo-amrwbenc --enable-libwavpack --enable-libwebp --enable-libzvbi --enable-libx264 --enable-libx265 --enable-libsmbclient --enable-libssh", v.raw_configuration);
		assertEquals(v.libavutil_version, "55. 58.100 / 55. 58.100");
		assertEquals(v.libavcodec_version, "57. 89.100 / 57. 89.100");
		assertEquals(v.libavformat_version, "57. 71.100 / 57. 71.100");
		assertEquals(v.libavdevice_version, "57.  6.100 / 57.  6.100");
		assertEquals(v.libavfilter_version, "6. 82.100 /  6. 82.100");
		assertEquals(v.libswscale_version, "4.  6.100 /  4.  6.100");
		assertEquals(v.libswresample_version, "2.  7.100 /  2.  7.100");
		assertEquals(v.libpostproc_version, "54.  5.100 / 54.  5.100");
	}
	
	public void testCodecs() {
		List<FFCodec> list = FFCodec.parse(readLinesFromResource("test-codecs.txt"));
		
		List<FFCodec> test1 = list.stream().filter(c -> {
			return c.type == CodecType.AUDIO & c.encoding_supported & c.decoding_supported & c.lossy_compression & c.name.equals("adpcm_g722");
		}).collect(Collectors.toUnmodifiableList());
		
		assertEquals(1, test1.size());
		assertTrue(test1.get(0).long_name.equals("G.722 ADPCM"));
		assertTrue(test1.get(0).toString().startsWith(test1.get(0).long_name));
		
		assertEquals(7, list.stream().filter(c -> {
			return c.type == CodecType.DATA;
		}).count());
		
		assertEquals(10, list.stream().filter(c -> {
			return c.encoding_supported == false & c.decoding_supported == false & c.lossless_compression == false && c.lossy_compression == false;
		}).count());
		
		FFCodec t = list.stream().filter(c -> {
			return c.name.equals("dirac");
		}).findFirst().get();
		
		assertTrue(t.long_name.equals("Dirac"));
		assertTrue(t.decoders.contains("dirac"));
		assertTrue(t.encoders.contains("vc2"));
		
		assertTrue(t.encoders.contains("libschroedinger"));
		assertTrue(t.decoders.contains("libschroedinger"));
		
		assertEquals(2, t.encoders.size());
		assertEquals(2, t.decoders.size());
	}
	
	public void testFormats() {
		List<FFFormat> list = FFFormat.parseFormats(readLinesFromResource("test-formats.txt"));
		
		assertEquals(326, list.size());
		
		List<FFFormat> test1 = list.stream().filter(f -> {
			return f.muxing == false & f.demuxing == true & f.name.equals("bfi");
		}).collect(Collectors.toUnmodifiableList());
		
		assertEquals(1, test1.size());
		assertTrue(test1.get(0).long_name.equals("Brute Force & Ignorance"));
		
		assertEquals(2, list.stream().filter(f -> {
			return f.name.equals("hls");
		}).count());
		
		assertEquals(2, list.stream().filter(f -> {
			return f.alternate_tags.contains("mp4");
		}).count());
		
	}
	
	public void testDevices() {
		List<FFDevice> list = FFDevice.parseDevices(readLinesFromResource("test-devices.txt"));
		assertEquals(6, list.size());
		
		int i = 0;
		assertEquals("DV1394 A/V grab [dv1394] demuxing only supported", list.get(i++).toString());
		assertEquals("Linux framebuffer [fbdev] muxing and demuxing supported", list.get(i++).toString());
		assertEquals("Libavfilter virtual input device [lavfi] demuxing only supported", list.get(i++).toString());
		assertEquals("OSS (Open Sound System) playback [oss] muxing and demuxing supported", list.get(i++).toString());
		assertEquals("Video4Linux2 output device [v4l2] muxing only supported", list.get(i++).toString());
		assertEquals("Video4Linux2 device grab [video4linux2, v4l2] demuxing only supported", list.get(i++).toString());
	}
	
	public void testBSFS() {
		Set<String> filters = FFAbout.parseBSFS(readLinesFromResource("test-bsfs.txt").stream());
		
		assertTrue(filters.contains("noise"));
		assertEquals(17, filters.size());
	}
	
	public void testProtocols() {
		FFProtocols p = new FFProtocols(readLinesFromResource("test-protocols.txt"));
		
		assertFalse(p.input.contains("Input:") | p.input.contains("input:"));
		assertFalse(p.input.contains("Output:") | p.input.contains("output:"));
		assertFalse(p.output.contains("Input:") | p.output.contains("input:"));
		assertFalse(p.output.contains("Output:") | p.output.contains("output:"));
		
		assertEquals(29, p.input.size());
		assertEquals(24, p.output.size());
		
		assertTrue(p.input.contains("concat"));
		assertFalse(p.output.contains("concat"));
		
		assertFalse(p.input.contains("icecast"));
		assertTrue(p.output.contains("icecast"));
	}
	
	public void testFilters() {
		List<FFFilter> list = FFFilter.parseFilters(readLinesFromResource("test-filters.txt"));
		
		assertEquals(299, list.size());
		
		assertTrue(list.stream().anyMatch(f -> {
			return f.tag.equals("afftfilt");
		}));
		
		assertEquals(3, list.stream().filter(f -> {
			return f.source_connectors_count == 2 && f.source_connector == ConnectorType.AUDIO;
		}).count());
		
		assertEquals(1, list.stream().filter(f -> {
			return f.source_connectors_count == 2 && f.source_connector == ConnectorType.VIDEO && f.dest_connectors_count == 2 && f.dest_connector == ConnectorType.VIDEO;
		}).filter(f -> {
			return f.tag.equals("scale2ref");
		}).filter(f -> {
			return f.long_name.equals("Scale the input video size and/or convert the image format to the given reference.");
		}).count());
		
		assertTrue(list.get(0).toString().startsWith(list.get(0).long_name));
		
	}
	
	public void testPixelFormats() {
		List<FFPixelFormat> list = FFPixelFormat.parsePixelsFormats(readLinesFromResource("test-pixelsformats.txt"));
		
		assertEquals(183, list.size());
		
		assertEquals(1, list.stream().filter(pf -> {
			return pf.tag.equals("pal8") && pf.supported_input && pf.supported_output == false && pf.paletted && pf.nb_components == 1 && pf.bits_per_pixel == 8;
		}).count());
		
		assertEquals(1, list.stream().filter(pf -> {
			return pf.tag.equals("yuv444p16le") && pf.supported_input && pf.supported_output && pf.paletted == false && pf.hardware_accelerated == false && pf.nb_components == 3 && pf.bits_per_pixel == 48;
		}).count());
	}
	
	public void testHwaccels() {
		Set<String> list = FFAbout.parseBSFS(readLinesFromResource("test-hwaccels.txt").stream());
		assertEquals(6, list.size());
		
		assertTrue(list.contains("cuvid"));
		assertTrue(list.contains("cuda"));
		assertTrue(list.contains("dxva2"));
		assertTrue(list.contains("qsv"));
		assertTrue(list.contains("d3d11va"));
		assertTrue(list.contains("qsv"));
	}
	
	public void testParams() throws FileNotFoundException {
		FFbaseImpl b = new FFbaseImpl(new ExecutableFinder(), new CommandLineProcessor().createEmptyCommandLine("ffmpeg"));
		assertFalse(b.isLogLevelSet());
		
		int skip_base_cmdline = b.command_line.toString().length();
		
		b.setLogLevel(FFLogLevel.fatal, true, true);
		assertEquals("-loglevel repeat+level+fatal", b.command_line.toString().substring(skip_base_cmdline));
		
		b.setLogLevel(FFLogLevel.debug, false, false);
		assertEquals("-loglevel repeat+level+fatal", b.command_line.toString().substring(skip_base_cmdline));
		
		assertTrue(b.isLogLevelSet());
		
		b.command_line.clear();
		
		assertFalse(b.isHidebanner());
		b.setHidebanner();
		assertEquals("-hide_banner", b.command_line.toString().substring(skip_base_cmdline));
		assertTrue(b.isHidebanner());
		
		b.command_line.clear();
		
		assertFalse(b.isOverwriteOutputFiles());
		b.setOverwriteOutputFiles();
		assertEquals("-y", b.command_line.toString().substring(skip_base_cmdline));
		assertTrue(b.isOverwriteOutputFiles());
		
		b.command_line.clear();
		
		assertFalse(b.isNeverOverwriteOutputFiles());
		b.setNeverOverwriteOutputFiles();
		assertEquals("-n", b.command_line.toString().substring(skip_base_cmdline));
		assertTrue(b.isNeverOverwriteOutputFiles());
	}
	
}
