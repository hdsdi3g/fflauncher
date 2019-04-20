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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.cmdline.Parameters;

public class ConversionToolTest extends TestCase {

	public void test() throws IOException {
		final ConversionTool ct = new ConversionTool("java");
		ct.getInternalParameters().addParameters("-firstparam");

		assertFalse(ct.isRemoveParamsIfNoVarToInject());
		ct.setRemoveParamsIfNoVarToInject(true);
		assertTrue(ct.isRemoveParamsIfNoVarToInject());
		ct.setRemoveParamsIfNoVarToInject(false);
		assertFalse(ct.isRemoveParamsIfNoVarToInject());

		assertNotNull(ct.getExecutableName());

		Assert.assertNull(ct.getMaxExecTimeScheduler());
		final ScheduledThreadPoolExecutor max_exec_time_scheduler = new ScheduledThreadPoolExecutor(1);
		ct.setMaxExecTimeScheduler(max_exec_time_scheduler);
		assertEquals(max_exec_time_scheduler, ct.getMaxExecTimeScheduler());

		assertEquals(5, ct.getMaxExecTime(TimeUnit.SECONDS));
		ct.setMaxExecutionTimeForShortCommands(1, TimeUnit.SECONDS);
		assertEquals(1, ct.getMaxExecTime(TimeUnit.SECONDS));

		final File working_directory = new File(".");
		assertNull(ct.getWorkingDirectory());
		ct.setWorkingDirectory(working_directory);
		assertEquals(working_directory, ct.getWorkingDirectory());

		final Parameters p = ct.getInternalParameters();
		assertNotNull(p);
		assertNotNull(ct.getReadyToRunParameters());

		assertEquals("-firstparam", p.getParameters().stream().findFirst().get());

		p.addParameters("<%varsource1%>");
		ct.addInputSource("source1", "varsource1", "-pre1-source1", "-pre2-source1");

		assertEquals(2, p.getParameters().size());

		p.addParameters("<%varsource2%>");
		ct.addInputSource("source2", "varsource2", Arrays.asList("-pre1-source2", "-pre2-source2"), Arrays.asList("-post1-source2", "-post2-source2"));

		p.addParameters("<%vardest1%>");
		ct.addOutputDestination("dest1", "vardest1", "-pre1-dest1", "-pre2-dest1");

		p.addParameters("<%vardest2%>");
		ct.addOutputDestination("dest2", "vardest2", Arrays.asList("-pre1-dest2", "-pre2-dest2"), Arrays.asList("-post1-dest2", "-post2-dest2"));

		ct.addSimpleOutputDestination("dest-simple");

		assertEquals(6, p.getParameters().size());

		assertEquals("source1 source2", ct.getDeclaredSources().stream().collect(Collectors.joining(" ")));
		assertEquals("dest1 dest2 dest-simple", ct.getDeclaredDestinations().stream().collect(Collectors.joining(" ")));

		final String processed_cmdline = ct.getReadyToRunParameters().getParameters().stream().collect(Collectors.joining(" "));

		final String expected = "-firstparam -pre1-source1 -pre2-source1 source1 -pre1-source2 -pre2-source2 source2 -post1-source2 -post2-source2 -pre1-dest1 -pre2-dest1 dest1 -pre1-dest2 -pre2-dest2 dest2 -post1-dest2 -post2-dest2 dest-simple";
		assertEquals(expected, processed_cmdline);

		assertEquals("source1", ct.getDeclaredSourceByVarName("varsource1").orElse("nope"));
		assertEquals("dest2", ct.getDeclaredDestinationByVarName("vardest2").orElse("nope"));
	}

	public void testCatchMissingOutVar() {
		final LinkedHashMap<String, String> catchs = new LinkedHashMap<>();

		class CT extends ConversionTool {

			public CT() {
				super("java");
			}

			@Override
			protected void onMissingInputOutputVar(final String var_name, final String ressource) {
				synchronized (catchs) {
					catchs.put(var_name, ressource);
				}
			}
		}

		final CT ct = new CT();
		final Parameters p = ct.getInternalParameters();
		p.addParameters("-1", "<%not_found_var%>", "-2", "<%found_var%>");
		ct.addInputSource("source", "found_var");
		assertEquals("-1 -2 source", ct.getReadyToRunParameters().toString());

		ct.setRemoveParamsIfNoVarToInject(true);
		assertEquals("-2 source", ct.getReadyToRunParameters().toString());

		assertTrue(catchs.isEmpty());
		p.clear();
		assertEquals("", ct.getReadyToRunParameters().toString());
		assertEquals(1, catchs.size());
		assertTrue(catchs.containsKey("found_var"));
		assertEquals("source", catchs.get("found_var"));
	}

	public void testManageOutFiles() throws IOException {
		final File f1 = File.createTempFile("test", ".txt");
		final File d1 = new File(f1.getParent() + File.separator + "sub1-" + f1.getName() + File.separator + "sub2");
		assertTrue(d1.mkdirs());
		final File f2 = File.createTempFile("test", ".txt", d1.getParentFile());
		final File f3 = File.createTempFile("test", ".txt", d1);

		assertTrue(f1.exists());
		assertTrue(f2.exists());
		assertTrue(f3.exists());
		assertTrue(d1.exists());
		assertTrue(d1.getParentFile().exists());

		final ConversionTool ct = new ConversionTool("java");
		ct.getInternalParameters().addParameters("-firstparam");
		ct.addSimpleOutputDestination("nothing");
		ct.addSimpleOutputDestination(f1.getAbsolutePath());
		ct.addSimpleOutputDestination(f2.toURI().toURL().toString());
		ct.addSimpleOutputDestination(f3.getAbsolutePath());
		ct.addSimpleOutputDestination(d1.getAbsolutePath());
		ct.addSimpleOutputDestination("http://not.this/");

		List<File> founded = ct.getOutputFiles(false, false, false);

		assertEquals(5, founded.size());
		assertEquals("nothing", founded.get(0).getPath());
		assertEquals(f1, founded.get(1));
		assertEquals(f2, founded.get(2));
		assertEquals(f3, founded.get(3));
		assertEquals(d1, founded.get(4));

		founded = ct.getOutputFiles(true, false, false);
		assertEquals(4, founded.size());
		assertEquals(f1, founded.get(0));
		assertEquals(f2, founded.get(1));
		assertEquals(f3, founded.get(2));
		assertEquals(d1, founded.get(3));

		founded = ct.getOutputFiles(true, true, false);
		assertEquals(3, founded.size());
		assertEquals(f1, founded.get(0));
		assertEquals(f2, founded.get(1));
		assertEquals(f3, founded.get(2));

		founded = ct.getOutputFiles(true, false, true);
		assertEquals(1, founded.size());
		assertEquals(d1, founded.get(0));

		ct.cleanUpOutputFiles(false, true);

		assertFalse(f1.exists());
		assertFalse(f2.exists());
		assertFalse(f3.exists());
		assertFalse(d1.exists());
		assertTrue(d1.getParentFile().exists());
		assertTrue(d1.getParentFile().delete());
	}

}
