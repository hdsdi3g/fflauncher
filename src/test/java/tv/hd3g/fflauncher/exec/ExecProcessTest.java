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

import junit.framework.TestCase;
import tv.hd3g.fflauncher.exec.processdemo.Test1;
import tv.hd3g.fflauncher.exec.processdemo.Test2;

public class ExecProcessTest extends TestCase {
	
	static ThreadFactory createTF() {
		return new ThreadFactory() {
			
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "JUnit test");
				t.setDaemon(true);
				return t;
			}
		};
	}
	
	static final File java_exec = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
	
	static ExecProcessText createExec(Class<?> exec_class) {
		try {
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
	
	// XXX tests !
	
	// ept.addEndExecutionCallback(onEnd, executor)
	// ept.addParams(params)
	// ept.addStdOutErrObserver(stdouterr_observer, executor)
	// ept.alterProcessBuilderBeforeStartIt(alter_process_builder)
	// ept.getCaptureStreamsBehavior()
	// ept.getEnvironment();
	// ept.getMaxExecTime(unit)
	// ept.getParams()
	// ept.isExecCodeMustBeZero()
	// ept.isKeepStderr()
	// ept.isKeepStdout()
	// ept.makeProcessBuilder()
	// ept.setInteractive_handler(interactive_handler, executor)
	// ept.start(executor)
	// ept.transfertSystemEnvironment()
	
	// result.getCommandline()
	// result.getCPUDuration(unit)
	// result.getEndStatus()
	// result.getEnvironment()
	// result.getExecutable()
	// result.getExitCode()
	// result.getMaxExecTime(unit)
	// result.getPID()
	// result.getProcess()
	// result.getStartDate()
	// result.getStderr(keep_empty_lines, new_line_separator)
	// result.getStderrLines(keep_empty_lines)
	// result.getStdInInjection()
	// result.getUptime(unit)
	// result.getUserExec()
	// result.isCorrectlyDone() ...
	// result.kill() + sub process...
	// result.waitForEnd(executor)
	// result.waitForEnd(timeout, unit)
	
}
