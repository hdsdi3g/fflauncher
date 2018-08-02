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
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.ffmpeg.ffprobe.StreamType;

import junit.framework.TestCase;
import tv.hd3g.execprocess.CommandLineProcessor;
import tv.hd3g.execprocess.CommandLineProcessor.CommandLine;
import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecProcessTextResult;
import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.fflauncher.FFmpeg.FFHardwareCodec;
import tv.hd3g.fflauncher.FFmpeg.Preset;
import tv.hd3g.fflauncher.FFmpeg.Tune;
import tv.hd3g.fflauncher.recipes.ProbeMedia;

public class FFmpegTest extends TestCase {
	
	private FFmpeg create() {
		try {
			return new FFmpeg(new ExecutableFinder(), new CommandLineProcessor().createEmptyCommandLine("ffmpeg"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Can't found ffmpeg", e);
		}
	}
	
	public void testSimpleOutputDestination() {
		FFmpeg ffmpeg = create();
		ffmpeg.addSimpleOutputDestination("dest", "container");
		
		assertTrue(ffmpeg.createProcessedCommandLine().getParameters().stream().collect(Collectors.joining(" ")).endsWith("-f container dest"));
	}
	
	public void testParameters() throws FileNotFoundException {
		FFmpeg ffmpeg = create();
		int header = ffmpeg.getCommandLine().toString().length();
		
		ffmpeg.addPreset(Preset.placebo).addTune(Tune.ssim).addBitrate(123, FFUnit.giga, 1);
		ffmpeg.addBitrateControl(10, 20, 30, FFUnit.mega).addCRF(40).addVideoCodecName("NoPe", 2);
		ffmpeg.addGOPControl(50, 60, 70).addIBQfactor(1.5f, 2.5f).addQMinMax(80, 90);
		ffmpeg.addBitrate(100, FFUnit.mega, -1).addVideoCodecName("NoPe2", -1);
		
		assertEquals("-preset placebo -tune ssim -b:v:1 123G -minrate 10M -maxrate 20M -bufsize 30M -crf 40 -c:v:2 NoPe -bf 50 -g 60 -ref 70 -i_qfactor 1.5 -b_qfactor 2.5 -qmin 80 -qmax 90 -b:v 100M -c:v NoPe2", ffmpeg.getCommandLine().toString().substring(header));
	}
	
	public void testNV() throws IOException, InterruptedException, ExecutionException, MediaException {
		if (System.getProperty("ffmpeg.test.nvidia", "").equals("1") == false) {
			return;
		}
		
		FFmpeg ffmpeg = create();
		ffmpeg.setOverwriteOutputFiles();
		ffmpeg.setOnErrorDeleteOutFiles(Runnable::run);
		
		FFAbout about = ffmpeg.getAbout();
		
		// this is buggy... assertTrue("NV Toolkit is not avaliable: " + ffmpeg.getAbout().getAvailableHWAccelerationMethods(), ffmpeg.getAbout().isNVToolkitIsAvaliable());
		
		CommandLine cmd = ffmpeg.getCommandLine();
		cmd.addBulkParameters("-f lavfi -i smptehdbars=duration=" + 5 + ":size=1280x720:rate=25");
		
		ffmpeg.addHardwareVideoEncoding("h264", -1, FFHardwareCodec.NV, ffmpeg.getAbout()).addCRF(0);
		assertTrue(cmd.getValues("-c:v").stream().findFirst().orElseThrow(() -> new NullPointerException("No codecs was added: " + cmd)).contains("nvenc"));
		
		File test_file = File.createTempFile("smptebars", ".mkv");
		ffmpeg.addSimpleOutputDestination(test_file.getPath());
		
		ExecProcessText exec = ffmpeg.createExec();
		System.out.println("Generate test file to \"" + test_file.getPath() + "\"");
		
		ExecProcessTextResult result = exec.run();
		if (result.isCorrectlyDone().get() == false) {
			fail(result.getStderrLines(false).filter(ffmpeg.filterOutErrorLines).collect(Collectors.joining(System.lineSeparator())));
		}
		
		assertTrue(test_file.exists());
		
		ffmpeg = create();
		ffmpeg.setOverwriteOutputFiles();
		ffmpeg.setOnErrorDeleteOutFiles(Runnable::run);
		
		ffmpeg.addHardwareVideoDecoding(test_file.getPath(), new ProbeMedia().doAnalysing(test_file.getPath()).get(), FFHardwareCodec.NV, about);
		ffmpeg.addHardwareVideoEncoding("h264", -1, FFHardwareCodec.NV, ffmpeg.getAbout()).addCRF(40);
		
		File test_file2 = File.createTempFile("smptebars", ".mkv");
		ffmpeg.addSimpleOutputDestination(test_file2.getPath());
		
		exec = ffmpeg.createExec();
		System.out.println("Hardware decoding to \"" + test_file.getPath() + "\"");
		
		result = exec.run();
		if (result.isCorrectlyDone().get() == false) {
			fail(result.getStderrLines(false).filter(ffmpeg.filterOutErrorLines).collect(Collectors.joining(System.lineSeparator())));
		}
		
		assertTrue(test_file2.exists());
		
		assertTrue(test_file.delete());
		assertTrue(test_file2.delete());
	}
	
	public void testGetFirstVideoStream() throws IOException, InterruptedException, ExecutionException, MediaException {
		if (System.getProperty("ffmpeg.test.nvidia", "").equals("1") == false) {
			return;
		}
		
		FFmpeg ffmpeg = create();
		ffmpeg.setOverwriteOutputFiles();
		ffmpeg.setOnErrorDeleteOutFiles(Runnable::run);
		
		CommandLine cmd = ffmpeg.getCommandLine();
		cmd.addBulkParameters("-f lavfi -i smptehdbars=duration=" + 5 + ":size=1280x720:rate=25");
		ffmpeg.addVideoCodecName("ffv1", -1);
		
		File test_file = File.createTempFile("smptebars", ".mkv");
		ffmpeg.addSimpleOutputDestination(test_file.getPath());
		
		ExecProcessText exec = ffmpeg.createExec();
		System.out.println("Generate test file to \"" + test_file.getPath() + "\"");
		
		ExecProcessTextResult result = exec.run();
		if (result.isCorrectlyDone().get() == false) {
			fail(result.getStderrLines(false).filter(ffmpeg.filterOutErrorLines).collect(Collectors.joining(System.lineSeparator())));
		}
		
		assertTrue(test_file.exists());
		
		StreamType s = FFmpeg.getFirstVideoStream(new ProbeMedia().doAnalysing(test_file.getPath()).get()).get();
		assertEquals("ffv1", s.getCodecName());
	}
	
	public void testNV_NPP() {
		if (System.getProperty("ffmpeg.test.libnpp", "").equals("1")) {
			return;
		}
		// XXX test public FFmpeg addSimpleSourceHardwareNVDecoded(String source, int device_id_to_use, String source_cuvid_codec_engine, Collection<String> additional_params) {
		// XXX test public FFmpeg addHardwareNVScalerFilter(Point new_size, String pixel_format, String interp_algo) {
		// XXX test public FFmpeg addHardwareNVMultipleScalerFilterComplex(LinkedHashMap<String, String> configuration, int device_id_to_use) {
	}
	
	// XXX test maximum parallel transcoding NV
	
}
