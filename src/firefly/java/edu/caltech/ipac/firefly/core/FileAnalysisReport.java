package edu.caltech.ipac.firefly.core;
/**
 * User: roby
 * Date: 3/25/20
 * Time: 11:00 AM
 */


import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Trey Roby
 */
public class FileAnalysisReport {

    public enum ReportType {Brief,              // expect to only get a report with one part without details
        Normal,             // a report with all parts populated, but not details
        Details}            // a full report with details

    public enum Type {Image, Table, Spectrum, HeaderOnly, PDF, TAR, REGION, PNG, ErrorResponse, LoadInBrowser, UWS, Unknown}
    public enum UIRender {Table, Chart, Image, NotSpecified}
    public enum UIEntry {UseSpecified, UseGuess, UseAll}
    public enum ChartTableDefOption {auto, showChart, showTable, showImage};



    private ReportType type;
    private long fileSize;
    private String filePath;
    private String fileName;
    private String fileFormat;
    private List<Part> parts;
    private String dataType;
    private boolean disableAllImagesOption = false;
    private String dataProductsAnalyzerId;
    private boolean analyzerFound= false;
    private Map<String,String> additionalImageParams;


    public FileAnalysisReport(ReportType type, String fileFormat, long fileSize, String filePath) {
        this.type = type;
        this.fileFormat = fileFormat;
        this.fileSize = fileSize;
        this.filePath = filePath;
    }

    public ReportType getType() {
        return type;
    }

