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
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
 */
package tv.hd3g.fflauncher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConversionToolParameterReferenceTest {

	final File tempFile;

	ConversionToolParameterReferenceTest() throws IOException {
		tempFile = File.createTempFile("bintest", ".txt");
	}

	ConversionToolParameterReference ctprS;
	ConversionToolParameterReference ctprF;

	@BeforeEach
	void setUp() throws Exception {
		ctprS = new ConversionToolParameterReference("reference", "var", Arrays.asList("before1", "before2"), Arrays
		        .asList("after1", "after2"));
		ctprF = new ConversionToolParameterReference(tempFile, "var", Arrays.asList("before1", "before2"), Arrays
		        .asList("after1", "after2"));
	}

	@Test
	void testNullConstructor() {
		final ConversionToolParameterReference ctprS = new ConversionToolParameterReference("reference", "var", null,
		        null);
		final ConversionToolParameterReference ctprF = new ConversionToolParameterReference(tempFile, "var", null,
		        null);

		assertEquals(Collections.emptyList(), ctprS.getParametersListBeforeRef());
		assertEquals(Collections.emptyList(), ctprS.getParametersListAfterRef());
		assertEquals(Collections.emptyList(), ctprF.getParametersListBeforeRef());
		assertEquals(Collections.emptyList(), ctprF.getParametersListAfterRef());
	}

	@Test
	void testGetRessource() {
		assertEquals("reference", ctprS.getRessource());
		assertEquals(tempFile.getPath(), ctprF.getRessource());
	}

	@Test
	void testGetParametersListAfterRef() {
		assertEquals(Arrays.asList("after1", "after2"), ctprS.getParametersListAfterRef());
	}

	@Test
	void testGetParametersListBeforeRef() {
		assertEquals(Arrays.asList("before1", "before2"), ctprS.getParametersListBeforeRef());
	}

	@Test
	void testGetVarNameInParameters() {
		assertEquals("var", ctprS.getVarNameInParameters());
	}

	@Test
	void testIsVarNameInParametersEquals() {
		assertTrue(ctprS.isVarNameInParametersEquals("var"));
	}

	@Test
	void testToString() {
		assertEquals(ctprS.getRessource(), ctprS.toString());
		assertEquals(ctprF.getRessource(), ctprF.toString());
	}

	@Test
	void testCheckOpenRessourceAsFile() throws IOException, InterruptedException {
		ctprS.checkOpenRessourceAsFile();
		ctprF.checkOpenRessourceAsFile();
		assertThrows(IOException.class, () -> {
			new ConversionToolParameterReference(new File("nope"), "var", null, null).checkOpenRessourceAsFile();
		}, "Expected exception from here: file not exists");

	}

}
