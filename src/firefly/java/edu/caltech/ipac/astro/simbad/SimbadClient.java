/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class SimbadClient
//
//--- Description -------------------------------------------------------------
//	SimbadClient provides the ability to search the SIMBAD astronomical database
//	and retrieve results as Java objects.
//
//--- Notes -------------------------------------------------------------------
//	Currently only BY NAME, NEAR NAME, and NEAR POSITION searches are supported.
//
//--- Development History -----------------------------------------------------
//
//	08/06/98	J. Jones / 588
//
//		Original implementation.
//
//  09/14/98    J. Jones / 588
//
//		Added the SIMBAD DataSource.  Now parses V Magnitude.
//
//  10/02/98    J. Jones / 588
//
//		Added B Magnitude and sets normalizer band.
//
//  02/24/99    J. Jones / 588
//
//		Updated to read new SIMBAD output format.  New SIMBAD doesn't
//		seem to want the username/password anymore, so disabled it
//		in SimbadClient.
//
//	03/03/00	J. Jones / 588
//
//		Removed all the username/password code, since no longer necessary.
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//
//=== End File Prolog =========================================================

package edu.caltech.ipac.astro.simbad;

import edu.caltech.ipac.util.Assert;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * SimbadClient provides the ability to search the <A HREF="http://cdsweb.u-strasbg.fr/Simbad.html">SIMBAD</A>
 * astronomical database and retrieve results as Java objects.
 *
 * <P>Note: Currently only BY NAME, NEAR NAME, and NEAR POSITION searches are supported.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the NGST SEA project.
 *
 * @version	03/03/00
 * @author	J. Jones / 588
**/
public class SimbadClient 
{
	/**
	 * Name of the SIMBAD host, could be passed in when instantiating the
	 * object  
	**/
	private String			fServerName = "simbad.harvard.edu";

	/**
	 * Name of the object search script, could be passed in when 
	 * instantiating the object  

	**/
	private String			fScriptLocation = "/sim-id.pl";

	/**
	 * Creates a new SimbadClient instance (not yet connected).
	**/
	public SimbadClient(String serverName, String scriptLocation )
	{	
		super();
        	fServerName = 	serverName ;
	        fScriptLocation = scriptLocation;
		
	}
	public SimbadClient() 
	{
		super();
		
	}

	/**
	 * Get the name of the server machine to connect to.
	**/
	public String getServerName()
	{
		return fServerName;
	}

	/**
	 * Set the name of the server machine to connect to.
	**/
	public void setServerName(String name)
	{
		fServerName = name;
	}

	/**
	 * Get the location of the object search script.
	**/
	public String getScriptLocation()
	{
		return fScriptLocation;
	}

	/**
	 * Set the location of the object search script.
	**/
	public void setScriptLocation(String location)
	{
		fScriptLocation = location;
	}

	

