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
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import junit.framework.TestCase;
import tv.hd3g.fflauncher.exec.processdemo.Test1;
import tv.hd3g.fflauncher.exec.processdemo.Test2;
import tv.hd3g.fflauncher.exec.processdemo.Test3;
import tv.hd3g.fflauncher.exec.processdemo.Test4;
import tv.hd3g.fflauncher.exec.processdemo.Test5;
import tv.hd3g.fflauncher.exec.processdemo.Test6;
import tv.hd3g.fflauncher.exec.processdemo.Test7;

public class ExecProcessTest extends TestCase {
	
	public static ThreadFactory createTF() {
		return r -> {
			Thread t = new Thread(r, "JUnit test");
			t.setDaemon(true);
			return t;
		};
	}
	
	static final File java_exec = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
	
	public static ExecProcessText createExec(Class<?> exec_class) {
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
	
	public void testStdObserver() {
		ExecProcessText ept = createExec(Test4.class);
		
		ept.setKeepStdout(false);
		ept.setKeepStderr(false);
		
		assertFalse(ept.isKeepStdout());
		assertFalse(ept.isKeepStderr());
		assertEquals(CaptureOutStreamsBehavior.BOTH_STDOUT_STDERR, ept.getCaptureStreamsBehavior());
		
		LinkedBlockingQueue<String> catch_stdout = new LinkedBlockingQueue<>();
		LinkedBlockingQueue<String> catch_stderr = new LinkedBlockingQueue<>();
		LinkedBlockingQueue<ExecProcessTextResult> catch_source = new LinkedBlockingQueue<>();
		
		ept.addStdOutErrObserver((source, line, is_std_err) -> {
			if (is_std_err) {
				catch_stderr.add(line);
			} else {
				catch_stdout.add(line);
			}
			catch_source.add(source);
		}, r -> r.run());
		
		ExecProcessTextResult result = ept.start(createTF()).waitForEnd();
		
		assertEquals(1, catch_stdout.size());
		assertEquals(1, catch_stderr.size());
		assertEquals(2, catch_source.size());
		
		assertEquals(Test4.std_out, catch_stdout.poll());
		assertEquals(Test4.std_err, catch_stderr.poll());
		
		assertEquals(result, catch_source.poll());
		assertEquals(result, catch_source.poll());
		
		assertNotNull(result.getStdoutLines(true));
		assertNotNull(result.getStderrLines(true));
		assertNotNull(result.getStdouterrLines(true));
		
		assertEquals(0, result.getStdoutLines(true).count());
		assertEquals(0, result.getStderrLines(true).count());
		assertEquals(0, result.getStdouterrLines(true).count());
	}
	
	public void testNotCaptureStreams() {
		ExecProcessText ept = createExec(Test4.class);
		
		/**
		 * Only stdout
		 */
		ept.setCaptureOutStream(CaptureOutStreamsBehavior.ONLY_STDOUT);
		assertEquals(CaptureOutStreamsBehavior.ONLY_STDOUT, ept.getCaptureStreamsBehavior());
		
		LinkedBlockingQueue<String> catch_stdout = new LinkedBlockingQueue<>();
		LinkedBlockingQueue<String> catch_stderr = new LinkedBlockingQueue<>();
		
		ept.addStdOutErrObserver((source, line, is_std_err) -> {
			if (is_std_err) {
				catch_stderr.add(line);
			} else {
				catch_stdout.add(line);
			}
		}, r -> r.run());
		
		ExecProcessTextResult result = ept.start(createTF()).waitForEnd();
		
		assertEquals(1, catch_stdout.size());
		assertEquals(0, catch_stderr.size());
		
		assertNotNull(result.getStdoutLines(true));
		assertNotNull(result.getStderrLines(true));
		assertNotNull(result.getStdouterrLines(true));
		
		assertEquals(1, result.getStdoutLines(true).count());
		assertEquals(0, result.getStderrLines(true).count());
		assertEquals(1, result.getStdouterrLines(true).count());
		
		/**
		 * Only stderr
		 */
		ept.setCaptureOutStream(CaptureOutStreamsBehavior.ONLY_STDERR);
		assertEquals(CaptureOutStreamsBehavior.ONLY_STDERR, ept.getCaptureStreamsBehavior());
		
		catch_stdout.clear();
		catch_stderr.clear();
		
		result = ept.start(createTF()).waitForEnd();
		
		assertEquals(0, catch_stdout.size());
		assertEquals(1, catch_stderr.size());
		
		assertNotNull(result.getStdoutLines(true));
		assertNotNull(result.getStderrLines(true));
		assertNotNull(result.getStdouterrLines(true));
		
		assertEquals(0, result.getStdoutLines(true).count());
		assertEquals(1, result.getStderrLines(true).count());
		assertEquals(1, result.getStdouterrLines(true).count());
	}
	
	public void testMaxExecTime() {
		ExecProcess ept = createExec(Test5.class);
		
		ScheduledThreadPoolExecutor max_exec_time_scheduler = new ScheduledThreadPoolExecutor(1);
		ept.setMaxExecutionTime(Test5.MAX_DURATION, TimeUnit.MILLISECONDS, max_exec_time_scheduler);
		
		assertEquals(Test5.MAX_DURATION, ept.getMaxExecTime(TimeUnit.MILLISECONDS));
		
		long start_time = System.currentTimeMillis();
		ExecProcessResult result = ept.start(createTF()).waitForEnd();
		
		long duration = System.currentTimeMillis() - start_time;
		
		assertTrue(duration < Test5.MAX_DURATION + 300);/** 300 is a "startup time bonus" */
		assertEquals(EndStatus.TOO_LONG_EXECUTION_TIME, result.getEndStatus());
		
		assertTrue(result.isTooLongTime());
		assertFalse(result.isCorrectlyDone());
		assertFalse(result.isKilled());
		assertFalse(result.isRunning());
		
		assertEquals(Test5.MAX_DURATION, result.getMaxExecTime(TimeUnit.MILLISECONDS));
	}
	
	public void testKill() {
		ExecProcess ept = createExec(Test5.class);
		
		ScheduledThreadPoolExecutor max_exec_time_scheduler = new ScheduledThreadPoolExecutor(1);
		
		long start_time = System.currentTimeMillis();
		ExecProcessResult result = ept.start(createTF());
		
		max_exec_time_scheduler.schedule(() -> {
			result.kill();
		}, Test5.MAX_DURATION, TimeUnit.MILLISECONDS);
		
		result.waitForEnd();
		
		long duration = System.currentTimeMillis() - start_time;
		
		assertTrue(duration < Test5.MAX_DURATION + 300);/** 300 is a "startup time bonus" */
		assertEquals(EndStatus.KILLED, result.getEndStatus());
		
		assertFalse(result.isTooLongTime());
		assertFalse(result.isCorrectlyDone());
		assertTrue(result.isKilled());
		assertFalse(result.isRunning());
	}
	
	public void testKillSubProcess() throws InterruptedException {
		ExecProcess ept = createExec(Test6.class);
		
		ScheduledThreadPoolExecutor max_exec_time_scheduler = new ScheduledThreadPoolExecutor(1);
		
		long start_time = System.currentTimeMillis();
		ExecProcessResult result = ept.start(createTF());
		
		max_exec_time_scheduler.schedule(() -> {
			result.kill();
		}, Test5.MAX_DURATION * 4, TimeUnit.MILLISECONDS);
		
		Thread.sleep(Test5.MAX_DURATION);
		assertEquals(1, result.process.descendants().count());
		
		result.waitForEnd();
		
		long duration = System.currentTimeMillis() - start_time;
		
		assertTrue(duration < Test5.MAX_DURATION * 4 * 2);
		assertEquals(EndStatus.KILLED, result.getEndStatus());
		
		assertFalse(result.isTooLongTime());
		assertFalse(result.isCorrectlyDone());
		assertTrue(result.isKilled());
		assertFalse(result.isRunning());
		
		assertEquals(0, result.process.descendants().count());
	}
	
	public void testTimesAndProcessProps() {
		ExecProcess ept = createExec(Test5.class);
		
		ScheduledThreadPoolExecutor max_exec_time_scheduler = new ScheduledThreadPoolExecutor(1);
		ept.setMaxExecutionTime(Test5.MAX_DURATION, TimeUnit.MILLISECONDS, max_exec_time_scheduler);
		
		long start_time = System.currentTimeMillis();
		ExecProcessResult result = ept.start(createTF()).waitForEnd();
		
		assertEquals(result.process, result.getProcess());
		assertEquals(result.process.info().totalCpuDuration().orElse(Duration.ZERO).toMillis(), result.getCPUDuration(TimeUnit.MILLISECONDS));
		assertEquals(result.process.info().startInstant().orElse(Instant.EPOCH).toEpochMilli(), result.getStartDate());
		
		long duration = System.currentTimeMillis() - start_time;
		assertTrue(duration >= result.getUptime(TimeUnit.MILLISECONDS));
		
		assertEquals(result.process.pid(), result.getPID());
		assertEquals(ept.executable, result.getExecutable());
		
		assertEquals(Stream.concat(Stream.of(ept.executable.getPath()), ept.getParams().stream()).collect(Collectors.joining(" ")), result.getCommandline());
		assertTrue(result.getUserExec().endsWith(System.getProperty("user.name")));
		assertEquals(ept.working_directory, result.getWorkingDirectory());
	}
	
	static final Function<String[], Stream<String>> makeStringStream = s -> StreamSupport.stream(Arrays.spliterator(s), false);
	static final Predicate<String> withoutEmptyLines = l -> l.equals("") == false;
	static final Collector<CharSequence, ?, String> joinWithPipe = Collectors.joining("|");
	
	public void testOutErrStreams() {
		ExecProcessText ept = createExec(Test7.class);
		ept.addParams("n");
		ExecProcessTextResult result = ept.start(createTF()).waitForEnd();
		
		assertEquals(makeStringStream.apply(Test7.std_out).collect(joinWithPipe), result.getStdout(true, "|"));
		assertEquals(makeStringStream.apply(Test7.std_err).collect(joinWithPipe), result.getStderr(true, "|"));
		
		ept = createExec(Test7.class);
		ept.addParams("1");
		result = ept.start(createTF()).waitForEnd();
		
		assertEquals(makeStringStream.apply(Test7.std_out).filter(withoutEmptyLines).collect(joinWithPipe), result.getStdout(false, "|"));
		assertEquals(makeStringStream.apply(Test7.std_err).filter(withoutEmptyLines).collect(joinWithPipe), result.getStderr(false, "|"));
		
		assertEquals(Stream.concat(makeStringStream.apply(Test7.std_out), makeStringStream.apply(Test7.std_err)).collect(joinWithPipe), result.getStdouterr(true, "|"));
		assertEquals(Stream.concat(makeStringStream.apply(Test7.std_out), makeStringStream.apply(Test7.std_err)).filter(withoutEmptyLines).collect(joinWithPipe), result.getStdouterr(false, "|"));
		
		assertEquals(makeStringStream.apply(Test7.std_out).collect(joinWithPipe), result.getStdoutLines(true).collect(joinWithPipe));
		assertEquals(makeStringStream.apply(Test7.std_out).filter(withoutEmptyLines).collect(joinWithPipe), result.getStdoutLines(false).collect(joinWithPipe));
		
		assertEquals(makeStringStream.apply(Test7.std_err).collect(joinWithPipe), result.getStderrLines(true).collect(joinWithPipe));
		assertEquals(makeStringStream.apply(Test7.std_err).filter(withoutEmptyLines).collect(joinWithPipe), result.getStderrLines(false).collect(joinWithPipe));
		
		assertEquals(Stream.concat(makeStringStream.apply(Test7.std_out), makeStringStream.apply(Test7.std_err)).collect(joinWithPipe), result.getStdouterrLines(true).collect(joinWithPipe));
		assertEquals(Stream.concat(makeStringStream.apply(Test7.std_out), makeStringStream.apply(Test7.std_err)).filter(withoutEmptyLines).collect(joinWithPipe), result.getStdouterrLines(false).collect(joinWithPipe));
	}
	
	// XXX Test8 + ept.setInteractive_handler(interactive_handler, executor)
	
	// XXX tests ! + coverage
	
	// ept.alterProcessBuilderBeforeStartIt(alter_process_builder)
	// ept.makeProcessBuilder()
	// ept.start(executor)
	
	// result.getStdInInjection()
	// result.waitForEnd(executor)
	// result.waitForEnd(timeout, unit)
	
}
