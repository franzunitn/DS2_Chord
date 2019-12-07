package dS2_Chord;

import java.math.BigInteger;

public class Util {

	/**
	 * Method to obtain a BigInteger with 2 to a certain exponential
	 * @param exponential of the two
	 * @return The big Integer
	 */
	public static BigInteger two_exponential(int exponential) {
		return new BigInteger("2").pow(exponential);
	}
	
}
