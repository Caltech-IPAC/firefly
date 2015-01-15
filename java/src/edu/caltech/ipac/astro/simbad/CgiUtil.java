/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.astro.simbad;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.net.MalformedURLException;

//import netscape.security.PrivilegeManager;
//import netscape.security.ForbiddenTargetException;

/**
* contains some static methods to do CGI related operations
* @author Xiuqin Wu, assembled and modified code by Jeremy Jones
**/
public class CgiUtil {
	
	/**
	 * Encodes illegal characters in a string to hex values for 
	 * transmission via CGI.
	**/
	public static String encodeCgiString(String input)
	{
		StringBuffer outBuffer = new StringBuffer(64); // Initial capacity of 64 characters
		char c;
		for (int i = 0; i < input.length(); ++i)
		{
			c = input.charAt(i);
			
			if (Character.isWhitespace(c))
			{
				outBuffer.append("%20");
			}
			else if (c == '+')
			{
				outBuffer.append("%2B");
			}
			else
			{
				outBuffer.append(c);
			}
		}
		
		return outBuffer.toString();
	}
	

	/**
	 * Connects to the specified URL and retrieves the contents of 
	 * the URL document as a string buffer.
	 *
	 * @param	url	URL to retrieve
	 * @return		the contents of the URL as a StringBuffer
	**/
	public static StringBuffer fetchUrl(String url)
		throws IOException, MalformedURLException   //  ForbiddenTargetException
	{
		StringBuffer content = new StringBuffer(2048); // Initial capacity of 2K
		String thisLine;
		URLConnection uc;
		BufferedReader in;

	//	MessageLogger.getInstance().writeDebug(this, "Opening connection...");
		
		try
		{
			uc = (new URL(url)).openConnection();
			in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
		}
		catch (SecurityException ex)
		{
			// Then try to enable the netscape security privilege
			// If this fails, then ForbiddenTargetException will be thrown

			//PrivilegeManager.enablePrivilege("UniversalConnect");
			
			uc = (new URL(url)).openConnection();
			in = new BufferedReader(new InputStreamReader(uc.getInputStream()));			
		}
		
		//MessageLogger.getInstance().writeDebug(this, "Reading from server...");
		
		/*
		String line;
		while ((line = in.readLine()) != null)
		{
			content.append(line);
			content.append("\n");
		}
		*/
		char [] line = new char[1024];
		int length;
		while ((length = in.read(line, 0, 1024)) != -1)
		{
		//System.out.println("length: " +length);
			content.append(line, 0, length);
		}
		
		in.close();
		//MessageLogger.getInstance().writeDebug(this, "Connection closed.");
	
//MessageLogger.getInstance().writeDebug(this, "CONTENTS: " + content.toString());
	
		return content;
	}
}


