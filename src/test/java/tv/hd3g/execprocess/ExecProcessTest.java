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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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
	
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	static final ExecutableFinder executable_finder;
	
	static {
		executable_finder = new ExecutableFinder();
		executable_finder.addPath(new File(System.getProperty("java.home") + File.separator + "bin"));
	}
	
	public static ExecProcessText createExec(Class<?> exec_class) {
		try {
			return new ExecProcessText("java", executable_finder).addParameters("-cp", System.getProperty("java.class.path")).addParameters(exec_class.getName());
		} catch (IOException e) {
			throw new RuntimeException("Can't found java exec", e);
		}
	}
	
	public void testSimpleExec() throws InterruptedException, ExecutionException {
		ExecProcessText ept = createExec(Test1.class);
		ExecProcessTextResult result = ept.start(executor).waitForEnd().get();
		
		assertEquals(Test1.expected, result.getStdouterr(true, ""));
		assertEquals(0, (int) result.getExitCode().get());
		assertEquals(EndStatus.CORRECTLY_DONE, result.getEndStatus().get());
	}
	
	public void testWorkingDirectory() throws IOException, InterruptedException, ExecutionException {
		ExecProcessText ept = createExec(Test2.class);
		File wd = new File(System.getProperty("user.dir")).getCanonicalFile();
		ept.setWorkingDirectory(wd);
		
		assertEquals(wd, ept.getWorkingDirectory());
		
		ExecProcessTextResult result = ept.start(executor).waitForEnd().get();
		assertEquals(wd, result.getWorkingDirectory());
		
		assertEquals(wd.getPath(), result.getStdouterr(true, ""));
		assertEquals(EndStatus.CORRECTLY_DONE, result.getEndStatus().get());
	}
	
	public void testEndExecutionCallback() {
		ExecProcessText ept = createExec(Test1.class);
		
		AtomicReference<ExecProcessResult> expected_result = new AtomicReference<>();
		ept.addEndExecutionCallback(r -> {
			expected_result.set(r);
		}, t -> {
			new Thread(t).start();
		});
		
		ExecProcessTextResult result = ept.start(executor);
		
		assertTrue(expected_result.get() == null);
		
		result.waitForEnd();
		
		while (expected_result.get() == null) {
			Thread.onSpinWait();
		}
		
		assertEquals(result, expected_result.get());
	}
	
	public void testResultValues() throws InterruptedException, ExecutionException {
		long start_date = System.currentTimeMillis() - 1;
		
		ExecProcessText ept = createExec(Test3.class);
		
		assertTrue(ept.isExecCodeMustBeZero());
		ept.setExecCodeMustBeZero(false);
		assertFalse(ept.isExecCodeMustBeZero());
		
		ept.addParameters(Test3.expected_in);
		assertEquals(Test3.expected_in, ept.getParameters().get(ept.getParameters().size() - 1));
		
		ept.setEnvironmentVar(Test3.ENV_KEY, Test3.ENV_VALUE);
		
		ExecProcessTextResult result = ept.start(executor).waitForEnd().get();
		
		assertEquals(Test3.expected_out, result.getStdout(false, ""));
		assertEquals(Test3.expected_err, result.getStderr(false, ""));
		assertEquals(Test3.exit_ok, (int) result.getExitCode().get());
		assertEquals(EndStatus.CORRECTLY_DONE, result.getEndStatus().get());
		
		assertTrue(result.getCommandline().endsWith(Test3.expected_in));
		assertEquals(ept.executable, result.getExecutable());
		assertTrue(result.getPID().get() > 0);
		assertTrue(result.getUserExec().get().get().endsWith(System.getProperty("user.name")));
		
		assertEquals(Test3.exit_ok, result.getProcess().get().exitValue());
		assertEquals((long) result.getPID().get(), result.getProcess().get().pid());
		assertFalse(result.getProcess().get().isAlive());
		
		assertTrue(result.getStartDate().get() > start_date);
		assertTrue(result.getStartDate().get() < System.currentTimeMillis());
		assertTrue(result.getEnvironment().getOrDefault(Test3.ENV_KEY, "").equals(Test3.ENV_VALUE));
		
	}
	
	public void testStdObserver() throws InterruptedException, ExecutionException {
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
		
		ExecProcessTextResult result = ept.start(executor).waitForEnd().get();
		
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
	
	public void testNotCaptureStreams() throws InterruptedException, ExecutionException {
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
		
		ExecProcessTextResult result = ept.start(executor).waitForEnd().get();
		
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
		
		result = ept.start(executor).waitForEnd().get();
		
		assertEquals(0, catch_stdout.size());
		assertEquals(1, catch_stderr.size());
		
		assertNotNull(result.getStdoutLines(true));
		assertNotNull(result.getStderrLines(true));
		assertNotNull(result.getStdouterrLines(true));
		
		assertEquals(0, result.getStdoutLines(true).count());
		assertEquals(1, result.getStderrLines(true).count());
		assertEquals(1, result.getStdouterrLines(true).count());
	}
	
	public void testMaxExecTime() throws InterruptedException, ExecutionException {
		ExecProcess ept = createExec(Test5.class);
		
		ScheduledThreadPoolExecutor max_exec_time_scheduler = new ScheduledThreadPoolExecutor(1);
		ept.setMaxExecutionTime(Test5.MAX_DURATION, TimeUnit.MILLISECONDS, max_exec_time_scheduler);
		
		assertEquals(Test5.MAX_DURATION, ept.getMaxExecTime(TimeUnit.MILLISECONDS));
		
		long start_time = System.currentTimeMillis();
		ExecProcessResult result = ept.start(executor).waitForEnd().get();
		
		long duration = System.currentTimeMillis() - start_time;
		
		assertTrue(duration < Test5.MAX_DURATION + 300);/** 300 is a "startup time bonus" */
		assertEquals(EndStatus.TOO_LONG_EXECUTION_TIME, result.getEndStatus().get());
		
		assertTrue(result.isTooLongTime());
		assertFalse(result.isCorrectlyDone().get());
		assertFalse(result.isKilled());
		assertFalse(result.isRunning().get());
		
		assertEquals(Test5.MAX_DURATION, result.getMaxExecTime(TimeUnit.MILLISECONDS));
	}
	
	public void testKill() throws InterruptedException, ExecutionException {
		ExecProcess ept = createExec(Test5.class);
		
		ScheduledThreadPoolExecutor max_exec_time_scheduler = new ScheduledThreadPoolExecutor(1);
		
		long start_time = System.currentTimeMillis();
		ExecProcessResult result = ept.start(executor);
		
		max_exec_time_scheduler.schedule(() -> {
			try {
				result.kill(r -> r.run()).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException("Can't kill", e);
			}
		}, Test5.MAX_DURATION, TimeUnit.MILLISECONDS);
		
		result.waitForEnd().get();
		
		long duration = System.currentTimeMillis() - start_time;
		
		assertTrue(duration < Test5.MAX_DURATION + 300);/** 300 is a "startup time bonus" */
		assertEquals(EndStatus.KILLED, result.getEndStatus().get());
		
		assertFalse(result.isTooLongTime());
		assertFalse(result.isCorrectlyDone().get());
		assertTrue(result.isKilled());
		assertFalse(result.isRunning().get());
	}
	
	public void testKillSubProcess() throws InterruptedException, ExecutionException {
		ExecProcess ept = createExec(Test6.class);
		
		ScheduledThreadPoolExecutor max_exec_time_scheduler = new ScheduledThreadPoolExecutor(1);
		
		long start_time = System.currentTimeMillis();
		ExecProcessResult result = ept.start(executor);
		
		max_exec_time_scheduler.schedule(() -> {
			try {
				result.kill(r -> r.run()).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException("Can't kill", e);
			}
		}, Test5.MAX_DURATION * 4, TimeUnit.MILLISECONDS);
		
		Thread.sleep(Test5.MAX_DURATION);
		assertEquals(1, result.getProcess().get().descendants().count());
		
		result.waitForEnd().get();
		
		long duration = System.currentTimeMillis() - start_time;
		
		assertTrue(duration < Test5.MAX_DURATION * 4 * 2);
		assertEquals(EndStatus.KILLED, result.getEndStatus().get());
		
		assertFalse(result.isTooLongTime());
		assertFalse(result.isCorrectlyDone().get());
		assertTrue(result.isKilled());
		assertFalse(result.isRunning().get());
		
		assertEquals(0, result.getProcess().get().descendants().count());
	}
	
	public void testTimesAndProcessProps() throws InterruptedException, ExecutionException {
		ExecProcess ept = createExec(Test5.class);
		
		ScheduledThreadPoolExecutor max_exec_time_scheduler = new ScheduledThreadPoolExecutor(1);
		ept.setMaxExecutionTime(Test5.MAX_DURATION, TimeUnit.MILLISECONDS, max_exec_time_scheduler);
		
		long start_time = System.currentTimeMillis();
		ExecProcessResult result = ept.start(executor).waitForEnd().get();
		
		assertEquals(result.getProcess().get().info().totalCpuDuration().orElse(Duration.ZERO).toMillis(), (long) result.getCPUDuration(TimeUnit.MILLISECONDS).get());
		assertEquals(result.getProcess().get().info().startInstant().orElse(Instant.EPOCH).toEpochMilli(), (long) result.getStartDate().get());
		
		long duration = System.currentTimeMillis() - start_time;
		assertTrue(duration >= result.getUptime(TimeUnit.MILLISECONDS).get());
		
		assertEquals(result.getProcess().get().pid(), (long) result.getPID().get());
		assertEquals(ept.executable, result.getExecutable());
		
		assertEquals(Stream.concat(Stream.of(ept.executable.getPath()), ept.getParameters().stream()).collect(Collectors.joining(" ")), result.getCommandline());
		assertTrue(result.getUserExec().get().get().endsWith(System.getProperty("user.name")));
		assertEquals(ept.working_directory, result.getWorkingDirectory());
	}
	
	static final Function<String[], Stream<String>> makeStringStream = s -> StreamSupport.stream(Arrays.spliterator(s), false);
	static final Predicate<String> withoutEmptyLines = l -> l.equals("") == false;
	static final Collector<CharSequence, ?, String> joinWithPipe = Collectors.joining("|");
	
	public void testOutErrStreams() throws InterruptedException, ExecutionException {
		ExecProcessText ept = createExec(Test7.class);
		ept.addParameters("n");
		ExecProcessTextResult result = ept.start(executor).waitForEnd().get();
		
		assertEquals(makeStringStream.apply(Test7.std_out).collect(joinWithPipe), result.getStdout(true, "|"));
		assertEquals(makeStringStream.apply(Test7.std_err).collect(joinWithPipe), result.getStderr(true, "|"));
		
		ept = createExec(Test7.class);
		ept.addParameters("1");
		result = ept.start(executor).waitForEnd().get();
		
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
	
	public void testInteractiveHandler() throws InterruptedException, ExecutionException {
		ExecProcessText ept = createExec(Test8.class);
		ept.addParameters("foo");
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
		}, r -> {
			r.run();
		});
		
		ExecProcessTextResult result = ept.start(executor).waitForEnd().get();
		
		assertNotNull(a_source.get());
		assertEquals(result, a_source.get());
		
		if (errors.isEmpty() == false) {
			errors.forEach(e -> {
				e.printStackTrace();
			});
			fail();
		}
		
		assertEquals(EndStatus.CORRECTLY_DONE, result.getEndStatus().get());
		assertTrue(result.isCorrectlyDone().get());
	}
	
	public void testStdInInjection() throws IOException, InterruptedException, ExecutionException {
		ExecProcessText ept = createExec(Test9.class);
		ept.setMaxExecutionTime(500, TimeUnit.MILLISECONDS, new ScheduledThreadPoolExecutor(1));
		ExecProcessTextResult result = ept.start(executor);
		
		result.getStdInInjection(executor).println(Test9.QUIT);
		
		result.waitForEnd().get();
		assertTrue(result.isCorrectlyDone().get());
	}
	
	public void testWaitForEnd() throws InterruptedException, ExecutionException, TimeoutException {
		ExecProcessText ept = createExec(Test10.class);
		
		assertTrue(ept.start(executor).waitForEnd().get(500, TimeUnit.MILLISECONDS).isCorrectlyDone().get());
		assertTrue(ept.start(executor).waitForEnd(500, TimeUnit.MILLISECONDS).get().isCorrectlyDone().get());
	}
	
	public void testExecutor() throws InterruptedException, ExecutionException, TimeoutException {
		ExecProcessText ept = createExec(Test1.class);
		assertTrue(ept.start(r -> r.run()).waitForEnd().get().isCorrectlyDone().get());
	}
	
	public void testToString() throws IOException {
		ExecProcessText ept = createExec(Test1.class);
		
		assertNotNull(ept.toString());
		assertNotNull(ept.start(r -> r.run()).toString());
		
		ExecProcess ep = new ExecProcess(ept.executable);
		ep.importParametersFrom(ept);
		
		assertNotNull(ep.toString());
		assertNotNull(ep.start(r -> r.run()).toString());
		assertNotNull(ep.start(executor).toString());
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
