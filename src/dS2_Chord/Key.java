package dS2_Chord;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Key {
	
	public Key() {
		
	}
	
	
	public BigInteger encryptThisString(String input) { 
		try { 
			// getInstance() method is called with algorithm SHA-1 
	       MessageDigest md = MessageDigest.getInstance("SHA-1"); 
	  
	       // digest() method is called 
	       // to calculate message digest of the input string 
	       // returned as array of byte 
	       byte[] messageDigest = md.digest(input.getBytes()); 
	  
	       // Convert byte array into signum representation 
	       BigInteger no = new BigInteger(messageDigest); 
	       // Convert message digest into hex value 
	       //String hashtext = no.toString(16); 
	       return no; 
	    } 
	  
	    // For specifying wrong message digest algorithms 
	    catch (NoSuchAlgorithmException e) { 
	        throw new RuntimeException(e); 
	    } 
	} 

}
