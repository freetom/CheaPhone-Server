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
 * Class that handles the clients that connects spawning a per client thread.
 * It also handle the protocol between client best offer and server, the initial 
 * key exchange, and the "big" loop
 * This class is written to drive the server side of the scene
 * 
 * Developer: Bortoli Tomas
 * 
 * */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.crypto.spec.SecretKeySpec;

/*
 * Class that handles clients. Every client request from a network perspective.
 * 
 * Bortoli Tomas
 * 
 * */
class ThreadDispatchClients implements Runnable{
	
	
	static final short MAX_CLIENTS_N=50;
	
	//Static variable that count how many clients are attached to the server
	static short nOfClients=0;
	
	//Socket of the connection client-server
	Socket s;
	
	//Reference class
	Networking n;
	
	//Object used to handle cryptography
	Cryptography c;
	
	//Local variables, stream for read and write data from and to the socket
	DataOutputStream dataOut;
	DataInputStream dataIn;
	
	
	static short readers=0;
	
	//Constructor, gets the socket
	public ThreadDispatchClients(Socket s, Networking n) throws SocketException{
		this.s=s;
		this.n=n;
		
		//10 sec of max timeout
		this.s.setSoTimeout(10000);
		
		
		c=new Cryptography();
		
		nOfClients++;
		
		//System.out.println("Got connection from: "+s.getRemoteSocketAddress().toString());
		//Utilities.printTimestamp();
		
		
		
	}
	
	private static void print_conn_state(){
		System.out.print("n of connections: "+nOfClients+"/"+MAX_CLIENTS_N+"\t\t");
		Utilities.printTimestampInline();
		System.out.print("\r");
		System.out.flush();
	}
	private static String randomPadding(){
		Random r=new Random();
		int len=(Math.abs(r.nextInt())%48)+16;
		
		char[] pad=new char[len];
		for(int i=0;i<len;i++)
			pad[i]=(char)((Math.abs(r.nextInt())%94)+32);
		
		return String.copyValueOf(pad);
	}
	
	//Function that handle the sending of a message, including cryptography, and signing (does not handle exception, must be handled from external)
	private void sendMessage(String message) throws Exception{
		
		message=randomPadding()+Networking.SEPARATOR+message+Networking.SEPARATOR+randomPadding();
		
		//If not multiple of 32, fill with padding ___	AES-256-bit-blocks (128-bits is the correct size), 256 is "expanded"
		int rest=message.length()%32;
		if(rest!=0)	rest=32-rest;
		while(rest>0){
			message+=Networking.filler;
			rest--;
		}
		
		//encrypt the message
		byte[] enc=c.encryptAES(message.getBytes());
		
		//Send the encrypted message
		dataOut.writeInt(enc.length);
	    dataOut.write(enc);
	    
		//Send the sign
		sendMessageNotEncrypted(c.sign(message.getBytes()));
		
	}
	
	private void sendMessageNotEncrypted(byte[] message) throws Exception{
		
		dataOut.writeInt(message.length);
	    dataOut.write(message);
	}
	
	//Function that receive a message, decrypt it and return it!
	private String receiveMessage() throws Exception{
		
		byte[] message=null;
		int length = dataIn.readInt();                    // read length of incoming message
		if(length>0) {
		    message = new byte[length];
		    dataIn.readFully(message, 0, message.length); // read the message
		    
		}
		
		message=(c.decryptAES(message));
		
		String msg=new String(message);
		
		return msg.substring(msg.indexOf(Networking.SEPARATOR)+1,msg.lastIndexOf(Networking.SEPARATOR));
		
		
	}
	
	//Function that receive a non encrypted message
	private byte[] receiveMessageNotEncrypted() throws Exception{
		
		byte[] message=null;
		int length = dataIn.readInt();                    // read length of incoming message
		if(length>0) {
		    message = new byte[length];
		    dataIn.readFully(message, 0, message.length); // read the message
		    
		}
		
		return (message);
		
	}
	
	
	private void byeMessage(){
		//one client less connected to the server
		nOfClients--;
		
		//System.out.println("Connection terminated, "+nOfClients+" remained");
		//Utilities.printTimestamp();
		print_conn_state();
	}
	
