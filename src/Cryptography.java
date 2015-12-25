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
 * Cryptography class, is used to generate public and private RSA keys, encrypt and decrypt also with RSA
 * Is possible to get a public key(client side) by parameter and set it up to have a full RSA involvment. Key exchange.
 * Provides methods to sign chunk of data and verify it. 
 * Besides, support AES encryption and key generation.
 * There are constants to set the key length for the alghoritms.
 * This class is configured to drive as the server side of the scene
 * 
 * Developer: Bortoli Tomas
 * 
 * */

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


import java.io.*;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Cryptography {
	
	
	MessageDigest sha1;
	
	PublicKey puk;
	PrivateKey pvk;
	
	SecretKey aesKey;
	Cipher aesEnc,aesDec;
	
	final short rsaKeyLenBits=1024;
	final short aesKeyLenBits=128;
	
	static Signature sig;
	static PrivateKey sigPvk;
	static PublicKey sigPuk;
	
	final String rsaCipher="RSA/ECB/PKCS1Padding";
	
	public Cryptography(){
		try {
			sha1 = MessageDigest.getInstance("SHA-1");
			
			aesEnc= Cipher.getInstance("AES");
			
			aesDec= Cipher.getInstance("AES");
			
			init_sig_keys();
			
			
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		
	}
	
	public byte[] sign(byte[] message) throws SignatureException{
		sig.update(message);
		return sig.sign();
	}
	
	public String Sha1Hash(String s){
		
        try {
        	sha1.reset();
			sha1.update(s.getBytes("utf8"));
			return (new String(sha1.digest()));
		} catch (Exception e) {
			return null;
		}
        
	}
	
	public byte[] Sha1Hash(byte[] s){
		
        try {
        	sha1.reset();
			sha1.update(s);
			return (sha1.digest());
		} catch (Exception e) {
			return null;
		}
        
	}
	
	public void setupAes() throws Exception{
		aesEnc.init(Cipher.ENCRYPT_MODE, aesKey);
		aesDec.init(Cipher.DECRYPT_MODE, aesKey);
	}
	
	public void setPublicKeyFromString(String s){
		
		try {
			
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			KeySpec publicKeySpec = new X509EncodedKeySpec(s.getBytes());
			puk = keyFactory.generatePublic(publicKeySpec);
			
		} catch (Exception e) {
		}
		
		
	}
	
	/**
     * Generate an RSA key pair
     */
    public void genKeys() throws Exception {
    	
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(rsaKeyLenBits);
        KeyPair kp = kpg.generateKeyPair();
        pvk=kp.getPrivate();
        puk=kp.getPublic();
    }
    
    public byte[] generateAESKeyAndEncryptWithRSA() throws Exception {
        Cipher rsa = Cipher.getInstance(rsaCipher);

        // create new AES key
        KeyGenerator gen = KeyGenerator.getInstance("AES");
        gen.init(aesKeyLenBits);
        aesKey = gen.generateKey();
        
        // RSA encrypt AES key
        byte[] keyEnc = aesKey.getEncoded();
        rsa.init(Cipher.ENCRYPT_MODE, puk);
        byte[] keySec = rsa.doFinal(keyEnc);
        
        return keySec;
    }
    
    
    public String decryptRSA(String input) throws Exception {
        Cipher rsa = Cipher.getInstance(rsaCipher);
        rsa.init(Cipher.DECRYPT_MODE, pvk);
        return (rsa.doFinal(input.getBytes())).toString();
    }
    
    public byte[] decryptRSA(byte[] input) throws Exception {
        Cipher rsa = Cipher.getInstance(rsaCipher);
        rsa.init(Cipher.DECRYPT_MODE, pvk);
        return (rsa.doFinal(input));
    }
    
    public String encryptAES(String input) throws Exception{
    	return (aesEnc.doFinal(input.getBytes())).toString();
    }
    
    public byte[] encryptAES(byte[] input) throws Exception{
    	return (aesEnc.doFinal(input));
    }
    
    public String decryptAES(String input) throws Exception{
    	return (aesDec.doFinal(input.getBytes())).toString();
    }
    
    public byte[] decryptAES(byte[] input) throws Exception{
    	return (aesDec.doFinal(input));
    }
    
    
    
    //Function for initialize the public and private keys used for signing
    private static boolean init_sig=false;
    private void init_sig_keys() throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException{
    	
    	if(init_sig) return;
    	
    	sig = Signature.getInstance("SHA1WithRSA");
    	
    	sigPvk=(PrivateKey)readPEMKey(new File("CA_key.pkcs8.pem"));
    	RSAPrivateCrtKey privk = (RSAPrivateCrtKey)sigPvk;
    	RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(privk.getModulus(),privk.getPublicExponent());

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        sigPuk = keyFactory.generatePublic(publicKeySpec);
    	
        sig.initSign(sigPvk);
        
        /*Verifica che funzi
        byte[] b=new byte[]{1, 2, 3,4,5};
        try {
        	sig.update(b);
        	byte[] bb=sig.sign();
        	sig.initVerify(sigPuk);
        	sig.update(b);
        	System.out.println("Funziona: "+ sig.verify(bb));
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
        
        init_sig=true;
    }
 
    private static Key readPEMKey(File key) {
    	
        DataInputStream dis = null;
        BufferedReader reader = null;
        try {

            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(key)));
            byte[] encoded = new byte[(int) key.length()];
            dis.readFully(encoded);

            // PKCS8 decode the encoded RSA private key
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privKey = kf.generatePrivate(keySpec);
            return privKey;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (reader != null) {try {reader.close();} catch (Exception e) {e.printStackTrace();}}
            if (dis != null) {try {dis.close();} catch (Exception e) {e.printStackTrace();}}
        }
        return null;
    }
    

}