    public String getFormat() {
        return fileFormat;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public List<Part> getParts() {
        return parts;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    public String getDataProductsAnalyzerId() { return dataProductsAnalyzerId; }
    public void setDataProductsAnalyzerId(String dataProductsAnalyzerId) {
        this.dataProductsAnalyzerId = dataProductsAnalyzerId;
    }

    public boolean isAnalyzerFound() { return analyzerFound; }
    public void setAnalyzerFound(boolean analyzerFound) { this.analyzerFound = analyzerFound; }

    public boolean isDisableAllImagesOption() { return disableAllImagesOption; }

    public void setDisableAllImagesOption(boolean d) { this.disableAllImagesOption = d; }

    public void replaceParts(List<Part> parts) {
        this.parts= new ArrayList<>(parts);
    }
    public void insertPartAtTop(Part part) { insertPart(0,part); }

    public void insertPart(int pos,Part part) {
        if (parts == null) parts = new ArrayList<>();
        parts.add(pos,part);
    }

    public void insertPartAfter(Part existingPart, Part newPart) {
        if (parts == null) parts = new ArrayList<>();
        int idx= parts.indexOf(existingPart);
        if (idx==-1 || idx+1>=parts.size()) parts.add(newPart);
        else parts.add(idx+1,newPart);


    }

    public void addPart(Part part) {
        if (parts == null) parts = new ArrayList<>();
        parts.add(part);
    }

    public void setPart(Part part, int i) { if (i<parts.size()) parts.set(i,part); }
    public Part getPart(int i) { return parts.get(i); }

    public String getDataType() {
        if (dataType == null) {
            if (parts != null) {
                if (parts.size() == 1) {
                    dataType = parts.get(0).type.name();
                } else {
                    List<String> types = parts.stream().map(part -> part.type.name()).distinct().collect(Collectors.toList());
                    dataType = StringUtils.toString(types);
                }
            } else {
                dataType = "";
            }
        }
        return dataType;
    }


    public void setAdditionalImageParams(Map<String,String> additionalImageParams){
        this.additionalImageParams = additionalImageParams;
    }


    public Map<String, String> getAdditionalImageParams(){
        return this.additionalImageParams;
    }




    public FileAnalysisReport copy() { return copy(true); }

    public FileAnalysisReport copy(boolean includeParts) {
        FileAnalysisReport r= new FileAnalysisReport(type,fileFormat,fileSize,filePath);
        r.fileName= this.fileName;
        if (includeParts) r.parts= parts.stream().map(Part::copy).collect(Collectors.toList());
        r.dataType= this.dataType;
        r.additionalImageParams = this.additionalImageParams;
        return r;
    }

    /**
     * convert this report into a Brief version.
     */
    void makeBrief() {
        if (type == ReportType.Brief) return;       // nothing to do
        getDataType();  // init dataType
        if (parts != null) {
            // keep only the first part with data.
            Part first = parts.stream()
                    .filter(p -> !Arrays.asList(Type.HeaderOnly, Type.Unknown).contains(p.getType()))
                    .findFirst()
                    .orElse(null);
            if (first != null) {
                parts = Collections.singletonList(first);
            }
        }
    }


    public static class Part {
        private final Type type;
        private UIRender uiRender=UIRender.NotSpecified;
        private UIEntry uiEntry= UIEntry.UseAll;
        private String url; //this is only used by UWS for now
        private int index= 0;
        private String desc;
        private int fileLocationIndex = -1;   // todo: populate: fits (hdu idx) or VO (table idx)
        private String convertedFileName= null; // only set if this entry is has a alternate file than the one analyzed
        private String convertedFileFormat = null;
        private List<ChartParams> chartParams= null;
        private List<String> tableColumnNames= null; //only use for a fits image that is read as a table
        private List<String> tableColumnUnits= null; //only use for a fits image that is read as a table
        private boolean defaultPart= false;
        private boolean interpretedData= false;
        private String searchProcessorId="";
        private DataGroup details;
        private ChartTableDefOption chartTableDefOption= ChartTableDefOption.auto;
        private int totalTableRows=-1;

        public Part(Type type) {
            this(type,null);
        }

        public Part(Type type, String desc) {
            this.type = type;
            this.desc = desc;
            if (type.equals(Type.Image)){
                setChartTableDefOption(ChartTableDefOption.showImage);
            }
        }

        public Type getType() { return type; }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }

        public String getDesc() { return desc;}
        public void setDesc(String desc) { this.desc = desc; }

        public DataGroup getDetails() {
            return details;
        }
        public void setDetails(DataGroup details) {
            this.details = details;
        }

        public int getTotalTableRows() { return totalTableRows; }
        public void setTotalTableRows(int totalTableRows) { this.totalTableRows = totalTableRows; }


        public int getFileLocationIndex() { return fileLocationIndex; }
        public void setFileLocationIndex(int fileLocationIndex) { this.fileLocationIndex = fileLocationIndex; }

        public String getConvertedFileName() { return convertedFileName; }
        public void setConvertedFileName(String convertedFileName) { this.convertedFileName = convertedFileName; }

        public String getConvertedFileFormat() { return convertedFileFormat; }
        public void setConvertedFileFormat(String convertedFileFormat) { this.convertedFileFormat = convertedFileFormat; }

        public List<ChartParams> getChartParams() { return chartParams; }
        public void setChartParams(ChartParams chartParams) {
            this.chartParams = Collections.singletonList(chartParams);
        }
        public void setChartParams(List<ChartParams> chartParams) {
            this.chartParams = chartParams;
        }

        public List<String> getTableColumnNames() { return tableColumnNames; }
        public void setTableColumnNames(List<String> tableColumnNames) { this.tableColumnNames = tableColumnNames; }

        public List<String> getTableColumnUnits() { return tableColumnUnits; }
        public void setTableColumnUnits(List<String> tableColumnUnits) { this.tableColumnUnits = tableColumnUnits; }

        public boolean isDefaultPart() { return defaultPart; }
        public void setDefaultPart(boolean defaultPart) { this.defaultPart = defaultPart; }

        public boolean isInterpretedData() { return interpretedData; }
        public void setInterpretedData(boolean interpretedData) { this.interpretedData = interpretedData; }

        public UIRender getUiRender() { return uiRender; }
        public void setUiRender(UIRender uiRender) { this.uiRender = uiRender; }

        public UIEntry getUiEntry() { return uiEntry; }
        public void setUiEntry(UIEntry uiEntry) { this.uiEntry = uiEntry; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public void setSearchProcessorId(String searchProcessorId) {this.searchProcessorId=searchProcessorId;}
        public String getSearchProcessorId() {return searchProcessorId;}

        public ChartTableDefOption getChartTableDefOption() { return chartTableDefOption; }
        public void setChartTableDefOption(ChartTableDefOption chartTableDefOption) {
            this.chartTableDefOption = chartTableDefOption;
        }

        public Part copy() {
            Part p= new Part(type, desc);
            p.details= details;
            p.uiRender= uiRender;
            p.uiEntry= uiEntry;
            p.url = url;
            p.index= index;
            p.fileLocationIndex = fileLocationIndex;
            p.convertedFileName= convertedFileName;
            if (chartParams!=null) {
                p.chartParams = chartParams.stream().map(ChartParams::copy).distinct().collect(Collectors.toList());
            }
            p.tableColumnNames= tableColumnNames!=null ? new ArrayList<>(tableColumnNames) : null;
            p.tableColumnUnits= tableColumnUnits!=null ? new ArrayList<>(tableColumnUnits) : null;
            p.defaultPart= defaultPart;
            p.interpretedData = interpretedData;
            return p;
        }
    }

    public static class ChartParams {

        public enum ChartType {XYChart, Histogram };

        private boolean layoutAdds= false;
        private boolean simpleData= true;
        private ChartType simpleChartType= ChartType.XYChart;
        private Map<String,Object> layout= null;
        private String xAxisColName = null;
        private String yAxisColName= null;
        private String mode= null;
        private int numBins= 0;
        private List<Map<String,Object>> traces= null;

        public String getxAxisColName() { return xAxisColName; }
        public void setxAxisColName(String xAxisColName) { this.xAxisColName = xAxisColName; }
        public String getyAxisColName() { return yAxisColName; }
        public void setyAxisColName(String yAxisColName) { this.yAxisColName = yAxisColName; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public int getNumBins() { return numBins; }

        public void setNumBins(int numBins) { this.numBins = numBins; }

        public ChartType getSimpleChartType() { return simpleChartType; }
        public void setSimpleChartType(ChartType simpleChartType) { this.simpleChartType = simpleChartType; }

        public void setLayout(Map<String,Object> layout) {
            layoutAdds= false;
            this.layout= layout;
        }

        public void addLayout(Map<String,Object> layoutEntries) {
            layoutAdds= true;
            if (layout==null) layout= new HashMap<>();
            layout.putAll(layoutEntries);
        }

        public Map<String, Object> getLayout() {
            return layout;
        }

        public List<Map<String, Object>> getTraces() {
            return traces;
        }

        public void setTraces(List<Map<String,Object>> traces) {
            simpleData= false;
            this.traces= traces;
        }

        public boolean isLayoutAdds() { return layoutAdds; }
        public boolean isSimpleData() { return simpleData; }

        public ChartParams copy() {
            ChartParams c= new ChartParams();
            c.layoutAdds= layoutAdds;
            c.simpleData= simpleData;
            c.simpleChartType= simpleChartType;
            c.layout= layout!=null ? new HashMap<>(layout) : null;
            c.xAxisColName = xAxisColName;
            c.yAxisColName= yAxisColName;
            c.mode= mode;
            c.numBins= numBins;
            if (traces!=null) {
                c.traces = traces.stream().map(HashMap::new).distinct().collect(Collectors.toList());
            }
            return c;
        }

    }
}