	/**
	 * Searches for objects that match a given object name.
	 * The search will be performed synchronously with the results
	 * returned immediately.
	 *
	 * @param	objectName	search for objects that match this name
	 * @return				array of Targets that match the search criteria
	**/
	public SimbadObject searchByName(String objectName)
			throws SimbadException, IOException 
	{
		SimbadObject simbadO = new SimbadObject();

		System.out.println("Searching for \"" + objectName + "\"...");

		String urlString = createSearchByNameUrl(objectName);

		// Retrieve contents as string buffer
		StringBuffer contents = CgiUtil.fetchUrl(urlString);

		// Parse buffer into SimbadObject information
		simbadO.setName(objectName);
		double  [] position ;
		float   [] properMotion ;
		//double majorAxis = Double.NaN;
		//double minorAxis = Double.NaN;
		String token = "";
		String name = "";
		StringTokenizer tokens = new StringTokenizer(contents.toString(), " \t\n\r<>+");
		try {
		     while (tokens.hasMoreTokens() && 
		           token.equalsIgnoreCase("error message") == false)
		     {
				token = tokens.nextToken();
		     }
		     if (token.equalsIgnoreCase("error message")) 
			   throw new SimbadException("object name is not" +
                  " recognizable by the SIMBAD name interpreter");

		} catch (NoSuchElementException nsee) {
			   System.out.println("Error checking problem");
			   throw new IOException("Error found during Simbad search");
	        }
		tokens = new StringTokenizer(contents.toString(), " \t\n\r<>+");
		try
		{
		     while (tokens.hasMoreTokens() && token.equals("--") == false)
			{
				token = tokens.nextToken();

				if (token.toLowerCase().equals("translated"))
				{
					// Skip "to :"
					token = tokens.nextToken();
					token = tokens.nextToken();

					// Extract the new name
					name = parseIdentifier(tokens, "/b");
				}
			}

			// Extract type as string
			simbadO.setType ( parseIdentifier(tokens, "/b"));

			// Extract position
			skipToToken(tokens, "coordinates");
			/*
			token = tokens.nextToken(); // br
			token = tokens.nextToken(); // /td
			token = tokens.nextToken(); // td
			token = tokens.nextToken(); // b
			*/
			skipToToken(tokens, "b"); // XW
			position = parseCoordinates(tokens);
			simbadO.setRa(position[0]);
			simbadO.setDec(position[1]);

		  }
		  catch (NoSuchElementException nsee) {
			   System.out.println("ID/coordinates parsing Exception");
			   throw new IOException("Error found during Simbad search");
			   }
//		  /*
		  try
		  {
			// Extract other optional fields
			while (tokens.hasMoreTokens() && token.equals("Identifiers") == false)
			{
				token = tokens.nextToken();

                                if (token.toLowerCase().equals("motion")) // "Proper motion"
                                {
                                    skipToToken(tokens, "b"); // XW
                                    properMotion = parsePM(tokens);

                                    simbadO.setRaPM(properMotion[0]);
                                    simbadO.setDecPM(properMotion[1]);
                                }
/*
				if (token.toLowerCase().equals("dimensions"))
				{
					skipToToken(tokens, ":");
					token = tokens.nextToken(); // bold
					majorAxis = Double.valueOf(tokens.nextToken()).doubleValue();
					// Covert the weird axis value to arcminutes
					majorAxis = Math.exp(majorAxis) * 10;
					minorAxis = Double.valueOf(tokens.nextToken()).doubleValue(); // log(major/minor)
					if (minorAxis == 0.0)
					{
						minorAxis = majorAxis;
					}
					else
					{
						// Convert the ratio value to the minor axis
						minorAxis = majorAxis / Math.exp(minorAxis);
					}
				}
*/
				if (token.toLowerCase().equals("morphological"))
				{
					token = tokens.nextToken(); // "type"
					token = tokens.nextToken(); // br
					token = tokens.nextToken(); // /td
					token = tokens.nextToken(); // td
					token = tokens.nextToken(); // b
					simbadO.setMorphology ( parseIdentifier(tokens, "/b"));
				}

				if (token.toLowerCase().indexOf("redshift") >= 0)
				{
                                        token = tokens.nextToken(); // (z)
					token = tokens.nextToken(); // br
					token = tokens.nextToken(); // /td
					token = tokens.nextToken(); // td
					token = tokens.nextToken(); // b
					token = tokens.nextToken();
					if (token.equals(":"))
					{
						token = tokens.nextToken();
					}
					if (token.toLowerCase().equals("z"))
					{
						// Is Redshift (Z)
						simbadO.setRedshift (
						   Double.valueOf(tokens.nextToken()).doubleValue());
					}
					else if (token.toLowerCase().equals("v"))
					{
						// Is Radial Velocity
						simbadO.setRadialVelocity (
						   Double.valueOf(tokens.nextToken()).doubleValue());
					}
				}

				if (token.toLowerCase().equals("b"))
				{
					String magn = tokens.nextToken();
					if (magn.toLowerCase().startsWith("magn"))
					{
						// Try Magnitude in the B band
						skipToToken(tokens, "b");
						String magStr = tokens.nextToken();
						if (!magStr.toLowerCase().equals("/b"))
						{
							try
							{
                                                            double val = Double.valueOf(magStr).doubleValue();
                                                            simbadO.setMagnitude (val);
                                                            simbadO.setMagBand ( "B");
                                                            simbadO.setBMagnitude(val);
							}
							catch (NumberFormatException numex)
							{
							}
						}

						// Try Magnitude in the Visible band
						// If both B and V mags exist, V will override B
						skipToToken(tokens, "b");
						magStr = tokens.nextToken();
						if (!magStr.toLowerCase().equals("/b"))
						{
							try
							{
								simbadO.setMagnitude (
								   Double.valueOf(magStr).doubleValue());
								simbadO.setMagBand ( "V");
							}
							catch (NumberFormatException numex)
							{
							}
						}
					}
				}

                                if (token.toLowerCase().equals("spectral"))
                                {
                                    token = tokens.nextToken(); // "type"
                                    token = tokens.nextToken(); // br
                                    token = tokens.nextToken(); // /td
                                    token = tokens.nextToken(); // td
                                    token = tokens.nextToken(); // b
                                    simbadO.setSpectralType( parseIdentifier(tokens, "/b") );
                                }

                                if (token.toLowerCase().equals("parallaxes"))
                                {
                                    token = tokens.nextToken(); // "i"
                                    token = tokens.nextToken(); // "(mas)"
                                    token = tokens.nextToken(); // "/i"
                                    token = tokens.nextToken(); // br
                                    token = tokens.nextToken(); // /td
                                    token = tokens.nextToken(); // td
                                    token = tokens.nextToken(); // b
                                    simbadO.setParallax( Double.valueOf(tokens.nextToken()).doubleValue() );
                                }

			}
/*
			// Read identifiers
			Vector idents = new Vector();
			while (tokens.hasMoreTokens() && token.equals("Measurements") == false)
			{
				token = tokens.nextToken();

				if (token.toLowerCase().startsWith("href="))
				{
					idents.addElement(parseIdentifier(tokens, "/a"));
				}
			}
*/
		}
		catch (Exception ex)
		{
			System.out.println("Exception occurred in parsing SIMBAD output: " + ex.toString());
		}
//		*/



		return simbadO;
	}



