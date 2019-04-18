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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Deprecated
public class DeprecatedCommandLineProcessor {

	private final String start_var_tag;
	private final String end_var_tag;

	/**
	 * Use "<%" and "%>" by default
	 */
	public DeprecatedCommandLineProcessor() {
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

	public DeprecatedCommandLineProcessor(final String start_var_tag, final String end_var_tag) {
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

	/**
	 * @param full_command_line_with_vars MUST containt an executable reference (exec name or path)
	 */
	public DeprecatedCommandLine createCommandLine(final String full_command_line_with_vars) {
		if (full_command_line_with_vars == null) {
			throw new NullPointerException("\"full_command_line_with_vars\" can't to be null");
		}
		return new DeprecatedCommandLine(full_command_line_with_vars);
	}

	/*
	 * @param params_with_vars must NOT containt an executable reference
	 */
	/*public CommandLine createCommandLine(String exec_name, String params_with_vars) {
		if (params_with_vars == null) {
			throw new NullPointerException("\"full_command_line_with_vars\" can't to be null");
		}
		return new CommandLine(exec_name + " " + params_with_vars);
	}*/

	public DeprecatedCommandLine createEmptyCommandLine(final String exec_name) {
		return new DeprecatedCommandLine(exec_name);
	}

	/**
	 * @param param like
	 * @return true if like "<%myvar%>"
	 */
	public boolean isTaggedParameter(final String param) {
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
	public String extractVarNameFromTaggedParameter(final String param) {
		if (isTaggedParameter(param) == false) {
			return null;
		}
		if (param.length() == start_var_tag.length() + end_var_tag.length()) {
			return null;
		}
		return param.substring(start_var_tag.length(), param.length() - end_var_tag.length());
	}

	public class DeprecatedCommandLine extends DeprecatedParametersUtility implements Cloneable {

		private final String exec_name;

		private DeprecatedCommandLine(final String full_command_line_with_vars) {
			super(full_command_line_with_vars);

			if (parameters.isEmpty()) {
				throw new RuntimeException("Empty params");
			}

			exec_name = parameters.get(0);
			parameters.remove(0);
		}

		/**
		 * Usable only for clone
		 */
		private DeprecatedCommandLine(final DeprecatedCommandLine referer) {
			exec_name = referer.exec_name;
			importParametersFrom(referer);
		}

		@Override
		public DeprecatedCommandLine clone() {
			return new DeprecatedCommandLine(this);
		}

		public String getExecName() {
			return exec_name;
		}

		/**
		 * @return var_name
		 */
		public String addVariable(final String var_name) {
			addParameters(start_var_tag + var_name + end_var_tag);
			return var_name;
		}

		/*
		 * @return var_name
		 */
		/*public String prependVariable(String var_name) {
			prependParameters(start_var_tag + var_name + end_var_tag);
			return var_name;
		}*/

		/**
		 * @return true if the update is done
		 */
		public boolean injectParamsAroundVariable(final String var_name, final Collection<String> add_before, final Collection<String> add_after) {
			if (var_name == null) {
				throw new NullPointerException("\"var_name\" can't to be null");
			} else if (add_before == null) {
				throw new NullPointerException("\"add_before\" can't to be null");
			} else if (add_after == null) {
				throw new NullPointerException("\"add_after\" can't to be null");
			}

			final AtomicBoolean is_done = new AtomicBoolean(false);

			final List<String> new_parameters = parameters.stream().reduce(Collections.unmodifiableList(new ArrayList<String>()), (list, arg) -> {
				if (isTaggedParameter(arg)) {
					final String current_var_name = extractVarNameFromTaggedParameter(arg);
					if (current_var_name.equals(var_name)) {
						is_done.set(true);
						return Stream.concat(list.stream(), Stream.concat(Stream.concat(add_before.stream(), Stream.of(arg)), add_after.stream())).collect(Collectors.toUnmodifiableList());
					}
				}

				return Stream.concat(list.stream(), Stream.of(arg)).collect(Collectors.toUnmodifiableList());
			}, LIST_COMBINER);

			parameters.clear();
			parameters.addAll(new_parameters);

			return is_done.get();
		}

		/**
		 * @return with var names
		 */
		@Override
		public String toString() {
			return exec_name + " " + super.toString();
		}

		public ProcessedCommandLine process() {
			return process(Collections.emptyMap(), false);
		}

		public ProcessedCommandLine process(final Map<String, String> vars_to_inject, final boolean remove_params_if_no_var_to_inject) {
			ProcessedCommandLine new_instance;
			if (remove_params_if_no_var_to_inject) {
				new_instance = new ProcessedCommandLine(parameters.stream().reduce(Collections.unmodifiableList(new ArrayList<String>()), (list, arg) -> {
					if (isTaggedParameter(arg)) {
						final String var_name = extractVarNameFromTaggedParameter(arg);
						if (vars_to_inject.containsKey(var_name)) {
							return Stream.concat(list.stream(), Stream.of(vars_to_inject.get(var_name))).collect(Collectors.toUnmodifiableList());
						} else {
							if (list.isEmpty()) {
								return list;
							} else if (isArgIsAParametersKey(list.get(list.size() - 1))) {
								return list.stream().limit(list.size() - 1).collect(Collectors.toUnmodifiableList());
							} else {
								return list;
							}
						}
					} else {
						return Stream.concat(list.stream(), Stream.of(arg)).collect(Collectors.toUnmodifiableList());
					}
				}, LIST_COMBINER));
			} else {
				new_instance = new ProcessedCommandLine(parameters.stream().map(arg -> {
					final String var_name = extractVarNameFromTaggedParameter(arg);
					if (var_name != null) {
						return vars_to_inject.get(var_name);
					} else {
						return arg;
					}
				}).filter(arg -> arg != null).collect(Collectors.toUnmodifiableList()));
			}

			transfertThisConfigurationTo(new_instance);
			return new_instance;

		}

		public class ProcessedCommandLine extends DeprecatedParametersUtility {

			private ProcessedCommandLine(final List<String> processed_params) {
				super(processed_params);
			}

			@Override
			public String toString() {
				return exec_name + " " + super.toString();
			}

			public String getExecName() {
				return exec_name;
			}

		}

	}

	private static final BinaryOperator<List<String>> LIST_COMBINER = (list1, list2) -> Stream.concat(list1.stream(), list2.stream()).collect(Collectors.toUnmodifiableList());

}
