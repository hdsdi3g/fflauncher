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
 * Copyright (C) hdsdi3g for hd3g.tv 2020
 *
 */
package tv.hd3g.fflauncher.filtering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class FilterTest {

	Filter f;

	@Test
	void testFilter_WO_parsing() {
		f = new Filter("foobar", List.of(new FilterArgument("a", "b"), new FilterArgument("c")));
		assertEquals("foobar=a=b:c", f.toString());
	}

	@Test
	void testFilter_varargs() {
		f = new Filter("foobar", new FilterArgument("a", "b"), new FilterArgument("c"));
		assertEquals("foobar=a=b:c", f.toString());
	}

	@Test
	void testFilter_in_out() {
		f = new Filter("[i]overlay=w:h[o]");

		assertEquals("overlay", f.getFilterName());
		assertEquals(List.of("i"), f.getSourceBlocks());
		assertEquals(List.of("o"), f.getDestBlocks());

		assertEquals(2, f.getArguments().size());
		assertEquals("w", f.getArguments().get(0).getKey());
		assertEquals("h", f.getArguments().get(1).getKey());
	}

	@Test
	void testFilter_in_out_multiple() {
		f = new Filter("[y][d]overlay=w:h[out][out2]");

		assertEquals("overlay", f.getFilterName());

		assertEquals(List.of("y", "d"), f.getSourceBlocks());
		assertEquals(List.of("out", "out2"), f.getDestBlocks());

		assertEquals(2, f.getArguments().size());
		assertEquals("w", f.getArguments().get(0).getKey());
		assertEquals("h", f.getArguments().get(1).getKey());
	}

	@Test
	void testFilter_in() {
		f = new Filter("[y][d]overlay=w:h");

		assertEquals("overlay", f.getFilterName());
		assertEquals(List.of("y", "d"), f.getSourceBlocks());
		assertEquals(List.of(), f.getDestBlocks());

		assertEquals(2, f.getArguments().size());
		assertEquals("w", f.getArguments().get(0).getKey());
		assertEquals("h", f.getArguments().get(1).getKey());
	}

	@Test
	void testFilter_out() {
		f = new Filter("overlay=w:h[out][out2]");

		assertEquals("overlay", f.getFilterName());
		assertEquals(List.of(), f.getSourceBlocks());
		assertEquals(List.of("out", "out2"), f.getDestBlocks());

		assertEquals(2, f.getArguments().size());
		assertEquals("w", f.getArguments().get(0).getKey());
		assertEquals("h", f.getArguments().get(1).getKey());
	}

	@Test
	void testFilter_escape() {
		f = new Filter("[i]ove\\[rla\\]y=\\[w\\]:h[o]");

		assertEquals("ove\\[rla\\]y", f.getFilterName());
		assertEquals(List.of("i"), f.getSourceBlocks());
		assertEquals(List.of("o"), f.getDestBlocks());

		assertEquals(2, f.getArguments().size());
		assertEquals("\\[w\\]", f.getArguments().get(0).getKey());
		assertEquals("h", f.getArguments().get(1).getKey());
	}

	@Test
	void testFilter_quoted() {
		f = new Filter("[i]overlay=\'w[:]o[h]\'[o]");

		assertEquals("overlay", f.getFilterName());
		assertEquals(List.of("i"), f.getSourceBlocks());
		assertEquals(List.of("o"), f.getDestBlocks());

		assertEquals(1, f.getArguments().size());
		assertEquals("'w[:]o[h]'", f.getArguments().get(0).getKey());
	}

	@Test
	void testFilter_noIO() {
		f = new Filter("overlay=w:h");

		assertEquals("overlay", f.getFilterName());
		assertEquals(List.of(), f.getSourceBlocks());
		assertEquals(List.of(), f.getDestBlocks());

		assertEquals(2, f.getArguments().size());
		assertEquals("w", f.getArguments().get(0).getKey());
		assertEquals("h", f.getArguments().get(1).getKey());
	}

	@Test
	void testFilter_noIO_2param() {
		f = new Filter("vstack=inputs=2");

		assertEquals("vstack", f.getFilterName());
		assertEquals(List.of(), f.getSourceBlocks());
		assertEquals(List.of(), f.getDestBlocks());

		assertEquals(1, f.getArguments().size());
		assertEquals("inputs", f.getArguments().get(0).getKey());
		assertEquals("2", f.getArguments().get(0).getValue());
	}

	@Test
	void testHashCode() {
		assertEquals(new Filter("[i]overlay=\'w[:]o[h]\'[o]").hashCode(),
		        new Filter("[i]overlay=\'w[:]o[h]\'[o]").hashCode());
	}

	@Test
	void testEquals() {
		assertEquals(new Filter("[i]overlay=\'w[:]o[h]\'[o]"),
		        new Filter("[i]overlay=\'w[:]o[h]\'[o]"));
	}

	@Test
	void testToString() {
		assertEquals("[y][d]overlay=w:h[out][out2]", new Filter("[y][d] overlay=w:h [out] [out2]").toString());
	}

	@Test
	void testFilter_rejectChains() {
		assertThrows(IllegalArgumentException.class, () -> new Filter("[i]overlay=w:h[o];ddd"));
		assertThrows(IllegalArgumentException.class, () -> new Filter("[i]overlay=w:h[o],ddd"));
	}

}