	/**
	 * Creates a URL string for the SIMBAD CGI script that searches for objects
	 * by name.
	 *
	 * @param	objectName	name of object to search for
	 * @return				the formatted URL string
	**/
	protected String createSearchByNameUrl(String objectName)
	{
            String url= null;
            try {
                url = "http://" + fServerName + fScriptLocation
                      + "?protocol=html&Ident="
                      + URLEncoder.encode(objectName, "UTF-8")
                      + "&CooEqui=2000"
                      + "&-source=simbad&NbIdent=1&Radius=10&Radius.unit=arcmin"
                      + "&output.max=all&o.catall=on&output.mesdisp=N&Bibyear1=1983"
                      + "&Bibyear2=1998&Equi1=2000.0&Equi2=1950.0";

                //return CgiUtil.encodeCgiString(url);
            } catch (UnsupportedEncodingException e) {
                Assert.stop("this should never happen: " + e.toString());
            }
            return url;
        }


	protected String skipToToken(StringTokenizer tokens, String skipTo)
			throws NoSuchElementException
	{
		String token = tokens.nextToken();
		String skipToLower = skipTo.toLowerCase();

		while (!token.toLowerCase().equals(skipToLower))
		{
			token = tokens.nextToken();
		}

		return token;
	}

	protected String skipTableTags(StringTokenizer tokens)
	{
		String newToken = tokens.nextToken();

		while (newToken.toLowerCase().equals("td"))
		{
			newToken = tokens.nextToken();
		}

		return newToken;
	}



	protected String parseIdentifier(StringTokenizer tokens, String endMarker)
	{
		String marker = endMarker.toLowerCase();
		String token = tokens.nextToken();
		String ident = token;
		token = tokens.nextToken();
		while (token.toLowerCase().equals(marker) == false)
		{
			ident += " " + token;
			token = tokens.nextToken();
	    }

	    return ident;
	}

	protected double[] parseCoordinates(StringTokenizer tokens)
	{
	 // Treats coords as J2000

	     double ra = 0.0, dec = 0.0;

	     ra = Integer.valueOf(tokens.nextToken()).intValue();

	     // Must do this test because the SIMBAD syntax is sloppy and inconsistent
	     String next = tokens.nextToken();
	     if (next.indexOf('.') >= 0)
	     {
		     // No seconds value for RA.  Assume none for DEC too.

		  double temp = Double.valueOf(next).doubleValue();
		  ra += temp / 60.0;
		  ra *= 15.0;

		  String ds = tokens.nextToken();
		  double sign = 1.0;
		  int d ;
		  if (ds.charAt(0) == '-') {
		     sign = -1.0;
		     d = Integer.valueOf(ds.substring(1)).intValue();
		     }
		  else
		     d = Integer.valueOf(ds).intValue();
		  double m = Double.valueOf(tokens.nextToken()).doubleValue();
		  dec = sign * (d + (m / 60.0));
	     }
	     else
	     {
		     // Separate seconds value for RA

		  ra += Integer.valueOf(next).intValue() / 60.0;
		  ra += Double.valueOf(tokens.nextToken()).doubleValue() / 3600.0;
		  ra *= 15.0;

		  String ds = tokens.nextToken();
		  double sign = 1.0;
		  int d ;
		  if (ds.charAt(0) == '-') {
		     sign = -1.0;
		     d = Integer.valueOf(ds.substring(1)).intValue();
		     }
		  else
		     d = Integer.valueOf(ds).intValue();

		  String decNext = tokens.nextToken();
		  if (decNext.indexOf('.') >= 0)
		  {
			  // Then DEC has no separate seconds value

		       double temp = Double.valueOf(decNext).doubleValue();
		       dec = sign * (d + (temp / 60.0));
		  }
		  else
		  {
		       // Separate seconds value for DEC

		       int m = Integer.valueOf(decNext).intValue();
		       double s = Double.valueOf(tokens.nextToken()).doubleValue();
		       dec = sign * (d + (m / 60.0) + (s / 3600.0));
		  }
	     }

	     double [] p = new double[2];
	     p[0] = ra;
	     p[1] = dec;
	     return p;
	}

	protected float[] parsePM(StringTokenizer tokens)
	{
	    float raPM, decPM;
	    raPM = Float.valueOf(tokens.nextToken()).floatValue();
	    decPM = Float.valueOf(tokens.nextToken()).floatValue();
	    float [] pm = new float[2];
	    pm[0] = raPM;
	    pm[1] = decPM;
	    System.out.println("SimbadClient.parsePM: " + raPM +", " +decPM);
	    return pm;
	}
}
