/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.form;

import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.ValidationException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FieldSetDef implements Serializable {

    private String name;
    private String baseProp;

    private ArrayList<FieldDef> fields;

    public FieldSetDef() {}

    public FieldSetDef(String name) {
        this.name = name;
        fields = new ArrayList<FieldDef>();
    }

    public void validate(String aKey, Object aValue) throws ValidationException {
		throw new UnsupportedOperationException();
	}

    public void addFieldDef(FieldDef field) {
        fields.add(field);
    }

    public String getBaseProp() {
        return baseProp;
    }

    public void setBaseProp(String baseProp) {
        this.baseProp = baseProp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FieldDef> getFieldDefs() {
        return new ArrayList<FieldDef>(fields);
    }

    public FieldDef[] getFieldArray() {
        return fields.toArray(new FieldDef[fields.size()]);
    }

    /**
     * returns the FieldDef associated with this name.
     * This relativeName is appended to this Set's baseProp name to form
     * the absolute name when retrieving the field.
     * @param relativeName
     * @return
     */
    public FieldDef getFieldDef(String relativeName) {
        return getFieldDefUsingAbsName(baseProp + "." + relativeName);
	}

    public FieldDef getFieldDefUsingAbsName(String absName) {
        for (FieldDef fd : fields) {
            if (fd.getName().equals(absName)) {
                return fd;
            }
        }
        return null;
	}

    /**
     * Return a new FieldSetDef compose only of the given fields in the
     * order that it was given.
     * @param name      the name of the new set
     * @param fieldNames    an array of the names of the fields to
     *                      include in the new set.
     * @return
     */
    public FieldSetDef subset(String name, final String[] fieldNames) {
        FieldSetDef fsd = new FieldSetDef(name);
        fsd.setBaseProp(baseProp);
        for (String fieldName : fieldNames) {
            FieldDef fd = getFieldDef(fieldName);
            if (fd == null) {
                fd = getFieldDefUsingAbsName(fieldName);
            }
            if (fd == null) {
                throw new IllegalArgumentException("The given name is not in this set: " + fieldName);
            } else {
                fsd.addFieldDef(fd);
            }
        }
        return fsd;
    }


    /**
     * create a subset of the current FieldSetDef based on the given
     * Comparator.  Only items that the filter accept will be in the
     * returned set.
     *
     * @param name      name of the new FieldSetDef
     * @param filter    use to create the new set.
     * @return
     */
    public FieldSetDef subset(String name, Filter filter) {

        FieldSetDef fsd = new FieldSetDef(name);
        fsd.setBaseProp(baseProp);
        for (FieldDef field : fields) {
            FieldDef fd = (FieldDef) field;
            if (filter.accept(fd)) {
                fsd.addFieldDef(fd);
            }
        }
        return fsd;
    }


    /**
     * iterate through all of the FieldDef in this set
     * @return
     */
    public Iterator<FieldDef> iterator() {
        return getFieldDefs().iterator();
	}

    public int size() {
        return fields.size();
    }

    public boolean validate(Map params)        // validate the given maps of param/value(<String, Object>)
    {
        return true;
    }


    public static interface Filter {
        public boolean accept(FieldDef item);
    }
}
