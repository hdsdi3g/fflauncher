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
package tv.hd3g.fflauncher.exec;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import tv.hd3g.fflauncher.exec.processdemo.Test1;
import tv.hd3g.fflauncher.exec.processdemo.Test2;
import tv.hd3g.fflauncher.exec.processdemo.Test3;

public class ExecProcessTest extends TestCase {
	
	static ThreadFactory createTF() {
		return r -> {
			Thread t = new Thread(r, "JUnit test");
			t.setDaemon(true);
			return t;
		};
	}
	
	static final File java_exec = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
	
	static ExecProcessText createExec(Class<?> exec_class) {
		try {
			if (java_exec.exists() == false) {// TODO2 replace this by get ExecFinder
				return (ExecProcessText) new ExecProcessText(new File(java_exec.getAbsolutePath() + ".exe")).addParams("-cp", System.getProperty("java.class.path")).addParams(exec_class.getName());
			}
			return (ExecProcessText) new ExecProcessText(java_exec).addParams("-cp", System.getProperty("java.class.path")).addParams(exec_class.getName());
		} catch (IOException e) {
			throw new RuntimeException("Can't found java exec", e);
		}
	}
	
	public void testSimpleExec() {
		ExecProcessText ept = createExec(Test1.class);
		ExecProcessTextResult result = ept.start(createTF()).waitForEnd();
		
		assertEquals(Test1.expected, result.getStdouterr(true, ""));
		assertEquals(0, result.getExitCode());
		assertEquals(EndStatus.CORRECTLY_DONE, result.getEndStatus());
	}
	
	public void testWorkingDirectory() throws IOException {
		ExecProcessText ept = createExec(Test2.class);
		File wd = new File(System.getProperty("user.dir")).getCanonicalFile();
		ept.setWorkingDirectory(wd);
		
		assertEquals(wd, ept.getWorkingDirectory());
		
		ExecProcessTextResult result = ept.start(createTF()).waitForEnd();
		assertEquals(wd, result.getWorkingDirectory());
		
		assertEquals(wd.getPath(), result.getStdouterr(true, ""));
		assertEquals(EndStatus.CORRECTLY_DONE, result.getEndStatus());
	}
	
	public void testEndExecutionCallback() {
		ExecProcessText ept = createExec(Test1.class);
		
		AtomicReference<ExecProcessResult> expected_result = new AtomicReference<>();
		ept.addEndExecutionCallback(r -> {
			expected_result.set(r);
		}, t -> {
			new Thread(t).start();
		});
		
		ExecProcessTextResult result = ept.start(createTF());
		
		assertTrue(expected_result.get() == null);
		
		result.waitForEnd();
		
		while (expected_result.get() == null) {
			Thread.onSpinWait();
		}
		
		assertEquals(result, expected_result.get());
	}
	
	public void testResultValues() throws InterruptedException {
		long start_date = System.currentTimeMillis() - 1;
		
		ExecProcessText ept = createExec(Test3.class);
		ept.setExecCodeMustBeZero(false);
		ept.addParams(Test3.expected_in);
		assertEquals(Test3.expected_in, ept.getParams().get(ept.getParams().size() - 1));
		
		if (System.getenv().containsKey("PATH")) {
			ept.transfertSystemEnvironment();
		} else {
			/**
			 * No importance
			 */
			ept.getEnvironment().put("PATH", "/bin");
		}
		ept.getEnvironment().put(Test3.ENV_KEY, Test3.ENV_VALUE);
		
		ExecProcessTextResult result = ept.start(createTF()).waitForEnd();
		
		assertEquals(Test3.expected_out, result.getStdout(false, ""));
		assertEquals(Test3.expected_err, result.getStderr(false, ""));
		assertEquals(Test3.exit_ok, result.getExitCode());
		assertEquals(EndStatus.CORRECTLY_DONE, result.getEndStatus());
		
		assertTrue(result.getCommandline().endsWith(Test3.expected_in));
		assertEquals(ept.executable, result.getExecutable());
		assertTrue(result.getPID() > 0);
		assertTrue(result.getUserExec().endsWith(System.getProperty("user.name")));
		
		assertEquals(Test3.exit_ok, result.getProcess().exitValue());
		assertEquals(result.getPID(), result.getProcess().pid());
		assertFalse(result.getProcess().isAlive());
		
		assertTrue(result.getStartDate() > start_date);
		assertTrue(result.getStartDate() < System.currentTimeMillis());
		assertTrue(result.getEnvironment().getOrDefault(Test3.ENV_KEY, "").equals(Test3.ENV_VALUE));
		
	}
	
	// XXX tests !
	
	// ept.addStdOutErrObserver(stdouterr_observer, executor)
	// ept.alterProcessBuilderBeforeStartIt(alter_process_builder)
	// ept.getCaptureStreamsBehavior()
	// ept.getMaxExecTime(unit)
	// ept.isKeepStderr()
	// ept.isKeepStdout()
	// ept.makeProcessBuilder()
	// ept.setInteractive_handler(interactive_handler, executor)
	// ept.start(executor)
	
	// result.getCPUDuration(unit)
	// result.getMaxExecTime(unit)
	// result.getStderr(keep_empty_lines, new_line_separator)
	// result.getStderrLines(keep_empty_lines)
	// result.getStdInInjection()
	// result.getUptime(unit)
	// result.kill() + sub process...
	// result.waitForEnd(executor)
	// result.waitForEnd(timeout, unit)
}
