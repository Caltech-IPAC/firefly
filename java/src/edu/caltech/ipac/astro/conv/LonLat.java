package edu.caltech.ipac.astro.conv;

    public class LonLat {
       private double _lon;
       private double _lat;

       public LonLat(double lon, double lat) {
          _lon = lon;
          _lat = lat;
          }

       public double getLon() {return _lon; }
       public double getLat() {return _lat; }


        @Override
        public String toString() {
            return "lon: "+_lon+", lat: "+_lat;
        }
    }


