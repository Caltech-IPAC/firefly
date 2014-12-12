package edu.caltech.ipac.planner.io;

/**
* Holds 4 pieces of data for one AOR from an AOR text file.
*
* @author Su Potts
* @version $Id: TextAOR.java,v 1.2 2005/12/08 22:31:01 tatianag Exp $
*
*/
 
public class TextAOR extends Object {

    private String _aotType;
    private String _aorLabel;
    private String _targetType;
    private String _targetName;    

    public TextAOR(String _aotType,    String _aorLabel,
                   String _targetType, String _targetName) {
        this._aotType    = _aotType;
        this._aorLabel   = _aorLabel;
        this._targetType = _targetType;
        this._targetName = _targetName;
    }

    public void setAOTType(String aorData) {
        _aotType = aorData;
    }

    public void setAORLabel(String aorData) {
        _aorLabel = aorData;
    }
   
    public void setTargetType(String aorData) {
        _targetType = aorData;
    }

    public void setTargetName(String aorData) {
        _targetName = aorData;
    }

    public String getAOTType() { return _aotType; }

    public String getAORLabel() { return _aorLabel; }

    public String getTargetType() { return _targetType; }

    public String getTargetName() { return _targetName; }
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
