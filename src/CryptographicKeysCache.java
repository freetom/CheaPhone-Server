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

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map.Entry;


public class CryptographicKeysCache {
	
	
	private final String cacheFilePath="keysCache.data";
	
	//is important that this value is the same on the server. Inconsistencies will cause fault of communication!!!
	private final short EXPIRATION_CACHE_PERIOD=4;
	
	//hash table for caching. the key is the hash and the hash is the hashish 
	Hashtable<String,Pair<byte[],Date>> htKeys;
	
	File f;
	
	Networking n;
	
	public CryptographicKeysCache(Networking n){
		
		this.n=n;
		
		f=new File(cacheFilePath);
		
		htKeys=(Hashtable<String,Pair<byte[],Date>>)Serialize.loadSerializedObject(f);
		
		if(htKeys==null){
			htKeys=new Hashtable<String,Pair<byte[],Date>>();
			
			System.out.println("New cache for cryptographic keys created.");
		}
		else
			System.out.println("Loaded cache with "+htKeys.size()+" keys");
		
		
		System.out.println("Flushing expired keys..");
		while(flushExpired()) ;
		System.out.println("Done. Actual number of keys: "+htKeys.size());
		
		Serialize.saveObject(htKeys, f);
	}
	
	public void addKey(byte[] key){
		
		try{
			byte[] hash=n.c.Sha1Hash(key);
			Date d= new Date();
			d=Utilities.addDays(d,EXPIRATION_CACHE_PERIOD);
			
			Pair<byte[],Date> p=new Pair<byte[],Date>(key,d);
			
			String s = new String(hash,"ISO-8859-1");
			htKeys.put(s, p);
			
			Serialize.saveObject(htKeys, f);
		}
		catch(Exception e){}
		
		
	}
	
	public byte[] getKey(byte[] hash){
		
		try{
			Pair<byte[],Date> ret=htKeys.get(new String(hash,"ISO-8859-1"));
			
			if(ret==null)
				return null;
			
			Date now=new Date();
			if(now.before(ret.getSecond()))
				return ret.getFirst();
			else{
				htKeys.remove(new String(hash));
				return null;
			}
			}
		catch(Exception e){}
		return null;
	}
	
	public boolean flushExpired(){
		Date now=new Date();
		Collection<Entry<String,Pair<byte[],Date>>> all= htKeys.entrySet();
		
		for(Entry<String, Pair<byte[],Date>> e : all){
			if(now.after(e.getValue().getSecond())){
				htKeys.remove(e.getKey());
				return true;
			}
		}
		return false;
		
	}

}
