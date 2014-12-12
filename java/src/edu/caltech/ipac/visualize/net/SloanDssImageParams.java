package edu.caltech.ipac.visualize.net;

public class SloanDssImageParams extends  BaseIrsaParams  {


    public enum SDSSBand {u,g,r,i,z}

    private float  _sizeInDeg = .1F;
    private SDSSBand _band = SDSSBand.r;
    private int  _timeout  = 0;
    private boolean _queryKey= false;

    public SloanDssImageParams() { }

    public void  setSizeInDeg(float s)      { _sizeInDeg= s; }
    public float getSizeInDeg()      { return _sizeInDeg; }

    public void setBand(SDSSBand band) { _band = band; }
    public SDSSBand getBand() { return _band; }

    public void   setTimeout(int timeout) { _timeout= timeout;}
    public int    getTimeout() { return _timeout;}

    public String getUniqueString() {
         return "SloanDssImage-" + super.toString() + "--" + _sizeInDeg + "--" + _band +
                 (_queryKey? "--queryKey": "");
    }

    public String toString() {
         return getUniqueString();
    }

    public SloanDssImageParams makeQueryKey() {
        SloanDssImageParams newParam= new SloanDssImageParams();
        newParam._queryKey= true;
        newParam.setSizeInDeg(_sizeInDeg);
        newParam.setBand(_band);
        newParam.setTimeout(_timeout);
        newParam.setRaJ2000(this.getRaJ2000());
        newParam.setDecJ2000(this.getDecJ2000());
        return newParam;
    }
}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
