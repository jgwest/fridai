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

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.MAGENTA;

import java.util.ArrayList;
import java.util.List;

import org.fusesource.jansi.Ansi;

import com.fridai.Card;
import com.fridai.GameState;
import com.fridai.PirateCard;
import com.fridai.PirateGameState;
import com.fridai.util.MapCards;

/** Utility class used to represent cards and game states with ANSI coloured text. */
public class AnsiCards {

	public static String asHazardCard(Card c, GameState gs /* may be null */) {
		String title = "";

		title = ansi().fgBrightGreen().a(c.getHazardTitle()).reset().toString()+" ";
		
		Ansi hazardValues = ansi();
		{
			int effectivePhase = gs.calculateEffectivePhase();
			
			for(int index = 0; index < c.getHazardValues().length; index++) {
				String val = c.getHazardValues()[index]+" ";
				if(index == effectivePhase) {
					hazardValues = hazardValues.fg(MAGENTA).bold().a(val).reset();
				} else {
					hazardValues = hazardValues.a(val);
				}
			}
		}

		
		title += "- Free cards: "+ansi().fgBrightCyan().a(c.getFreeCards()).reset().toString();
		
		
		String cStr = ansi().a("["+hazardValues+"] "+title+" ").toString();

		return cStr;

	}
	
	public static String asFightingCard(Card c, GameState gs /* may be null */) {
		String title = "";
		if(c.getFightingValue() < 0) {
			title = ansi().fgBrightRed().a(c.getTitle()).reset().toString();
		} else if(c.getFightingValue() == 0) {
			title = ansi().fgBrightDefault().a(c.getTitle()).reset().toString();
		} else if(c.getFightingValue() > 0) {
			title = ansi().fgBrightGreen().a(c.getTitle()).reset().toString();
		}
		
		String fightingVal = ""+c.getFightingValue();
		while(fightingVal.length() < 2) { fightingVal = " "+fightingVal; }
		
		if(c.getDifficulty() != null && c.getDifficulty() != Card.Difficulty.NONE) {
			if(c.getDifficulty() == Card.Difficulty.NORMAL) {
				title = ansi().fgBrightYellow().a("[!]").reset().toString()+"  "+title;
			} else {
				title = ansi().fgBrightRed().a("[!]").reset().toString()+"  "+title;	
			}
			
		}
		
		boolean doubled = false;
		if(gs != null) {
			MapCards m = gs.getLhsOrRhsFightingCardDoubled();
			if(m != null) {
				doubled = m.get(c.getPhysicalCardId()) == true;
			}
			
		}
		
		String cStr = ansi().a("["+fightingVal+"] "+title+" -> "+c.getAbility().name()).toString();
		
		if(doubled) {
			cStr += ansi().fgBrightYellow().a(" (x2) ").reset();
		}

		cStr += "   @"+c.getPhysicalCardId();
		
		return cStr;
	}
	
	public static String asPirateFightingCard(Card c, PirateGameState gs /* may be null */) {
		String title = "";
		if(c.getFightingValue() < 0) {
			title = ansi().fgBrightRed().a(c.getTitle()).reset().toString();
		} else if(c.getFightingValue() == 0) {
			title = ansi().fgBrightDefault().a(c.getTitle()).reset().toString();
		} else if(c.getFightingValue() > 0) {
			title = ansi().fgBrightGreen().a(c.getTitle()).reset().toString();
		}
		
		String fightingVal = ""+c.getFightingValue();
		while(fightingVal.length() < 2) { fightingVal = " "+fightingVal; }
		
		if(c.getDifficulty() != null && c.getDifficulty() != Card.Difficulty.NONE) {
			if(c.getDifficulty() == Card.Difficulty.NORMAL) {
				title = ansi().fgBrightYellow().a("[!]").reset().toString()+"  "+title;
			} else {
				title = ansi().fgBrightRed().a("[!]").reset().toString()+"  "+title;	
			}
			
		}
		
		boolean doubled = false;
		if(gs != null) {
			MapCards m = gs.getLhsOrRhsFightingCardDoubled();
			if(m != null) {
				doubled = m.get(c.getPhysicalCardId()) == true;
			}
			
		}
		
		String cStr = ansi().a("["+fightingVal+"] "+title+" -> "+c.getAbility().name()).toString();
		
		if(doubled) {
			cStr += ansi().fgBrightYellow().a(" (x2) ").reset();
		}

		cStr += "   @"+c.getPhysicalCardId();
		
		return cStr;
	}

	
	public static List<String> asHazardInList(Card c, GameState gs, Integer optionalNumber) {
		List<String> lines = new ArrayList<>();
		
		lines.add(ansi().fgBrightGreen().a( (optionalNumber != null ?  (optionalNumber+1)+": " : "")  +c.getHazardTitle()).reset().toString()    );
		lines.add("Free cards: "+ansi().fgBrightCyan().a(""+c.getFreeCards()).reset().toString()  );
						
		Ansi hazardValues = ansi();
		{
			int effectivePhase = gs.calculateEffectivePhase();
			
			for(int index = 0; index < c.getHazardValues().length; index++) {
				String val = c.getHazardValues()[index]+" ";
				if(index == effectivePhase) {
					hazardValues = hazardValues.fg(MAGENTA).bold().a(val).reset();
				} else {
					hazardValues = hazardValues.a(val);
				}
			}
		}
		
		lines.add("Hazard values: "+hazardValues.toString());
		
		lines.add("");
		lines.add("Other:");
		lines.add("- Ability: "+c.getAbility().name());
		lines.add("- Fighting value: " + c.getFightingValue() );
		lines.add("- Title: " + c.getTitle() );

		return lines;
	}

	public static List<String> simplePirateTextInList(PirateCard c) {
		
		List<String> lines = new ArrayList<>();
		
		lines.add("Pirate:");
		lines.add( ansi().a("Hazard value: ").fg(MAGENTA).a(c.getHazardValue()).reset().toString());
		
		lines.add(ansi().a("Free cards: ").fgBrightCyan().a(c.getFreeCards()).reset().toString());
		
		lines.add(ansi().a("Ability: "+c.getAbility().name()).reset().toString());
		

		return lines;
	}

	
	public static String simplePirateText(PirateCard c) {
		
		Ansi a = ansi().a("Pirate -  Hazard value: ").fg(MAGENTA).a(c.getHazardValue()).reset().a("  Free cards: ").fgBrightCyan().a(c.getFreeCards()).reset().a("  Ability: "+c.getAbility().name());
		
		return a.toString();
	}
}
