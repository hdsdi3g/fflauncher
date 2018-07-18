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

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import junit.framework.TestCase;
import tv.hd3g.execprocess.processdemo.Test1;
import tv.hd3g.execprocess.processdemo.Test10;
import tv.hd3g.execprocess.processdemo.Test2;
import tv.hd3g.execprocess.processdemo.Test3;
import tv.hd3g.execprocess.processdemo.Test4;
import tv.hd3g.execprocess.processdemo.Test5;
import tv.hd3g.execprocess.processdemo.Test6;
import tv.hd3g.execprocess.processdemo.Test7;
import tv.hd3g.execprocess.processdemo.Test8;
import tv.hd3g.execprocess.processdemo.Test9;

public class ExecProcessTest extends TestCase {
	
	public static ThreadFactory createTF() {
		return r -> {
			Thread t = new Thread(r, "JUnit test");
			t.setDaemon(true);
			return t;
		};
	}
	
	private static final ExecutableFinder executable_finder;
	
	static {
		executable_finder = new ExecutableFinder();
		executable_finder.addPath(new File(System.getProperty("java.home") + File.separator + "bin"));
	}
	
	public static ExecProcessText createExec(Class<?> exec_class) {
		try {
			return (ExecProcessText) new ExecProcessText("java", executable_finder).addParams("-cp", System.getProperty("java.class.path")).addParams(exec_class.getName());
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
		
		assertTrue(ept.isExecCodeMustBeZero());
		ept.setExecCodeMustBeZero(false);
		assertFalse(ept.isExecCodeMustBeZero());
		
		ept.addParams(Test3.expected_in);
		assertEquals(Test3.expected_in, ept.getParams().get(ept.getParams().size() - 1));
		
		ept.setEnvironmentVar(Test3.ENV_KEY, Test3.ENV_VALUE);
		
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
	
	public void testInteractiveHandler() {
		ExecProcessText ept = createExec(Test8.class);
		ept.addParams("foo");
		ept.setMaxExecutionTime(500, TimeUnit.MILLISECONDS, new ScheduledThreadPoolExecutor(1));
		
		AtomicReference<ExecProcessTextResult> a_source = new AtomicReference<>();
		LinkedBlockingQueue<Exception> errors = new LinkedBlockingQueue<>();
		
		ept.setInteractiveHandler((source, line, is_std_err) -> {
			a_source.set(source);
			if (is_std_err) {
				System.err.println("Process say: " + line);
				errors.add(new Exception("is_std_err is true"));
				return Test8.QUIT;
			} else if (line.equals("FOO")) {
				return "bar";
			} else if (line.equals("foo")) {
				errors.add(new Exception("foo is in lowercase"));
				return Test8.QUIT;
			} else if (line.equals("BAR")) {
				return Test8.QUIT;
			} else if (line.equals("bar")) {
				errors.add(new Exception("bar is in lowercase"));
				return Test8.QUIT;
			} else {
				errors.add(new Exception("Invalid line " + line));
				return null;
			}
		}, r -> r.run());
		
		ExecProcessTextResult result = ept.start(createTF()).waitForEnd();
		
		assertEquals(result, a_source.get());
		
		if (errors.isEmpty() == false) {
			errors.forEach(e -> {
				e.printStackTrace();
			});
			fail();
		}
		
		assertTrue(result.isCorrectlyDone());
	}
	
	public void testStdInInjection() throws IOException {
		ExecProcessText ept = createExec(Test9.class);
		ept.setMaxExecutionTime(500, TimeUnit.MILLISECONDS, new ScheduledThreadPoolExecutor(1));
		ExecProcessTextResult result = ept.start(createTF());
		
		result.getStdInInjection().println(Test9.QUIT);
		
		result.waitForEnd();
		assertTrue(result.isCorrectlyDone());
	}
	
	public void testWaitForEnd() throws InterruptedException, ExecutionException, TimeoutException {
		ExecProcessText ept = createExec(Test10.class);
		
		assertTrue(ept.start(createTF()).waitForEnd(r -> r.run()).get(500, TimeUnit.MILLISECONDS).isCorrectlyDone());
		assertTrue(ept.start(createTF()).waitForEnd(500, TimeUnit.MILLISECONDS).isCorrectlyDone());
	}
	
	public void testExecutor() throws InterruptedException, ExecutionException, TimeoutException {
		ExecProcessText ept = createExec(Test1.class);
		assertTrue(ept.start(r -> r.run()).waitForEnd().isCorrectlyDone());
	}
	
	public void testParams() throws IOException {
		ExecProcessText ept = createExec(Test1.class);
		ExecProcess ep = new ExecProcess(ept.executable);
		
		assertEquals(ep.params, ep.getParams());
		
		ep.addParams("a", "b", null, "c");
		ep.addParams("d");
		assertEquals("abcd", ep.params.stream().collect(Collectors.joining()));
		
		ep.setParams(Arrays.asList("a", "b", null, "c"));
		assertEquals("abc", ep.params.stream().collect(Collectors.joining()));
		
		ep.setParams("a", "b", "c", "d", null, "e");
		assertEquals("abcde", ep.params.stream().collect(Collectors.joining()));
		
		ep.setSpacedParams("a b  c d", "e", null, "f");
		assertEquals("abcdef".length(), ep.params.size());
		assertEquals("abcdef", ep.params.stream().collect(Collectors.joining()));
		
		ep.addSpacedParams("ggg h i ", " e", null, "fff", " ");
		assertEquals("abcdef".length() + 3 + 1 + 1, ep.params.size());
		assertEquals("abcdefggghiefff", ep.params.stream().collect(Collectors.joining()));
	}
	
	public void testToString() throws IOException {
		ExecProcessText ept = createExec(Test1.class);
		
		assertNotNull(ept.toString());
		assertNotNull(ept.start(r -> r.run()).toString());
		
		ExecProcess ep = new ExecProcess(ept.executable);
		ep.setParams(ept.getParams());
		
		assertNotNull(ep.toString());
		assertNotNull(ep.start(r -> r.run()).toString());
		assertNotNull(ep.start(createTF()).toString());
	}
	
	public void testProcessBuilder() {
		ExecProcessText ept = createExec(Test1.class);
		
		AtomicReference<ProcessBuilder> a_pb = new AtomicReference<>();
		ept.alterProcessBuilderBeforeStartIt(pb -> {
			a_pb.set(pb);
		});
		
		AtomicReference<Runnable> a_run = new AtomicReference<>();
		ExecProcessTextResult epr = ept.start(r -> a_run.set(r));
		
		assertNull(a_pb.get());
		assertNotNull(a_run.get());
		
		a_run.get().run();
		
		assertNotNull(a_pb.get());
		
		assertEquals(epr.getCommandline(), a_pb.get().command().stream().collect(Collectors.joining(" ")));
		
		ept.setEnvironmentVar("PATH", "c:\\bin");
		
		ProcessBuilder new_pb = ept.makeProcessBuilder();
		assertEquals(epr.getCommandline(), new_pb.command().stream().collect(Collectors.joining(" ")));
		assertEquals(ept.working_directory, new_pb.directory());
		
		assertEquals(ept.environment.get("PATH"), new_pb.environment().get("PATH"));
	}
	
}
