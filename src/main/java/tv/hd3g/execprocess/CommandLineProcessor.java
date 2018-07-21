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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandLineProcessor {
	
	private final String start_var_tag;
	private final String end_var_tag;
	
	/**
	 * Use "<%" and "%>" by default
	 */
	public CommandLineProcessor() {
		this("<%", "%>");
	}
	
	/**
	 * @return like "%>"
	 */
	public String getEndVarTag() {
		return end_var_tag;
	}
	
	/**
	 * @return like "<%"
	 */
	public String getStartVarTag() {
		return start_var_tag;
	}
	
	public CommandLineProcessor(String start_var_tag, String end_var_tag) {
		this.start_var_tag = start_var_tag;
		if (start_var_tag == null) {
			throw new NullPointerException("\"start_var_tag\" can't to be null");
		} else if (start_var_tag.isEmpty()) {
			throw new NullPointerException("\"start_var_tag\" can't to be empty");
		}
		this.end_var_tag = end_var_tag;
		if (end_var_tag == null) {
			throw new NullPointerException("\"end_var_tag\" can't to be null");
		} else if (end_var_tag.isEmpty()) {
			throw new NullPointerException("\"end_var_tag\" can't to be empty");
		}
		
	}
	
	public CommandLine createCommandLine(String full_command_line_with_vars) {
		if (full_command_line_with_vars == null) {
			throw new NullPointerException("\"full_command_line_with_vars\" can't to be null");
		}
		return new CommandLine(full_command_line_with_vars);
	}
	
	/**
	 * @param param like
	 * @return true if like "<%myvar%>"
	 */
	public boolean isTaggedParameter(String param) {
		if (param == null) {
			throw new NullPointerException("\"param\" can't to be null");
		} else if (param.isEmpty()) {
			return false;
		} else if (param.contains(" ")) {
			return false;
		}
		return param.startsWith(start_var_tag) & param.endsWith(end_var_tag);
	}
	
	/**
	 * @param param like <%myvar%>
	 * @return like "myvar" or null if param is not a valid variable of if it's empty.
	 */
	public String extractVarNameFromTaggedParameter(String param) {
		if (isTaggedParameter(param) == false) {
			return null;
		}
		if (param.length() == start_var_tag.length() + end_var_tag.length()) {
			return null;
		}
		return param.substring(start_var_tag.length(), param.length() - end_var_tag.length());
	}
	
	private static final Character QUOTE = '"';
	private static final Character SPACE = ' ';
	
	private static class Arg {
		final boolean is_in_quotes;
		final StringBuilder content;
		
		Arg(boolean is_in_quotes) {
			this.is_in_quotes = is_in_quotes;
			content = new StringBuilder();
		}
		
		Arg add(char arg) {
			content.append(arg);
			return this;
		}
		
		public String toString() {
			return content.toString();
		}
	}
	
	public class CommandLine {
		
		private final String exec_name;
		private final List<String> original_params;
		private String param_keys_starts_with = "-";
		
		CommandLine(String full_command_line_with_vars) {
			
			/**
			 * Split >-a -b "c d" e< to [-a, -b, c d, e]
			 */
			List<String> full_args = full_command_line_with_vars.trim().chars().mapToObj(i -> (char) i).reduce(new ArrayList<Arg>(), (list, chr) -> {
				if (list.isEmpty()) {
					/**
					 * First entry
					 */
					if (chr == QUOTE) {
						/**
						 * Start quote zone
						 */
						list.add(new Arg(true));
					} else if (chr == SPACE) {
						/**
						 * Trailing space > ignore it
						 */
					} else {
						/**
						 * Start first "classic" arg
						 */
						list.add(new Arg(false).add(chr));
					}
				} else {
					/**
					 * Get current entry
					 */
					int last_pos = list.size() - 1;
					Arg last_entry = list.get(last_pos);
					
					if (chr == QUOTE) {
						if (last_entry.is_in_quotes) {
							/**
							 * Switch off quote zone
							 */
							list.add(new Arg(false));
						} else {
							/**
							 * Switch on quote zone
							 */
							if (last_entry.content.length() == 0) {
								/**
								 * Remove previous empty Arg
								 */
								list.remove(last_pos);
							}
							list.add(new Arg(true));
						}
					} else if (chr == SPACE) {
						if (last_entry.is_in_quotes) {
							/**
							 * Add space in quotes
							 */
							last_entry.add(chr);
						} else {
							if (last_entry.content.length() > 0) {
								/**
								 * New space -> new arg (and ignore space)
								 */
								list.add(new Arg(false));
							} else {
								/**
								 * Space between args > ignore it
								 */
							}
						}
					} else {
						last_entry.add(chr);
					}
				}
				return list;
			}, (list1, list2) -> {
				ArrayList<Arg> args = new ArrayList<>(list1);
				args.addAll(list2);
				return args;
			}).stream().map(arg -> {
				return arg.toString();
			}).collect(Collectors.toList());
			
			if (full_args.isEmpty()) {
				throw new RuntimeException("Empty args");
			}
			
			exec_name = full_args.get(0);
			if (full_args.size() > 1) {
				original_params = Collections.unmodifiableList(full_args.stream().skip(1).collect(Collectors.toList()));
			} else {
				original_params = Collections.emptyList();
			}
		}
		
		/**
		 * @param param_keys_starts_with "-" by default
		 */
		public CommandLine setParamKeysStartsWith(String param_keys_starts_with) {
			this.param_keys_starts_with = param_keys_starts_with;
			return this;
		}
		
		/**
		 * @return "-" by default
		 */
		public String getParamKeysStartsWith() {
			return param_keys_starts_with;
		}
		
		public String getExecName() {
			return exec_name;
		}
		
		private boolean isArgIsAParamKey(String arg) {
			return arg.startsWith(param_keys_starts_with);
		}
		
		public ProcessedCommandLine process() {
			return process(Collections.emptyMap(), false);
		}
		
		public ProcessedCommandLine process(Map<String, String> vars_to_inject, boolean remove_params_if_no_var_to_inject) {
			if (remove_params_if_no_var_to_inject) {
				return new ProcessedCommandLine(original_params.stream().reduce(Collections.unmodifiableList(new ArrayList<String>()), (list, arg) -> {
					if (isTaggedParameter(arg)) {
						String var_name = extractVarNameFromTaggedParameter(arg);
						if (vars_to_inject.containsKey(var_name)) {
							return Stream.concat(list.stream(), Stream.of(vars_to_inject.get(var_name))).collect(Collectors.toList());
						} else {
							if (list.isEmpty()) {
								return list;
							} else if (isArgIsAParamKey(list.get(list.size() - 1))) {
								return list.stream().limit(list.size() - 1).collect(Collectors.toList());
							} else {
								return list;
							}
						}
					} else {
						return Stream.concat(list.stream(), Stream.of(arg)).collect(Collectors.toList());
					}
				}, LIST_COMBINER));
			} else {
				return new ProcessedCommandLine(original_params.stream().map(arg -> {
					String var_name = extractVarNameFromTaggedParameter(arg);
					if (var_name != null) {
						return vars_to_inject.get(var_name);
					} else {
						return arg;
					}
				}).filter(arg -> arg != null).collect(Collectors.toList()));
			}
		}
		
		public class ProcessedCommandLine {
			
			private final ArrayList<String> params;
			
			ProcessedCommandLine(List<String> processed_params) {
				params = new ArrayList<>(processed_params);
			}
			
			public String getExecName() {
				return exec_name;
			}
			
			/**
			 * @return current list, editable
			 */
			public ArrayList<String> getParameters() {
				return params;
			}
			
			/**
			 * @param param_key add "-" in front of param_key if needed
			 */
			private String conformParameterKey(String param_key) {
				if (isArgIsAParamKey(param_key) == false) {
					return param_keys_starts_with + param_key;
				}
				return param_key;
			}
			
			/**
			 * @param param_key can have "-" or not (it will be added).
			 * @return For "-param val1 -param val2 -param val3" -> val1, val2, val3 ; null if param_key can't be found, empty if not values for param
			 */
			public List<String> getValues(String param_key) {
				if (param_key == null) {
					throw new NullPointerException("\"param_key\" can't to be null");
				}
				
				final String param = conformParameterKey(param_key);
				
				ArrayList<String> result = new ArrayList<>();
				
				boolean has = false;
				for (int pos = 0; pos < params.size(); pos++) {
					String current = params.get(pos);
					if (current.equals(param)) {
						has = true;
						if (params.size() > pos + 1) {
							String next = params.get(pos + 1);
							if (isArgIsAParamKey(next) == false) {
								result.add(next);
							}
						}
					}
				}
				
				if (has) {
					return Collections.unmodifiableList(result);
				} else {
					return null;
				}
			}
			
			/**
			 * Search a remove all parameters with param_key as name, even associated values.
			 * @param param_key can have "-" or not (it will be added).
			 */
			public boolean removeParameter(String param_key, int param_as_this_key_pos) {
				if (param_key == null) {
					throw new NullPointerException("\"param_key\" can't to be null");
				}
				
				final String param = conformParameterKey(param_key);
				
				int to_skip = param_as_this_key_pos + 1;
				
				for (int pos = 0; pos < params.size(); pos++) {
					String current = params.get(pos);
					if (current.equals(param)) {
						to_skip--;
						if (to_skip == 0) {
							if (params.size() > pos + 1) {
								String next = params.get(pos + 1);
								if (isArgIsAParamKey(next) == false) {
									params.remove(pos + 1);
								}
							}
							params.remove(pos);
							return true;
						}
					}
				}
				
				return false;
			}
			
			/**
			 * @param param_key can have "-" or not (it will be added).
			 * @return true if done
			 */
			public boolean alterParameter(String param_key, String new_value, int param_as_this_key_pos) {
				if (param_key == null) {
					throw new NullPointerException("\"param_key\" can't to be null");
				} else if (new_value == null) {
					throw new NullPointerException("\"new_value\" can't to be null");
				}
				
				final String param = conformParameterKey(param_key);
				
				int to_skip = param_as_this_key_pos + 1;
				
				for (int pos = 0; pos < params.size(); pos++) {
					String current = params.get(pos);
					if (current.equals(param)) {
						to_skip--;
						if (to_skip == 0) {
							if (params.size() > pos + 1) {
								String next = params.get(pos + 1);
								if (isArgIsAParamKey(next) == false) {
									params.set(pos + 1, new_value);
								} else {
									params.add(pos + 1, new_value);
								}
							} else {
								params.add(new_value);
							}
							return true;
						}
					}
				}
				
				return false;
			}
			
		}
		
	}
	
	private static final BinaryOperator<List<String>> LIST_COMBINER = (list1, list2) -> Stream.concat(list1.stream(), list2.stream()).collect(Collectors.toList());
	
}
