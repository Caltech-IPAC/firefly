/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.astro.CoordException;

final class UserPosition extends Position {


   private final String lonStr;
   private final String latStr;

    

   public UserPosition(String        lonStr,
                       String        latStr,
                       ProperMotion  pm,
                       CoordinateSys coordSystem,
                       float         epoch)  throws CoordException {

      super(TargetUtil.convertStringToLon(lonStr, coordSystem),
            TargetUtil.convertStringToLat(latStr, coordSystem),
            pm, coordSystem, epoch);
      this.lonStr = lonStr;
      this.latStr = latStr;
   }


   public String getUserLonStr() { return lonStr; }

   public String getUserLatStr() { return latStr; }

   public String toString() {
      String outstr = "User Lon String: "+ lonStr +
                      ", User Lat String: "+ latStr +"\n"+
                      super.toString();
      return outstr;
   }
}


   
