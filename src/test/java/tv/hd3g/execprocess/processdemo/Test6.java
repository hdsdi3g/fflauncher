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

import java.util.concurrent.Executors;

import tv.hd3g.execprocess.ExecProcessTest;

public class Test6 {
	
	public static void main(String[] args) throws Exception {
		ExecProcessTest.createExec(Test5.class).start(Executors.newSingleThreadExecutor()).waitForEnd();
	}
	
}
