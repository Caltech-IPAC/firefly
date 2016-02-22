/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


// custom converter used (ProjectConverter) - no annotations needed within class
@XStreamAlias("Project")
public class ProjectTag implements Serializable {

    // xml element 'Name'
    protected String name;

    // xml element 'Title'
    protected String title;

    // xml element 'Properties'
    protected List<ParamTag> properties;

    // xml element 'OverrideProperties'
    protected List<ParamTag> oProperties;

    // xml element 'Catalog'
    protected List<CatalogTag> catalogs;

    // xml element 'SearchGroup*'
    protected List<SearchGroupTag> searchGroupTags;

    // xml element 'Param*'
    protected List<ParamTag> params;


    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
    }


    public List<ParamTag> getProperties() {
        if (properties == null) {
            properties = new ArrayList<ParamTag>();
        }
        return properties;
    }

    public String getPropertyValue(String id) {
        if (properties == null) {
            return null;
        } else {
            for (ParamTag p : properties) {
                if (p.getKey().equals(id)) {
                    return p.getValue();
                }
            }
            return null;
        }
    }

    public void addProperty(ParamTag p) {
        if (properties == null) {
            properties = new ArrayList<ParamTag>();
        }
        properties.add(p);
    }


    public List<ParamTag> getOverrideProperties() {
        if (oProperties == null) {
            oProperties = new ArrayList<ParamTag>();
        }
        return oProperties;
    }

    public void addOverrideProperty(ParamTag p) {
        if (oProperties == null) {
            oProperties = new ArrayList<ParamTag>();
        }
        oProperties.add(p);
    }
    

    public List<CatalogTag> getCatalogs() {
        if (catalogs == null) {
            catalogs = new ArrayList<CatalogTag>();
        }
        return catalogs;
    }

    public void addCatalog(CatalogTag c) {
        if (catalogs == null) {
            catalogs = new ArrayList<CatalogTag>();
        }
        catalogs.add(c);
    }


    public List<SearchGroupTag> getSearchGroups() {
        if (searchGroupTags == null) {
            searchGroupTags = new ArrayList<SearchGroupTag>();
        }
        return searchGroupTags;
    }

    public List<ParamTag> getParams() {
        if (params == null) {
            params = new ArrayList<ParamTag>();
        }
        return params;
    }

    public List<SearchTypeTag> getSearchTypes() {
        List<SearchGroupTag> groups = getSearchGroups();
        ArrayList<SearchTypeTag> retval = new ArrayList<SearchTypeTag>();
        for (SearchGroupTag g : groups) {
            retval.addAll(g.getSearchTypes());
        }
        return retval;
    }

    public SearchTypeTag getSearchType(String id) {

        List<SearchTypeTag> searchTypeTags = getSearchTypes();
        if (searchTypeTags.size() == 0) {
            return null;
        } else {
            for (SearchTypeTag stt : searchTypeTags) {
                String cmdId = stt.getCommandId();
                if (!StringUtils.isEmpty(cmdId) && cmdId.equals(id)) {
                    return stt;
                }

                String preSearchId = DynUtils.HYDRA_COMMAND_NAME_PREFIX + name + "_";
                String searchTagId = id.substring(id.indexOf(preSearchId) + preSearchId.length());
                if (stt.getName().equals(searchTagId)) {
                    return stt;
                }
            }
            return null;
        }
    }

    public SearchTypeTag getSearchType(int idx) {
        List<SearchTypeTag> searchTypeTags = getSearchTypes();
        if (searchTypeTags == null) {
            return null;
        }

        return searchTypeTags.get(idx);
    }

    public void addSearchGroup(SearchGroupTag st) {
        getSearchGroups().add(st);
    }

    public void addParam(ParamTag p) {
        getParams().add(p);
    }

}

