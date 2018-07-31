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

import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.CommandLineProcessor.CommandLine;
import tv.hd3g.execprocess.ExecutableFinder;

public class FFmpeg extends FFbase {
	
	private static final Logger log = LogManager.getLogger();
	
	public FFmpeg(ExecutableFinder exec_finder, CommandLine command_line) throws FileNotFoundException {
		super(exec_finder, command_line);
	}
	
	/**
	 * Define cmd var name like <%OUT_AUTOMATIC_n%> with "n" the # of setted destination.
	 * Add "-f container destination"
	 */
	public FFmpeg addSimpleOutputDestination(String destination_name, String destination_container) {
		if (destination_name == null) {
			throw new NullPointerException("\"destination_name\" can't to be null");
		} else if (destination_container == null) {
			throw new NullPointerException("\"destination_container\" can't to be null");
		}
		
		/*Stream<String> s_source_options = Stream.empty();
		if (source_options != null) {
			s_source_options = Arrays.stream(source_options);
		}*/
		
		String varname = command_line.addVariable("OUT_AUTOMATIC_" + output_expected_destinations.size());
		addOutputDestination(destination_name, varname, "-f", destination_container);
		return this;
	}
	
	/**
	 * Add "-movflags faststart"
	 * Please, put it a the end of command line, before output stream.
	 */
	public FFmpeg addFastStartMovMp4File() {
		command_line.addBulkParameters("-movflags faststart");
		return this;
	}
	
	/**
	 * Not checks will be done
	 * @param device_id_to_use -1 for default, 0 for first graphic card...
	 * @param source_codec, decode from codec, like h264_cuvid, mjpeg_cuvid... ALL CODECS ARE NOT AVAILABLE FOR ALL GRAPHICS CARDS, EVEN IF FFMPEG SUPPORT IT.
	 */
	public FFmpeg addSimpleSourceHardwareNVDecoded(String source, int device_id_to_use, String source_cuvid_codec_engine, Collection<String> additional_params) {
		/**
		 * [-hwaccel_device 0] -hwaccel cuvid -c:v source_cuvid_codec [-vsync 0] -i source
		 */
		ArrayList<String> source_options = new ArrayList<>();
		if (device_id_to_use > -1) {
			source_options.add("-hwaccel_device");
			source_options.add(Integer.toString(device_id_to_use));
		}
		source_options.add("-hwaccel");
		source_options.add("cuvid");
		source_options.add("-vsync");
		source_options.add("0");
		source_options.add("-c:v");
		source_options.add(source_cuvid_codec_engine);
		
		log.debug("Add source: " + source_options.stream().collect(Collectors.joining(" ")) + " -i " + source);
		addSimpleInputSource(source, source_options);
		return this;
	}
	
	/**
	 * Not checks will be done
	 * NVIDIA Performance Primitives via libnpp.
	 * Via -vf ffmpeg's option.
	 * @param new_size like 1280x720 or -1x720
	 * @param pixel_format can be null ( -> same) or nv12, yuv444p16...
	 * @param interp_algo can be null or nn (Nearest neighbour), linear (2-parameter cubic (B=1, C=0)), cubic2p_catmullrom (2-parameter cubic (B=0, C=1/2)), cubic2p_b05c03 (2-parameter cubic (B=1/2, C=3/10)), super (Supersampling), lanczos ...
	 */
	public FFmpeg addHardwareNVScalerFilter(Point new_size, String pixel_format, String interp_algo) {
		StringBuilder scale = new StringBuilder();
		
		scale.append("scale_npp=");
		scale.append("w=" + new_size.x + ":");
		scale.append("h=" + new_size.y + ":");
		if (pixel_format != null) {
			scale.append("format=" + pixel_format + ":");
		}
		if (interp_algo != null) {
			scale.append("interp_algo=" + interp_algo);
		}
		
		log.debug("Add vf: " + scale.toString());
		command_line.addParameters("-vf", scale.toString());
		
		return this;
	}
	
