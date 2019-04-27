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
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
*/
package tv.hd3g.fflauncher;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.processlauncher.cmdline.Parameters;

class ConversionToolParameterReference {

	private static final int TRY_COUNT_TO_OPEN_FILE = 5;

	private static Logger log = LogManager.getLogger();
	private static final Set<OpenOption> openOptionsReadOnly = Set.of(StandardOpenOption.READ);

	private final String ressource;
	private final boolean ressourceAsFile;
	private final String varNameInParameters;
	private final Parameters parametersBeforeRef;
	private final Parameters parametersAfterRef;

	/**
	 * @param parametersBeforeRef can be null
	 * @param parametersAfterRef can be null
	 */
	ConversionToolParameterReference(final String reference, final String varNameInParameters, final Collection<String> parametersBeforeRef, final Collection<String> parametersAfterRef) {
		ressource = Objects.requireNonNull(reference, "\"reference\" can't to be null");
		this.varNameInParameters = Objects.requireNonNull(varNameInParameters, "\"var_name_in_parameters\" can't to be null");
		this.parametersBeforeRef = Optional.ofNullable(parametersBeforeRef).map(p -> new Parameters(p)).orElseGet(() -> new Parameters());
		this.parametersAfterRef = Optional.ofNullable(parametersAfterRef).map(p -> new Parameters(p)).orElseGet(() -> new Parameters());
		ressourceAsFile = false;
	}

	/**
	 * @param parametersBeforeRef can be null
	 * @param parametersAfterRef can be null
	 */
	ConversionToolParameterReference(final File reference, final String varNameInParameters, final Collection<String> parametersBeforeRef, final Collection<String> parametersAfterRef) {
		ressource = Objects.requireNonNull(reference, "\"reference\" can't to be null").getPath();
		this.varNameInParameters = Objects.requireNonNull(varNameInParameters, "\"var_name_in_parameters\" can't to be null");
		this.parametersBeforeRef = Optional.ofNullable(parametersBeforeRef).map(p -> new Parameters(p)).orElseGet(() -> new Parameters());
		this.parametersAfterRef = Optional.ofNullable(parametersAfterRef).map(p -> new Parameters(p)).orElseGet(() -> new Parameters());
		ressourceAsFile = true;
	}

	String getRessource() {
		return ressource;
	}

	List<String> getParametersListAfterRef() {
		return parametersAfterRef.getParameters();
	}

	List<String> getParametersListBeforeRef() {
		return parametersBeforeRef.getParameters();
	}

	String getVarNameInParameters() {
		return varNameInParameters;
	}

	boolean isVarNameInParametersEquals(final String var_name) {
		return varNameInParameters.equals(var_name);
	}

	void checkOpenRessourceAsFile() throws IOException, InterruptedException {
		if (ressourceAsFile == false) {
			return;
		}
		final File file = new File(ressource);
		if (file.isDirectory()) {
			return;
		}

		for (int pos = 0; pos < TRY_COUNT_TO_OPEN_FILE; ++pos) {
			if (file.canRead()) {
				try (SeekableByteChannel sbc = Files.newByteChannel(file.toPath(), openOptionsReadOnly)) {
					log.debug("Successfully open file \"" + file + "\" for check access");
					return;
				} catch (final IOException e) {
					if (pos + 1 == TRY_COUNT_TO_OPEN_FILE) {
						throw e;
					}
					Thread.sleep(10 + 100 * pos);
				}
			} else {
				if (pos + 1 == TRY_COUNT_TO_OPEN_FILE) {
					throw new IOException("Can't read file \"" + file + "\" for check access");
				}
				Thread.sleep(10 + 100 * pos);
			}
		}
	}

	/**
	 * @return getRessource()
	 */
	@Override
	public String toString() {
		return getRessource();
	}
}
