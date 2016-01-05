/*
	
	Copyright 2014 Bortoli Tomas

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
    
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

public class Utilities {
	
	public static void printTimestamp(){
		java.util.Date date= new java.util.Date();
		System.out.println(new Timestamp(date.getTime()));
	}
	
	public static void printTimestampInline(){
		java.util.Date date= new java.util.Date();
		System.out.print(new Timestamp(date.getTime()));
	}
	
	public static Date addDays(Date date, int days)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
    }
}