	private Boolean keyCaching() throws Exception{
		
		boolean ret=false;
		
		//receive the hash of the key
		byte[] hash=receiveMessageNotEncrypted();
		
		//if the hash it totally null, no key was stored in the client
		for(int i=0;i<hash.length;i++){
			if(hash[i]!=0) {ret=true; break;}
		}
		
		//if is a valid hash
		if(ret){
			//retrieve the key from the db
			byte[] key=n.keysCache.getKey(hash);
			
			//if valid key really exist, setup it!
			if(key!=null){
				c.aesKey= new SecretKeySpec(key, 0, key.length, "AES");
				c.setupAes();
			}
			//else, someone is sending  an expired or inexistent key...?
			else{
				System.out.println("Someone is sending an invalid cached key");
				return null;
			}
			
		}
		
		return ret;
		
	}
	
	private void keyExchangeServerSide() throws Exception{
		//Generate the public,private key pair
	    c.genKeys();
	    //Send the public key to the client
	    sendMessageNotEncrypted(c.puk.getEncoded());
	    sendMessageNotEncrypted(c.sign(c.puk.getEncoded()));
	    
	    //Receive the aes key generated from the client
	    byte[] aesK=receiveMessageNotEncrypted();
	    c.aesKey=new SecretKeySpec(c.decryptRSA(aesK),"AES");
	    c.setupAes();
	    
	    n.keysCache.addKey(c.aesKey.getEncoded()); //add the key in the cache!
	}
	
	//Main of the thread, handle the client.
	@Override
	public void run() {
		
		
		print_conn_state();
		if(nOfClients>MAX_CLIENTS_N){
			System.out.println("Server has enough clients for now, terminating connection.");
			try {
				s.close();
			} catch (IOException e) {}
			byeMessage();
			return;
		}
		
		//string to contain temporary commands
		String command;
		
		try{
		    
		    dataOut= new DataOutputStream(s.getOutputStream());
		    dataIn = new DataInputStream(s.getInputStream());
		    
		    
		    //check, if the client has already a valid cryptographic key, let him use it!
		    Boolean b=keyCaching();
		    if(b==null){
		    	byeMessage();
		    	return;
		    }
		    
		    //Make the key exchange to support cryptography with the client
		    if(b==false)
		    	keyExchangeServerSide();
		}
		catch(Exception e){
			//System.out.println(e.toString());
			byeMessage();
			return;
		}
		
		while(true){
			try{
				command=receiveMessage();
				
				//System.out.println(command);
				String pieces[]=command.split(Networking.SEPARATOR);
				
				
				//If the client is asking to translate numbers into operators
				if(pieces[0].equals(Networking.TRANSLATE_NUMBERS_TO_OPERATORS)){
					//System.out.println("into translations");
					
					String responseCommand="";
					
					//System.out.println("Answering for "+(pieces.length-1)+" numbers");
					
					int i=1;
					while(i<pieces.length){
						//System.out.println(pieces[i]);
						//Converts the numbers, save the association in cache, and the send them to the client!
						String operator=n.cache.getValue(pieces[i]);
						if(operator==null){
							operator=SendReceiveSms.numberToOperator(pieces[i]);
							
							System.out.println(pieces[i]+" -> "+operator);
							
							//if is in timeout, add an invalid entry. in this way, the next time the user will connect, he will have the value ready.
							//otherwise it will wait for another timeout, and no response will arrive
							if(operator.equals(SendReceiveSms.TIMEOUT))
								n.cache.addValue(pieces[i], SendReceiveSms.INVALID);
							//If is invalid, don't cache it, we don't want invalid string in cache. May corrupt the structure.
							else if(!operator.equals(SendReceiveSms.INVALID))
								n.cache.addValue(pieces[i], operator);
						}
						//else System.out.println("already in cache: " + pieces[i]+" -> "+operator);
						
						responseCommand+=operator+Networking.SEPARATOR;
						
						i++;
					}
					
					//Send all the operators of the numbers asked from the client
					sendMessage(responseCommand.substring(0,responseCommand.length()-1));
				}
				//else if the client is asking for the updated file, send it!
				else{
					//Eliminates the space from the command, or matching will fail.
					
					
					
					if(pieces[0].equals(Networking.UPDATE_FILE_OF_OFFERS)){
						
						n.rwlock.readLock().lock();
						//System.out.println("A client's dispatcher has taken the mutex in read mode. N readers: "+(++readers));
						
						try{
							byte[] hash=receiveMessageNotEncrypted();
							boolean equals=true;
							for(int i=0;i<hash.length && i<n.offersHash.length;i++){
								if(hash[i]==n.offersHash[i])
									;
								else{
									equals=false;
									break;
								}
							}
							
							//Update him!
							if(!equals){
								sendMessage(Integer.toString(n.offers.length()));
								sendMessage(n.offers);
							}
							//Already updated
							else
								sendMessage(Networking.OK);
						}
						catch(Exception e){}
						
						//System.out.println("A client's dispatcher has released the mutex in read mode. N readers: "+(--readers));
						n.rwlock.readLock().unlock();
					}
					else{
						byeMessage();
						return;
					}
				}
				
			}
			catch(Exception e){
				//System.out.println(e.getMessage());
				byeMessage();
				return;
			}
			
			
		}
	}
	
}

