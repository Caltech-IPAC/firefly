/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

