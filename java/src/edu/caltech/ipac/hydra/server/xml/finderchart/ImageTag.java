package edu.caltech.ipac.hydra.server.xml.finderchart;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 9/25/13
 *
 * @author loi
 * @version $Id: $
 */
public class ImageTag {
    String surveyname;
    String band;
    String obsdate;
    String fitsurl;
    String jpgurl;
    String shrunkjpgurl;


    public String getSurveyname() {
        return surveyname;
    }

    public void setSurveyname(String surveyname) {
        this.surveyname = surveyname;
    }

    public String getBand() {
        return band;
    }

    public void setBand(String band) {
        this.band = band;
    }

    public String getObsdate() {
        return obsdate;
    }

    public void setObsdate(String obsdate) {
        this.obsdate = obsdate;
    }

    public String getFitsurl() {
        return fitsurl;
    }

    public void setFitsurl(String fitsurl) {
        this.fitsurl = fitsurl;
    }

    public String getJpgurl() {
        return jpgurl;
    }

    public void setJpgurl(String jpgurl) {
        this.jpgurl = jpgurl;
    }

    public String getShrunkjpgurl() {
        return shrunkjpgurl;
    }

    public void setShrunkjpgurl(String shrunkjpgurl) {
        this.shrunkjpgurl = shrunkjpgurl;
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
