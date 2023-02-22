/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2023 MaMuT development team.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.mamut.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TextFileAccess {
	public static BufferedReader openFileRead(final File file) {
		BufferedReader inputFile;
		try {
			inputFile = new BufferedReader(new FileReader(file));
		} catch (final IOException e) {
			System.out.println("TextFileAccess.openFileRead(): " + e);
			inputFile = null;
		}
		return (inputFile);
	}

	public static BufferedReader openFileRead(final String fileName) {
		BufferedReader inputFile;
		try {
			inputFile = new BufferedReader(new FileReader(fileName));
		} catch (final IOException e) {
			System.out.println("TextFileAccess.openFileRead(): " + e);
			inputFile = null;
		}
		return (inputFile);
	}

	public static PrintWriter openFileWrite(final File file) {
		PrintWriter outputFile;
		try {
			outputFile = new PrintWriter(new FileWriter(file));
		} catch (final IOException e) {
			System.out.println("TextFileAccess.openFileWrite(): " + e);
			outputFile = null;
		}
		return (outputFile);
	}

	public static PrintWriter openFileWrite(final String fileName) {
		PrintWriter outputFile;
		try {
			outputFile = new PrintWriter(new FileWriter(fileName));
		} catch (final IOException e) {
			System.out.println("TextFileAccess.openFileWrite(): " + e);
			outputFile = null;
		}
		return (outputFile);
	}

	public static PrintWriter openFileWriteEx(final File file) throws IOException {
		return new PrintWriter(new FileWriter(file));
	}

	public static BufferedReader openFileReadEx(final File file) throws IOException {
		return new BufferedReader(new FileReader(file));
	}

	public static PrintWriter openFileWriteEx(final String fileName) throws IOException {
		return new PrintWriter(new FileWriter(fileName));
	}

	public static BufferedReader openFileReadEx(final String fileName) throws IOException {
		return new BufferedReader(new FileReader(fileName));
	}
}
