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
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ffmpeg.ffprobe.StreamType;

import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.processlauncher.cmdline.Parameters;

public class FFmpeg extends FFbase {

	private static final Logger log = LogManager.getLogger();
	private int device_id_to_use = -1;

	public FFmpeg(final String execName, final Parameters parameters) {
		super(execName, parameters);
	}

	/**
	 * Define cmd var name like <%OUT_AUTOMATIC_n%> with "n" the # of setted destination.
	 * Add "-f container destination"
	 */
	public FFmpeg addSimpleOutputDestination(final String destination_name, final String destination_container) {
		if (destination_name == null) {
			throw new NullPointerException("\"destination_name\" can't to be null");
		} else if (destination_container == null) {
			throw new NullPointerException("\"destination_container\" can't to be null");
		}

		final String varname = getInternalParameters().addVariable("OUT_AUTOMATIC_" + output_expected_destinations.size());
		addOutputDestination(destination_name, varname, "-f", destination_container);
		return this;
	}

	/**
	 * Define cmd var name like <%OUT_AUTOMATIC_n%> with "n" the # of setted destination.
	 * Add "-f container /destination"
	 */
	public FFmpeg addSimpleOutputDestination(final File destination_file, final String destination_container) {
		if (destination_file == null) {
			throw new NullPointerException("\"destinatdestination_fileion_name\" can't to be null");
		} else if (destination_container == null) {
			throw new NullPointerException("\"destination_container\" can't to be null");
		}

		final String varname = getInternalParameters().addVariable("OUT_AUTOMATIC_" + output_expected_destinations.size());
		addOutputDestination(destination_file, varname, "-f", destination_container);
		return this;
	}

