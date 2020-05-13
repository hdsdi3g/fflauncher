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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import tv.hd3g.fflauncher.FFCodec.CodecType;
import tv.hd3g.fflauncher.FFFilter.ConnectorType;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

public class FFbaseTest {

	private final ExecutableFinder executableFinder;

	public FFbaseTest() {
		executableFinder = new ExecutableFinder();
	}

	private static class FFbaseImpl extends FFbase {
		private static final String execName = "ffmpeg";

		public FFbaseImpl(final Parameters parameters) throws IOException {
			super(execName, parameters);
		}

	}

	@Test
	public void testBase() throws Exception {
		final FFbaseImpl b = new FFbaseImpl(new Parameters());
		final var about = b.getAbout(executableFinder);

		assertNotNull(about.getVersion(), "version");
		assertFalse(about.getCodecs().isEmpty(), "codecs empty");
		assertFalse(about.getFormats().isEmpty(), "formats empty");
		assertFalse(about.getDevices().isEmpty(), "devices empty");
		assertFalse(about.getBitStreamFilters().isEmpty(), "bitstream empty");
		assertNotNull(about.getProtocols(), "protocols");
		assertFalse(about.getFilters().isEmpty(), "filters empty");
		assertFalse(about.getPixelFormats().isEmpty(), "pixelFormats empty");

		assertTrue(about.isCoderIsAvaliable("ffv1"), "Coder Avaliable");
		assertFalse(about.isCoderIsAvaliable("nonono"), "Coder notAvaliable");
		assertTrue(about.isDecoderIsAvaliable("rl2"), "Decoder Avaliable");
		assertFalse(about.isDecoderIsAvaliable("nonono"), "Decoder notAvaliable");
		assertTrue(about.isFilterIsAvaliable("color"), "Filter Avaliable");
		assertFalse(about.isFilterIsAvaliable("nonono"), "Filter notAvaliable");
		assertTrue(about.isToFormatIsAvaliable("wav"), "Format Avaliable");
		assertFalse(about.isToFormatIsAvaliable("nonono"), "Format notAvaliable");
	}

	@Test
	public void testNVPresence() throws Exception {
		final FFbaseImpl b = new FFbaseImpl(new Parameters());

		if (System.getProperty("ffmpeg.test.nvidia", "").equals("1")) {
			assertTrue(b.getAbout(executableFinder)
			        .isNVToolkitIsAvaliable(), "Can't found NV lib like cuda, cuvid and nvenc");
		}
		if (System.getProperty("ffmpeg.test.libnpp", "").equals("1")) {
			assertTrue(b.getAbout(executableFinder).isHardwareNVScalerFilterIsAvaliable(), "Can't found libnpp");
		}
	}

	private static List<String> readLinesFromResource(final String resource) {
		try {
			return Files.readAllLines(new File("src/test/resources/" + resource).toPath(), UTF_8);
		} catch (final IOException e) {
			throw new RuntimeException("Can't get resource " + resource, e);
		}
	}

	@Test
	public void testVersion() {
		final FFVersion v = new FFVersion(readLinesFromResource("test-version.txt"));

		assertEquals("3.3.3 Copyright (c) 2000-2017 the FFmpeg developers", v.headerVersion);
		assertEquals("gcc 4.9.2 (Debian 4.9.2-10)", v.builtWith);

		Arrays.stream(
		        "gpl version3 nonfree yasm libmp3lame libbluray libopenjpeg libtheora libvorbis libtwolame libvpx libxvid libgsm libopencore-amrnb libopencore-amrwb libopus librtmp libschroedinger libsmbclient libspeex libssh libvo-amrwbenc libwavpack libwebp libzvbi libx264 libx265 libsmbclient libssh"
		                .split(" ")).forEach(cf -> {
			                assertTrue(v.configuration.contains(cf), "Missing " + cf);
		                });

		assertEquals(
		        "--enable-gpl --enable-version3 --enable-nonfree --as=yasm --enable-libmp3lame --enable-libbluray --enable-libopenjpeg --enable-libtheora --enable-libvorbis --enable-libtwolame --enable-libvpx --enable-libxvid --enable-libgsm --enable-libopencore-amrnb --enable-libopencore-amrwb --enable-libopus --enable-librtmp --enable-libschroedinger --enable-libsmbclient --enable-libspeex --enable-libssh --enable-libvo-amrwbenc --enable-libwavpack --enable-libwebp --enable-libzvbi --enable-libx264 --enable-libx265 --enable-libsmbclient --enable-libssh",
		        v.rawConfiguration);
		assertEquals("55. 58.100 / 55. 58.100", v.libavutilVersion);
		assertEquals("57. 89.100 / 57. 89.100", v.libavcodecVersion);
		assertEquals("57. 71.100 / 57. 71.100", v.libavformatVersion);
		assertEquals("57.  6.100 / 57.  6.100", v.libavdeviceVersion);
		assertEquals("6. 82.100 /  6. 82.100", v.libavfilterVersion);
		assertEquals("4.  6.100 /  4.  6.100", v.libswscaleVersion);
		assertEquals("2.  7.100 /  2.  7.100", v.libswresampleVersion);
		assertEquals("54.  5.100 / 54.  5.100", v.libpostprocVersion);
	}

