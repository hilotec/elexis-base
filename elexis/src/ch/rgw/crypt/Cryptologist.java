package ch.rgw.crypt;

import ch.rgw.tools.Result;

/**
 * A Cryptologist knows how to encrypt, decrypt, sign and verify byte arrays
 * 
 * @author gerry
 * 
 */
public interface Cryptologist {
	
	/**
	 * encrypt a byte array
	 * 
	 * @param source
	 *            the plain bytes
	 * @param receiverKeyName
	 *            name of the receiver's public key
	 * 
	 * @return the encrypted bytes or null if encryption failed
	 */
	public byte[] encrypt(byte[] source, String receiverKeyName);
	
	/**
	 * Sign a byte array (create and sign a MAC)
	 * 
	 * @param source
	 *            the bytes to sign
	 * @param pwd
	 *            the password of the signer's secret key
	 * @return the signature
	 */
	public byte[] sign(byte[] source, char[] pwd);
	
	/**
	 * decrypt a byte array
	 * 
	 * @param encrypted
	 *            the encrypted bytes
	 * @param pwd
	 *            the passweord of the destinator's secret key
	 * @return the plain array or null of decryption failed
	 */
	public byte[] decrypt(byte[] encrypted, char[] pwd);
	
	/**
	 * Verify a MAC
	 * 
	 * @param data the signed data
	 * @param signature the signed digest
	 * @param signerKeyName name of the signer's public key
	 * @return
	 */
	public boolean verify(byte[] data, byte[] signature, String signerKeyName);
}
