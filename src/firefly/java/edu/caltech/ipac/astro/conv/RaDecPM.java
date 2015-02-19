/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.conv;

    public class RaDecPM {
       private double _ra;
       private double _dec;
       private double _raPM;
       private double _decPM;

       public RaDecPM(double ra, double dec, double raPM, double decPM) {
          _ra = ra;
          _dec= dec;
          _raPM = raPM;
          _decPM= decPM;
          }

       public double getRa() {return _ra; }
       public double getDec() {return _dec; }
       public double getRaPM() {return _raPM; }
       public double getDecPM() {return _decPM; }
       }




