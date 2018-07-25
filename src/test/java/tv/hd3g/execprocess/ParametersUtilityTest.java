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
package tv.hd3g.execprocess;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import junit.framework.TestCase;

public class ParametersUtilityTest extends TestCase {
	
	public void testParams() {
		String test_chaotic_line = "-aa 1  -single --cc 3 -U  \"dsfdsf sdf s  -e foo\" -g 2 42 -f=f -h=i;j,k:l -m Ah! -l \"u \" m ";
		
		ParametersUtility pu = new ParametersUtility(test_chaotic_line);
		
		assertEquals("-", pu.getParametersKeysStartsWith());
		assertFalse(pu.getParameters().isEmpty());
		assertEquals(pu.parameters, pu.getParameters());
		
		String actual = pu.toString();
		pu = new ParametersUtility();
		pu.addBulkParameters(test_chaotic_line);
		assertEquals(actual, pu.toString());
		
		List<String> compare = Arrays.asList("-aa", "1", "-single", "--cc", "3", "-U", "dsfdsf sdf s  -e foo", "-g", "2", "42", "-f=f", "-h=i;j,k:l", "-m", "Ah!", "-l", "u ", "m");
		
		assertTrue(Arrays.equals(compare.toArray(), pu.getParameters().toArray()));
		assertEquals(1, pu.getValues("-l").size());
		assertEquals("u ", pu.getValues("-l").get(0));
		
		assertEquals(1, pu.getValues("--cc").size());
		assertEquals("3", pu.getValues("--cc").get(0));
		
		assertEquals(0, pu.getValues("-single").size());
		assertEquals(0, pu.getValues("-h=i;j,k:l").size());
		assertNull(pu.getValues("-NOPE"));
		
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-a", "2", "-a", "3").toArray(), new ParametersUtility("-a 1 -a 2 -a 3").getParameters().toArray()));
		
		assertTrue(Arrays.equals(Arrays.asList("1", "2", "3").toArray(), new ParametersUtility("-a 1 -a 2 -a 3").getValues("-a").toArray()));
		assertTrue(new ParametersUtility("-a 1 -a 2 -b -a 3").getValues("-b").isEmpty());
		
		pu = new ParametersUtility("-a 1 -c 4 -a 2 -a 3 -b");
		assertTrue(pu.removeParameter("-a", 0));
		assertTrue(Arrays.equals(Arrays.asList("-c", "4", "-a", "2", "-a", "3", "-b").toArray(), pu.getParameters().toArray()));
		
		pu = new ParametersUtility("-a 1 -c 4 -a 2 -a 3 -b");
		assertTrue(pu.removeParameter("-a", 1));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-c", "4", "-a", "3", "-b").toArray(), pu.getParameters().toArray()));
		assertFalse(pu.removeParameter("-a", 2));
		assertFalse(pu.removeParameter("-N", 0));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-c", "4", "-a", "3", "-b").toArray(), pu.getParameters().toArray()));
		assertTrue(pu.removeParameter("-a", 0));
		assertTrue(pu.removeParameter("-a", 0));
		assertTrue(Arrays.equals(Arrays.asList("-c", "4", "-b").toArray(), pu.getParameters().toArray()));
		assertTrue(pu.removeParameter("-b", 0));
		assertTrue(Arrays.equals(Arrays.asList("-c", "4").toArray(), pu.getParameters().toArray()));
		
		pu = new ParametersUtility("-a -b -c -d");
		assertTrue(pu.removeParameter("-a", 0));
		assertTrue(Arrays.equals(Arrays.asList("-b", "-c", "-d").toArray(), pu.getParameters().toArray()));
		assertTrue(pu.removeParameter("-c", 0));
		assertTrue(Arrays.equals(Arrays.asList("-b", "-d").toArray(), pu.getParameters().toArray()));
		
		pu = new ParametersUtility("-a 1 -c 4 -a 2 -a 3 -b");
		assertTrue(pu.alterParameter("-a", "Z2", 1));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-c", "4", "-a", "Z2", "-a", "3", "-b").toArray(), pu.getParameters().toArray()));
		assertFalse(pu.alterParameter("-a", "Z2", 3));
		assertFalse(pu.alterParameter("-N", "Z2", 0));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-c", "4", "-a", "Z2", "-a", "3", "-b").toArray(), pu.getParameters().toArray()));
		
		pu = new ParametersUtility("-a -b");
		assertTrue(pu.alterParameter("-a", "1", 0));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-b").toArray(), pu.getParameters().toArray()));
		assertTrue(pu.alterParameter("-b", "2", 0));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-b", "2").toArray(), pu.getParameters().toArray()));
		
		pu = new ParametersUtility("-a -a -a");
		assertTrue(pu.alterParameter("-a", "1", 0));
		assertTrue(pu.alterParameter("-a", "2", 1));
		assertTrue(pu.alterParameter("-a", "3", 2));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-a", "2", "-a", "3").toArray(), pu.getParameters().toArray()));
		
		pu.clear();
		assertTrue(pu.getParameters().isEmpty());
		
		pu.addParameters("a", "b", null, "c");
		pu.addParameters("d");
		assertEquals("abcd", pu.parameters.stream().collect(Collectors.joining()));
		
		pu.clear().addParameters(Arrays.asList("a", "b", null, "c"));
		assertEquals("abc", pu.parameters.stream().collect(Collectors.joining()));
		
		pu.clear().addParameters("a", "b", "c", "d", null, "e");
		assertEquals("abcde", pu.parameters.stream().collect(Collectors.joining()));
		
		pu.clear().addParameters("a b  c d", "e", null, "f");
		assertEquals(3, pu.parameters.size());
		assertEquals("a b  c def", pu.parameters.stream().collect(Collectors.joining()));
		
		pu.addParameters("ggg h i ", " e", null, "fff", " ");
		assertEquals(3 + 4, pu.parameters.size());
		assertEquals("a b  c defggg h i  efff ", pu.parameters.stream().collect(Collectors.joining()));
	}
	