//Thread that update the file of the offers every 4 minutes
class ThreadUpdateOffers implements Runnable{
	
	//equivalent to 4 minutes
	static int sleep_time=1000*60*4;
	
	Networking n;
	public ThreadUpdateOffers(Networking n){
		this.n=n;
	}
	
	@Override
	public void run() {
		
		while(true){
			
			//UPDATE OFFERS
			
			//Get the lock
			n.rwlock.writeLock().lock();
			//System.out.println("Check for new offers thread has taken the mutex");
			try{
				String old_offers=n.offers;
				
				//Read the file of offers
				File file = new File(System.getProperty("user.dir")+Networking.pathFileOfOffers);
			    FileInputStream fis = new FileInputStream(file);
			    byte[] data = new byte[(int)file.length()];
			    fis.read(data);
			    fis.close();
			    //
			    n.offers = new String(data,"UTF8");
			    
			    n.offersHash=n.c.Sha1Hash(n.offers.getBytes());
			    
			    
			    
			    if(!old_offers.equals(n.offers)){
			    	System.out.println("***New offers loaded successfully***!!! [size "+n.offers.length()+" byte]");
			    	Utilities.printTimestamp();
			    }
			}
			catch(Exception e){
				System.out.println("Exception in update offers thread!");
				System.out.println(e.toString());
			}
			
			//System.out.println("Check for new offers thread has released the mutex");
		    //Free the lock
		    n.rwlock.writeLock().unlock();
		    
		    
		    //Sleep for 4 minutes
		    try {
				Thread.sleep(sleep_time);
			} catch (InterruptedException e) {
			}
		}
		
	}
	
}


/*
 * 
 * Class for manage the network connectivity. Configure everything needed, start an update thread to check for a newer file of offers, 
 * and wait for clients. When a client arrives, a new thread is spawned.
 * Contains also the protocol, the cache and various things.
 * 
 * Bortoli Tomas
 * 
 * */
