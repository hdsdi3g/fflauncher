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

/**
 * Get by ffmpeg -layouts
 */
public enum Channel {
	/** front left */
	FL {
		public String toString() {
			return "front left";
		}
	},
	/** front right */
	FR {
		public String toString() {
			return "front right";
		}
	},
	/** front center */
	FC {
		public String toString() {
			return "front center";
		}
	},
	/** low frequency */
	LFE {
		public String toString() {
			return "low frequency";
		}
	},
	/** back left */
	BL {
		public String toString() {
			return "back left";
		}
	},
	/** back right */
	BR {
		public String toString() {
			return "back right";
		}
	},
	/** front left-of-center */
	FLC {
		public String toString() {
			return "front left-of-center";
		}
	},
	/** front right-of-center */
	FRC {
		public String toString() {
			return "front right-of-center";
		}
	},
	/** back center */
	BC {
		public String toString() {
			return "back center";
		}
	},
	/** side left */
	SL {
		public String toString() {
			return "side left";
		}
	},
	/** side right */
	SR {
		public String toString() {
			return "side right";
		}
	},
	/** top center */
	TC {
		public String toString() {
			return "top center";
		}
	},
	/** top front left */
	TFL {
		public String toString() {
			return "top front left";
		}
	},
	/** top front center */
	TFC {
		public String toString() {
			return "top front center";
		}
	},
	/** top front right */
	TFR {
		public String toString() {
			return "top front right";
		}
	},
	/** top back left */
	TBL {
		public String toString() {
			return "top back left";
		}
	},
	/** top back center */
	TBC {
		public String toString() {
			return "top back center";
		}
	},
	/** top back right */
	TBR {
		public String toString() {
			return "top back right";
		}
	},
	/** downmix left */
	DL {
		public String toString() {
			return "downmix left";
		}
	},
	/** downmix right */
	DR {
		public String toString() {
			return "downmix right";
		}
	},
	/** wide left */
	WL {
		public String toString() {
			return "wide left";
		}
	},
	/** wide right */
	WR {
		public String toString() {
			return "wide right";
		}
	},
	/** surround direct left */
	SDL {
		public String toString() {
			return "surround direct left";
		}
	},
	/** surround direct right */
	SDR {
		public String toString() {
			return "surround direct right";
		}
	},
	/** low frequency 2 */
	LFE2 {
		public String toString() {
			return "low frequency 2";
		}
	};
	
}
