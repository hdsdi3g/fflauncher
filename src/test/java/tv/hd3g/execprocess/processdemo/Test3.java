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
package tv.hd3g.execprocess.processdemo;

public class Test3 {
	
	public static final String expected_out = "OUT";
	public static final String expected_err = "ERR";
	public static final String expected_in = "InputValue\\1";
	
	public static final int exit_ok = 1;
	public static final int exit_bad_param_len = 2;
	public static final int exit_bad_param_value = 3;
	public static final int exit_bad_env = 4;
	public static final int exit_bad_import_env = 5;
	
	public static final String ENV_KEY = "EnvKey";
	public static final String ENV_VALUE = "EnvValue";
	
	public static void main(String[] args) {
		System.out.println(expected_out);
		System.err.println(expected_err);
		
		if (System.getenv().getOrDefault(ENV_KEY, "").equals(ENV_VALUE) == false) {
			System.exit(exit_bad_env);
		}
		
		if (System.getenv().containsKey("PATH") == false) {
			System.exit(exit_bad_import_env);
		}
		
		if (args.length != 1) {
			System.exit(exit_bad_param_len);
		} else {
			if (args[0].equals(expected_in) == false) {
				System.exit(exit_bad_param_value);
			} else {
				System.exit(exit_ok);
			}
		}
	}
	
}
