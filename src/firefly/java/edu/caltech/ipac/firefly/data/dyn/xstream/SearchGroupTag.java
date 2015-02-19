/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@XStreamAlias("SearchGroup")
public class SearchGroupTag implements Serializable {

    // xml element 'Name'
    @XStreamAlias("Name")
    protected String name;

    // xml element 'Title'
    @XStreamAlias("Title")
    protected String title;

    // xml element 'SearchType*'
    @XStreamImplicit
    protected List<SearchTypeTag> searchTypeTags;


    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }


    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public List<SearchTypeTag> getSearchTypes() {
        if (searchTypeTags == null) {
            searchTypeTags = new ArrayList<SearchTypeTag>();
        }
        return searchTypeTags;
    }
}

