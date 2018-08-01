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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.ParametersUtility;
import tv.hd3g.fflauncher.ConversionTool;

public abstract class Snippet extends ParametersUtility {
	
	private static final Logger log = LogManager.getLogger();
	
	protected final ConversionTool referer;
	
	public Snippet(ConversionTool referer) {
		this.referer = referer;
		if (referer == null) {
			throw new NullPointerException("\"referer\" can't to be null");
		}
	}
	
	public void commit() {
		log.debug("Add to {}: {}", toolName(), toString());
		referer.getCommandLine().addAllFrom(this);
	}
	
	protected abstract String toolName();
	
}