	/**
	 * Add "-movflags faststart"
	 * Please, put it a the end of command line, before output stream.
	 */
	public FFmpeg addFastStartMovMp4File() {
		getInternalParameters().addBulkParameters("-movflags faststart");
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
	public FFmpeg addHardwareNVScalerFilter(final Point new_size, final String pixel_format, final String interp_algo) {
		final StringBuilder scale = new StringBuilder();

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
		getInternalParameters().addParameters("-vf", scale.toString());

		return this;
	}

	/**
	 * Use nvresize
	 * Not checks will be done
	 * @param configuration resolution -> filter out name ; resolution can be litteral like hd1080 or cif and filter out name can be "out0", usable after with "-map [out0] -vcodec xxx out.ext"
	 * @param device_id_to_use -1 for default, 0 for first graphic card...
	 */
	public FFmpeg addHardwareNVMultipleScalerFilterComplex(final LinkedHashMap<String, String> configuration) {
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

		final StringBuilder nvresize = new StringBuilder();
		nvresize.append("nvresize=outputs=" + configuration.size() + ":");
		nvresize.append("size=" + configuration.keySet().stream().collect(Collectors.joining("|")) + ":");

		if (device_id_to_use > -1) {
			nvresize.append("gpu=" + device_id_to_use + ":");
		}

		nvresize.append("readback=0" + configuration.keySet().stream().map(resolution -> configuration.get(resolution)).collect(Collectors.joining("", "[", "]")));

		log.debug("Add filter_complex: " + nvresize.toString());
		getInternalParameters().addParameters("-filter_complex", nvresize.toString());
		return this;
	}

	public static Optional<StreamType> getFirstVideoStream(final FFprobeJAXB analysing_result) {
		final Optional<StreamType> o_video_stream = analysing_result.getVideoStreams().findFirst();

		if (o_video_stream.isPresent()) {
			if (o_video_stream.get().getDisposition().getAttachedPic() == 0) {
				return o_video_stream;
			}
		}
		return Optional.empty();
	}

	public enum FFHardwareCodec {
		/**
		 * cuvid and nvenc
		 * ALL CODECS ARE NOT AVAILABLE FOR ALL GRAPHICS CARDS, EVEN IF FFMPEG SUPPORT IT.
		 */
		NV;
	}

	/**
	 * Used with hardware transcoding.
	 * @param device_id_to_use -1 by default
	 */
	public FFmpeg setDeviceIdToUse(final int device_id_to_use) {
		this.device_id_to_use = device_id_to_use;
		return this;
	}

	/**
	 * @return -1 by default
	 */
	public int getDevice_id_to_use() {
		return device_id_to_use;
	}

	/**
	 * "Patch" ffmpeg command line for hardware decoding. Only first video stream will be decoded.
	 * Hardware decoding often works in tandem with hardware coding.
	 * @param device_id_to_use -1 for default, 0 for first graphic card...
	 * @throws MediaException if hardware decoding is not possible.
	 */
	public FFmpeg addHardwareVideoDecoding(final String source, final FFprobeJAXB analysing_result, final FFHardwareCodec hardware_codec, final FFAbout about) throws MediaException {
		final Optional<StreamType> o_video_stream = getFirstVideoStream(analysing_result);

		if (o_video_stream.isPresent() == false) {
			throw new MediaException("Can't found \"valid\" video stream on \"" + source + "\"");
		}

		final StreamType video_stream = o_video_stream.get();

		final FFCodec codec = about.getCodecs().stream().filter(c -> {
			return c.decoding_supported & c.name.equals(video_stream.getCodecName());
		}).findFirst().orElseThrow(() -> new MediaException("Can't found a valid decoder codec for " + video_stream.getCodecName() + " in \"" + source + "\""));

		if (hardware_codec == FFHardwareCodec.NV & about.isNVToolkitIsAvaliable()) {
			final Optional<String> o_source_cuvid_codec_engine = codec.decoders.stream().filter(decoder -> decoder.endsWith("_cuvid")).findFirst();

			if (o_source_cuvid_codec_engine.isPresent()) {
				/**
				 * [-hwaccel_device 0] -hwaccel cuvid -c:v source_cuvid_codec [-vsync 0] -i source
				 */
				final ArrayList<String> source_options = new ArrayList<>();
				if (device_id_to_use > -1) {
					source_options.add("-hwaccel_device");
					source_options.add(Integer.toString(device_id_to_use));
				}
				source_options.add("-hwaccel");
				source_options.add("cuvid");
				source_options.add("-vsync");
				source_options.add("0");
				source_options.add("-c:v");
				source_options.add(o_source_cuvid_codec_engine.get());

				log.debug("Add hardware decoded source " + source_options.stream().collect(Collectors.joining(" ")) + " -i " + source);
				addSimpleInputSource(source, source_options);
				return this;
			}
		}

		throw new MediaException("Can't found a valid hardware decoder on \"" + source + "\" (\"" + video_stream.getCodecLongName() + "\")");
	}

	/**
	 * Set codec name, and if it possible, use hardware encoding.
	 * @param dest_codec_name
	 * @param output_video_stream_index (-1 by default), X -> -c:v:X
	 */
	public FFmpeg addHardwareVideoEncoding(final String dest_codec_name, final int output_video_stream_index, final FFHardwareCodec hardware_codec, final FFAbout about) throws MediaException {
		String coder = dest_codec_name;

		if (dest_codec_name.equals("copy")) {
			new MediaException("\"copy\" codec can't be handled by hardware !");
		}

		final FFCodec codec = about.getCodecs().stream().filter(c -> {
			return c.encoding_supported & c.name.equals(dest_codec_name);
		}).findFirst().orElseThrow(() -> new MediaException("Can't found a valid codec for " + dest_codec_name));

		if (hardware_codec == FFHardwareCodec.NV & about.isNVToolkitIsAvaliable()) {
			coder = codec.encoders.stream().filter(encoder -> {
				return encoder.endsWith("_nvenc") | encoder.startsWith("nvenc_") | encoder.equals("nvenc");
			}).findFirst().orElseThrow(() -> new MediaException("Can't found a valid hardware " + hardware_codec + " codec for " + dest_codec_name));

		} else {
			throw new MediaException("Can't found a valid hardware coder to \"" + dest_codec_name + "\"");
		}

		if (output_video_stream_index > -1) {
			getInternalParameters().addParameters("-c:v:" + output_video_stream_index, coder);
		} else {
			getInternalParameters().addParameters("-c:v", coder);
		}

		return this;
	}

	public enum Preset {
		ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow, placebo
	}

	public enum Tune {
		film, animation, grain, stillimage, psnr, ssim, fastdecode, zerolatency
	}

	public FFmpeg addPreset(final Preset preset) {
		getInternalParameters().addParameters("-preset", preset.name());
		return this;
	}

	public FFmpeg addTune(final Tune tune) {
		getInternalParameters().addParameters("-tune", tune.name());
		return this;
	}

	/**
	 * @param output_video_stream_index -1 by default
	 */
	public FFmpeg addBitrate(final int bitrate, final FFUnit bitrate_unit, final int output_video_stream_index) {
		if (output_video_stream_index > -1) {
			getInternalParameters().addParameters("-b:v:" + output_video_stream_index, bitrate + bitrate_unit.toString());
		} else {
			getInternalParameters().addParameters("-b:v", bitrate + bitrate_unit.toString());
		}
		return this;
	}

	/**
	 * @param min_rate/max_rate/bufsize, set -1 for default.
	 */
	public FFmpeg addBitrateControl(final int min_rate, final int max_rate, final int bufsize, final FFUnit bitrate_unit) {
		if (min_rate > 0) {
			getInternalParameters().addParameters("-minrate", min_rate + bitrate_unit.toString());
		}
		if (max_rate > 0) {
			getInternalParameters().addParameters("-maxrate", max_rate + bitrate_unit.toString());
		}
		if (bufsize > 0) {
			getInternalParameters().addParameters("-bufsize", bufsize + bitrate_unit.toString());
		}
		return this;
	}

	/**
	 * Constant bitrate factor, 0=lossless.
	 */
	public FFmpeg addCRF(final int crf) {
		getInternalParameters().addParameters("-crf", String.valueOf(crf));
		return this;
	}

	/**
	 * No checks will be done.
	 * @see FFmpeg.addVideoEncoding for hardware use
	 * @param output_video_stream_index -1 by default
	 */
	public FFmpeg addVideoCodecName(final String codec_name, final int output_video_stream_index) {
		if (output_video_stream_index > -1) {
			getInternalParameters().addParameters("-c:v:" + output_video_stream_index, codec_name);
		} else {
			getInternalParameters().addParameters("-c:v", codec_name);
		}
		return this;
	}

	/**
	 * @param min_rate/max_rate/bufsize, set -1 for default.
	 */
	public FFmpeg addGOPControl(final int b_frames, final int gop_size, final int ref_frames) {
		if (b_frames > 0) {
			getInternalParameters().addParameters("-bf", String.valueOf(b_frames));
		}
		if (gop_size > 0) {
			getInternalParameters().addParameters("-g", String.valueOf(gop_size));
		}
		if (ref_frames > 0) {
			getInternalParameters().addParameters("-ref", String.valueOf(ref_frames));
		}
		return this;
	}

	/**
	 * @param i_qfactor/b_qfactor set 0 for default
	 */
	public FFmpeg addIBQfactor(final float i_qfactor, final float b_qfactor) {
		if (i_qfactor > 0f) {
			getInternalParameters().addParameters("-i_qfactor", String.valueOf(i_qfactor));
		}
		if (b_qfactor > 0f) {
			getInternalParameters().addParameters("-b_qfactor", String.valueOf(b_qfactor));
		}
		return this;
	}

	/**
	 * @param qmin/qmax set 0 for default
	 */
	public FFmpeg addQMinMax(final int qmin, final int qmax) {
		if (qmin > 0) {
			getInternalParameters().addParameters("-qmin", String.valueOf(qmin));
		}
		if (qmax > 0) {
			getInternalParameters().addParameters("-qmax", String.valueOf(qmax));
		}
		return this;
	}

	/**
	 * No checks will be done.
	 * @param output_audio_stream_index -1 by default
	 */
	public FFmpeg addAudioCodecName(final String codec_name, final int output_audio_stream_index) {
		if (output_audio_stream_index > -1) {
			getInternalParameters().addParameters("-c:a:" + output_audio_stream_index, codec_name);
		} else {
			getInternalParameters().addParameters("-c:a", codec_name);
		}
		return this;
	}

	/**
	 * No checks will be done.
	 * like -vsync value
	 */
	public FFmpeg addVsync(final int value) {
		getInternalParameters().addParameters("-vsync", String.valueOf(value));
		return this;
	}

	/**
	 * No checks will be done.
	 * like -map source_index:stream_index_in_source ; 0 is the first.
	 */
	public FFmpeg addMap(final int source_index, final int stream_index_in_source) {
		getInternalParameters().addParameters("-map", String.valueOf(source_index) + ":" + String.valueOf(stream_index_in_source));
		return this;
	}

	/*public FFmpeg prepareResize(String source, Point new_size, FFprobeJAXB analysing_result) {
		// TODO2 ffmpeg.addHardwareNVScalerFilter(new_size, pixel_format, interp_algo)
		// about.isHardwareNVScalerFilterIsAvaliable()
		return this;
	}*/

	// TODO2 ffmpeg.addHardwareNVMultipleScalerFilterComplex(configuration, device_id_to_use)

	/*
	    https://developer.nvidia.com/ffmpeg
	    https://developer.nvidia.com/video-encode-decode-gpu-support-matrix
	    https://trac.ffmpeg.org/wiki/HWAccelIntro#NVENC
	 * */

	// TODO2 ffmetadata import/export: ffmpeg -i INPUT -f ffmetadata FFMETADATAFILE / ffmpeg -i INPUT -i FFMETADATAFILE -map_metadata 1 -codec copy OUTPUT

}
