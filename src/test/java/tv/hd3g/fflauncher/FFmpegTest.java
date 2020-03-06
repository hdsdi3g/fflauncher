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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.ffmpeg.ffprobe.StreamType;

import junit.framework.TestCase;
import tv.hd3g.fflauncher.FFmpeg.FFHardwareCodec;
import tv.hd3g.fflauncher.FFmpeg.Preset;
import tv.hd3g.fflauncher.FFmpeg.Tune;
import tv.hd3g.fflauncher.recipes.ProbeMedia;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.tool.ToolRunner;

public class FFmpegTest extends TestCase {

	private final ToolRunner toolRun;
	private final ScheduledExecutorService maxExecTimeScheduler;
	private final ProbeMedia probeMedia;

	public FFmpegTest() {
		toolRun = new ToolRunner(new ExecutableFinder());
		maxExecTimeScheduler = Executors.newSingleThreadScheduledExecutor();
		probeMedia = new ProbeMedia(toolRun, maxExecTimeScheduler);
	}

	private FFmpeg create() {
		return new FFmpeg("ffmpeg", new Parameters());
	}

	public void testSimpleOutputDestination() {
		final FFmpeg ffmpeg = create();
		ffmpeg.addSimpleOutputDestination("dest", "container");

		assertTrue(ffmpeg.getReadyToRunParameters().toString().endsWith("-f container dest"));
	}

	public void testParameters() throws FileNotFoundException {
		final FFmpeg ffmpeg = create();
		final int header = ffmpeg.getInternalParameters().toString().length();

		ffmpeg.addPreset(Preset.placebo).addTune(Tune.ssim).addBitrate(123, FFUnit.giga, 1);
		ffmpeg.addBitrateControl(10, 20, 30, FFUnit.mega).addCRF(40).addVideoCodecName("NoPe", 2);
		ffmpeg.addGOPControl(50, 60, 70).addIBQfactor(1.5f, 2.5f).addQMinMax(80, 90);
		ffmpeg.addBitrate(100, FFUnit.mega, -1).addVideoCodecName("NoPe2", -1);

		assertEquals(
		        "-preset placebo -tune ssim -b:v:1 123G -minrate 10M -maxrate 20M -bufsize 30M -crf 40 -c:v:2 NoPe -bf 50 -g 60 -ref 70 -i_qfactor 1.5 -b_qfactor 2.5 -qmin 80 -qmax 90 -b:v 100M -c:v NoPe2",
		        ffmpeg.getInternalParameters().toString().substring(header));
	}

	public void testNV() throws IOException, MediaException {
		if (System.getProperty("ffmpeg.test.nvidia", "").equals("1") == false) {
			return;
		}

		FFmpeg ffmpeg = create();
		ffmpeg.setOverwriteOutputFiles();
		ffmpeg.setOnErrorDeleteOutFiles(true);

		final FFAbout about = ffmpeg.getAbout(new ExecutableFinder());

		// this is buggy... assertTrue("NV Toolkit is not avaliable: " + about.getAvailableHWAccelerationMethods(), about.isNVToolkitIsAvaliable());

		final Parameters cmd = ffmpeg.getInternalParameters();
		cmd.addBulkParameters("-f lavfi -i smptehdbars=duration=" + 5 + ":size=1280x720:rate=25");

		ffmpeg.addHardwareVideoEncoding("h264", -1, FFHardwareCodec.NV, about).addCRF(0);
		assertTrue(cmd.getValues("-c:v").stream().findFirst().orElseThrow(() -> new NullPointerException(
		        "No codecs was added: " + cmd)).contains("nvenc"));

		final File test_file = File.createTempFile("smptebars", ".mkv");
		ffmpeg.addSimpleOutputDestination(test_file.getPath());

		System.out.println("Generate test file to \"" + test_file.getPath() + "\"");

		toolRun.execute(ffmpeg).checkExecutionGetText();

		assertTrue(test_file.exists());

		ffmpeg = create();
		ffmpeg.setOverwriteOutputFiles();
		ffmpeg.setOnErrorDeleteOutFiles(true);

		ffmpeg.addHardwareVideoDecoding(test_file.getPath(), probeMedia.doAnalysing(test_file.getPath()),
		        FFHardwareCodec.NV, about);
		ffmpeg.addHardwareVideoEncoding("h264", -1, FFHardwareCodec.NV, about).addCRF(40);

		final File test_file2 = File.createTempFile("smptebars", ".mkv");
		ffmpeg.addSimpleOutputDestination(test_file2.getPath());

		System.out.println("Hardware decoding to \"" + test_file.getPath() + "\"");
		toolRun.execute(ffmpeg).checkExecutionGetText();

		assertTrue(test_file2.exists());
		assertTrue(test_file.delete());
		assertTrue(test_file2.delete());
	}

	public void testGetFirstVideoStream() throws IOException, InterruptedException, ExecutionException, MediaException {
		if (System.getProperty("ffmpeg.test.nvidia", "").equals("1") == false) {
			return;
		}

		final FFmpeg ffmpeg = create();
		ffmpeg.setOverwriteOutputFiles();
		ffmpeg.setOnErrorDeleteOutFiles(true);

		final Parameters cmd = ffmpeg.getInternalParameters();
		cmd.addBulkParameters("-f lavfi -i smptehdbars=duration=" + 5 + ":size=1280x720:rate=25");
		ffmpeg.addVideoCodecName("ffv1", -1);

		final File test_file = File.createTempFile("smptebars", ".mkv");
		ffmpeg.addSimpleOutputDestination(test_file.getPath());

		System.out.println("Generate test file to \"" + test_file.getPath() + "\"");
		toolRun.execute(ffmpeg).checkExecutionGetText();

		assertTrue(test_file.exists());

		final StreamType s = FFmpeg.getFirstVideoStream(probeMedia.doAnalysing(test_file.getPath())).get();
		assertEquals("ffv1", s.getCodecName());
	}

	public void testNV_NPP() {
		if (System.getProperty("ffmpeg.test.libnpp", "").equals("1")) {
			return;
		}
		// TODO3 test public FFmpeg addSimpleSourceHardwareNVDecoded(String source, int device_id_to_use, String source_cuvid_codec_engine, Collection<String> additional_params) {
		// TODO3 test public FFmpeg addHardwareNVScalerFilter(Point new_size, String pixel_format, String interp_algo) {
		// TODO3 test public FFmpeg addHardwareNVMultipleScalerFilterComplex(LinkedHashMap<String, String> configuration, int device_id_to_use) {
	}

	// TODO3 test maximum parallel transcoding NV

}