	/**
	 * Use nvresize
	 * Not checks will be done
	 * @param configuration resolution -> filter out name ; resolution can be litteral like hd1080 or cif and filter out name can be "out0", usable after with "-map [out0] -vcodec xxx out.ext"
	 * @param device_id_to_use -1 for default, 0 for first graphic card...
	 */
	public FFmpeg addHardwareNVMultipleScalerFilterComplex(LinkedHashMap<String, String> configuration, int device_id_to_use) {
		if (configuration == null) {
			throw new NullPointerException("\"configuration\" can't to be null");
		}
		if (configuration.isEmpty()) {
			throw new NullPointerException("\"configuration\" can't to be empty");
		}
		/*
		+    { "outputs",  "set number of outputs",  OFFSET(nb_outputs),  AV_OPT_TYPE_INT, { .i64 = 1 }, 1, MAX_OUTPUT, FLAGS },
		+    { "readback", "read result back to FB", OFFSET(readback_FB), AV_OPT_TYPE_INT, { .i64 = 0 }, 0, 1, FLAGS },
		+    { "size",     "set video size",         OFFSET(size_str),    AV_OPT_TYPE_STRING, {.str = NULL}, 0, FLAGS },
		+    { "s",        "set video size",         OFFSET(size_str),    AV_OPT_TYPE_STRING, {.str = NULL}, 0, FLAGS },
		+    { "gpu", "Selects which NVENC capable GPU to use. First GPU is 0, second is 1, and so on.", OFFSET(gpu), AV_OPT_TYPE_INT, { .i64 = 0 }, 0, INT_MAX, FLAGS },
		+    { "force_original_aspect_ratio", "decrease or increase w/h if necessary to keep the original AR", OFFSET(force_original_aspect_ratio), AV_OPT_TYPE_INT, { .i64 = 0}, 0, 2, FLAGS, "force_oar" },
		
		ffmpeg -y -i INPUT.mp4 -filter_complex nvresize=5:size=hd1080\|hd720\|hd480\|wvga\|cif:gpu=0:readback=0[out0][out1][out2][out3][out4] \
		-map [out0] -acodec copy -vcodec nvenc -b:v 5M out0nv.mkv \
		-map [out1] -acodec copy -vcodec nvenc -b:v 4M out1nv.mkv \
		-map [out2] -acodec copy -vcodec nvenc -b:v 3M out2nv.mkv \
		-map [out3] -acodec copy -vcodec nvenc -b:v 2M out3nv.mkv \
		-map [out4] -acodec copy -vcodec nvenc -b:v 1M out4nv.mkv
		 * */
		
		StringBuilder nvresize = new StringBuilder();
		nvresize.append("nvresize=outputs=" + configuration.size() + ":");
		nvresize.append("size=" + configuration.keySet().stream().collect(Collectors.joining("|")) + ":");
		
		if (device_id_to_use > -1) {
			nvresize.append("gpu=" + device_id_to_use + ":");
		}
		
		nvresize.append("readback=0" + configuration.keySet().stream().map(resolution -> configuration.get(resolution)).collect(Collectors.joining("", "[", "]")));
		
		log.debug("Add filter_complex: " + nvresize.toString());
		command_line.addParameters("-filter_complex", nvresize.toString());
		return this;
	}
	
	// TODO2 ffmetadata import/export: ffmpeg -i INPUT -f ffmetadata FFMETADATAFILE / ffmpeg -i INPUT -i FFMETADATAFILE -map_metadata 1 -codec copy OUTPUT
	
	/*
	   -preset slow -b:v 5M -maxrate 10M -bufsize:v 10M -bf 2 -ref 1 -g 150 -i_qfactor 1.1 -b_qfactor 1.25 -qmin 1 -qmax 50
	   
	    https://developer.nvidia.com/ffmpeg
	    https://developer.nvidia.com/video-encode-decode-gpu-support-matrix
	    https://trac.ffmpeg.org/wiki/HWAccelIntro#NVENC
	 * */
}
