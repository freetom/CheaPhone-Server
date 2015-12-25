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

/* 
 * Server BestOffer 
 * 
 * 
 * The cache that contains the association between numbers and operators
 * It handles initialization, added of values, and retrievement of it. 
 * Also the expiration time of the data, setted by a constant.
 * 
 * 
 * Developer: Bortoli Tomas
 * 
 * */

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

public class CacheOfNumbersToOperators implements Serializable {
	
	private final String cacheFilePath="cacheNumbersToOperators.data";
	private final int EXPIRATION_CACHE_PERIOD=90;
	
	private Hashtable<String,Pair<String,Date>> htCache;
	private File cacheFile;
	
	public CacheOfNumbersToOperators(){
		
		cacheFile=new File(cacheFilePath);
		if(!cacheFile.exists())
			System.out.println("File of the cache does not exist, it will be created.");
		
		htCache=(Hashtable<String,Pair<String,Date>>)Serialize.loadSerializedObject(cacheFile);
		
		if(htCache==null){
			htCache=new Hashtable<String,Pair<String,Date>>();
			System.out.println("New empty cache generated.");
		}
		else
			System.out.println("Cache with "+htCache.size()+" elements loaded.");
		
		
	}
	
	public void addValue(String key, String value){
		try{
			Date d= new Date();
			d=Utilities.addDays(d,EXPIRATION_CACHE_PERIOD);
			htCache.put(key, new Pair(value,d));
			
			Serialize.saveObject(htCache, cacheFile );
		}
		catch(Exception e){}
	}
	
	public String getValue(String key){
		Pair<String,Date> v=htCache.get(key);
		if(v!=null){
			Date now=new Date();
			if(now.before(v.getSecond()))
				return new String(v.getFirst());
			else
				htCache.remove(key);
		}
		
		return null;
	}
	
	
	
	
}
