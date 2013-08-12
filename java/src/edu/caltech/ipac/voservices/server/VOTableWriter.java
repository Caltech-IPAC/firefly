package edu.caltech.ipac.voservices.server;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.voservices.server.tablemapper.*;
import voi.vowrite.*;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * $ Id: $
 */
public class VOTableWriter {

    private VODataProvider dataProvider;

    public VOTableWriter(VODataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    /**
     * The server failed to process the query. Possible reasons include:
     *
     * - The input query contained a syntax error.
     * - The way the query was posed was invalid for some reason, e.g., due to an invalid query region specification.
     * - A constraint parameter value was given an illegal value; e.g. DEC=91.
     * - The server trapped an internal error (e.g., failed to connect to its database) preventing further processing.
     *
     * @param ps   output stream
     * @param error  detailed error
     * @throws java.io.IOException io error
     */
    public static void sendError(PrintStream ps, String error) throws IOException {
        boolean isOverflow = error.contains("[OVERFLOW]");
        Logger.debug("sendError", error);
        //res.sendError(HttpServletResponse.SC_BAD_REQUEST, error);
        VOTableStreamWriter voWrite = startVO(ps, "Error");

        //Create a new resource element.
        VOTableResource voResource = new VOTableResource() ;
        voResource.setType("results");


        VOTableInfo info = new VOTableInfo();
        info.setName("QUERY_STATUS");
        if (isOverflow) {
            info.setValue("OVERFLOW");
            error = error.replace("[OVERFLOW]","");
            error = error.trim();
        } else {
            info.setValue("ERROR");
        }
        info.setContent(error);

        voResource.addInfo(info);

        // Write the Resource element to outputStream.
        voWrite.writeResource(voResource) ;

        endVO(voWrite);
    }


    public  void sendMetadata(PrintStream ps, TableMapper tableMapper) throws IOException {
        VOTableStreamWriter voWrite = startVO(ps, tableMapper.getTableDesc());

        //Create a new resource element.
        VOTableResource voResource = new VOTableResource() ;
        voResource.setType("results");
        setParameters(voResource, tableMapper, null); // show default values of parameters

        try {
            // Create a new Table element
            VOTableTable voTab = new VOTableTable();
            List<VOTableParam> paramList = setFields(voTab, tableMapper);

            // add parameters for constant value fields
            for (VOTableParam p : paramList) {
                voResource.addParam(p);
            }

            VOTableInfo info = new VOTableInfo();
            info.setName("QUERY_STATUS");
            info.setValue("OK");
            voResource.addInfo(info);

            // Write the Resource element to outputStream.
            voWrite.writeResource(voResource) ;

            // Write the Table element to outputStream.
            voWrite.writeTable(voTab) ;

            // End the TABLE element.
            voWrite.endTable() ;
        } catch (Exception e) {
            Logger.error(e, "Unable to convert data into vo table");

            //Create a new resource element.
            voResource.setType("results");

            VOTableInfo info = new VOTableInfo();
            info.setName("QUERY_STATUS");
            info.setValue("ERROR");
            info.setContent("Unable to convert data into vo table");

            voResource.addInfo(info);

            // Write the Resource element to outputStream.
            voWrite.writeResource(voResource) ;
        }

        // end resource and vo table
        endVO(voWrite);

    }

    public void sendData(PrintStream ps, Map<String, String> paramMap) throws IOException {
        Collection<MappedField> mappedFields = null;
        boolean noMatch = false;
        try {
            mappedFields = dataProvider.getMappedFields();
        } catch (NoDataException e) {
            if (e.getMessage().toLowerCase().contains("match")) {
                noMatch = true;
            } else {
                sendError(ps, "Failed to get data. "+e.getMessage());
                return;
            }
        }

        if (!noMatch && mappedFields == null) {
            sendError(ps, "Failed to get "+dataProvider.getVOMetadata().getTableName()+" data. ");
            return;
        }

        VOTableStreamWriter voWrite = startVO(ps, dataProvider.getVOMetadata().getTableDesc());

        //Create a new resource element.
        VOTableResource voResource = new VOTableResource() ;
        voResource.setType("results");

        boolean resourceStarted = false;
        boolean tabStarted = false;
        VOMetadata tableMapper = dataProvider.getVOMetadata();
        try {
            // create VOTableTable element
            VOTableTable voTab = new VOTableTable();
            List<VOTableParam> paramList = setFields(voTab, tableMapper);

            VOTableInfo info = new VOTableInfo();
            info.setName("QUERY_STATUS");
            if (dataProvider.getOverflowMessage() != null) {
                info.setValue("OVERFLOW");
                info.setContent(dataProvider.getOverflowMessage());
            } else {
                info.setValue("OK");
                if (noMatch) {
                    info.setContent("No match");
                }
            }
            voResource.addInfo(info);

            // parameters values are set from paramMap
            setParameters(voResource, tableMapper, paramMap);

            // add parameters for constant value fields
            for (VOTableParam p : paramList) {
                voResource.addParam(p);
            }            

            // Write the Resource element to outputStream.
            voWrite.writeResource(voResource) ;
            resourceStarted = true;

            // Write the Table element to outputStream.
            voWrite.writeTable(voTab) ;
            tabStarted = true;

            // Write the data to outputStream.
            int rowsAdded = 0;
            if (!noMatch) {
                DataObject row = dataProvider.getNextRow();
                while (row != null) {
                    String [] values = new String[mappedFields.size()];
                    int i = 0;
                    for (MappedField mappedField : mappedFields) {
                        values[i] = mappedField.getMappedValue(row);
                        i++;
                    }
                    voWrite.addRow(values, values.length);
                    //LOG.briefDebug("Row added: "+CollectionUtil.toString(values));
                    rowsAdded++;
                    row = dataProvider.getNextRow();
                }
            }

            Logger.briefDebug(rowsAdded+" rows written");

            // End the TABLE element.
            voWrite.endTable() ;
        } catch (Exception e) {
            Logger.error(e, "Unable to convert data into vo table");

            if (tabStarted) {
                // end TABLEDATA element
                voWrite.endTable() ;
            }

            if (resourceStarted) {
                voWrite.endResource();
                //Create a new resource element.
                voResource = new VOTableResource() ;
                voResource.setType("results");
            }

            VOTableInfo info = new VOTableInfo();
            info.setName("QUERY_STATUS");
            info.setValue("ERROR");
            info.setContent("Unable to convert data into vo table");

            voResource.addInfo(info);

            // Write the Resource element to outputStream.
            voWrite.writeResource(voResource) ;
        }

        // end resource and vo table
        endVO(voWrite);

    }


    private void setParameters(VOTableResource voResource, VOMetadata metadata, Map<String, String> paramMap) throws IOException {
        VOTableParam param;

        for (VoServiceParam p : metadata.getVoParams()) {
            param = new VOTableParam();
            param.setName("INPUT:"+p.getName());
            String value = (paramMap != null && paramMap.containsKey(p.getName())) ? paramMap.get(p.getName()) : p.getValue();
            if (value != null) {
                param.setValue(value);
            }
            param.setDataType(p.getDataType());
            String arraySize = p.getArraySize();
            if (arraySize != null) {
                param.setArraySize(arraySize);
            }
            String desc = p.getDesc();
            if (desc != null) {
                param.setDescription(desc);
            }
            String precision = p.getPrecision();
            if (precision != null) {
                param.setPrecision(precision);
            }
            String unit = p.getUnit();
            if (unit != null) {
                param.setUnit(unit);
            }
            String ucd = p.getUcd();
            if (ucd != null) {
                param.setUcd(ucd);
            }
            String utype = p.getUtype();
            if (utype !=null) {
                param.setUtype(utype);
            }
            VoValues voValues = p.getVoValues();
            if (voValues != null) {
                param.setValues(toVoTableValues(voValues));
            }
		    voResource.addParam(param);
        }
    }

    private static List<VOTableParam> setFields(VOTableTable voTab, VOMetadata metadata) throws IOException {
        VOTableField voField;
        VOTableParam voParam;
        List<VOTableParam> voParamList = new ArrayList<VOTableParam>();

        List<IpacField> ipacFlds;
        for (VoField f : metadata.getVoFields()) {
            ipacFlds = f.getIpacFields();
            if (ipacFlds == null || ipacFlds.size()<1 ) {
                // fixed value field should be a parameter
                voParam = new VOTableParam();
                voParam.setName(f.getName());
                String ucd = f.getUcd();
                if (ucd != null) {
                    voParam.setUcd(ucd);
                }
                String utype = f.getUtype();
                if (utype != null) {
                    voParam.setUtype(utype);
                }
                voParam.setDataType(f.getDataType());
                String arraySize = f.getArraySize();
                if (arraySize != null) {
                    voParam.setArraySize(arraySize);
                }
                String desc = f.getDesc();
                if (desc != null) {
                    voParam.setDescription(desc);
                }
                String unit = f.getUnit();
                if (unit != null) {
                    voParam.setUnit(unit);
                }
                voParam.setValue(f.getDefaultValue());

                //voTab.addParam(voParam);
                voParamList.add(voParam);
            }
        }
        for (VoField f : metadata.getVoFields()) {
            ipacFlds = f.getIpacFields();
            if (ipacFlds != null && ipacFlds.size()>0 ) {

                voField = new VOTableField();
                voField.setName(f.getName());
                String ucd = f.getUcd();
                if (ucd != null) {
                    voField.setUcd(ucd);
                }
                String utype = f.getUtype();
                if (utype != null) {
                    voField.setUtype(utype);
                }
                voField.setDataType(f.getDataType());
                String arraySize = f.getArraySize();
                if (arraySize != null) {
                    voField.setArraySize(arraySize);
                }
                String desc = f.getDesc();
                if (desc != null) {
                    voField.setDescription(desc);
                }
                String unit = f.getUnit();
                if (unit != null) {
                    voField.setUnit(unit);
                }
                /*
                VoValues voValues = f.getVoValues();
                if (voValues != null) {
                    voField.setValues(toVoTableValues(voValues));
                }
                */

                voTab.addField(voField);
            }
        }
        return voParamList;
    }

    private static VOTableStreamWriter startVO(PrintStream prnStream, String desc) throws IOException {

        // Create an instance of VOTableStreamingWriter class.
        VOTableStreamWriter voWrite = new VOTableStreamWriter(prnStream) ;

        //Create a votable element
        VOTable vot = new VOTable() ;
        //no way to override DOCTYPE, which make it version 1.0
        //vot.setVersion("1.1");

        vot.setDescription(desc) ;

        // Write the VOTable element to outputStream.
        voWrite.writeVOTable(vot) ;

        return voWrite;
    }

    private static void endVO(VOTableStreamWriter voWrite) {
        // End the RESOURCE element.
        voWrite.endResource() ;

        // End the VOTABLE element.
        voWrite.endVOTable() ;
    }

    private VOTableValues toVoTableValues(VoValues voValues) {
        VOTableValues vals = new VOTableValues();
        List<VoOption> voOptions = voValues.getVoOptions();
        if (voOptions != null) {
            List<VOTableOption> options = new ArrayList<VOTableOption>();
            for (VoOption option : voOptions) {
                VOTableOption o = new VOTableOption();
                o.setValue(option.getValue());
                options.add(o);
            }
            vals.addAllOptions(options);
        }
        VoMin voMin = voValues.getVoMin();
        if (voMin != null) {
            VOTableMin min = new VOTableMin();
            min.setInclusive(voMin.isInclusive()?"yes":"no");
            min.setValue(voMin.getValue());
            vals.setMin(min);
        }
        VoMax voMax = voValues.getVoMax();
        if (voMax != null) {
            VOTableMax max = new VOTableMax();
            max.setInclusive(voMax.isInclusive()?"yes":"no");
            max.setValue(voMax.getValue());
            vals.setMax(max);
        }

        return vals;
    }

}
