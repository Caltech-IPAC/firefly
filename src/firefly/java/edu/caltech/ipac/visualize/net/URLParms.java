/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;



import edu.caltech.ipac.util.Assert;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;


public class URLParms
{
   ArrayList<String> params= new ArrayList<String>();;

   public URLParms() { }

   public void add(String keyword, String value)
   {
       params.add(encode(keyword));
       params.add(encode(value));
   }

   public String getKeyword(int i)
   {
      if(params.size() < 2 * i)
         return null;

      return params.get(2*i);
   }

   public String getValue(int i)
   {
      if(params.size() < 2 * i)
         return null;

      return params.get(2*i + 1);
   }

   public int getLength()
   {
      return(params.size() / 2);
   }

    public static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Assert.tst(false, "this should never happen");
            return null;
        }
    }
}
