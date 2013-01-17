package edu.caltech.ipac.heritage.server.persistence;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

/**
 * Date: Apr 2, 2010
*
* @author loi
* @version $Id: ProprietaryInfo.java,v 1.3 2010/05/12 18:43:48 loi Exp $
*/
public class ProprietaryInfo implements Serializable {
    private HashMap<String, Data> propData = new HashMap<String, Data>();

    public void addData(String reqKey, String progId, Date releaseDate) {
        propData.put(reqKey, new Data(releaseDate, progId));
    }

    public Data getData(String reqKey) {
        return propData.get(reqKey);
    }
    
    public static class Data implements Serializable {
        Date releaseDate;
        String progId;

        Data(Date releaseDate, String progId) {
            this.releaseDate = releaseDate;
            this.progId = progId;
        }

        public Date getReleaseDate() {
            return releaseDate;
        }

        public String getProgId() {
            return progId;
        }
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
