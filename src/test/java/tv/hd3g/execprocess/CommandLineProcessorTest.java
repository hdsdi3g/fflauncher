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
import java.util.List;

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
		
		String full_command_line_with_vars = "\"aaa bbb\" -aa 1  -single --cc 3 -U  \"dsfdsf sdf s  -e foo\" -g 2 42 -f=f -h=i;j,k:l -m Ah! -l \"u \" m ";
		CommandLine cmd = clp.createCommandLine(full_command_line_with_vars);
		assertNotNull(cmd);
		
		assertEquals("aaa bbb", cmd.getExecName());
		assertEquals("-", cmd.getParamKeysStartsWith());
		
		ProcessedCommandLine pcl = cmd.process();
		assertNotNull(pcl.getParameters());
		
		List<String> compare = Arrays.asList("-aa", "1", "-single", "--cc", "3", "-U", "dsfdsf sdf s  -e foo", "-g", "2", "42", "-f=f", "-h=i;j,k:l", "-m", "Ah!", "-l", "u ", "m");
		
		assertTrue(Arrays.equals(compare.toArray(), pcl.getParameters().toArray()));
		assertEquals(1, pcl.getValues("-l").size());
		assertEquals("u ", pcl.getValues("-l").get(0));
		
		assertEquals(1, pcl.getValues("--cc").size());
		assertEquals("3", pcl.getValues("--cc").get(0));
		
		assertEquals(0, pcl.getValues("-single").size());
		assertEquals(0, pcl.getValues("-h=i;j,k:l").size());
		assertNull(pcl.getValues("-NOPE"));
		
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-a", "2", "-a", "3").toArray(), clp.createCommandLine("cmd -a 1 -a 2 -a 3").process().getParameters().toArray()));
		
		assertTrue(Arrays.equals(Arrays.asList("1", "2", "3").toArray(), clp.createCommandLine("cmd -a 1 -a 2 -a 3").process().getValues("-a").toArray()));
		assertTrue(clp.createCommandLine("cmd -a 1 -a 2 -b -a 3").process().getValues("-b").isEmpty());
		
		pcl = clp.createCommandLine("cmd -a 1 -c 4 -a 2 -a 3 -b").process();
		assertTrue(pcl.removeParameter("-a", 0));
		assertTrue(Arrays.equals(Arrays.asList("-c", "4", "-a", "2", "-a", "3", "-b").toArray(), pcl.getParameters().toArray()));
		
		pcl = clp.createCommandLine("cmd -a 1 -c 4 -a 2 -a 3 -b").process();
		assertTrue(pcl.removeParameter("-a", 1));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-c", "4", "-a", "3", "-b").toArray(), pcl.getParameters().toArray()));
		assertFalse(pcl.removeParameter("-a", 2));
		assertFalse(pcl.removeParameter("-N", 0));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-c", "4", "-a", "3", "-b").toArray(), pcl.getParameters().toArray()));
		assertTrue(pcl.removeParameter("-a", 0));
		assertTrue(pcl.removeParameter("-a", 0));
		assertTrue(Arrays.equals(Arrays.asList("-c", "4", "-b").toArray(), pcl.getParameters().toArray()));
		assertTrue(pcl.removeParameter("-b", 0));
		assertTrue(Arrays.equals(Arrays.asList("-c", "4").toArray(), pcl.getParameters().toArray()));
		
		pcl = clp.createCommandLine("cmd -a -b -c -d").process();
		assertTrue(pcl.removeParameter("-a", 0));
		assertTrue(Arrays.equals(Arrays.asList("-b", "-c", "-d").toArray(), pcl.getParameters().toArray()));
		assertTrue(pcl.removeParameter("-c", 0));
		assertTrue(Arrays.equals(Arrays.asList("-b", "-d").toArray(), pcl.getParameters().toArray()));
		
		pcl = clp.createCommandLine("cmd -a 1 -c 4 -a 2 -a 3 -b").process();
		assertTrue(pcl.alterParameter("-a", "Z2", 1));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-c", "4", "-a", "Z2", "-a", "3", "-b").toArray(), pcl.getParameters().toArray()));
		assertFalse(pcl.alterParameter("-a", "Z2", 3));
		assertFalse(pcl.alterParameter("-N", "Z2", 0));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-c", "4", "-a", "Z2", "-a", "3", "-b").toArray(), pcl.getParameters().toArray()));
		
		pcl = clp.createCommandLine("cmd -a -b").process();
		assertTrue(pcl.alterParameter("-a", "1", 0));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-b").toArray(), pcl.getParameters().toArray()));
		assertTrue(pcl.alterParameter("-b", "2", 0));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-b", "2").toArray(), pcl.getParameters().toArray()));
		
		pcl = clp.createCommandLine("cmd -a -a -a").process();
		assertTrue(pcl.alterParameter("-a", "1", 0));
		assertTrue(pcl.alterParameter("-a", "2", 1));
		assertTrue(pcl.alterParameter("-a", "3", 2));
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-a", "2", "-a", "3").toArray(), pcl.getParameters().toArray()));
		
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
	
	public void testParamStyleChange() {
		CommandLineProcessor clp = new CommandLineProcessor();
		CommandLine cmd = clp.createCommandLine("exec -a 1 /b 2");
		
		ProcessedCommandLine pcl = cmd.setParamKeysStartsWith("/").process();
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "/b", "2").toArray(), pcl.getParameters().toArray()));
		assertNull(pcl.getValues("-a"));
		assertNotNull(pcl.getValues("/b"));
		assertEquals(1, pcl.getValues("/b").size());
		assertEquals("2", pcl.getValues("/b").get(0));
		assertTrue(pcl.alterParameter("/b", "Z", 0));
		assertEquals("Z", pcl.getValues("/b").get(0));
		
	}
	
	public void testExecProcess() throws IOException {
		ProcessedCommandLine pcl = new CommandLineProcessor().createCommandLine("java -a 1 -b 2").process();
		
		ExecProcess ep1 = new ExecProcess(pcl, ExecProcessTest.executable_finder);
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-b", "2").toArray(), ep1.getParams().toArray()));
		
		ExecProcessText ep2 = new ExecProcessText(pcl, ExecProcessTest.executable_finder);
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-b", "2").toArray(), ep2.getParams().toArray()));
	}
	
}
