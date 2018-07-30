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
	
	// TODO2 ffmetadata import/export: ffmpeg -i INPUT -f ffmetadata FFMETADATAFILE / ffmpeg -i INPUT -i FFMETADATAFILE -map_metadata 1 -codec copy OUTPUT
	
	// TODO handle NVidia transc
	/*
	 * --enable-cuda --enable-cuvid --enable-nvenc
	 * Decode : [-hwaccel_device 0] -hwaccel cuvid -c:v h264_cuvid -vsync 0 -i <input.mp4> {H.264, HEVC, MJPEG, MPEG-1/2/4, VP8/VP9, VC-1, VP9 + 10bits}
	 * Code: -c:v h264_nvenc or hevc_nvenc
	 *  
	 * NVIDIA Performance Primitives: If --enable-libnpp >> hardware scaler >> -vf scale_npp=-1:720  or 1280:720
	 	format									The pixel format of the output CUDA frames. If set to the string "same" (the default), the input format will be kept. Note that automatic format negotiation and conversion is not yet supported for hardware frames
		interp_algo
			The interpolation algorithm used for resizing. One of the following:
			nn									Nearest neighbour.
			linear / cubic / cubic2p_bspline	2-parameter cubic (B=1, C=0)
			cubic2p_catmullrom					2-parameter cubic (B=0, C=1/2)
			cubic2p_b05c03						2-parameter cubic (B=1/2, C=3/10)
			super								Supersampling
			lanczos
	 * 
	 * ffmpeg -y -i INPUT.mp4 -filter_complex nvresize=5:s=hd1080\|hd720\|hd480\|wvga\|cif:readback=0[out0][out1][out2][out3][out4] \
	   -map [out0] -acodec copy -vcodec nvenc -b:v 5M out0nv.mkv \
	   -map [out1] -acodec copy -vcodec nvenc -b:v 4M out1nv.mkv \
	   -map [out2] -acodec copy -vcodec nvenc -b:v 3M out2nv.mkv \
	   -map [out3] -acodec copy -vcodec nvenc -b:v 2M out3nv.mkv \
	   -map [out4] -acodec copy -vcodec nvenc -b:v 1M out4nv.mkv
	   
	   -preset slow -b:v 5M -maxrate 10M -bufsize:v 10M -bf 2 -ref 1 -g 150 -i_qfactor 1.1 -b_qfactor 1.25 -qmin 1 -qmax 50
	    HWaccell : cuda cuvid
	    
	    https://developer.nvidia.com/ffmpeg
	    https://developer.nvidia.com/video-encode-decode-gpu-support-matrix
	    https://trac.ffmpeg.org/wiki/HWAccelIntro#NVENC
	 * */
}
