package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


// custom converter used (CatalogConverter) - no annotations needed within class
@XStreamAlias("Catalog")
public class CatalogTag implements Serializable {

    // xml attribute 'name'
    protected String name;

    // xml element 'Host'
    protected String host;

    // xml element Credentials, attribute login
    protected String login;

    // xml element Credentials, attribute password
    protected String password;

    // xml element CatalogURL
    protected String catalogUrl;

    // xml element OriginalFilename
    protected String originalFilename;

    // xml element MasterCatFilename
    protected String masterCatFilename;

    // xml element AddtlReqSearchParams
    protected Map<String, String> searchParams;


    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }


    public String getHost() {
        return host;
    }

    public void setHost(String value) {
        host = value;
    }


    public String getLogin() {
        return login;
    }

    public void setLogin(String value) {
        login = value;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String value) {
        password = value;
    }


    public String getCatalogUrl() {
        return catalogUrl;
    }

    public void setCatalogUrl(String value) {
        catalogUrl = value;
    }


    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String value) {
        originalFilename = value;
    }


    public String getMasterCatFilename() {
        return masterCatFilename;
    }

    public void setMasterCatFilename(String value) {
        masterCatFilename = value;
    }


    public Map<String, String> getSearchParams() {
        if (searchParams == null) {
            searchParams = new HashMap<String, String>();
        }

        return searchParams;
    }

    public void addSearchParam(String requestName, String masterCatName) {
        if (searchParams == null) {
            searchParams = new HashMap<String, String>();
        }

        searchParams.put(requestName, masterCatName);
    }

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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