	public void testParamStyleChange() {
		ParametersUtility pu = new ParametersUtility("-a 1 /b 2").setParametersKeysStartsWith("/");
		assertEquals("/", pu.getParametersKeysStartsWith());
		
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "/b", "2").toArray(), pu.getParameters().toArray()));
		assertNull(pu.getValues("-a"));
		assertNotNull(pu.getValues("/b"));
		assertEquals(1, pu.getValues("/b").size());
		assertEquals("2", pu.getValues("/b").get(0));
		assertTrue(pu.alterParameter("/b", "Z", 0));
		assertEquals("Z", pu.getValues("/b").get(0));
		
	}
	
	public void testTransfert() {
		ParametersUtility pu1 = new ParametersUtility("!ok1").setParametersKeysStartsWith("!");
		ParametersUtility pu2 = new ParametersUtility("-ok2");
		
		pu1.transfertThisConfigurationTo(pu2);
		assertEquals("!", pu2.getParametersKeysStartsWith());
		assertFalse(pu1.toString().equals(pu2.toString()));
		
		pu1.setParametersKeysStartsWith("$");
		assertEquals("!", pu2.getParametersKeysStartsWith());
		
		pu2.importParametersFrom(pu1);
		assertTrue(pu1.toString().equals(pu2.toString()));
		assertEquals("$", pu2.getParametersKeysStartsWith());
	}
	
	public void testPrepend() {
		ParametersUtility pu = new ParametersUtility("-3 -4");
		pu.prependBulkParameters("-1 -2");
		
		assertEquals(4, pu.getParameters().size());
		assertEquals("-1 -2 -3 -4", pu.toString());
		
		pu.clear();
		pu.prependParameters("-3", "-4");
		pu.prependParameters("-1", "-2");
		assertEquals(4, pu.getParameters().size());
		assertEquals("-1 -2 -3 -4", pu.toString());
		
		pu.clear();
		pu.prependParameters(Arrays.asList("-3", "-4"));
		pu.prependParameters(Arrays.asList("-1", "-2"));
		assertEquals(4, pu.getParameters().size());
		assertEquals("-1 -2 -3 -4", pu.toString());
	}
}
