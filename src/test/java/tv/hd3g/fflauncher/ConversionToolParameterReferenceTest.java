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
import java.util.Arrays;
import java.util.Collections;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ConversionToolParameterReferenceTest extends TestCase {

	private final File tempFile;

	public ConversionToolParameterReferenceTest() throws IOException {
		tempFile = File.createTempFile("bintest", ".txt");
	}

	private ConversionToolParameterReference ctprS;
	private ConversionToolParameterReference ctprF;

	@Override
	protected void setUp() throws Exception {
		ctprS = new ConversionToolParameterReference("reference", "var", Arrays.asList("before1", "before2"), Arrays.asList("after1", "after2"));
		ctprF = new ConversionToolParameterReference(tempFile, "var", Arrays.asList("before1", "before2"), Arrays.asList("after1", "after2"));
	}

	public void testNullConstructor() {
		final ConversionToolParameterReference ctprS = new ConversionToolParameterReference("reference", "var", null, null);
		final ConversionToolParameterReference ctprF = new ConversionToolParameterReference(tempFile, "var", null, null);

		Assert.assertEquals(Collections.emptyList(), ctprS.getParametersListBeforeRef());
		Assert.assertEquals(Collections.emptyList(), ctprS.getParametersListAfterRef());
		Assert.assertEquals(Collections.emptyList(), ctprF.getParametersListBeforeRef());
		Assert.assertEquals(Collections.emptyList(), ctprF.getParametersListAfterRef());
	}

	public void testGetRessource() {
		Assert.assertEquals("reference", ctprS.getRessource());
		Assert.assertEquals(tempFile.getPath(), ctprF.getRessource());
	}

	public void testGetParametersListAfterRef() {
		Assert.assertEquals(Arrays.asList("after1", "after2"), ctprS.getParametersListAfterRef());
	}

	public void testGetParametersListBeforeRef() {
		Assert.assertEquals(Arrays.asList("before1", "before2"), ctprS.getParametersListBeforeRef());
	}

	public void testGetVarNameInParameters() {
		Assert.assertEquals("var", ctprS.getVarNameInParameters());
	}

	public void testIsVarNameInParametersEquals() {
		Assert.assertTrue(ctprS.isVarNameInParametersEquals("var"));
	}

	public void testToString() {
		Assert.assertEquals(ctprS.getRessource(), ctprS.toString());
		Assert.assertEquals(ctprF.getRessource(), ctprF.toString());
	}

	public void testCheckOpenRessourceAsFile() {
		try {
			ctprS.checkOpenRessourceAsFile();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Unexpected exception from here", e);
		}

		try {
			ctprF.checkOpenRessourceAsFile();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Unexpected exception from here", e);
		}

		try {
			new ConversionToolParameterReference(new File("nope"), "var", null, null).checkOpenRessourceAsFile();
			Assert.fail("Expected exception from here: file not exists");
		} catch (IOException | InterruptedException e) {
		}
	}

}