	@Test
	public void testCodecs() {
		final List<FFCodec> list = FFCodec.parse(readLinesFromResource("test-codecs.txt"));

		final List<FFCodec> test1 = list.stream().filter(c -> (c.type == CodecType.AUDIO & c.encodingSupported
		                                                       & c.decodingSupported & c.lossyCompression
		                                                       & c.name.equals("adpcm_g722"))).collect(Collectors
		                                                               .toUnmodifiableList());

		assertEquals(1, test1.size());
		assertTrue(test1.get(0).longName.equals("G.722 ADPCM"));
		assertTrue(test1.get(0).toString().startsWith(test1.get(0).longName));

		assertEquals(7, list.stream().filter(c -> (c.type == CodecType.DATA)).count());

		assertEquals(10, list.stream().filter(c -> (c.encodingSupported == false & c.decodingSupported == false
		                                            & c.losslessCompression == false
		                                            && c.lossyCompression == false)).count());

		final FFCodec t = list.stream().filter(c -> c.name.equals("dirac")).findFirst().get();

		assertTrue(t.longName.equals("Dirac"));
		assertTrue(t.decoders.contains("dirac"));
		assertTrue(t.encoders.contains("vc2"));

		assertTrue(t.encoders.contains("libschroedinger"));
		assertTrue(t.decoders.contains("libschroedinger"));

		assertEquals(2, t.encoders.size());
		assertEquals(2, t.decoders.size());
	}

	@Test
	public void testFormats() {
		final List<FFFormat> list = FFFormat.parseFormats(readLinesFromResource("test-formats.txt"));

		assertEquals(326, list.size());

		final List<FFFormat> test1 = list.stream().filter(f -> (f.muxing == false & f.demuxing == true & f.name.equals(
		        "bfi"))).collect(Collectors.toUnmodifiableList());

		assertEquals(1, test1.size());
		assertTrue(test1.get(0).longName.equals("Brute Force & Ignorance"));

		assertEquals(2, list.stream().filter(f -> f.name.equals("hls")).count());

		assertEquals(2, list.stream().filter(f -> f.alternateTags.contains("mp4")).count());

	}

	@Test
	public void testDevices() {
		final List<FFDevice> list = FFDevice.parseDevices(readLinesFromResource("test-devices.txt"));
		assertEquals(7, list.size());

		int i = 0;
		assertEquals("DV1394 A/V grab [dv1394] demuxing only supported", list.get(i++).toString());
		assertEquals("Linux framebuffer [fbdev] muxing and demuxing supported", list.get(i++).toString());
		assertEquals("Libavfilter virtual input device [lavfi] demuxing only supported", list.get(i++).toString());
		assertEquals("OSS (Open Sound System) playback [oss] muxing and demuxing supported", list.get(i++).toString());
		assertEquals("Video4Linux2 output device [v4l2] muxing only supported", list.get(i++).toString());
		assertEquals("Video4Linux2 device grab [video4linux2, v4l2] demuxing only supported", list.get(i++).toString());
		assertEquals("[libcdio] demuxing only supported", list.get(i++).toString());
	}

