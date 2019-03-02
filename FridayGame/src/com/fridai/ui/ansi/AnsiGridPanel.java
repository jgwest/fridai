/**
 * Copyright (C) 2019  Jonathan West
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fridai.ui.ansi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiString;

/** This class is part of the Friday UI, and allows you to create a horizontal table
 * with individual horizontal cells that expand based on the size of the content they contain.
 * 
 * This class contains the contents of the individual cell of the table, which is the packaged into
 * a table by AnsiGrid.
 * 
 * This class is ANSI-aware. */
public class AnsiGridPanel {

	private final List<String> panelContents = new ArrayList<>();
	
	public AnsiGridPanel() {
	}
	
	public void addLineToPanel(Ansi ansiStr) {
		
		panelContents.add(ansiStr.toString());
	}

	public void addLineToPanel(String str) {
		
		panelContents.add(str);
	}
	
	public void addLinesToPanel(List<String> strList) {
		panelContents.addAll(strList);
	}
	
	public int getMaxStrippedLength() {
		
		List<Integer> l = panelContents.stream().map(e -> new AnsiString(e).length()).sorted().collect(Collectors.toList());
		
		if(l.isEmpty()) {
			return 0;
		}
		
		return l.get(l.size()-1);
		
		
	}
	
	public List<String> renderWithWidth(int width) {
		List<String> result = new ArrayList<>();
		
		for(String line : panelContents) {

			line = padWithSpaces(line, width);
			result.add(line);
			
		}
		
		return result;
		
	}
	
	public static int getStrippedWidth(String str) {
		return new AnsiString(str).length();
	}
	
	public static String padWithSpaces(String line, int width) {
		while(getStrippedWidth(line) < width) {
			line = line +" ";
		}
		return line;
		
	}
}
