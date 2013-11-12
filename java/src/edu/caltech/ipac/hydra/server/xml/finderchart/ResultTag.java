package edu.caltech.ipac.hydra.server.xml.finderchart;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 9/25/13
 *
 * @author loi
 * @version $Id: $
 */
public class ResultTag {
    String datatag;
    String equCoord;
    String galCoord;
    String eclCoord;
    String totalimages;
    String htmlfile;
    String pdffile;
    List<ImageTag> images = new ArrayList<ImageTag>();
    String glint;
    String persistent;

    public String getDatatag() {
        return datatag;
    }

    public void setDatatag(String datatag) {
        this.datatag = datatag;
    }

    public String getEquCoord() {
        return equCoord;
    }

    public void setEquCoord(String equCoord) {
        this.equCoord = equCoord;
    }

    public String getGalCoord() {
        return galCoord;
    }

    public void setGalCoord(String galCoord) {
        this.galCoord = galCoord;
    }

    public String getEclCoord() {
        return eclCoord;
    }

    public void setEclCoord(String eclCoord) {
        this.eclCoord = eclCoord;
    }

    public String getTotalimages() {
        return totalimages;
    }

    public void setTotalimages(String totalimages) {
        this.totalimages = totalimages;
    }

    public String getPdffile() {
        return pdffile;
    }

    public void setPdffile(String pdffile) {
        this.pdffile = pdffile;
    }

    public String getHtmlfile() {
        return htmlfile;
    }

    public void setHtmlfile(String htmlfile) {
        this.htmlfile = htmlfile;
    }

    public List<ImageTag> getImages() {
        return images;
    }

    public void setImages(List<ImageTag> images) {
        this.images = images;
    }

    public String getGlint() {
        return glint;
    }

    public void setGlint(String glint) {
        this.glint = glint;
    }

    public String getPersistent() {
        return persistent;
    }

    public void setPersistent(String persistent) {
        this.persistent = persistent;
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
