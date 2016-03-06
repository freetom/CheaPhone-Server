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
 * This class handles all the requests and responses from the GSM modems.
 * Used to send an sms, and receive the answer directly converted into an operator.
 * It also has 2 public method to execute command in shell.
 * Totally static
 * 
 * Developer: Bortoli Tomas
 * 
 * */

import java.io.BufferedReader;
import java.io.IOException;


public class SendReceiveSms {
	 
	/*
	 * This class does support multiples GSM/GPRS modems; 
	 * used to convert numbers into operators, using 456 TIM's service.
	 * To have gammu configured to work with this code, you need a configuration on the 
	 * @configFileLocation with different sections, one for each modem, starting from 0 to n-1. 
	 * The number of internet keys (or GSM modems) is hard-coded, for now.
	 * 
	 * Bortoli Tomas
	 * 
	 * /
	
	/**
	 * How many modems do we have connected to the server?
	 */
	static final short nOfModems=1;
	
	/**
	 * Mutexes for GSM modems. Mutual exclusion is required for 2 reasons:
	 * -Cannot launch more instance of gammu simultaneously on the same device
	 * -Cannot send more sms to 456 and wait for more answers. Because answers do 
	 * not contain information on the request. So must be made one by one.
	 */
	static MutualExclusion[] me=new MutualExclusion[nOfModems];
	
	static int counter=0;
	final static int delay_wait_sms_ms=600000; // equals to 10 minutes of maximum delay
	
	public static String TIM="TIM";
	public static String VODAFONE="VODAFONE";
	public static String WIND="WIND";
	public static String TRE="3";
	public static String COOP="COOP";
	public static String POSTE_MOBILE="POSTE MOBILE";
	public static String TISCALI="TISCALI";
	
	/**
	 * Value returned when some error occurs on the parsing of the input number 
	 * or some error from the GSM modem and the server program
	 */
	public static String INVALID="INVALID";
	
	/**
	 * Returned when the cache must save the number as INVALID;
	 * the value is used to prevent infinite requests of the same number
	 * (maybe a deactivated number) that never goes into cache because 456 
	 * bad answers.
	 * Also used when real timeout occurs on 456 response
	 */
	public static String TIMEOUT="TIMEOUT";
	
	/**
	 * the location of the configuration file for gammu
	 */
	private final static String configFileLocation="\"/root/.gammurc\"";
	
	/**
	 * initialize the GSM modems mutexes
	 */
	public static void initModems(){
		for(int i=0;i<me.length;i++)
			me[i]=new MutualExclusion();
	}
	
	public static boolean IsKeyAttached() throws IOException{
		boolean res=true;
		
		for(int i=0;i<me.length;i++){
			String ret=execCmd(new String[]{"/bin/sh", "-c", "gammu -c "+configFileLocation+" "+selectKey(i)+" identify"});
			if(ret.contains("Huawei"))
				res&=true;
			else
				res&=false;
		}
		
		return res;
	}
	
	private static int getFirstFreeModem() throws InterruptedException{
		boolean ok=false; int i=0;
		while(!ok){
			i%=me.length;
			if(!me[i].islock()) ok=true;
			else{
				i++;
				Thread.sleep(300);
			}
		}
		return i;
	}
	
	private static void commandDelete(String[] command) throws IOException, InterruptedException{
		while(true){
			String result=execCmd(command);
			System.out.println(command[2]+"=>"+result);
			//if the "phone" does not answer in time, retry the command 
			if(result.contains("Nessuna risposta")) Thread.sleep(500);
			else break;
			
		}
	}
	public static void cleanAllSmsFolders(){
		try{
			for(int i=0;i<me.length;i++){
				commandDelete(new String[]{"/bin/sh", "-c", "gammu -c "+configFileLocation+" "+selectKey(i)+" deleteallsms 1"});
				commandDelete(new String[]{"/bin/sh", "-c", "gammu -c "+configFileLocation+" "+selectKey(i)+" deleteallsms 2"});
				commandDelete(new String[]{"/bin/sh", "-c", "gammu -c "+configFileLocation+" "+selectKey(i)+" deleteallsms 3"});
				commandDelete(new String[]{"/bin/sh", "-c", "gammu -c "+configFileLocation+" "+selectKey(i)+" deleteallsms 4"});
			}
		}
		catch(Exception e){
			
		}
	}
	
	private static String selectKey(int i){
		return "-s "+i;
	}
	
