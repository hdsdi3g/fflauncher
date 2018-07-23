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
package tv.hd3g.fflauncher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public class Option {
	
	public final String param;
	public final String descr;
	public final boolean accept_values;
	// private final HashSet<String> accepted_values;
	private final ArrayList<String> alternate_params;
	
	/**
	 * With unlimited values
	 */
	public Option(String param, String descr, boolean accept_values) {
		this.param = param;
		if (param == null) {
			throw new NullPointerException("\"param\" can't to be null");
		}
		this.descr = descr;
		if (descr == null) {
			throw new NullPointerException("\"descr\" can't to be null");
		}
		this.accept_values = accept_values;
		// accepted_values = new HashSet<>(1);
		alternate_params = new ArrayList<>();
	}
	
	public String getDescr() {
		return descr;
	}
	
	public String getParam() {
		return param;
	}
	
	/*
	 * With limited values
	 */
	/*public Option(String param, String descr, Collection<String> accepted_values) {
		this(param, descr, true);
		//this.accepted_values.addAll(accepted_values);
	}*/
	
	/*
	 * With limited values
	 */
	/*public Option(String param, String descr, String... accepted_values) {
		this(param, descr, true);
		this.accepted_values.addAll(Arrays.stream(accepted_values).filter(v -> v != null).distinct().collect(Collectors.toSet()));
	}*/
	
	/*
	 * @return unmodifiableSet
	 */
	/*public Set<String> getAcceptedValues() {
		return Collections.unmodifiableSet(accepted_values);
	}*/
	
	/**
	 * @return unmodifiableList
	 */
	public List<String> getAlternateParams() {
		return Collections.unmodifiableList(alternate_params);
	}
	
	public Option setAlternateParams(String... params) {
		alternate_params.addAll(Arrays.stream(params).filter(v -> v != null).filter(v -> {
			return alternate_params.contains(v) == false;
		}).distinct().collect(Collectors.toList()));
		return this;
	}
	
	/**
	 * Simple switch, with no value
	 */
	public Option(String param, String descr) {
		this(param, descr, false);
	}
	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (param == null ? 0 : param.hashCode());
		return result;
	}
	
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Option other = (Option) obj;
		if (param == null) {
			if (other.param != null) {
				return false;
			}
		} else if (!param.equals(other.param)) {
			return false;
		}
		return true;
	}
	
	public OptionValue createValue(String v) {
		return new OptionValue(v, this);
	}
	
	public OptionValue prepareEmptyValue() {
		return new OptionValue(this);
	}
	
	public OptionSwitch createSwitch() {
		return new OptionSwitch(this);
	}
	
	public class OptionSwitch {
		
		public final Option option;
		protected boolean switched;
		
		protected OptionSwitch(Option option) {
			this.option = option;
			switched = false;
		}
		
		public synchronized boolean isSwitched() {
			return switched;
		}
		
		public synchronized OptionSwitch setSwitched(boolean switched) {
			this.switched = switched;
			return this;
		}
	}
	
	public class OptionValue extends OptionSwitch {
		
		private String v;
		
		private OptionValue(String v, Option option) {
			super(option);
			if (accept_values == false) {
				throw new RuntimeException("Can't accept values for " + param);
			}
			set(v);
		}
		
		private OptionValue(Option option) {
			super(option);
			if (accept_values == false) {
				throw new RuntimeException("Can't accept values for " + param);
			}
		}
		
		/**
		 * @return can be null
		 */
		public synchronized String get() {
			return v;
		}
		
		/**
		 * Forbidden
		 */
		public synchronized OptionSwitch setSwitched(boolean switched) {
			throw new RuntimeException("Can't change value with setSwitched");
		}
		
		public synchronized OptionValue empty() {
			v = null;
			switched = false;
			return this;
		}
		
		public synchronized OptionValue set(String v) {
			if (v == null) {
				throw new NullPointerException("\"v\" can't be null");
			} else if (v.equals("")) {
				empty();
				return this;
			} /*else if (accepted_values.isEmpty() == false & accepted_values.contains(v) == false) {
				throw new RuntimeException("Invalid value \"" + v + "\" for " + param + " (only " + accept_values + " are correct)");
				}*/
			this.v = v;
			switched = true;
			return this;
		}
		
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (v == null ? 0 : v.hashCode());
			return result;
		}
		
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			OptionValue other = (OptionValue) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (v == null) {
				if (other.v != null) {
					return false;
				}
			} else if (!v.equals(other.v)) {
				return false;
			}
			return true;
		}
		
		private Option getOuterType() {
			return Option.this;
		}
		
	}
	
}
