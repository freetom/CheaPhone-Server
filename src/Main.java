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
 * Main
 * 
 * Developer: Bortoli Tomas
 * 
 * */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Main {

	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		
		System.out.println("Cheaphone server starting..");
		
		
		while(!SendReceiveSms.IsKeyAttached()){
			System.out.println("Modem keys not found, waiting...");
			Thread.sleep(4000);
		}
		
		SendReceiveSms.initModems();
		
		System.out.println("Modem keys found! Starting server..");
		
		
		System.out.println("Cleaning all sms folders..");
		SendReceiveSms.cleanAllSmsFolders();
		System.out.println("Done.\n");
		
		Utilities.printTimestamp();
		
		Networking n=new Networking();
		while(true){
			n.handleNewClient();
			
		}

	}

}
