package edu.caltech.ipac.heritage.data.entity.download;

import edu.caltech.ipac.heritage.data.entity.DataType;

import java.io.Serializable;

/**
 * Package request. Assuming user can request to package either a list of AORs, BCDs, or post-BCDs
 * @author tatianag
 * @version $Id: PackageRequest.java,v 1.8 2010/11/24 00:20:10 tatianag Exp $
 */
public abstract class PackageRequest implements Serializable {


    protected DataType[] dataTypes;
    private final String baseFileName;
    private final String title;
    private final String email;
    private final long   maxBundleSize;

    PackageRequest(DataType[] dataTypes,
                   String baseFileName,
                   String title,
                   String email,
                   long   maxBundleSize) {
        this.dataTypes = dataTypes;
        this.baseFileName= baseFileName;
        this.title= title;
        this.email= email;
        this.maxBundleSize= maxBundleSize;
    }

    public DataType[] getDataTypes() { return dataTypes; }
    public String getEmail() { return email; }
    public String getTitle() { return title; }
    public String getBaseFileName() { return baseFileName; }
    public long   getMaxBundleSize() { return maxBundleSize; }
    public abstract int [] getIds();


    /**
     * package request for a number of AORs
     */
    public static class AOR extends PackageRequest {

        //private static DataType [] defaultDataTypes = {DataType.BCD, DataType.BCD_ANCIL, DataType.PBCD, DataType.PBCD_ANCIL, DataType.CAL};

        AorPackageUnit [] packageUnits;

        int [] reqkeys;

        public AOR(AorPackageUnit [] reqUnits,
                   DataType[] dataTypes,
                   String baseFileName,
                   String title,
                   String email,
                   long   maxBundleSize) {
            super(dataTypes,baseFileName,title,email,maxBundleSize);
            this.packageUnits = reqUnits;
            reqkeys = new int[reqUnits.length];
            for (int i=0; i<reqkeys.length; i++) { reqkeys[i] = reqUnits[i].getReqkey(); }
        }

        public AorPackageUnit [] getPackageUnits() {
            return packageUnits;
        }

        public int[] getIds() {
            return reqkeys;
        }
    }

    /**
     *  package request for a number of BCDs
     */
    public static class BCD extends PackageRequest {

        private static DataType[] defaultDataTypes = {DataType.BCD, DataType.BCD_ANCIL};
        private int [] bcdIds;


        public BCD (int [] bcdIds,
                    String baseFileName,
                    String title,
                    String email,
                    long   maxBundleSize) {
            this(bcdIds, defaultDataTypes,baseFileName,title,email,maxBundleSize);
        }

        public BCD (int [] bcdIds,
                    DataType[] dataTypes,
                    String baseFileName,
                    String title,
                    String email,
                    long   maxBundleSize) {
            super(dataTypes,baseFileName,title,email,maxBundleSize);
            this.bcdIds = bcdIds;
        }

        public int[] getIds() {
            return bcdIds;
        }
    }

    /**
     *  package request for a number of post-BCDs
     */
    public static class PBCD extends PackageRequest {

        private static DataType[] defaultDataTypes = {DataType.PBCD, DataType.PBCD_ANCIL};
        private int [] pbcdIds;

        public PBCD (int [] pbcdIds,
                     String baseFileName,
                     String title,
                     String email,
                     long   maxBundleSize) {
            this(pbcdIds, defaultDataTypes,baseFileName,title,email,maxBundleSize);
        }

        public PBCD (int [] pbcdIds,
                     DataType[] dataTypes,
                     String baseFileName,
                     String title,
                     String email,
                     long   maxBundleSize) {
            super(dataTypes,baseFileName,title,email,maxBundleSize);
            this.pbcdIds = pbcdIds;
        }

        public int[] getIds() {
            return pbcdIds;
        }
    }

    /**
     *  package request for a number of post-BCDs
     */
    public static class SM extends PackageRequest {

        private static DataType[] defaultDataTypes = {DataType.SM, DataType.SM_ANCIL};
        private int [] smpIds;

        public SM (int [] smpIds,
                     String baseFileName,
                     String title,
                     String email,
                     long   maxBundleSize) {
            this(smpIds, defaultDataTypes,baseFileName,title,email, maxBundleSize);
        }

        public SM (int [] smpIds,
                     DataType[] dataTypes,
                     String baseFileName,
                     String title,
                     String email,
                     long maxBundleSize) {
            super(dataTypes,baseFileName,title,email,maxBundleSize);
            this.smpIds = smpIds;
        }

        public int[] getIds() {
            return smpIds;
        }
    }


    public static class AorPackageUnit implements Serializable {
        private int reqkey;
        private short channum;

        public AorPackageUnit(int reqkey) {
            this(reqkey, (short)-1);
        }

        /**
         * @param reqkey  request key
         * @param channum -1 for all channels
         */
        public AorPackageUnit(int reqkey, short channum) {
            this.reqkey = reqkey;
            this.channum = channum;
        }

        public int getReqkey() { return reqkey; }
        public short getChannum() {return channum; }
    }
}

