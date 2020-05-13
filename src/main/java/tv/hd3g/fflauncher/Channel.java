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

/**
 * Get by ffmpeg -layouts
 */
public enum Channel {
	/** front left */
	FL {
		@Override
		public String toString() {
			return "front left";
		}
	},
	/** front right */
	FR {
		@Override
		public String toString() {
			return "front right";
		}
	},
	/** front center */
	FC {
		@Override
		public String toString() {
			return "front center";
		}
	},
	/** low frequency */
	LFE {
		@Override
		public String toString() {
			return "low frequency";
		}
	},
	/** back left */
	BL {
		@Override
		public String toString() {
			return "back left";
		}
	},
	/** back right */
	BR {
		@Override
		public String toString() {
			return "back right";
		}
	},
	/** front left-of-center */
	FLC {
		@Override
		public String toString() {
			return "front left-of-center";
		}
	},
	/** front right-of-center */
	FRC {
		@Override
		public String toString() {
			return "front right-of-center";
		}
	},
	/** back center */
	BC {
		@Override
		public String toString() {
			return "back center";
		}
	},
	/** side left */
	SL {
		@Override
		public String toString() {
			return "side left";
		}
	},
	/** side right */
	SR {
		@Override
		public String toString() {
			return "side right";
		}
	},
	/** top center */
	TC {
		@Override
		public String toString() {
			return "top center";
		}
	},
	/** top front left */
	TFL {
		@Override
		public String toString() {
			return "top front left";
		}
	},
	/** top front center */
	TFC {
		@Override
		public String toString() {
			return "top front center";
		}
	},
	/** top front right */
	TFR {
		@Override
		public String toString() {
			return "top front right";
		}
	},
	/** top back left */
	TBL {
		@Override
		public String toString() {
			return "top back left";
		}
	},
	/** top back center */
	TBC {
		@Override
		public String toString() {
			return "top back center";
		}
	},
	/** top back right */
	TBR {
		@Override
		public String toString() {
			return "top back right";
		}
	},
	/** downmix left */
	DL {
		@Override
		public String toString() {
			return "downmix left";
		}
	},
	/** downmix right */
	DR {
		@Override
		public String toString() {
			return "downmix right";
		}
	},
	/** wide left */
	WL {
		@Override
		public String toString() {
			return "wide left";
		}
	},
	/** wide right */
	WR {
		@Override
		public String toString() {
			return "wide right";
		}
	},
	/** surround direct left */
	SDL {
		@Override
		public String toString() {
			return "surround direct left";
		}
	},
	/** surround direct right */
	SDR {
		@Override
		public String toString() {
			return "surround direct right";
		}
	},
	/** low frequency 2 */
	LFE2 {
		@Override
		public String toString() {
			return "low frequency 2";
		}
	};

}
