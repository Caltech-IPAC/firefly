package edu.caltech.ipac.voservices.server;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.voservices.server.tablemapper.IpacField;
import edu.caltech.ipac.voservices.server.tablemapper.TableMapper;
import edu.caltech.ipac.voservices.server.tablemapper.VoField;
import edu.caltech.ipac.voservices.server.tablemapper.VoServiceParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * $Id: RemoteDataProvider.java,v 1.4 2011/12/08 19:34:02 loi Exp $
 */
public class RemoteDataProvider implements VODataProvider {

    private AdjustedMetadata metadata;
    private Map<String,String> paramMap;
    private boolean testMode;
    private BufferedReader reader;
    List<DataType> columns;

    public RemoteDataProvider(TableMapper tableMapper, Map<String,String> paramMap) {
        this.metadata = new AdjustedMetadata(tableMapper);
        this.paramMap = paramMap;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public VOMetadata getVOMetadata() {
        return metadata;
    }

    public Collection<MappedField> getMappedFields() throws NoDataException {
        try {
            reader = getDataReader(metadata, paramMap, testMode);
        } catch (IOException e) {
            String caused = e.getMessage();
            Throwable cause = e.getCause();
            if (cause != null) {
                caused += ". "+cause.getMessage();
            }
            throw new NoDataException("Call for "+metadata.getTableName()+" data has failed. "+caused);
        }

        try {
            List<DataGroup.Attribute> attributes = IpacTableUtil.readAttributes(reader);
            for (DataGroup.Attribute a : attributes) {
                if (a.getKey().equalsIgnoreCase("ERROR")) {
                    throw new NoDataException(a.formatValue());
                }
            }
            columns = IpacTableUtil.readColumns(reader);
            return assignSourceDataTypes(metadata.getVoFields());
        } catch (Exception e) {
            Logger.error(e);
            throw new NoDataException(e.getMessage());
        }
    }

    public DataObject getNextRow() throws IOException {
        DataGroup dg = new DataGroup(null, columns);
        String line = reader.readLine();
        DataObject row = null;
        while (line != null) {
            row = IpacTableUtil.parseRow(dg, line);
            if (row != null) {
                return row;
            }
            line = reader.readLine();
        }
        return row;
    }

    private BufferedReader getDataReader(AdjustedMetadata obj, Map<String, String> paramMap, boolean isTest) throws IOException {
        String urlStr = obj.getServiceUrl();
        String paramName;
        boolean first = true;
        if (isTest) {
            // inlude parameters with testvalue set
            String testvalue;
            String encodedvalue;
            for (VoServiceParam voParam : obj.getVoParams()) {
                paramName = voParam.getName();
                testvalue = voParam.getTestValue();
                if (testvalue != null) {
                    try {
                        encodedvalue = URLEncoder.encode(testvalue, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Logger.error(e);
                        encodedvalue="failedtoencode";
                    }
                    urlStr += (first ? "?" : "&")+paramName+"="+encodedvalue;
                    paramMap.put(paramName, testvalue);
                    first = false;
                 }
             }

        } else {
            for (VoServiceParam voParam : obj.getVoParams()) {
                paramName = voParam.getName();
                String encodedvalue;
                if (paramMap.containsKey(paramName)) {
                    try {
                        encodedvalue = URLEncoder.encode(paramMap.get(paramName), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Logger.error(e);
                        encodedvalue="failedtoencode";
                    }

                    urlStr += (first ? "?" : "&")+paramName+"="+encodedvalue;
                    first = false;
                }
            }
        }
        Logger.briefDebug("Requesting data from "+urlStr);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)(new URL(urlStr)).openConnection();
            return new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } catch (IOException e) {

            if (conn != null) {
                    // the code below gets html encoded error report
                    //try {
                    //StringWriter sw = new StringWriter();
                    //BufferedReader ereader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    //String eline = ereader.readLine();
                    //while (eline != null) {
                    //    sw.append(eline+" ");
                    //    eline = ereader.readLine();
                    //}
                    //} catch (Exception ee) {}
                throw new IOException(e.getMessage()+" "+conn.getResponseMessage());
            } else {
                throw e;
            }
        }
    }


    private List<MappedField> assignSourceDataTypes(Collection<VoField> fields) {
        List<MappedField> mappedFields = new ArrayList<MappedField>();
        SimpleMappedField mappedFld;
        DataType sourceType;
        String format;
        metadata.resetFields();
        boolean skipField;
        for (VoField field : fields) {
            if (field.getIpacFields() != null) {
                skipField = false;
                mappedFld = new SimpleMappedField(field.getDefaultValue());
                for (IpacField ipacFld : field.getIpacFields()) {
                    sourceType = getColumn(ipacFld.getName(),  columns);
                    if (sourceType != null) {
                        format = ipacFld.getFormat();
                        if (format != null) {
                            sourceType.getFormatInfo().setDataFormat(format);
                        }
                        mappedFld.addSourceType(sourceType);
                    } else {
                        if (field.isOptional()) {
                            skipField = true;
                            break;
                        } else {
                            throw new IllegalArgumentException ("VO Field "+field.getName()+" source ("+ipacFld.getName()+") is not found.");
                        }
                    }
                }
                if (skipField) {
                    metadata.removeField(field);
                } else {
                    mappedFields.add(mappedFld);
                }
            } else {
                    if  (field.getDefaultValue() == null) {
                        throw new IllegalArgumentException ("VO Field "+field.getName()+" can not be mapped (mapping is not defined).");
                    }
            }
        }
        return mappedFields;
    }

    private DataType getColumn(String columnName, List<DataType> columns) {
        for (DataType dt : columns) {
            if (dt.getKeyName().equals(columnName)) {
                return dt;
            }
        }
        return null;
    }


    private static class AdjustedMetadata implements VOMetadata{
        private TableMapper _tm;
        private Collection<VoField> _fields;

        AdjustedMetadata(TableMapper tm) {
            _tm = tm;
            _fields = new ArrayList<VoField>(_tm.getVoFields());
        }

        void resetFields() {
            _fields = new ArrayList<VoField>(_tm.getVoFields());
        }

        void removeField(VoField field) {
            _fields.remove(field);
        }

        String getServiceUrl() {
            return _tm.getServiceUrl();
        }

        public String getTableName() {
            return _tm.getTableName();
        }

        public String getTableDesc() {
            return _tm.getTableDesc();
        }

        public Collection<VoField> getVoFields() {
            return _fields;
        }

        public Collection<VoServiceParam> getVoParams() {
            return _tm.getVoParams();
        }

    }
}
