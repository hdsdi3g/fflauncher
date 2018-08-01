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
package tv.hd3g.fflauncher.snippets;

import tv.hd3g.fflauncher.FFDecimalPrefixUnit;
import tv.hd3g.fflauncher.FFmpeg;

public class FFVideoTranscoding extends Snippet {
	
	public enum Preset {
		ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow, placebo
	}
	
	public enum Tune {
		film, animation, grain, stillimage, psnr, ssim, fastdecode, zerolatency
	}
	
	// TODO test FFVideoTranscoding
	
	private Preset preset;
	private Tune tune;
	private int bitrate;
	private int min_rate;
	private int max_rate;
	private int bufsize;
	private FFDecimalPrefixUnit bitrate_unit;
	private int output_video_stream_index;
	private int crf;
	private String codec_name;
	private int b_frames;
	private int ref_frames;
	private int gop_size;
	private float i_qfactor;
	private float b_qfactor;
	private int qmin;
	private int qmax;
	
	public FFVideoTranscoding(FFmpeg ffmpeg) {
		super(ffmpeg);
		bitrate = 1;
		bitrate_unit = FFDecimalPrefixUnit.mega;
		output_video_stream_index = -1;
	}
	
	/**
	 * It don't clear before. Be carefull before reuse if you set manually some addParameters()
	 */
	public void commit() {
		if (preset != null) {
			addParameters("-preset", preset.name());
		}
		if (tune != null) {
			addParameters("-tune", tune.name());
		}
		
		if (crf > 0) {
			addParameters("-crf", String.valueOf(crf));
		} else {
			if (output_video_stream_index > -1) {
				addParameters("-b:v:" + output_video_stream_index, bitrate + bitrate_unit.toString());
			} else {
				addParameters("-b:v", bitrate + bitrate_unit.toString());
			}
			if (max_rate > 0) {
				addParameters("-maxrate", max_rate + bitrate_unit.toString());
			}
			if (min_rate > 0) {
				addParameters("-minrate", min_rate + bitrate_unit.toString());
			}
			if (bufsize > 0) {
				addParameters("-bufsize", bufsize + bitrate_unit.toString());
			}
		}
		
		if (codec_name != null) {
			if (output_video_stream_index > -1) {
				addParameters("-c:v:" + output_video_stream_index, codec_name);
			} else {
				addParameters("-c:v", codec_name);
			}
		}
		
		if (b_frames > 0) {
			addParameters("-bf", String.valueOf(b_frames));
		}
		if (ref_frames > 0) {
			addParameters("-ref", String.valueOf(ref_frames));
		}
		if (gop_size > 0) {
			addParameters("-g", String.valueOf(gop_size));
		}
		if (i_qfactor > 0f) {
			addParameters("-i_qfactor", String.valueOf(i_qfactor));
		}
		if (b_qfactor > 0f) {
			addParameters("-b_qfactor", String.valueOf(b_qfactor));
		}
		if (qmin > 0) {
			addParameters("-qmin", String.valueOf(qmin));
		}
		if (qmax > 0) {
			addParameters("-qmax", String.valueOf(qmax));
		}
		
		super.commit();
	}
	
	public Preset getPreset() {
		return preset;
	}
	
	public FFVideoTranscoding setPreset(Preset preset) {
		this.preset = preset;
		return this;
	}
	
	public Tune getTune() {
		return tune;
	}
	
	public FFVideoTranscoding setTune(Tune tune) {
		this.tune = tune;
		return this;
	}
	
	public int getBitrate() {
		return bitrate;
	}
	
	/**
	 * Set unit with setBitrateUnit()
	 */
	public FFVideoTranscoding setBitrate(int bitrate) {
		this.bitrate = bitrate;
		return this;
	}
	
	public int getMinRate() {
		return min_rate;
	}
	
	/**
	 * Set unit with setBitrateUnit()
	 */
	public FFVideoTranscoding setMinEate(int min_rate) {
		this.min_rate = min_rate;
		return this;
	}
	
	public int getMaxEate() {
		return max_rate;
	}
	
	/**
	 * Set unit with setBitrateUnit()
	 */
	public FFVideoTranscoding setMaxRate(int max_rate) {
		this.max_rate = max_rate;
		return this;
	}
	
	public int getBufsize() {
		return bufsize;
	}
	
	/**
	 * Set unit with setBitrateUnit()
	 */
	public FFVideoTranscoding setBufsize(int bufsize) {
		this.bufsize = bufsize;
		return this;
	}
	
	public FFDecimalPrefixUnit getBitrateUnit() {
		return bitrate_unit;
	}
	
	/**
	 * @param bitrate_unit for bitrate, min_rate, max_rate and bufsize.
	 */
	public FFVideoTranscoding setBitrateUnit(FFDecimalPrefixUnit bitrate_unit) {
		this.bitrate_unit = bitrate_unit;
		return this;
	}
	
	public int getOutputVideoStreamIndex() {
		return output_video_stream_index;
	}
	
	public FFVideoTranscoding setOutputVideoStreamIndex(int output_video_stream_index) {
		this.output_video_stream_index = output_video_stream_index;
		return this;
	}
	
	public int getCrf() {
		return crf;
	}
	
	/**
	 * Exclude all manual bitrate settings.
	 */
	public FFVideoTranscoding setCrf(int crf) {
		this.crf = crf;
		return this;
	}
	
	public String getCodecName() {
		return codec_name;
	}
	
	/**
	 * @see FFmpeg.addVideoEncoding for hardware use
	 */
	public FFVideoTranscoding setCodecName(String codec_name) {
		this.codec_name = codec_name;
		return this;
	}
	
	public int getBFrames() {
		return b_frames;
	}
	
	public FFVideoTranscoding setBFrames(int b_frames) {
		this.b_frames = b_frames;
		return this;
	}
	
	public int getRefFrames() {
		return ref_frames;
	}
	
	public FFVideoTranscoding setRefFrames(int ref_frames) {
		this.ref_frames = ref_frames;
		return this;
	}
	
	public int getGopSize() {
		return gop_size;
	}
	
	public FFVideoTranscoding setGopSize(int gop_size) {
		this.gop_size = gop_size;
		return this;
	}
	
	public float getIQfactor() {
		return i_qfactor;
	}
	
	public FFVideoTranscoding setIQfactor(float i_qfactor) {
		this.i_qfactor = i_qfactor;
		return this;
	}
	
	public float getBQfactor() {
		return b_qfactor;
	}
	
	public FFVideoTranscoding setBQfactor(float b_qfactor) {
		this.b_qfactor = b_qfactor;
		return this;
	}
	
	public int getQmin() {
		return qmin;
	}
	
	public FFVideoTranscoding setQmin(int qmin) {
		this.qmin = qmin;
		return this;
	}
	
	public int getQmax() {
		return qmax;
	}
	
	public FFVideoTranscoding setQmax(int qmax) {
		this.qmax = qmax;
		return this;
	}
	
	protected String toolName() {
		return "ffmpeg";
	}
	
}
