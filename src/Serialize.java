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
 * Class that serialize objects into file and read it out.
 * Totally static
 * 
 * Developer: Bortoli Tomas
 * 
 * */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Hashtable;


public class Serialize {
	
	
	public static void saveObject(Object c , File f){
        try
        {
           ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f)); //Select where you wish to save the file...
           oos.writeObject(c); // write the class as an 'object'
           oos.flush(); // flush the stream to insure all of the information was written to 'save_object.bin'
           oos.close();// close the stream
        }
        catch(Exception ex)
        {
           System.out.println("Serialization Save Error : "+ex.getMessage());
           //ex.printStackTrace();
        }
   }
    
	public static Object loadSerializedObject(File f)
   {
       try
       {
           ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
           Object o = ois.readObject();
           return o;
       }
       catch(Exception ex)
       {
    	   System.out.println("Serialization Read Error : "+ex.getMessage());
           //ex.printStackTrace();
       }
       return null;
   }
	
}
