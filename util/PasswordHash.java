/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package edu.caltech.ipac.util;

import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This is utility class implements a hashing methods
 * used to provide one way encryption of user password.
 *
 * @author Joe Chavez
 * @version $Id: PasswordHash.java,v 1.3 2009/01/27 19:11:19 tatianag Exp $
 */
public class PasswordHash {

    public static int HASH_ALGORITHM_MD5 = 0;

    private static String HASH_ALGORITHMS[] = { "MD5" };

    private static java.util.Random rand = new java.util.Random();

    private static char[] valid = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
            'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P',
            'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '2', '3', '4', '5', '6', '7', '8', '9' };
    

    /**
     * Create a one-way hash of a character array.
     *
     * @param algorithm the algorithm to be used to create the hash
     *        (e.g. SHA-1, MD5)
     * @param message characters to create the hash from
     * @return the hashed password
     * @throws java.security.NoSuchAlgorithmException
     */
    public static String getHash(int algorithm, char[] message) throws NoSuchAlgorithmException {
        MessageDigest md;
        String digestString = "";
        String strMsg = new String(message);
        md = MessageDigest.getInstance(HASH_ALGORITHMS[algorithm]);
        byte[] digest = md.digest(strMsg.getBytes());
        digestString = PasswordHash.byteArrayToHexString(digest);
        return digestString;
    }

    /**
     * Create a one-way hash of the input string.
     * @param algorithm the algorithm to be used to create the hash
     *        (e.g. SHA-1, MD5)
     * @param message The input message string
     * @return the hashed message string.
     * @throws java.security.NoSuchAlgorithmException
     */
    public static String getHash(int algorithm, String message) throws NoSuchAlgorithmException {
        MessageDigest md;
        String digestString = "";
        md = MessageDigest.getInstance(HASH_ALGORITHMS[algorithm]);
        byte[] digest = md.digest(message.getBytes());
        digestString = PasswordHash.byteArrayToHexString(digest);
        return digestString;
    }

    /**
     * Generate alphanumeric password
     * @param length length requested
     * @return generated password
     */
    public static String generatePassword(int length) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            sb.append(valid[rand.nextInt(valid.length)]);
        }
        return sb.toString();
    }

    /**
     *  Convert a byte array representation of a checksum to string.
     *  @param data - byte array representation of checksum
     *  @return string representation of checksum
     */
    private static String byteArrayToHexString(byte[] data) {
        StringBuffer sb = new StringBuffer("");
        String hex = "0123456789abcdef";
        byte value;
        int length = data.length;

        for (int i = 0; i < length; i++) {
            value = Array.getByte(data, i);
            sb.append(hex.charAt((value & 0xf0) >>> 4));
            sb.append(hex.charAt(value & 0x0f));
        }
        return sb.toString();
    }

    /**
     * Convert HEX ascii string to byte array.
     * @param hexStr - string representation of checksum
     * @return checksum byte array
     */
    private static byte[] hexStringToByte(String hexStr) {
        int byteValue;
        int strLen = hexStr.length();
        byte[] data = new byte[strLen / 2];

        // Convert string to byte array.
        for (int i = 0; i < strLen; i += 2) {
            byteValue = Integer.parseInt(hexStr.substring(i, i + 2), 16);
            data[i / 2] = (byte) (byteValue & 0x000000ff);
        }
        return data;
    }

    public static void main(String args []) {
        // create MD5 hash (hex representation) of an argument string
        if (args.length != 1) {
            System.out.println("Please, provide a word to be hashed");
            System.exit(1);
        } else {
            try {
                System.out.println(getHash(HASH_ALGORITHM_MD5,args[0]));
            } catch (NoSuchAlgorithmException e) {
                System.out.println(e.getMessage());
            }
            System.exit(0);
        }
    }

}
