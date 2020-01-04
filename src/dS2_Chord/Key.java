package dS2_Chord;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Key {
	
	private static MessageDigest md;
	
	public Key() {
		try {
		md = MessageDigest.getInstance("SHA-1"); 
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e); 
		}
	}
	
	
	public BigInteger encryptThisString(String input) { 
       // digest() method is called 
       // to calculate message digest of the input string 
       // returned as array of byte 
       md.update(input.getBytes());
       byte[] messageDigest = md.digest();
       // Convert byte array into signum representation 
       BigInteger no = new BigInteger(1, messageDigest);
       // Convert message digest into hex value 
       //String hashtext = no.toString(16); 
       return no; 
	} 

}
