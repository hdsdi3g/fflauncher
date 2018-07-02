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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

public class ExecutableFinder {
	private static Logger log = Logger.getLogger(ExecutableFinder.class);
	
	public static final String[] WINDOWS_EXEC_EXTENSIONS = { "exe", "com", "bat", "cmd" };
	
	/**
	 * synchronizedList
	 */
	private final List<File> paths;
	private final LinkedHashMap<String, File> declared_in_configuration;
	private final boolean is_windows;
	
	private static Predicate<File> isValidDirectory = f -> {
		return f.exists() && f.isDirectory() && f.canRead();
	};
	
	public ExecutableFinder() {
		declared_in_configuration = new LinkedHashMap<>();
		is_windows = File.pathSeparator.equals("\\");// System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
		
		String[] PATH = System.getenv("PATH").split(File.pathSeparator);
		
		/**
		 * Add only valid dirs
		 */
		paths = Collections.synchronizedList(new ArrayList<>(Arrays.asList(PATH).stream().map(p -> new File(p)).filter(isValidDirectory).collect(Collectors.toList())));
		
		log.debug("Get current path: " + paths);
	}
	
	public String getFullPath() {
		return paths.stream().map(f -> f.getPath()).reduce((BinaryOperator<String>) (left, right) -> {
			return left + File.pathSeparator + right;
		}).get();
	}
	
	public ExecutableFinder addLocalPath(String relative_user_home_path) {
		if (is_windows) {
			relative_user_home_path.replaceAll("/", "\\");
		} else {
			relative_user_home_path.replaceAll("\\", "/");
		}
		
		String user_home = System.getProperty("user.home");
		File f = (new File(user_home + File.separator + relative_user_home_path)).getAbsoluteFile();
		
		return addPath(f);
	}
	
	public ExecutableFinder addPath(File file_path) {
		File f = file_path.getAbsoluteFile();
		
		if (isValidDirectory.test(f)) {
			paths.add(f);
			log.debug("Manually register path: " + f.getPath());
		}
		return this;
	}
	
	private boolean validExec(File exec) {
		if (exec.exists() == false) {
			return false;
		} else if (exec.isFile() == false) {
			return false;
		} else if (exec.canRead() == false) {
			return false;
		}
		if (is_windows) {
			for (int pos_w_exe = 0; pos_w_exe < WINDOWS_EXEC_EXTENSIONS.length; pos_w_exe++) {
				if (exec.getName().toLowerCase().endsWith("." + WINDOWS_EXEC_EXTENSIONS[pos_w_exe])) {
					return true;
				}
			}
			return false;
		} else {
			return exec.canExecute();
		}
	}
	
	public ExecutableFinder registerExecutable(String name, File full_path) throws IOException {
		if (validExec(full_path) == false) {
			throw new IOException("Invalid declared_in_configuration executable: " + name + " can't be correctly found in " + full_path);
		}
		declared_in_configuration.put(name, full_path);
		return this;
	}
	
	/**
	 * Can add .exe to name if OS == Windows and if missing.
	 * @param name can be a simple exec name, or a full path.
	 * @return never null
	 * @throws FileNotFoundException if exec don't exists or is not correctly registed.
	 */
	public File get(String name) throws IOException {
		if (declared_in_configuration.containsKey(name)) {
			return declared_in_configuration.get(name);
		}
		
		File exec = new File(name);
		if (validExec(exec)) {
			return exec;
		}
		
		List<File> all_file_candidates = Stream.concat(paths.stream(), declared_in_configuration.values().stream().map(file -> {
			return file.getParentFile();
		})).map(dir -> {
			return (new File(dir + File.separator + name)).getAbsoluteFile();
		}).distinct().collect(Collectors.toList());
		
		return all_file_candidates.stream().filter(file -> {
			return validExec(file);
		}).findFirst().or(() -> {
			if (is_windows == false) {
				return Optional.empty();
			}
			/**
			 * Try with add windows ext
			 */
			return all_file_candidates.stream().flatMap(file -> {
				return Arrays.asList(WINDOWS_EXEC_EXTENSIONS).stream().map(ext -> {
					return new File(file + "." + ext);
				}).filter(file_ext -> {
					return validExec(file_ext);
				});
			}).findFirst();
		}).orElseThrow(() -> new IOException("Can't found executable \"" + name + "\""));
	}
	
}
