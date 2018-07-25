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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import junit.framework.TestCase;
import tv.hd3g.execprocess.CommandLineProcessor.CommandLine;
import tv.hd3g.execprocess.CommandLineProcessor.CommandLine.ProcessedCommandLine;

public class CommandLineProcessorTest extends TestCase {
	
	public void test() {
		CommandLineProcessor clp = new CommandLineProcessor();
		
		assertEquals("<%", clp.getStartVarTag());
		assertEquals("%>", clp.getEndVarTag());
		assertTrue(clp.isTaggedParameter("<%ok%>"));
		assertFalse(clp.isTaggedParameter("<%nope"));
		assertFalse(clp.isTaggedParameter("nope%>"));
		assertFalse(clp.isTaggedParameter("<nope>"));
		assertFalse(clp.isTaggedParameter("nope"));
		assertFalse(clp.isTaggedParameter("%>nope<%"));
		assertFalse(clp.isTaggedParameter("<%nope %>"));
		assertEquals("my_var", clp.extractVarNameFromTaggedParameter("<%my_var%>"));
		
		clp = new CommandLineProcessor("{", "}");
		assertEquals("{", clp.getStartVarTag());
		assertEquals("}", clp.getEndVarTag());
		assertTrue(clp.isTaggedParameter("{ok}"));
		assertFalse(clp.isTaggedParameter("{ok }"));
		assertFalse(clp.isTaggedParameter("{nope"));
		assertFalse(clp.isTaggedParameter("nope}"));
		assertFalse(clp.isTaggedParameter("nope"));
		assertFalse(clp.isTaggedParameter("}nope{"));
		assertEquals("my_var", clp.extractVarNameFromTaggedParameter("{my_var}"));
		
		clp = new CommandLineProcessor();
		assertNull(clp.extractVarNameFromTaggedParameter("<%%>"));
		assertNull(clp.extractVarNameFromTaggedParameter("<%"));
		assertNull(clp.extractVarNameFromTaggedParameter("%>"));
		assertNull(clp.extractVarNameFromTaggedParameter("nope"));
	}
	
	public void testInjectVar() {
		CommandLineProcessor clp = new CommandLineProcessor();
		CommandLine cmd = clp.createCommandLine("exec -a <%var1%> <%var2%> <%varNOPE%> -b");
		
		HashMap<String, String> vars = new HashMap<>();
		vars.put("var1", "value1");
		vars.put("var2", "value2");
		ProcessedCommandLine pcl = cmd.process(vars, false);
		assertEquals("exec", pcl.getExecName());
		assertTrue(Arrays.equals(Arrays.asList("-a", "value1", "value2", "-b").toArray(), pcl.getParameters().toArray()));
		
		assertTrue(Arrays.equals(Arrays.asList("-a").toArray(), clp.createCommandLine("exec -a <%varNOPE%>").process().getParameters().toArray()));
		assertTrue(Arrays.equals(Arrays.asList("-b").toArray(), clp.createCommandLine("exec -a <%varNOPE%> -b").process(new HashMap<>(), true).getParameters().toArray()));
	}
	
	public void testExecProcess() throws IOException {
		ProcessedCommandLine pcl = new CommandLineProcessor().createCommandLine("java -a 1 -b 2").process();
		
		ExecProcess ep1 = new ExecProcess(pcl, ExecProcessTest.executable_finder);
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-b", "2").toArray(), ep1.getParameters().toArray()));
		
		ExecProcessText ep2 = new ExecProcessText(pcl, ExecProcessTest.executable_finder);
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-b", "2").toArray(), ep2.getParameters().toArray()));
	}
	
}