public class Networking{
	/************************************************************************************/
	//Protocol
	//This is the BestOffer client-server protocol
	//The protocol includes encryption with initial key exchange, by RSA encryption. In the case that the AES crypto key is cached and not expired, use it!
	//After, all the messages (except the signing) are encrypted using the AES and the key initially exchanged.
	//After each message the server will include a non encrypted message with his sign of the previous message.
	//Structure of the single message is like this:
	//
	//Length of the message
	//Message bytes
	//
	//************************
	//If the message is sent from the server to the client after the message there will be the sign of the server for the message, in this format:
	//Length of the sign
	//Sign bytes
	//************************
	//Key words
	public final static String TRANSLATE_NUMBERS_TO_OPERATORS="TRANSLATE_NUMBERS_TO_OPERATORS";
	public final static String UPDATE_FILE_OF_OFFERS="UPDATE_FILE_OF_OFFERS";
	public final static String OK="OK";
	public final static String SEPARATOR="~";
	
	public final static String filler=" ";
	
	//Protocol init:
	//If the client has an AES key cached, he will send the sha1 hash to the server. If the server has the key cached (and this should be the case)
	//They will use that key to communicate. Instead, if the server does not found the key, the communication will be broken; until the client
	//Will remake the initial key exchange. Inconsistence problems 
	//If the client doesn't has a cached value, or the key is expired, he will send a 0 bytes array and the server will understand. After this step
	//a new key exchange will be made
	
	//Protocol branches:
	//Client may ask for translating one or more number to operators, or updating his file of offers.
	//N.B At the end of each command there is an end of the line char to indicate that the line is ended, and the command with the line.
	
	//1 Number to operator translation
	//Client send:
	//TRANSLATE_NUMBERS_TO_OPERATORS~firstNumber~secondNumber~toinfinite.
	//Server answer:
	//operatorOfFirstNumber~operatorOfSecondNumber~toinfinite. N.B: Ends with a ~
	//2 updating file of offers
	//Client send:
	//UPDATE_FILE_OF_OFFERS
	//sha1HashOfHisFile bytes (NOT ENCRYPTED)
	//Server answer:
	//OK
	//if the file is updated
	//otherwise it will answer
	//Length of the file of offers
	//Whole file of offers 
	
	//End Protocol
	/************************************************************************************/
	
	//Object used to handle cryptography
	Cryptography c;
	
	
	public String offers="";
	public byte[] offersHash;
	//Mutex that support one writer and more readers to a single object
	public ReadWriteLock rwlock;
	
	//Name of the file of offers
	final static String pathFileOfOffers="/fileOfOffers";
	
	//Local port where the clients will connect
	final static short port=1003;
	//List of threads that dispatch the clients
	List<Thread> threadsClients;
	//Local socket to receive connections and spawn clients socket
	ServerSocket serverSocket;
	
	CacheOfNumbersToOperators cache;
	CryptographicKeysCache keysCache;
	
	//Constructor: initialize the list of socket
	public Networking(){
		try{
			serverSocket=new ServerSocket(port);
			
			System.out.println("Opened socket on port "+port);
			
			
			threadsClients=new ArrayList<Thread>();
			
			c=new Cryptography();
			
			rwlock= new ReentrantReadWriteLock();
			
			//Create and start the thread that update the offers
			(new Thread(new ThreadUpdateOffers(this))).start();
			
			cache=new CacheOfNumbersToOperators();
			
			keysCache=new CryptographicKeysCache(this);
			
		    //Wait while the offers are loaded
		    while(offers.equals("")){
		    	System.out.println("Waiting for offers..");
		    	Thread.sleep(1500);
		    }
			System.out.println("Offers arrived, go waiting for clients..");
			
		}
		catch(Exception e){
			System.out.println(e.toString());
		}
		
	}
	
	public void handleNewClient(){
		
		try {
			//Accept a connection
			Socket s=serverSocket.accept();
			
			//Create the Runnable object to create the thread
			ThreadDispatchClients t=new ThreadDispatchClients(s,this);
			
			//Create the thread, add the thread to the list of thread, and start it!
			Thread tr=(new Thread(t));
			threadsClients.add(tr);
			tr.start();
			
			
		} catch (IOException e) {
			System.out.println(e.toString());
			
			//Terminate
			for(int i=0;i<threadsClients.size();i++){
				threadsClients.get(i).interrupt();
			}
		}
		
	}
	
}