	@Test
	public void testBSFS() {
		final Set<String> filters = FFAbout.parseBSFS(readLinesFromResource("test-bsfs.txt").stream());

		assertTrue(filters.contains("noise"));
		assertEquals(17, filters.size());
	}

	@Test
	public void testProtocols() {
		final FFProtocols p = new FFProtocols(readLinesFromResource("test-protocols.txt"));

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

	@Test
	public void testFilters() {
		final List<FFFilter> list = FFFilter.parseFilters(readLinesFromResource("test-filters.txt"));

		assertEquals(299, list.size());

		assertTrue(list.stream().anyMatch(f -> f.tag.equals("afftfilt")));

		assertEquals(3, list.stream().filter(f -> (f.sourceConnectorsCount == 2
		                                           && f.sourceConnector == ConnectorType.AUDIO)).count());

		assertEquals(1, list.stream().filter(f -> (f.sourceConnectorsCount == 2
		                                           && f.sourceConnector == ConnectorType.VIDEO
		                                           && f.destConnectorsCount == 2
		                                           && f.destConnector == ConnectorType.VIDEO)).filter(f -> f.tag
		                                                   .equals("scale2ref")).filter(f -> f.longName.equals(
		                                                           "Scale the input video size and/or convert the image format to the given reference."))
		        .count());

		assertTrue(list.get(0).toString().startsWith(list.get(0).longName));

	}

	@Test
	public void testPixelFormats() {
		final List<FFPixelFormat> list = FFPixelFormat.parsePixelsFormats(readLinesFromResource(
		        "test-pixelsformats.txt"));

		assertEquals(183, list.size());

		assertEquals(1, list.stream().filter(pf -> (pf.tag.equals("pal8") && pf.supportedInput
		                                            && pf.supportedOutput == false && pf.paletted
		                                            && pf.nbComponents == 1 && pf.bitsPerPixel == 8)).count());

		assertEquals(1, list.stream().filter(pf -> (pf.tag.equals("yuv444p16le") && pf.supportedInput
		                                            && pf.supportedOutput && pf.paletted == false
		                                            && pf.hardwareAccelerated == false && pf.nbComponents == 3
		                                            && pf.bitsPerPixel == 48)).count());
	}

	@Test
	public void testHwaccels() {
		final Set<String> list = FFAbout.parseBSFS(readLinesFromResource("test-hwaccels.txt").stream());
		assertEquals(6, list.size());

		assertTrue(list.contains("cuvid"));
		assertTrue(list.contains("cuda"));
		assertTrue(list.contains("dxva2"));
		assertTrue(list.contains("qsv"));
		assertTrue(list.contains("d3d11va"));
		assertTrue(list.contains("qsv"));
	}

	@Test
	public void testParams() throws IOException {
		final FFbaseImpl b = new FFbaseImpl(new Parameters());
		assertFalse(b.isLogLevelSet());

		final int skip_base_cmdline = b.getInternalParameters().toString().length();

		b.setLogLevel(FFLogLevel.FATAL, true, true);
		assertEquals("-loglevel repeat+level+fatal", b.getInternalParameters().toString().substring(skip_base_cmdline));

		b.setLogLevel(FFLogLevel.DEBUG, false, false);
		assertEquals("-loglevel repeat+level+fatal", b.getInternalParameters().toString().substring(skip_base_cmdline));

		assertTrue(b.isLogLevelSet());

		b.getInternalParameters().clear();

		assertFalse(b.isHidebanner());
		b.setHidebanner();
		assertEquals("-hide_banner", b.getInternalParameters().toString().substring(skip_base_cmdline));
		assertTrue(b.isHidebanner());

		b.getInternalParameters().clear();

		assertFalse(b.isOverwriteOutputFiles());
		b.setOverwriteOutputFiles();
		assertEquals("-y", b.getInternalParameters().toString().substring(skip_base_cmdline));
		assertTrue(b.isOverwriteOutputFiles());

		b.getInternalParameters().clear();

		assertFalse(b.isNeverOverwriteOutputFiles());
		b.setNeverOverwriteOutputFiles();
		assertEquals("-n", b.getInternalParameters().toString().substring(skip_base_cmdline));
		assertTrue(b.isNeverOverwriteOutputFiles());
	}

}
