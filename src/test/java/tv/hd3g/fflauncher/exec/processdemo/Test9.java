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
package tv.hd3g.fflauncher.exec.processdemo;

import java.util.Scanner;

public class Test9 {
	
	public static final String QUIT = "q";
	
	public static void main(String[] args) throws InterruptedException {
		Scanner s = new Scanner(System.in);
		while (s.hasNext()) {
			String line = s.next();
			if (line.equals(QUIT)) {
				break;
			}
		}
		s.close();
	}
	
}