	//private static String[] prefixes={"01","02","03","05","06","07","08","09"};
	private static boolean startWithPrefix(String number){
		
		if(number.charAt(0)=='0' && (number.charAt(1)>='1' && number.charAt(1)<='9'))
			return true;
		else
			return false;
		
	}
	
	
	public static String numberToOperator(String number) throws IOException, InterruptedException{
		System.out.println("into \"numberToOperator\"");
		
		int index=getFirstFreeModem();
		//Enter into the critical region, lock the mutex.
		me[index].lock();
		
		
		
		String res=null;
		try{
			//If the number is not 10 ciphers, return invalid operator. 
			//No number can be longer than 13 also with +39 before the number (ITALY)
			//And I assume that mobile numbers arrive here with 10 digits, because client parse them
			if(number.length()!=10 || startWithPrefix(number)){
				me[index].unlock();
				return INVALID;
			}
			
			//Check if the string in input is composed only by a series of numbers.
			for(int i=0;i<number.length();i++){
				if(number.charAt(i)<'0' || number.charAt(i)>'9'){
					me[index].unlock();
					return INVALID;
				}
			}
			
			
			boolean ok=false;
			while(!ok){
				res=execCmd(new String[]{"/bin/sh", "-c","echo \"info "+number+"\" | gammu -c "+configFileLocation+" "+selectKey(index)+" sendsms TEXT 456"});
				if(!res.toLowerCase().contains("error") /*&& !res.toLowerCase().contains("timeout")*/) ok=true;
				else{
					//System.out.println("Error in sending sms");
					
					me[index].unlock();
					return INVALID;
				}
			}
			ok=false;
			
			//System.out.println("Sms sended successfully, now wait for answer");
			
			//while the message isn't arrived, loop this code. If it does not arrive in 5 minutes, return timeout, that will set the value INVALID for the number
			int counter=0;
			while(!ok){
				
				//System.out.println("waiting for answer..");
				
				res=execCmd(new String[]{"/bin/sh", "-c","gammu -c "+configFileLocation+" "+selectKey(index)+" getallsms"});
				
				if(res.contains("Remote number        : \"456\"")) break;
				
				counter+=10000;
				Thread.sleep(10000);
				System.out.println("counter: "+counter);
				if(counter>=delay_wait_sms_ms){
					commandDelete(new String[]{"/bin/sh", "-c", "gammu -c "+configFileLocation+" "+selectKey(index)+" deleteallsms 1"});
					commandDelete(new String[]{"/bin/sh", "-c", "gammu -c "+configFileLocation+" "+selectKey(index)+" deleteallsms 3"});
					me[index].unlock();
					return TIMEOUT;
				}
			}
			
			counter++;
			
			//Delete all the messages
			if(counter==50){
				counter=0;
				execCmd(new String[]{"/bin/sh", "-c", "gammu -c "+configFileLocation+" "+selectKey(index)+" deleteallsms 2"});
				execCmd(new String[]{"/bin/sh", "-c", "gammu -c "+configFileLocation+" "+selectKey(index)+" deleteallsms 4"});
				
			}
			
		}
		catch(Exception e){
			System.out.println("Fatal Exception in sendAndReceiveSms");
			System.out.println(e.toString());
		}
		finally{	
			commandDelete(new String[]{"/bin/sh", "-c", "gammu -c "+configFileLocation+" "+selectKey(index)+" deleteallsms 1"});
			commandDelete(new String[]{"/bin/sh", "-c", "gammu -c "+configFileLocation+" "+selectKey(index)+" deleteallsms 3"});
		}
		
		//Exit from the critical region, free the mutex
		me[index].unlock();
		
		//filter the first 7th rows, in this way everything not belonging to the content of the message will be erased.
		//If there are not 7th rows in the whole string, exception (this might not happen)
		String[] s=res.split("\n"); res="";
		for(int j=7;j<s.length;j++) res+=s[j];
		
		
		res=res.toUpperCase();
		
		if(res.contains("IL SERVIZIO NON E' AL MOMENTO DISPONIBILE.")){
			Thread.sleep(60000);
			return numberToOperator(number);
		}
		
		/**
		 * Check if there is any incongruence in the response string
		 */
		counter=0;
		if(res.contains(TIM))
			counter++;
		else if(res.contains(WIND))
			counter++;
		else if(res.contains(VODAFONE))
			counter++;
		else if(res.contains(TRE))
			counter++;
		else if(res.contains(COOP))
			counter++;
		else if(res.contains(POSTE_MOBILE))
			counter++;
		else if(res.contains(TISCALI))
			counter++;
		
		if(counter>1){
			return numberToOperator(number);
		}
		
		/**
		 * Return the correct operator
		 */
		if(res.contains(TIM))
			return TIM;
		else if(res.contains(WIND))
			return WIND;
		else if(res.contains(VODAFONE))
			return VODAFONE;
		else if(res.contains(TRE))
			return TRE;
		else if(res.contains(COOP))
			return COOP;
		else if(res.contains(POSTE_MOBILE))
			return POSTE_MOBILE;
		else if(res.contains(TISCALI))
			return TISCALI;
		else{
			System.out.println(res);
			return TIMEOUT;
		}
		
	}
	
	public static String execCmd(final String[] cmd) throws java.io.IOException {
        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
	

}
