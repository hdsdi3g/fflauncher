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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import junit.framework.TestCase;
import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.fflauncher.FFCodec.CodecType;

public class FFbaseTest extends TestCase {
	
	public void testBase() throws Exception {
		FFbase b = new FFbase(new ExecutableFinder(), "ffmpeg");
		
		assertNotNull(b.getVersion());
		assertFalse(b.getCodecs().isEmpty());
		assertFalse(b.getFormats().isEmpty());
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
			return c.type == CodecType.AUDIO & c.encoding_supported & c.decoding_supported & c.lossy_compression & c.tag.equals("adpcm_g722");
		}).collect(Collectors.toList());
		
		assertEquals(1, test1.size());
		assertTrue(test1.get(0).long_name.equals("G.722 ADPCM (decoders: g722 ) (encoders: g722 )"));
		
		assertEquals(7, list.stream().filter(c -> {
			return c.type == CodecType.DATA;
		}).count());
		
		assertEquals(10, list.stream().filter(c -> {
			return c.encoding_supported == false & c.decoding_supported == false & c.lossless_compression == false && c.lossy_compression == false;
		}).count());
	}
	
	public void testFormats() {
		List<FFFormat> list = FFFormat.parse(readLinesFromResource("test-formats.txt"));
		
		assertEquals(326, list.size());
		
		List<FFFormat> test1 = list.stream().filter(f -> {
			return f.muxing == false & f.demuxing == true & f.tag.equals("bfi");
		}).collect(Collectors.toList());
		
		assertEquals(1, test1.size());
		assertTrue(test1.get(0).long_name.equals("Brute Force & Ignorance"));
		
		assertEquals(2, list.stream().filter(f -> {
			return f.tag.equals("hls");
		}).count());
		
		assertEquals(2, list.stream().filter(f -> {
			return f.alternate_tags.contains("mp4");
		}).count());
		
	}
}
