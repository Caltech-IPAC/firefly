package edu.caltech.ipac.voservices.server;

import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;

import java.util.ArrayList;
import java.util.List;

/**
 * $Id: SimpleMappedField.java,v 1.2 2011/02/08 20:27:25 tatianag Exp $
 */
public class SimpleMappedField implements MappedField {
    private String defaultValue;
    private List<DataType> sourceTypes;

    public SimpleMappedField(String defaultValue) {
        this.defaultValue = defaultValue;
        sourceTypes = null;
    }

    public void addSourceType(DataType type) {
        if (sourceTypes == null) {
            sourceTypes = new ArrayList<DataType>(2);
        }
        sourceTypes.add(type);
    }

    public String getMappedValue(DataObject dataObject) {
        if (sourceTypes == null) {
            return defaultValue;
        }
        if (sourceTypes.size() == 1) {
            DataType sourceType = sourceTypes.get(0);
            Object value = dataObject.getDataElement(sourceType);
            // return formatted value
            return sourceType.getFormatInfo().formatData(value).trim();
        } else {
            DataType sourceType = sourceTypes.get(0);
            Object value = dataObject.getDataElement(sourceType);
            String formattedValue = sourceType.getFormatInfo().formatData(value).trim();
            for (int i=1; i<sourceTypes.size(); i++) {
                sourceType = sourceTypes.get(i);
                value = dataObject.getDataElement(sourceType);
                // If a cell contains an array of numbers or a complex number, it should be encoded as multiple numbers separated by whitespace.
                // http://www.ivoa.net/Documents/VOTable/20091130/REC-VOTable-1.2.html   (Section 5.1)
                formattedValue += " "+sourceType.getFormatInfo().formatData(value).trim();
            }
            return formattedValue;
        }
    }
}
