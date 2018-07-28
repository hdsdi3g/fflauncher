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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * It will resolve/find valid executable files in *NIX and valid executable extensions in Windows.
 * On system PATH, classpath, current dir, and local user dir (/bin).
 */
public class ExecutableFinder {
	private static final Logger log = LogManager.getLogger();
	
	/**
	 * unmodifiableList
	 */
	public static final List<String> WINDOWS_EXEC_EXTENSIONS;
	
	static {
		if (System.getenv().containsKey("PATHEXT")) {
			/**
			 * Like .COM;.EXE;.BAT;.CMD;.VBS;.VBE;.JS;.JSE;.WSF;.WSH;.MSC
			 */
			String path_ext = System.getenv("PATHEXT");
			if (path_ext.indexOf(";") > 0) {
				WINDOWS_EXEC_EXTENSIONS = Collections.unmodifiableList(Arrays.stream(path_ext.split(";")).map(ext -> ext.toLowerCase().substring(1)).collect(Collectors.toUnmodifiableList()));
			} else {
				log.warn("Invalid PATHEXT env.: " + path_ext);
				WINDOWS_EXEC_EXTENSIONS = Collections.unmodifiableList(Arrays.asList("exe", "com", "cmd", "bat"));
			}
		} else {
			WINDOWS_EXEC_EXTENSIONS = Collections.unmodifiableList(Arrays.asList("exe", "com", "cmd", "bat"));
		}
	}
	
	/**
	 * synchronizedList
	 */
	private final LinkedList<File> paths;
	private final LinkedHashMap<String, File> declared_in_configuration;
	private final boolean is_windows_style_path;
	
	private static Predicate<File> isValidDirectory = f -> {
		return f.exists() && f.isDirectory() && f.canRead();
	};
	
	public ExecutableFinder() {
		declared_in_configuration = new LinkedHashMap<>();
		is_windows_style_path = File.separator.equals("\\");// System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
		
		/**
		 * Add only valid dirs
		 */
		paths = new LinkedList<>();
		
		addLocalPath("/bin");
		addLocalPath("/App/bin");
		
		paths.add(new File(System.getProperty("user.dir")));
		
		paths.addAll(Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)).map(p -> {
			return new File(p);
		}).filter(isValidDirectory).collect(Collectors.toUnmodifiableList()));
		
		paths.addAll(Arrays.stream(System.getenv("PATH").split(File.pathSeparator)).map(p -> new File(p)).filter(isValidDirectory).collect(Collectors.toUnmodifiableList()));
		
		/**
		 * Remove duplicate entries
		 */
		List<File> new_list = paths.stream().distinct().collect(Collectors.toUnmodifiableList());
		paths.clear();
		paths.addAll(new_list);
		
		if (log.isTraceEnabled()) {
			log.trace("Full path: " + paths.stream().map(f -> f.getPath()).collect(Collectors.joining(File.pathSeparator)));
		}
	}
	
	public String getFullPathToString() {
		return paths.stream().map(f -> f.getPath()).reduce((BinaryOperator<String>) (left, right) -> {
			return left + File.pathSeparator + right;
		}).get();
	}
	
	/**
	 * @return unmodifiableList
	 */
	public List<File> getFullPath() {
		return Collections.unmodifiableList(paths);
	}
	
	/**
	 * Put in top priority.
	 * Path / or \ will be corrected
	 */
	public ExecutableFinder addLocalPath(String relative_user_home_path) {
		if (is_windows_style_path) {
			relative_user_home_path.replaceAll("/", "\\\\");
		} else {
			relative_user_home_path.replaceAll("\\\\", "/");
		}
		
		String user_home = System.getProperty("user.home");
		File f = new File(user_home + File.separator + relative_user_home_path).getAbsoluteFile();
		
		return addPath(f);
	}
	
	/**
	 * Put in top priority.
	 */
	public ExecutableFinder addPath(File file_path) {
		File f = file_path.getAbsoluteFile();
		
		if (isValidDirectory.test(f)) {
			synchronized (this) {
				log.debug("Register path: " + f.getPath());
				paths.addFirst(f);
			}
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
	 * Can add .exe to name if OS == Windows and if it's missing.
	 * @param name can be a simple exec name, or a full path.
	 * @return never null
	 * @throws FileNotFoundException if exec don't exists or is not correctly registed.
	 */
	public File get(String name) throws FileNotFoundException {
		if (declared_in_configuration.containsKey(name)) {
			return declared_in_configuration.get(name);
		}
		
		File exec = new File(name);
		if (validExec(exec)) {
			return exec;
		}
		
		List<File> all_file_candidates = Stream.concat(declared_in_configuration.values().stream().map(file -> {
			return file.getParentFile();
		}), paths.stream()).map(dir -> {
			return new File(dir + File.separator + name).getAbsoluteFile();
		}).distinct().collect(Collectors.toUnmodifiableList());
		
		if (is_windows_style_path == false) {
			/**
			 * *nix flavors
			 */
			return all_file_candidates.stream().filter(file -> {
				return validExec(file);
			}).findFirst().orElseThrow(() -> new FileNotFoundException("Can't found executable \"" + name + "\""));
		} else {
			/**
			 * Windows flavor
			 * Try with add windows ext
			 */
			return all_file_candidates.stream().flatMap(file -> {
				boolean has_already_valid_ext = WINDOWS_EXEC_EXTENSIONS.stream().anyMatch(ext -> {
					return file.getName().toLowerCase().endsWith("." + ext.toLowerCase());
				});
				
				if (has_already_valid_ext) {
					if (validExec(file)) {
						return Stream.of(file);
					} else {
						return Stream.empty();
					}
				} else {
					/**
					 * We must to add ext, we try with all avaliable ext.
					 */
					return WINDOWS_EXEC_EXTENSIONS.stream().flatMap(ext -> {
						/**
						 * Try with lower/upper case extensions.
						 */
						return Stream.of(new File(file + "." + ext.toLowerCase()), new File(file + "." + ext.toUpperCase()));
					}).filter(file_ext -> {
						return validExec(file_ext);
					});
				}
			}).findFirst().orElseThrow(() -> new FileNotFoundException("Can't found executable \"" + name + "\""));
		}
	}
	
}
