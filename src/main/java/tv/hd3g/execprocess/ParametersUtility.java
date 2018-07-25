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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manipulate command line parameters: add, parse, get, list...
 * Don't manage executable name here.
 */
public class ParametersUtility {
	
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
	
	private static Logger log = LogManager.getLogger();
	
	protected final ArrayList<String> parameters;
	private String parameter_keys_starts_with = "-";
	
	public ParametersUtility() {
		parameters = new ArrayList<>();
	}
	
	public ParametersUtility(String bulk_parameters) {
		this();
		addBulkParameters(bulk_parameters);
	}
	
	public ParametersUtility(Collection<String> parameters) {
		this();
		addParameters(parameters);
	}
	
	/**
	 * Don't touch to current parameters, only parameter_keys_starts_with
	 */
	public ParametersUtility transfertThisConfigurationTo(ParametersUtility new_instance) {
		new_instance.parameter_keys_starts_with = parameter_keys_starts_with;
		return this;
	}
	
	/**
	 * Transfer (clone) current parameters and parameter_keys_starts_with
	 */
	public ParametersUtility importParametersFrom(ParametersUtility previous_instance) {
		log.trace("Import from {}", () -> previous_instance);
		
		parameter_keys_starts_with = previous_instance.parameter_keys_starts_with;
		parameters.clear();
		parameters.addAll(previous_instance.parameters);
		return this;
	}
	
	private Function<String, Stream<Arg>> filterAnTransformParameter = p -> {
		/**
		 * Split >-a -b "c d" e< to [-a, -b, c d, e]
		 */
		return p.trim().chars().mapToObj(i -> (char) i).reduce(new ArrayList<Arg>(), (list, chr) -> {
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
		}).stream();
	};
	
	public ParametersUtility clear() {
		log.trace("Clear all");
		parameters.clear();
		return this;
	}
	
	/**
	 * @return never null
	 */
	public ArrayList<String> getParameters() {
		return parameters;
	}
	
	/**
	 * @param params don't alter params
	 */
	public ParametersUtility addParameters(String... params) {
		if (params == null) {
			throw new NullPointerException("\"params\" can't to be null");
		}
		
		parameters.addAll(Arrays.stream(params).filter(p -> {
			return p != null;
		}).collect(Collectors.toList()));
		
		log.trace("Add parameters: {}", () -> Arrays.stream(params).collect(Collectors.toList()));
		
		return this;
	}
	
	/**
	 * @param params don't alter params
	 */
	public ParametersUtility addParameters(Collection<String> params) {
		if (params == null) {
			throw new NullPointerException("\"params\" can't to be null");
		}
		
		parameters.addAll(params.stream().filter(p -> {
			return p != null;
		}).collect(Collectors.toList()));
		
		log.trace("Add parameters: {}", () -> params);
		
		return this;
	}
	
	/**
	 * @param params transform spaces in each param to new params: "a b c d" -> ["a", "b", "c", "d"], and it manage " but not tabs.
	 */
	public ParametersUtility addBulkParameters(String params) {
		if (params == null) {
			throw new NullPointerException("\"params\" can't to be null");
		}
		
		parameters.addAll(filterAnTransformParameter.apply(params).map(arg -> {
			return arg.toString();
		}).collect(Collectors.toList()));
		
		log.trace("Add parameters: " + params);
		
		return this;
	}
	
	public String toString() {
		return parameters.stream().collect(Collectors.joining(" "));
	}
	
	/**
	 * @param parameters_keys_starts_with "-" by default
	 */
	public ParametersUtility setParametersKeysStartsWith(String parameters_keys_starts_with) {
		parameter_keys_starts_with = parameters_keys_starts_with;
		log.debug("Set parameters key start with: " + parameters_keys_starts_with);
		return this;
	}
	
	/**
	 * @return "-" by default
	 */
	public String getParametersKeysStartsWith() {
		return parameter_keys_starts_with;
	}
	
	protected boolean isArgIsAParametersKey(String arg) {
		return arg.startsWith(parameter_keys_starts_with);
	}
	
	/**
	 * @param parameter_key add "-" in front of param_key if needed
	 */
	protected String conformParameterKey(String parameter_key) {
		if (isArgIsAParametersKey(parameter_key) == false) {
			return parameter_keys_starts_with + parameter_key;
		}
		return parameter_key;
	}
	
	/**
	 * @param parameter_key can have "-" or not (it will be added).
	 * @return For "-param val1 -param val2 -param val3" -> val1, val2, val3 ; null if param_key can't be found, empty if not values for param
	 */
	public List<String> getValues(String parameter_key) {
		if (parameter_key == null) {
			throw new NullPointerException("\"parameter_key\" can't to be null");
		}
		
		final String param = conformParameterKey(parameter_key);
		
		ArrayList<String> result = new ArrayList<>();
		
		boolean has = false;
		for (int pos = 0; pos < parameters.size(); pos++) {
			String current = parameters.get(pos);
			if (current.equals(param)) {
				has = true;
				if (parameters.size() > pos + 1) {
					String next = parameters.get(pos + 1);
					if (isArgIsAParametersKey(next) == false) {
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
	 * @param parameters_key can have "-" or not (it will be added).
	 */
	public boolean removeParameter(String parameters_key, int param_as_this_key_pos) {
		if (parameters_key == null) {
			throw new NullPointerException("\"parameters_key\" can't to be null");
		}
		
		final String param = conformParameterKey(parameters_key);
		
		int to_skip = param_as_this_key_pos + 1;
		
		for (int pos = 0; pos < parameters.size(); pos++) {
			String current = parameters.get(pos);
			if (current.equals(param)) {
				to_skip--;
				if (to_skip == 0) {
					if (parameters.size() > pos + 1) {
						String next = parameters.get(pos + 1);
						if (isArgIsAParametersKey(next) == false) {
							parameters.remove(pos + 1);
						}
					}
					log.trace("Remove parameter: " + parameters.remove(pos));
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * @param parameter_key can have "-" or not (it will be added).
	 * @return true if done
	 */
	public boolean alterParameter(String parameter_key, String new_value, int param_as_this_key_pos) {
		if (parameter_key == null) {
			throw new NullPointerException("\"param_key\" can't to be null");
		} else if (new_value == null) {
			throw new NullPointerException("\"new_value\" can't to be null");
		}
		
		final String param = conformParameterKey(parameter_key);
		
		int to_skip = param_as_this_key_pos + 1;
		
		for (int pos = 0; pos < parameters.size(); pos++) {
			String current = parameters.get(pos);
			if (current.equals(param)) {
				to_skip--;
				if (to_skip == 0) {
					if (parameters.size() > pos + 1) {
						String next = parameters.get(pos + 1);
						if (isArgIsAParametersKey(next) == false) {
							parameters.set(pos + 1, new_value);
						} else {
							parameters.add(pos + 1, new_value);
						}
					} else {
						parameters.add(new_value);
					}
					log.trace("Add parameter: " + new_value);
					return true;
				}
			}
		}
		
		return false;
	}
	
}
