package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.persistence.TempTable;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.data.entity.download.PackageRequest;
import edu.caltech.ipac.util.CollectionUtil;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author tatianag
 * @version $Id: FileInfoDao.java,v 1.49 2012/10/02 20:53:55 tatianag Exp $
 */
public class FileInfoDao {

    private static final Logger.LoggerImpl _log= Logger.getLogger();

    // using list - BCD
    static String SQL1_BCD = "FROM bcdproducts p WHERE p.bcdid in (";
    static String SQL1_BCD_ANCIL1 = "FROM bcdancilproducts p, bcdproducts o WHERE p.importancelevel = 1 and p.bcdid=o.bcdid and o.bcdid in (";
    static String SQL1_BCD_ANCIL2 = "FROM bcdancilproducts p, bcdproducts o WHERE p.importancelevel = 2 and p.bcdid=o.bcdid and o.bcdid in (";
    //static String SQL1_BCD_RAW = "FROM rawdatafiles p, bcdproducts o WHERE p.dceid=o.dceid and o.bcdid in (";
    static String SQL1_BCD_RAW_IRAC = "FROM dceinformation p, bcdproducts o WHERE o.instrument=='IRAC' and p.dceid=o.dceid and o.bcdid in (";
    //AR9682 static String SQL1_BCD_RAW_IRAC = "FROM dceinformation p, bcdproducts o WHERE o.instrument=='IRAC' and p.reqkey=o.reqkey and p.channum=o.channum and o.bcdid in (";
    static String SQL1_BCD_RAW_NOTIRAC = "FROM bcdancilproducts p, bcdproducts o WHERE o.instrument<>'IRAC' and p.bcdid=o.bcdid and p.importancelevel=0 and p.bcdid in (";
    static String SQL1_BCD_CAL = "FROM cal2bcd c, calibrationproducts p, bcdproducts pp where c.cpid=p.cpid and c.bcdid=pp.bcdid and c.bcdid in (";
    static String SQL1_BCD_CAL_ANCIL = "FROM cal2bcd c, calibrationancilproducts p, bcdproducts pp where c.cpid=p.cpid and c.bcdid=pp.bcdid and c.bcdid in (";
    static String SQL1_BCD_CAL_CAL = "FROM cal2cal c, calibrationproducts p, bcdproducts pp where c.calcpid=p.cpid and c.reqkey=pp.reqkey and c.channum=pp.channum and pp.bcdid in (";
    static String SQL1_BCD_CAL_CAL_ANCIL = "FROM cal2cal c, calibrationancilproducts p, bcdproducts pp where c.calcpid=p.cpid and c.reqkey=pp.reqkey and c.channum=pp.channum and pp.bcdid in (";
    static String SQL1_BCD_CAL_REQ = "FROM calibrationproducts p, bcdproducts pp where p.reqkey=pp.reqkey and p.channum=pp.channum and pp.bcdid in (";
    static String SQL1_BCD_CAL_REQ_ANCIL = "FROM calibrationproducts c, calibrationancilproducts p, bcdproducts pp where c.cpid=p.cpid and c.reqkey=pp.reqkey and c.channum=pp.channum and pp.bcdid in (";
    static String SQL1_BCD_QA = "FROM requestinformation p, bcdproducts pp WHERE p.reqkey=pp.reqkey and pp.bcdid in (";

    // using list - PBCD
    static String SQL1_PBCD = "FROM postbcdproducts p WHERE p.pbcdid in (";
    static String SQL1_PBCD_ANCIL1 = "FROM postbcdancilproducts p, postbcdproducts o WHERE p.importancelevel = 1 and p.pbcdid=o.pbcdid and o.pbcdid in (";
    static String SQL1_PBCD_ANCIL2 = "FROM postbcdancilproducts p, postbcdproducts o WHERE p.importancelevel = 2 and p.pbcdid=o.pbcdid and o.pbcdid in (";
    static String SQL1_PBCD_BCD = "FROM bcdproducts p, dcesets s, postbcdproducts o WHERE o.dcesetid=s.dcesetid and s.dceid=p.dceid and o.pbcdid in (";
    static String SQL1_PBCD_BCD_ANCIL1 = "FROM bcdancilproducts p where importancelevel=1 and bcdid in "+
                                            "(select bcdid from bcdproducts where dceid in "+
                                            "(select dceid from dcesets where dcesetid in "+
                                            "(select dcesetid from postbcdproducts where pbcdid in (?))))";
    //static String SQL1_PBCD_BCD_ANCIL1 = "FROM bcdancilproducts p, bcdproducts pp, dcesets s, postbcdproducts o WHERE p.importancelevel = 1 and p.bcdid=pp.bcdid and o.dcesetid=s.dcesetid and s.dceid=pp.dceid and o.pbcdid in (";
    static String SQL1_PBCD_BCD_ANCIL2 = "FROM bcdancilproducts p where importancelevel=2 and bcdid in "+
                                            "(select bcdid from bcdproducts where dceid in "+
                                            "(select dceid from dcesets where dcesetid in "+
                                            "(select dcesetid from postbcdproducts where pbcdid in (?))))";
    //static String SQL1_PBCD_BCD_ANCIL2 = "FROM bcdancilproducts p, bcdproducts pp, dcesets s, postbcdproducts o WHERE p.importancelevel = 2 and p.bcdid=pp.bcdid and o.dcesetid=s.dcesetid and s.dceid=pp.dceid and o.pbcdid in (";
    //static String SQL1_PBCD_RAW = "FROM rawdatafiles p, dcesets s, postbcdproducts o WHERE o.dcesetid=s.dcesetid and s.dceid=p.dceid and o.pbcdid in (";
    //static String SQL1_PBCD_RAW_IRAC = "FROM dceinformation p, dcesets s, postbcdproducts o WHERE  p.instrument=='IRAC' and o.dcesetid=s.dcesetid and s.dceid=p.dceid and o.pbcdid in (";
    static String SQL1_PBCD_RAW_IRAC = "FROM dceinformation p where dceid in "+
                                            "(select dceid from dcesets where dcesetid in "+
                                            "(select dcesetid from postbcdproducts where pbcdid in (?))) "+
                                            "and instrument=='IRAC'";
    //AR9682 static String SQL1_PBCD_RAW_IRAC = "FROM dceinformation p, postbcdproducts o WHERE  p.instrument=='IRAC' and o.reqkey=p.reqkey and o.channum=p.channum and o.pbcdid in (";
    //static String SQL1_PBCD_RAW_NOTIRAC = "FROM bcdancilproducts p, bcdproducts b, dcesets s, postbcdproducts o WHERE  b.instrument<>'IRAC' and o.dcesetid=s.dcesetid and s.dceid=b.dceid and b.bcdid=p.bcdid and p.importancelevel=0 and o.pbcdid in (";
    static String SQL1_PBCD_RAW_NOTIRAC = "FROM bcdancilproducts p where importancelevel=0 and bcdid in "+
                                            "(select bcdid from bcdproducts where instrument<>'IRAC' and dceid in "+
                                            "(select dceid from dcesets where dcesetid in "+
                                            "(select dcesetid from postbcdproducts where pbcdid in (?))))";
    static String SQL1_PBCD_CAL = "FROM cal2postbcd c, calibrationproducts p, postbcdproducts pp where c.cpid=p.cpid and c.pbcdid=pp.pbcdid and c.pbcdid in (";
    static String SQL1_PBCD_CAL_ANCIL = "FROM cal2postbcd c, calibrationancilproducts p, postbcdproducts pp where c.cpid=p.cpid and c.pbcdid=pp.pbcdid and c.pbcdid in (";
    static String SQL1_PBCD_CAL_CAL = "FROM cal2cal c, calibrationproducts p, postbcdproducts pp where c.calcpid=p.cpid and c.reqkey=pp.reqkey and c.channum=pp.channum and pp.pbcdid in (";
    static String SQL1_PBCD_CAL_CAL_ANCIL = "FROM cal2cal c, calibrationancilproducts p, postbcdproducts pp where c.calcpid=p.cpid and c.reqkey=pp.reqkey and c.channum=pp.channum and pp.pbcdid in (";
    static String SQL1_PBCD_CAL_REQ = "FROM calibrationproducts p, postbcdproducts pp where p.reqkey=pp.reqkey and p.channum=pp.channum and pp.pbcdid in (";
    static String SQL1_PBCD_CAL_REQ_ANCIL = "FROM calibrationproducts c, calibrationancilproducts p, postbcdproducts pp where c.cpid=p.cpid and c.reqkey=pp.reqkey and c.channum=pp.channum and pp.pbcdid in (";
    static String SQL1_PBCD_QA = "FROM requestinformation p, postbcdproducts pp WHERE p.reqkey=pp.reqkey and pp.pbcdid in (";

    // using list - AOR
    static String SQL1_AOR_BCD = "FROM bcdproducts p WHERE p.reqkey in (";
    static String SQL1_AOR_BCD_ANCIL1 = "FROM bcdancilproducts p, bcdproducts o WHERE p.importancelevel = 1 and p.bcdid=o.bcdid and o.reqkey in (";
    static String SQL1_AOR_BCD_ANCIL2 = "FROM bcdancilproducts p, bcdproducts o WHERE p.importancelevel = 2 and p.bcdid=o.bcdid and o.reqkey in (";
    static String SQL1_AOR_PBCD = "FROM postbcdproducts p WHERE p.reqkey in (";
    static String SQL1_AOR_PBCD_ANCIL1 = "FROM postbcdancilproducts p, postbcdproducts o WHERE p.importancelevel = 1 and p.pbcdid=o.pbcdid and o.reqkey in (";
    static String SQL1_AOR_PBCD_ANCIL2 = "FROM postbcdancilproducts p, postbcdproducts o WHERE p.importancelevel = 2 and p.pbcdid=o.pbcdid and o.reqkey in (";    
    //static String SQL1_AOR_RAW = "FROM rawdatafiles p, dceinformation d WHERE p.dceid=d.dceid and d.reqkey in (";
    // AR9358 static String SQL1_AOR_RAW_IRAC = "FROM dceinformation p, bcdproducts o WHERE p.instrument=='IRAC' and p.dceid=o.dceid and p.reqkey in (";
    static String SQL1_AOR_RAW_IRAC = "FROM dceinformation p WHERE p.instrument=='IRAC' and  p.reqkey in (";
    static String SQL1_AOR_RAW_NOTIRAC = "FROM bcdancilproducts p, bcdproducts o WHERE o.instrument<>'IRAC' and p.bcdid=o.bcdid and p.importancelevel=0 and o.reqkey in (";
    static String SQL1_AOR_BCD_CAL ="FROM calibrationproducts p, cal2bcd c, bcdproducts pp WHERE pp.bcdid=c.bcdid and c.cpid=p.cpid and pp.reqkey in (";
    static String SQL1_AOR_BCD_CAL_ANCIL ="FROM calibrationancilproducts p, cal2bcd c, bcdproducts pp WHERE pp.bcdid=c.bcdid and c.cpid=p.cpid and pp.reqkey in (";
    static String SQL1_AOR_PBCD_CAL ="FROM calibrationproducts p, cal2postbcd c, postbcdproducts pp WHERE pp.pbcdid=c.pbcdid and c.cpid=p.cpid and pp.reqkey in (";
    static String SQL1_AOR_PBCD_CAL_ANCIL ="FROM calibrationancilproducts p, cal2postbcd c, postbcdproducts pp WHERE pp.pbcdid=c.pbcdid and c.cpid=p.cpid and pp.reqkey in (";
    static String SQL1_AOR_CAL_CAL ="FROM calibrationproducts p, cal2cal pp WHERE pp.calcpid=p.cpid and pp.reqkey in (";
    static String SQL1_AOR_CAL_CAL_ANCIL ="FROM calibrationancilproducts p, cal2cal pp WHERE pp.calcpid=p.cpid and pp.reqkey in (";
    static String SQL1_AOR_CAL_REQ ="FROM calibrationproducts p, calibrationproducts pp WHERE p.cpid=pp.cpid and p.reqkey in (";
    static String SQL1_AOR_CAL_REQ_ANCIL ="FROM calibrationancilproducts p, calibrationproducts pp WHERE p.cpid=pp.cpid and p.reqkey in (";
    static String SQL1_AOR_QA = "FROM requestinformation p WHERE p.reqkey in (";

    // using list - SM
    static String SQL1_SM = "FROM smproducts p WHERE p.smpid in (";
    static String SQL1_SM_ANCIL1 = "FROM smancilproducts p, smproducts o WHERE p.importancelevel = 1 and p.smpid=o.smpid and o.smpid in (";
    static String SQL1_SM_ANCIL2 = "FROM smancilproducts p, smproducts o WHERE p.importancelevel = 2 and p.smpid=o.smpid and o.smpid in (";
    static String SQL1_SM_BCD = "FROM bcdproducts p, dcesets s, smproducts o WHERE o.dcesetid=s.dcesetid and s.dceid=p.dceid and o.smpid in (";
    static String SQL1_SM_BCD_ANCIL1 = "FROM bcdancilproducts p, bcdproducts pp, dcesets s, smproducts o WHERE p.importancelevel = 1 and p.bcdid=pp.bcdid and o.dcesetid=s.dcesetid and s.dceid=pp.dceid and o.smpid in (";
    static String SQL1_SM_BCD_ANCIL2 = "FROM bcdancilproducts p, bcdproducts pp, dcesets s, smproducts o WHERE p.importancelevel = 2 and p.bcdid=pp.bcdid and o.dcesetid=s.dcesetid and s.dceid=pp.dceid and o.smpid in (";
    // TODO: do we need to support RAW, CAL, QA?

    // using temp table - BCD
    static String SQL2_BCD = "FROM temp_bcd_ids t, bcdproducts p WHERE p.bcdid=t.bcdid";
    static String SQL2_BCD_ANCIL1 = "FROM temp_bcd_ids t, bcdancilproducts p WHERE p.importancelevel = 1 and p.bcdid=t.bcdid";
    static String SQL2_BCD_ANCIL2 = "FROM temp_bcd_ids t, bcdancilproducts p WHERE p.importancelevel = 2 and p.bcdid=t.bcdid";
    //static String SQL2_BCD_RAW = "FROM temp_bcd_ids t, bcdproducts o, rawdatafiles p WHERE o.bcdid=t.bcdid and o.dceid=p.dceid";
    static String SQL2_BCD_RAW_IRAC = "FROM temp_bcd_ids t, bcdproducts o, dceinformation p WHERE o.instrument=='IRAC' and o.bcdid=t.bcdid and o.dceid=p.dceid";
    //AR9682 static String SQL2_BCD_RAW_IRAC = "FROM temp_bcd_ids t, bcdproducts o, dceinformation p WHERE p.instrument=='IRAC' and o.bcdid=t.bcdid and p.reqkey=o.reqkey and p.channum=o.channum";
    static String SQL2_BCD_RAW_NOTIRAC = "FROM temp_bcd_ids t, bcdancilproducts p, bcdproducts o WHERE o.bcdid=t.bcdid and o.instrument<>'IRAC' and p.bcdid=o.bcdid and p.importancelevel=0";
    static String SQL2_BCD_CAL = "FROM temp_bcd_ids t, cal2bcd c, calibrationproducts p, bcdproducts pp WHERE c.cpid=p.cpid and c.bcdid=t.bcdid and c.bcdid=pp.bcdid";
    static String SQL2_BCD_CAL_ANCIL = "FROM temp_bcd_ids t, cal2bcd c, calibrationancilproducts p, bcdproducts pp WHERE c.cpid=p.cpid and c.bcdid=t.bcdid and c.bcdid=pp.bcdid";
    static String SQL2_BCD_CAL_CAL = "FROM temp_bcd_ids t, cal2cal c, calibrationproducts p, bcdproducts pp WHERE c.calcpid=p.cpid and c.reqkey=pp.reqkey and c.channum=pp.channum and pp.bcdid=t.bcdid";
    static String SQL2_BCD_CAL_CAL_ANCIL = "FROM temp_bcd_ids t, cal2cal c, calibrationancilproducts p, bcdproducts pp WHERE c.calcpid=p.cpid and c.reqkey=pp.reqkey and c.channum=pp.channum and pp.bcdid=t.bcdid";
    static String SQL2_BCD_CAL_REQ = "FROM temp_bcd_ids t, calibrationproducts p, bcdproducts pp WHERE p.reqkey=pp.reqkey and p.channum=pp.channum and pp.bcdid=t.bcdid";
    static String SQL2_BCD_CAL_REQ_ANCIL = "FROM temp_bcd_ids t, calibrationproducts c, calibrationancilproducts p, bcdproducts pp WHERE c.cpid=p.cpid and c.reqkey=pp.reqkey and c.channum=pp.channum and pp.bcdid=t.bcdid";
    static String SQL2_BCD_QA = "FROM temp_bcd_ids t, bcdproducts pp, requestinformation p WHERE t.bcdid=pp.bcdid and pp.reqkey=p.reqkey";

    // using temp table - PBCD
    static String SQL2_PBCD = "FROM temp_pbcd_ids t, postbcdProducts p WHERE p.pbcdid=t.pbcdid";
    static String SQL2_PBCD_ANCIL1 = "FROM temp_pbcd_ids t, postbcdancilproducts p WHERE p.importancelevel = 1 and p.pbcdid=t.pbcdid";
    static String SQL2_PBCD_ANCIL2 = "FROM temp_pbcd_ids t, postbcdancilproducts p WHERE p.importancelevel = 2 and p.pbcdid=t.pbcdid";
    static String SQL2_PBCD_BCD = "FROM temp_pbcd_ids t, bcdproducts p, dcesets s, postbcdproducts o WHERE o.dcesetid=s.dcesetid and s.dceid=p.dceid and o.pbcdid=t.pbcdid";
    static String SQL2_PBCD_BCD_ANCIL1 = "FROM bcdancilproducts p where importancelevel=1 and bcdid in "+
                                            "(select bcdid from bcdproducts where dceid in "+
                                            "(select dceid from dcesets where dcesetid in "+
                                            "(select dcesetid from postbcdproducts where pbcdid in (select pbcdid from temp_pbcd_ids))))";
    //static String SQL2_PBCD_BCD_ANCIL1 = "FROM temp_pbcd_ids t, bcdancilproducts p, bcdproducts pp, dcesets s, postbcdproducts o WHERE p.importancelevel = 1 and p.bcdid=pp.bcdid and o.dcesetid=s.dcesetid and s.dceid=pp.dceid and o.pbcdid=t.pbcdid";
    static String SQL2_PBCD_BCD_ANCIL2 = "FROM bcdancilproducts p where importancelevel=2 and bcdid in "+
                                            "(select bcdid from bcdproducts where dceid in "+
                                            "(select dceid from dcesets where dcesetid in "+
                                            "(select dcesetid from postbcdproducts where pbcdid in (select pbcdid from temp_pbcd_ids))))";
    //static String SQL2_PBCD_BCD_ANCIL2 = FROM temp_pbcd_ids t, bcdancilproducts p, bcdproducts pp, dcesets s, postbcdproducts o WHERE p.importancelevel = 2 and p.bcdid=pp.bcdid and o.dcesetid=s.dcesetid and s.dceid=pp.dceid and o.pbcdid=t.pbcdid";
    //static String SQL2_PBCD_RAW = "FROM temp_pbcd_ids t, rawdatafiles p, dcesets s, postbcdproducts o WHERE o.dcesetid=s.dcesetid and s.dceid=p.dceid and o.pbcdid=t.pbcdid";
    //static String SQL2_PBCD_RAW_IRAC = "FROM temp_pbcd_ids t, dceinformation p, dcesets s, postbcdproducts o WHERE p.instrument=='IRAC' and o.dcesetid=s.dcesetid and s.dceid=p.dceid and o.pbcdid=t.pbcdid";
    static String SQL2_PBCD_RAW_IRAC = "FROM dceinformation p where dceid in "+
                                            "(select dceid from dcesets where dcesetid in "+
                                            "(select dcesetid from postbcdproducts where pbcdid in (select pbcdid from temp_pbcd_ids))) "+
                                            "and instrument=='IRAC'";
    //AR9682 static String SQL2_PBCD_RAW_IRAC = "FROM temp_pbcd_ids t, dceinformation p, postbcdproducts o WHERE p.instrument=='IRAC' and o.pbcdid=t.pbcdid and p.reqkey=o.reqkey and p.channum=o.channum";
    //static String SQL2_PBCD_RAW_NOTIRAC = "FROM temp_pbcd_ids t, bcdancilproducts p, bcdproducts b, dcesets s, postbcdproducts o WHERE b.instrument<>'IRAC' and o.dcesetid=s.dcesetid and s.dceid=b.dceid and b.bcdid=p.bcdid and p.importancelevel=0 and o.pbcdid=t.pbcdid";
    static String SQL2_PBCD_RAW_NOTIRAC = "FROM bcdancilproducts p where bcdid in "+
                                            "(select bcdid from bcdproducts where instrument<>'IRAC' and dceid in "+
                                            "(select dceid from dcesets where dcesetid in "+
                                            "(select dcesetid from postbcdproducts where pbcdid in (select pbcdid from temp_pbcd_ids)))) "+
                                            "and importancelevel=0";

    static String SQL2_PBCD_CAL = "FROM temp_pbcd_ids t, cal2postbcd c, calibrationproducts p, postbcdproducts pp where c.cpid=p.cpid and c.pbcdid=t.pbcdid and c.pbcdid=pp.pbcdid";
    static String SQL2_PBCD_CAL_ANCIL = "FROM temp_pbcd_ids t, cal2postbcd c, calibrationancilproducts p, postbcdproducts pp where c.cpid=p.cpid and c.pbcdid=t.pbcdid and c.pbcdid=pp.pbcdid";
    static String SQL2_PBCD_CAL_CAL = "FROM temp_pbcd_ids t, cal2cal c, calibrationproducts p, postbcdproducts pp where c.calcpid=p.cpid and c.reqkey=pp.reqkey and c.channum=pp.channum and pp.pbcdid=t.pbcdid";
    static String SQL2_PBCD_CAL_CAL_ANCIL = "FROM temp_pbcd_ids t, cal2cal c, calibrationancilproducts p, postbcdproducts pp where c.calcpid=p.cpid and c.reqkey=pp.reqkey and c.channum=pp.channum and pp.pbcdid=t.pbcdid";
    static String SQL2_PBCD_CAL_REQ = "FROM temp_pbcd_ids t, calibrationproducts p, postbcdproducts pp where p.reqkey=pp.reqkey and p.channum=pp.channum and pp.pbcdid=t.pbcdid";
    static String SQL2_PBCD_CAL_REQ_ANCIL = "FROM temp_pbcd_ids t, calibrationproducts c, calibrationancilproducts p, postbcdproducts pp where c.cpid=p.cpid and c.reqkey=pp.reqkey and c.channum=pp.channum and pp.pbcdid=t.pbcdid";
    static String SQL2_PBCD_QA = "FROM temp_pbcd_ids t, postbcdproducts pp, requestinformation p WHERE t.pbcdid=pp.pbcdid and pp.reqkey=p.reqkey";


    // using temp table - AOR
    static String SQL2_AOR_BCD = "FROM temp_aors t, bcdproducts p WHERE t.reqkey=p.reqkey and t.channum=p.channum";
    static String SQL2_AOR_BCD_ANCIL1 = "FROM temp_aors t, bcdproducts pp, bcdancilproducts p WHERE p.importancelevel = 1 and t.reqkey=pp.reqkey and t.channum=pp.channum and pp.bcdid=p.bcdid";
    static String SQL2_AOR_BCD_ANCIL2 = "FROM temp_aors t, bcdproducts pp, bcdancilproducts p WHERE p.importancelevel = 2 and t.reqkey=pp.reqkey and t.channum=pp.channum and pp.bcdid=p.bcdid";
    static String SQL2_AOR_PBCD = "FROM temp_aors t, postbcdproducts p WHERE t.reqkey=p.reqkey and t.channum=p.channum"; //TODO: use chanmask?
    static String SQL2_AOR_PBCD_ANCIL1 = "FROM temp_aors t, postbcdproducts pp, postbcdancilproducts p WHERE p.importancelevel = 1 and t.reqkey=pp.reqkey and t.channum=pp.channum and pp.pbcdid=p.pbcdid"; //TODO: use chanmask?
    static String SQL2_AOR_PBCD_ANCIL2 = "FROM temp_aors t, postbcdproducts pp, postbcdancilproducts p WHERE p.importancelevel = 2 and t.reqkey=pp.reqkey and t.channum=pp.channum and pp.pbcdid=p.pbcdid"; //TODO: use chanmask?
    //AR9358 static String SQL2_AOR_RAW = "FROM temp_aors t, rawdatafiles p, dceinformation d WHERE t.reqkey=d.reqkey p.dceid=d.dceid and t.channum=d.channum";
    //static String SQL2_AOR_RAW_IRAC = "FROM temp_aors t, dceinformation p, bcdproducts o WHERE p.instrument='IRAC' and p.dceid=o.dceid and t.reqkey=p.reqkey and t.channum=p.channum";
    static String SQL2_AOR_RAW_IRAC = "FROM temp_aors t, dceinformation p WHERE p.instrument='IRAC' and t.reqkey=p.reqkey and t.channum=p.channum";
    static String SQL2_AOR_RAW_NOTIRAC = "FROM temp_aors t, bcdancilproducts p, bcdproducts o WHERE o.instrument<>'IRAC' and p.bcdid=o.bcdid and p.importancelevel=0 and t.reqkey=o.reqkey and t.channum=o.channum";
    static String SQL2_AOR_BCD_CAL = "FROM temp_aors t, calibrationproducts p, cal2bcd c, bcdproducts pp WHERE pp.bcdid=c.bcdid and c.cpid=p.cpid and t.reqkey=pp.reqkey and t.channum=pp.channum"; //TODO: use chanmask?
    static String SQL2_AOR_BCD_CAL_ANCIL = "FROM temp_aors t, calibrationancilproducts p, cal2bcd c, bcdproducts pp WHERE pp.bcdid=c.bcdid and c.cpid=p.cpid and t.reqkey=pp.reqkey and t.channum=pp.channum"; //TODO: use chanmask?
    static String SQL2_AOR_PBCD_CAL = "FROM temp_aors t, calibrationproducts p, cal2postbcd c, postbcdproducts pp  WHERE pp.pbcdid=c.pbcdid and c.cpid=p.cpid and t.reqkey=pp.reqkey and t.channum=pp.channum"; //TODO: use chanmask?
    static String SQL2_AOR_PBCD_CAL_ANCIL = "FROM temp_aors t, calibrationancilproducts p, cal2postbcd c, postbcdproducts pp  WHERE pp.pbcdid=c.pbcdid and c.cpid=p.cpid and t.reqkey=pp.reqkey and t.channum=pp.channum"; //TODO: use chanmask?
    static String SQL2_AOR_CAL_CAL = "FROM temp_aors t, calibrationproducts p, cal2cal pp WHERE pp.calcpid=p.cpid and t.reqkey=pp.reqkey and t.channum=pp.channum"; //TODO: use chanmask?
    static String SQL2_AOR_CAL_CAL_ANCIL = "FROM temp_aors t, calibrationancilproducts p, cal2cal pp WHERE pp.calcpid=p.cpid and t.reqkey=pp.reqkey and t.channum=pp.channum"; //TODO: use chanmask?
    static String SQL2_AOR_CAL_REQ = "FROM temp_aors pp, calibrationproducts p WHERE pp.reqkey=p.reqkey and pp.channum=p.channum"; //TODO: use chanmask?
    static String SQL2_AOR_CAL_REQ_ANCIL = "FROM temp_aors t, calibrationancilproducts p, calibrationproducts pp WHERE pp.cpid=p.cpid and t.reqkey=pp.reqkey and t.channum=pp.channum"; //TODO: use chanmask?

    static String SQL2_AOR_QA = "FROM temp_aors t, requestinformation p WHERE t.reqkey = p.reqkey";

    // using temp table - SM
    static String SQL2_SM = "FROM temp_smp_ids t, smProducts p WHERE p.smpid=t.smpid";
    static String SQL2_SM_ANCIL1 = "FROM temp_smp_ids t, smancilproducts p WHERE p.importancelevel = 1 and p.smpid=t.smpid";
    static String SQL2_SM_ANCIL2 = "FROM temp_smp_ids t, smancilproducts p WHERE p.importancelevel = 2 and p.smpid=t.smpid";
    static String SQL2_SM_BCD = "FROM temp_smp_ids t, bcdproducts p, dcesets s, smproducts o WHERE o.dcesetid=s.dcesetid and s.dceid=p.dceid and o.smpid=t.smpid";
    static String SQL2_SM_BCD_ANCIL1 = "FROM temp_smp_ids t, bcdancilproducts p, bcdproducts pp, dcesets s, smproducts o WHERE p.importancelevel = 1 and p.bcdid=pp.bcdid and o.dcesetid=s.dcesetid and s.dceid=pp.dceid and o.smpid=t.smpid";
    static String SQL2_SM_BCD_ANCIL2 = "FROM temp_smp_ids t, bcdancilproducts p, bcdproducts pp, dcesets s, smproducts o WHERE p.importancelevel = 2 and p.bcdid=pp.bcdid and o.dcesetid=s.dcesetid and s.dceid=pp.dceid and o.smpid=t.smpid";
    // TODO: Do we need to support RAW, CAL, QA?

    private static enum QueryType {
        BCD(SQL1_BCD, SQL2_BCD),
        BCD_ANCIL1(SQL1_BCD_ANCIL1, SQL2_BCD_ANCIL1),
        BCD_ANCIL2(SQL1_BCD_ANCIL2, SQL2_BCD_ANCIL2),
        //BCD_RAW(SQL1_BCD_RAW, SQL2_BCD_RAW),
        BCD_RAW_IRAC(SQL1_BCD_RAW_IRAC, SQL2_BCD_RAW_IRAC),
        BCD_RAW_NOTIRAC(SQL1_BCD_RAW_NOTIRAC, SQL2_BCD_RAW_NOTIRAC),        
        BCD_CAL(SQL1_BCD_CAL, SQL2_BCD_CAL),
        BCD_CAL_ANCIL(SQL1_BCD_CAL_ANCIL, SQL2_BCD_CAL_ANCIL),
        BCD_CAL_CAL(SQL1_BCD_CAL_CAL, SQL2_BCD_CAL_CAL),
        BCD_CAL_CAL_ANCIL(SQL1_BCD_CAL_CAL_ANCIL, SQL2_BCD_CAL_CAL_ANCIL),
        BCD_CAL_REQ(SQL1_BCD_CAL_REQ, SQL2_BCD_CAL_REQ),
        BCD_CAL_REQ_ANCIL(SQL1_BCD_CAL_REQ_ANCIL, SQL2_BCD_CAL_REQ_ANCIL),
        BCD_QA(SQL1_BCD_QA, SQL2_BCD_QA),

        PBCD(SQL1_PBCD, SQL2_PBCD),
        PBCD_ANCIL1(SQL1_PBCD_ANCIL1, SQL2_PBCD_ANCIL1),
        PBCD_ANCIL2(SQL1_PBCD_ANCIL2, SQL2_PBCD_ANCIL2),
        PBCD_BCD(SQL1_PBCD_BCD, SQL2_PBCD_BCD),
        PBCD_BCD_ANCIL1(SQL1_PBCD_BCD_ANCIL1, SQL2_PBCD_BCD_ANCIL1),
        PBCD_BCD_ANCIL2(SQL1_PBCD_BCD_ANCIL2, SQL2_PBCD_BCD_ANCIL2),
        //PBCD_RAW(SQL1_PBCD_RAW, SQL2_PBCD_RAW),
        PBCD_RAW_IRAC(SQL1_PBCD_RAW_IRAC, SQL2_PBCD_RAW_IRAC),
        PBCD_RAW_NOTIRAC(SQL1_PBCD_RAW_NOTIRAC, SQL2_PBCD_RAW_NOTIRAC),        
        PBCD_CAL(SQL1_PBCD_CAL, SQL2_PBCD_CAL),
        PBCD_CAL_ANCIL(SQL1_PBCD_CAL_ANCIL, SQL2_PBCD_CAL_ANCIL),
        PBCD_CAL_CAL(SQL1_PBCD_CAL_CAL, SQL2_PBCD_CAL_CAL),
        PBCD_CAL_CAL_ANCIL(SQL1_PBCD_CAL_CAL_ANCIL, SQL2_PBCD_CAL_CAL_ANCIL),
        PBCD_CAL_REQ(SQL1_PBCD_CAL_REQ, SQL2_PBCD_CAL_REQ),
        PBCD_CAL_REQ_ANCIL(SQL1_PBCD_CAL_REQ_ANCIL, SQL2_PBCD_CAL_REQ_ANCIL),
        PBCD_QA(SQL1_PBCD_QA, SQL2_PBCD_QA),

        AOR_BCD(SQL1_AOR_BCD, SQL2_AOR_BCD),
        AOR_BCD_ANCIL1(SQL1_AOR_BCD_ANCIL1, SQL2_AOR_BCD_ANCIL1),
        AOR_BCD_ANCIL2(SQL1_AOR_BCD_ANCIL2, SQL2_AOR_BCD_ANCIL2),
        AOR_PBCD(SQL1_AOR_PBCD, SQL2_AOR_PBCD),
        AOR_PBCD_ANCIL1(SQL1_AOR_PBCD_ANCIL1, SQL2_AOR_PBCD_ANCIL1),
        AOR_PBCD_ANCIL2(SQL1_AOR_PBCD_ANCIL2, SQL2_AOR_PBCD_ANCIL2),
        //AOR_RAW(SQL1_AOR_RAW, SQL2_AOR_RAW),
        AOR_RAW_IRAC(SQL1_AOR_RAW_IRAC, SQL2_AOR_RAW_IRAC),
        AOR_RAW_NOTIRAC(SQL1_AOR_RAW_NOTIRAC, SQL2_AOR_RAW_NOTIRAC),
        AOR_BCD_CAL(SQL1_AOR_BCD_CAL, SQL2_AOR_BCD_CAL),
        AOR_BCD_CAL_ANCIL(SQL1_AOR_BCD_CAL_ANCIL, SQL2_AOR_BCD_CAL_ANCIL),
        AOR_PBCD_CAL(SQL1_AOR_PBCD_CAL, SQL2_AOR_PBCD_CAL),
        AOR_PBCD_CAL_ANCIL(SQL1_AOR_PBCD_CAL_ANCIL, SQL2_AOR_PBCD_CAL_ANCIL),
        AOR_CAL_CAL(SQL1_AOR_CAL_CAL, SQL2_AOR_CAL_CAL),
        AOR_CAL_CAL_ANCIL(SQL1_AOR_CAL_CAL_ANCIL, SQL2_AOR_CAL_CAL_ANCIL),
        AOR_CAL_REQ(SQL1_AOR_CAL_REQ, SQL2_AOR_CAL_REQ),
        AOR_CAL_REQ_ANCIL(SQL1_AOR_CAL_REQ_ANCIL, SQL2_AOR_CAL_REQ_ANCIL),
        AOR_QA(SQL1_AOR_QA, SQL2_AOR_QA),

        SM(SQL1_SM, SQL2_SM),
        SM_ANCIL1(SQL1_SM_ANCIL1, SQL2_SM_ANCIL1),
        SM_ANCIL2(SQL1_SM_ANCIL2, SQL2_SM_ANCIL2),
        SM_BCD(SQL1_SM_BCD, SQL2_SM_BCD),
        SM_BCD_ANCIL1(SQL1_SM_BCD_ANCIL1, SQL2_SM_BCD_ANCIL1),
        SM_BCD_ANCIL2(SQL1_SM_BCD_ANCIL2, SQL2_SM_BCD_ANCIL2);
        //TODO: do we need to support RAW, CALS, and QA?


        final String querySql1;
        final String querySql2;

        QueryType(String sql1, String sql2) {
            this.querySql1 = sql1;
            this.querySql2 = sql2;
        }
    }

    //CR9682
    //private static QueryType [] RAW_QUERIES_FOR_UNIQUE = {
    //    QueryType.BCD_RAW_IRAC, QueryType.PBCD_RAW_IRAC
    //};

    private static QueryType [] CALIBRATION_QUERIES = {
            QueryType.BCD_CAL, QueryType.BCD_CAL_ANCIL, QueryType.BCD_CAL_CAL, QueryType.BCD_CAL_CAL_ANCIL, QueryType.BCD_CAL_REQ, QueryType.BCD_CAL_REQ_ANCIL,
            QueryType.PBCD_CAL, QueryType.PBCD_CAL_ANCIL, QueryType.PBCD_CAL_CAL, QueryType.PBCD_CAL_CAL_ANCIL, QueryType.PBCD_CAL_REQ, QueryType.PBCD_CAL_REQ_ANCIL,
            QueryType.AOR_BCD_CAL, QueryType.AOR_BCD_CAL_ANCIL,
            QueryType.AOR_PBCD_CAL, QueryType.AOR_PBCD_CAL_ANCIL,
            QueryType.AOR_CAL_CAL, QueryType.AOR_CAL_CAL_ANCIL, QueryType.AOR_CAL_REQ, QueryType.AOR_CAL_REQ_ANCIL
    };

    // wavelength statiostics is based on bcd, postbcd, and raw data only
    private static QueryType [] QUERIES_WITH_WAVELENGTH_STATISTICS1 = {
        QueryType.BCD, QueryType.PBCD, QueryType.AOR_BCD, QueryType.AOR_PBCD
    };

    private static QueryType [] QUERIES_WITH_WAVELENGTH_STATISTICS2 = {
            QueryType.PBCD_BCD
            //QueryType.BCD_RAW_IRAC, QueryType.BCD_RAW_NOTIRAC,          
            //QueryType.PBCD_RAW_IRAC, QueryType.PBCD_RAW_NOTIRAC,
            //QueryType.AOR_RAW_IRAC, QueryType.AOR_RAW_NOTIRAC
    };


    private static SQLErrorCodeSQLExceptionTranslator translator;


    static Set<FileInfo> getFileInfo(QueryType type, List<Integer> idList, Connection conn) throws SQLException {
        int idxWildCard = type.querySql1.indexOf("?");
        String qry;
        if (idxWildCard >= 0) {
            qry =  type.querySql1.replace("?", CollectionUtil.toString(idList, ","));   
        } else {
            qry = type.querySql1+ CollectionUtil.toString(idList, ",") + ")";
        }
        String sql = getSelectClause(type)+qry;
        return getFileInfo(sql, conn);
    }

    static Set<FileInfo> getFileInfo(QueryType type, Connection conn) throws SQLException {
        String sql = getSelectClause(type)+type.querySql2;
        return getFileInfo(sql, conn);
    }

    static Set<FileInfo> getFileInfo(String sql, Connection conn) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            //stmt.setFetchSize(250);
            long cTime = System.currentTimeMillis();
            //System.out.println("Executing SQL query: " + sql);
            _log.briefDebug ("Executing SQL query: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            _log.briefDebug ("SELECT took "+(System.currentTimeMillis()-cTime)+"ms");
            Set<FileInfo> fi = new LinkedHashSet<FileInfo>();
            int numCols = rs.getMetaData().getColumnCount();
            if (numCols == 4) {
                while (rs.next()) {
                    String externalName = rs.getString(2);
                    FileInfo fileInfo = new FileInfo(rs.getString(1), externalName, rs.getInt(3));
                    try {
                        fileInfo.setHasAccess(HeritageSecurityModule.checkHasAccess(getReqkey(externalName)));
                    } catch (Exception e) {
                        _log.warn(e, "Can not check access permission (assuming none) for "+externalName);
                        fileInfo.setHasAccess(false);
                    }
                    fileInfo.setExtraData(rs.getString(4));
                    // We'd like to see an error in README, if the file was not packaged because of access restriction
                    //if (fileInfo.hasAccess()) {
                        fi.add(fileInfo);
                    //}
                }
            } else {
                while (rs.next()) {
                    String externalName = rs.getString(2);
                    FileInfo fileInfo = new FileInfo(rs.getString(1), externalName, rs.getInt(3));
                    try {
                        fileInfo.setHasAccess(HeritageSecurityModule.checkHasAccess(getReqkey(externalName)));
                    } catch (Exception e) {
                        _log.warn(e, "Can not check access permission (assuming none) for "+externalName);
                        fileInfo.setHasAccess(false);
                    }
                    // We'd like to see an error in README, if the file was not packaged because of access restriction
                    //if (fileInfo.hasAccess()) {
                        fi.add(fileInfo);
                    //}
                }
            }
            //System.out.println("FETCH took "+(System.currentTimeMillis()-cTime)+"ms");
            return fi;
        } finally {
            closeStatement(stmt);
        }

    }

    static String getSelectClause(QueryType type) {
        if (isQAReadme(type)) {
            return "SELECT unique p.qareadmefile, p.qaexternalname, p.qafilesize ";
        } else {
            String wavelengthTableRef = wavelengthFrom(type); // table reference to get wavelength from (for statistics)                        
            return "SELECT "+(needsUnique(type)?"unique ":"")+"p.heritagefilename, "+getExternalNameField(type)+", p.filesize "+
                    ((wavelengthTableRef == null)?"":", "+wavelengthTableRef+".wavelength ");
        }
    }

    static String getExternalNameField(QueryType type) {
        if (isCalibration(type)) {
            // after DB switches to base name, remove CASE
            //return ("CASE WHEN not p.externalname like '%/%' THEN 'r' || pp.reqkey || '/ch' || pp.channum || '/cal/' || p.externalname ELSE p.externalname END");
            return "'r' || pp.reqkey || '/ch' || pp.channum || '/cal/' || p.externalname";
        } else {
            return "p.externalname";
        }
    }

    static boolean needsUnique(QueryType type) {
        // CR9682
        //for (QueryType qt : RAW_QUERIES_FOR_UNIQUE) {
        //    if (type.equals(qt)) {
        //        return true;
        //    }
        //}
        
        // all calibration queries need to be unique
        return isCalibration(type);
    }

    static boolean isCalibration(QueryType type) {
        for (QueryType qt : CALIBRATION_QUERIES) {
            if (type.equals(qt)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns table reference
     * "p" - if wavelength column is present in table referenced as "p"
     * "o" - if wavelength column is present in table referenced as "o"
     * null - if wavelength
     * @param type query type
     * @return table reference: "p", "o", or null
     */
    private static String wavelengthFrom(QueryType type) {
        for (QueryType qt : QUERIES_WITH_WAVELENGTH_STATISTICS1) {
            if (type.equals(qt)) {
                return "p";
            }
        }
        for (QueryType qt : QUERIES_WITH_WAVELENGTH_STATISTICS2) {
            if (type.equals(qt)) {
                return "o";
            }
        }

        return null;
    }


    static boolean isQAReadme(QueryType type) {
        return type.equals(QueryType.BCD_QA) || type.equals(QueryType.PBCD_QA) || type.equals(QueryType.AOR_QA);
    }

    //------------------------------------------

    public static Set<FileInfo> getBcdFileInfo(List<Integer> idList, Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.BCD,  idList, conn);
        Set<FileInfo> more = getFileInfo(QueryType.BCD_ANCIL1,  idList, conn);
        all.addAll(more);
        return (all);
    }

    public static Set<FileInfo> getPbcdFileInfo(List<Integer> idList, Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.PBCD,  idList, conn);
        Set<FileInfo> more = getFileInfo(QueryType.PBCD_ANCIL1,  idList, conn);
        all.addAll(more);
        return (all);
    }
    
    static Set<FileInfo> getSmFileInfo(List<Integer> idList, Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.SM,  idList, conn);
        Set<FileInfo> more = getFileInfo(QueryType.SM_ANCIL1,  idList, conn);
        all.addAll(more);
        return (all);
    }


    public static Set<FileInfo> getBcdAncilFileInfo(List<Integer> idList, Connection conn) throws SQLException {
        return getFileInfo(QueryType.BCD_ANCIL2, idList, conn);
    }

    public static Set<FileInfo> getPbcdAncilFileInfo(List<Integer> idList, Connection conn) throws SQLException {
        return getFileInfo(QueryType.PBCD_ANCIL2, idList, conn);
    }

    static Set<FileInfo> getSmAncilFileInfo(List<Integer> idList, Connection conn) throws SQLException {
        return getFileInfo(QueryType.SM_ANCIL2, idList, conn);
    }


    static Set<FileInfo> getAorBcdFileInfo(List<Integer> aorIds, Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.AOR_BCD, aorIds, conn);
        Set<FileInfo> more = getFileInfo(QueryType.AOR_BCD_ANCIL1, aorIds, conn);
        all.addAll(more);
        return (all);
    }

    static Set<FileInfo> getAorBcdAncilFileInfo(List<Integer> aorIds, Connection conn) throws SQLException {
        return getFileInfo(QueryType.AOR_BCD_ANCIL2, aorIds, conn);
    }

    static Set<FileInfo> getAorPbcdFileInfo(List<Integer> aorIds, Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.AOR_PBCD, aorIds, conn);
        Set<FileInfo> more = getFileInfo(QueryType.AOR_PBCD_ANCIL1, aorIds, conn);
        all.addAll(more);
        return (all);
    }

    static Set<FileInfo> getAorPbcdAncilFileInfo(List<Integer> aorIds, Connection conn) throws SQLException {
        return getFileInfo(QueryType.AOR_PBCD_ANCIL2, aorIds, conn);
    }

    static Set<FileInfo> getAorCalFileInfo(List<Integer> aorIds, Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.AOR_BCD_CAL, aorIds, conn);
        all.addAll(getFileInfo(QueryType.AOR_BCD_CAL_ANCIL, aorIds, conn));
        all.addAll(getFileInfo(QueryType.AOR_PBCD_CAL, aorIds, conn));
        all.addAll(getFileInfo(QueryType.AOR_PBCD_CAL_ANCIL, aorIds, conn));
        all.addAll(getFileInfo(QueryType.AOR_CAL_CAL, aorIds, conn));
        all.addAll(getFileInfo(QueryType.AOR_CAL_CAL_ANCIL, aorIds, conn));
        all.addAll(getFileInfo(QueryType.AOR_CAL_REQ, aorIds, conn));
        all.addAll(getFileInfo(QueryType.AOR_CAL_REQ_ANCIL, aorIds, conn));                
        return (all);
    }

    static Set<FileInfo> getAorRawFileInfo(List<Integer> aorIds, Connection conn) throws SQLException {
        //return getFileInfo(QueryType.AOR_RAW, aorIds, conn);
        Set<FileInfo> all = getFileInfo(QueryType.AOR_RAW_IRAC, aorIds, conn);
        Set<FileInfo> more = getFileInfo(QueryType.AOR_RAW_NOTIRAC, aorIds, conn);
        all.addAll(more);
        return (all);
    }

    public static Set<FileInfo> getBcdCalFileInfo(List<Integer> idList, Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.BCD_CAL, idList, conn);
        all.addAll(getFileInfo(QueryType.BCD_CAL_ANCIL, idList, conn));
        all.addAll(getFileInfo(QueryType.BCD_CAL_CAL, idList, conn));
        all.addAll(getFileInfo(QueryType.BCD_CAL_CAL_ANCIL, idList, conn));
        all.addAll(getFileInfo(QueryType.BCD_CAL_REQ, idList, conn));
        all.addAll(getFileInfo(QueryType.BCD_CAL_REQ_ANCIL, idList, conn));
        return all;
    }

    static Set<FileInfo> getBcdRawFileInfo(List<Integer> idList, Connection conn) throws SQLException {
        //return getFileInfo(QueryType.BCD_RAW, idList, conn);
         Set<FileInfo> all = getFileInfo(QueryType.BCD_RAW_IRAC, idList, conn);
         Set<FileInfo> more = getFileInfo(QueryType.BCD_RAW_NOTIRAC, idList, conn);
         all.addAll(more);
         return (all);

    }

    public static Set<FileInfo> getPbcdCalFileInfo(List<Integer> idList, Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.PBCD_CAL, idList, conn);
        all.addAll(getFileInfo(QueryType.PBCD_CAL_ANCIL, idList, conn));
        all.addAll(getFileInfo(QueryType.PBCD_CAL_CAL, idList, conn));
        all.addAll(getFileInfo(QueryType.PBCD_CAL_CAL_ANCIL, idList, conn));
        all.addAll(getFileInfo(QueryType.PBCD_CAL_REQ, idList, conn));
        all.addAll(getFileInfo(QueryType.PBCD_CAL_REQ_ANCIL, idList, conn));
        return all;
    }

    // post-bcd raw (raw of bcd products associated with post-bcds)
    static Set<FileInfo> getPbcdRawFileInfo(List<Integer> idList, Connection conn) throws SQLException {
        //return getFileInfo(QueryType.PBCD_RAW, idList, conn);
        Set<FileInfo> all = getFileInfo(QueryType.PBCD_RAW_IRAC, idList, conn);
        Set<FileInfo> more = getFileInfo(QueryType.PBCD_RAW_NOTIRAC, idList, conn);
        all.addAll(more);
        return (all);
    }

    // bcd products associated with post-bcds
    static Set<FileInfo> getPbcdBcdFileInfo(List<Integer> idList, Connection conn)  throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.PBCD_BCD, idList, conn);
        Set<FileInfo> more = getFileInfo(QueryType.PBCD_BCD_ANCIL1, idList, conn);
        all.addAll(more);
        return (all);
    }

    // bcd ancillary products associated bcds associated with post-bcds
    static Set<FileInfo> getPbcdBcdAncilFileInfo(List<Integer> idList, Connection conn)  throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.PBCD_BCD_ANCIL2, idList, conn);
        return (all);
    }

    // bcd products associated with supermosaics
    static Set<FileInfo> getSmBcdFileInfo(List<Integer> idList, Connection conn)  throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.SM_BCD, idList, conn);
        Set<FileInfo> more = getFileInfo(QueryType.SM_BCD_ANCIL1, idList, conn);
        all.addAll(more);
        return (all);
    }

    // bcd ancillary products associated bcds associated with supermosaics
    static Set<FileInfo> getSmBcdAncilFileInfo(List<Integer> idList, Connection conn)  throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.SM_BCD_ANCIL2, idList, conn);
        return (all);
    }


    // qa products associated with bcds
    static Set<FileInfo> getBcdQAFileInfo(List<Integer> idList, Connection conn)  throws SQLException {
        return getFileInfo(QueryType.BCD_QA, idList, conn);
    }

    // qa products associated with post-bcds
    static Set<FileInfo> getPbcdQAFileInfo(List<Integer> idList, Connection conn)  throws SQLException {
        return getFileInfo(QueryType.PBCD_QA, idList, conn);
    }

    // qa products associated with aors
    static Set<FileInfo> getAorQAFileInfo(List<Integer> idList, Connection conn)  throws SQLException {
        return getFileInfo(QueryType.AOR_QA, idList, conn);
    }


    // -------methods that load ids into temporary table--------------


    static int loadBcdIds(Connection conn, List<Integer> bcdIds) throws SQLException {
        return TempTable.loadIdsIntoTempTable(conn, bcdIds, "temp_bcd_ids", "bcdid");
    }

    static int loadPbcdIds(Connection conn, List<Integer> pbcdIds) throws SQLException {
        return TempTable.loadIdsIntoTempTable(conn, pbcdIds, "temp_pbcd_ids", "pbcdid");
    }

    static int loadSmpIds(Connection conn, List<Integer> smpIds) throws SQLException {
        return TempTable.loadIdsIntoTempTable(conn, smpIds, "temp_smp_ids", "smpid");
    }


    static int loadAorIds(Connection conn, HashMap<Integer, List<Short>> aorChannumMap) throws SQLException {

        // make sure prior temp table is dropped
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate("drop table temp_aors;");
        } catch (SQLException e) {
            _log.warn(e, "No temp_aors table to drop: "+e.getMessage());
        } finally {
            closeStatement(stmt);
        }

         // create temporary table
        String cmd = "create temp table temp_aors (reqkey integer, channum smallint) with no log";
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(cmd);
        } finally {
            closeStatement(stmt);
        }

        // load ids
        cmd =  "insert into temp_aors values (?, ?)";
        int [] updateCounts;
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(cmd);
            short [] defaultChanlist = {0, 1, 2, 3, 4};
            for (Integer id : aorChannumMap.keySet()) {

                List<Short> chanlist = aorChannumMap.get(id);
                if (chanlist == null) {
                    for (short ch : defaultChanlist) {
                        pstmt.setInt(1, id);
                        pstmt.setShort(2, ch);
                        pstmt.addBatch();
                    }
                } else {
                    // make sure there are no duplicates
                    Short [] chanarray = chanlist.toArray(new Short[chanlist.size()]);
                    Arrays.sort(chanarray);
                    short last = -1;
                    for (Short ch : chanarray) {
                        if (ch != last) {
                            pstmt.setInt(1, id);
                            pstmt.setShort(2, ch);
                            pstmt.addBatch();
                            last = ch;
                        }
                    }
                }

            }
            updateCounts = pstmt.executeBatch();
        } finally {
            closeStatement(pstmt);
        }

        // check that all rows were inserted successfully
        int updated = 0;
        for (int i=0; i<updateCounts.length; i++) {
            if (updateCounts[i] != 1) {
                _log.warn("Batch insert of tmp_aors failed on "+(i+1)+"th insert.");
                break;
            }
            updated++;
        }
        return updated;
    }

    static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Throwable th) {
                // log and ignore
                _log.warn(th, "Failed to close statement: "+th.getMessage());
            }
        }
    }

    // methods that assume the id list is loaded into temporary table

    static Set<FileInfo> getBcdFileInfo(Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.BCD, conn);
        Set<FileInfo> more = getFileInfo(QueryType.BCD_ANCIL1, conn);
        all.addAll(more);
        return (all);
    }

    static Set<FileInfo> getPbcdFileInfo(Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.PBCD, conn);
        Set<FileInfo> more = getFileInfo(QueryType.PBCD_ANCIL1, conn);
        all.addAll(more);
        return (all);
    }

    static Set<FileInfo> getSmFileInfo(Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.SM, conn);
        Set<FileInfo> more = getFileInfo(QueryType.SM_ANCIL1, conn);
        all.addAll(more);
        return (all);
    }


    static Set<FileInfo> getBcdAncilFileInfo(Connection conn) throws SQLException {
        return getFileInfo(QueryType.BCD_ANCIL2, conn);
    }

    static Set<FileInfo> getPbcdAncilFileInfo(Connection conn) throws SQLException {
        return getFileInfo(QueryType.PBCD_ANCIL2, conn);
    }

    static Set<FileInfo> getSmAncilFileInfo(Connection conn) throws SQLException {
        return getFileInfo(QueryType.SM_ANCIL2, conn);
    }


    static Set<FileInfo> getAorBcdFileInfo(Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.AOR_BCD, conn);
        Set<FileInfo> more = getFileInfo(QueryType.AOR_BCD_ANCIL1, conn);
        all.addAll(more);
        return (all);
    }

    static Set<FileInfo> getAorBcdAncilFileInfo(Connection conn) throws SQLException {
        return getFileInfo(QueryType.AOR_BCD_ANCIL2,  conn);
    }

    static Set<FileInfo> getAorPbcdFileInfo(Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.AOR_PBCD, conn);
        Set<FileInfo> more = getFileInfo(QueryType.AOR_PBCD_ANCIL1, conn);
        all.addAll(more);
        return (all);
    }

    static Set<FileInfo> getAorPbcdAncilFileInfo(Connection conn) throws SQLException {
        return getFileInfo(QueryType.AOR_PBCD_ANCIL2, conn);
    }

    static Set<FileInfo> getAorCalFileInfo(Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.AOR_BCD_CAL, conn);
        all.addAll(getFileInfo(QueryType.AOR_BCD_CAL_ANCIL, conn));
        all.addAll(getFileInfo(QueryType.AOR_PBCD_CAL, conn));
        all.addAll(getFileInfo(QueryType.AOR_PBCD_CAL_ANCIL, conn));
        all.addAll(getFileInfo(QueryType.AOR_CAL_CAL, conn));
        all.addAll(getFileInfo(QueryType.AOR_CAL_CAL_ANCIL, conn));
        all.addAll(getFileInfo(QueryType.AOR_CAL_REQ, conn));
        all.addAll(getFileInfo(QueryType.AOR_CAL_REQ_ANCIL, conn));
        return (all);
    }

    static Set<FileInfo> getAorRawFileInfo(Connection conn) throws SQLException {
        //return getFileInfo(QueryType.AOR_RAW, conn);
        Set<FileInfo> all = getFileInfo(QueryType.AOR_RAW_IRAC, conn);
        Set<FileInfo> more = getFileInfo(QueryType.AOR_RAW_NOTIRAC, conn);
        all.addAll(more);
        return (all);
    }

    static Set<FileInfo> getBcdCalFileInfo(Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.BCD_CAL, conn);
        all.addAll(getFileInfo(QueryType.BCD_CAL_ANCIL, conn));
        all.addAll(getFileInfo(QueryType.BCD_CAL_CAL, conn));
        all.addAll(getFileInfo(QueryType.BCD_CAL_CAL_ANCIL, conn));
        all.addAll(getFileInfo(QueryType.BCD_CAL_REQ, conn));
        all.addAll(getFileInfo(QueryType.BCD_CAL_REQ_ANCIL, conn));
        return all;
    }

    static Set<FileInfo> getBcdRawFileInfo(Connection conn) throws SQLException {
        //return getFileInfo(QueryType.BCD_RAW, conn);
        Set<FileInfo> all = getFileInfo(QueryType.BCD_RAW_IRAC, conn);
        Set<FileInfo> more = getFileInfo(QueryType.BCD_RAW_NOTIRAC, conn);
        all.addAll(more);
        return (all);
    }

    static Set<FileInfo> getPbcdCalFileInfo(Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.PBCD_CAL, conn);
        all.addAll(getFileInfo(QueryType.PBCD_CAL_ANCIL, conn));
        all.addAll(getFileInfo(QueryType.PBCD_CAL_CAL, conn));        
        all.addAll(getFileInfo(QueryType.PBCD_CAL_CAL_ANCIL, conn));
        all.addAll(getFileInfo(QueryType.PBCD_CAL_REQ, conn));
        all.addAll(getFileInfo(QueryType.PBCD_CAL_REQ_ANCIL, conn));
        return all;
    }

    // post-bcd raw (raw of bcd products associated with post-bcds)
    static Set<FileInfo> getPbcdRawFileInfo(Connection conn) throws SQLException {
        //return getFileInfo(QueryType.PBCD_RAW, conn);
        Set<FileInfo> all = getFileInfo(QueryType.PBCD_RAW_IRAC, conn);
        Set<FileInfo> more = getFileInfo(QueryType.PBCD_RAW_NOTIRAC, conn);
        all.addAll(more);
        return (all);        
    }

    // bcd products associated with post-bcds
    static Set<FileInfo> getPbcdBcdFileInfo(Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.PBCD_BCD, conn);
        Set<FileInfo> more = getFileInfo(QueryType.PBCD_BCD_ANCIL1, conn);
        all.addAll(more);
        return (all);
    }

    // bcd ancil products associated with bcd products associated with post-bcds
    static Set<FileInfo> getPbcdBcdAncilFileInfo(Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.PBCD_BCD_ANCIL2, conn);
        return (all);
    }


    // bcd products associated with supermosaics
    static Set<FileInfo> getSmBcdFileInfo(Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.SM_BCD, conn);
        Set<FileInfo> more = getFileInfo(QueryType.SM_BCD_ANCIL1, conn);
        all.addAll(more);
        return (all);
    }

    // bcd ancil products associated with bcd products associated with supermosaics
    static Set<FileInfo> getSmBcdAncilFileInfo(Connection conn) throws SQLException {
        Set<FileInfo> all = getFileInfo(QueryType.SM_BCD_ANCIL2, conn);
        return (all);
    }


    // qa products associated with bcds
    static Set<FileInfo> getBcdQAFileInfo(Connection conn)  throws SQLException {
        return getFileInfo(QueryType.BCD_QA, conn);
    }

    // qa products associated with post-bcds
    static Set<FileInfo> getPbcdQAFileInfo(Connection conn)  throws SQLException {
        return getFileInfo(QueryType.PBCD_QA, conn);
    }

    // qa products associated with aors
    static Set<FileInfo> getAorQAFileInfo(Connection conn)  throws SQLException {
        return getFileInfo(QueryType.AOR_QA, conn);
    }

    //--------------------------------------


    /**
     * The only method visible outside the package. Gets file info (name and size for the given PackageRequest
     * @param request packaging request
     * @param baseDir parent directory - file name in db is relative to this directory
     * @return List of FileGroups - one group per product type
     */
    public static List<FileGroup> computeFileGroup(PackageRequest request, File baseDir) {

        List<Integer> ids = p2o(request.getIds());
        boolean useTempTable = TempTable.useTempTable(ids);
        DataType [] dataTypes = request.getDataTypes();
        boolean includeQAReadme = includeQAReadme(dataTypes);
        List<FileGroup> fgs = new ArrayList<FileGroup>(request.getDataTypes().length);
        Set<FileInfo> fi;
        DataSource ds = JdbcFactory.getDataSource(DbInstance.archive);
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(ds);
            long cTime = System.currentTimeMillis();

            if (request instanceof PackageRequest.BCD) {
                if (useTempTable) loadBcdIds(conn, ids);
                for (DataType dt : dataTypes) {
                    if (dt.equals(DataType.BCD)) {
                        fi = useTempTable ? getBcdFileInfo(conn) : getBcdFileInfo(ids, conn);
                    } else if (dt.equals(DataType.BCD_ANCIL)) {
                        fi = useTempTable ? getBcdAncilFileInfo(conn) : getBcdAncilFileInfo(ids, conn);
                    } else if (dt.equals(DataType.RAW)) {
                        fi = useTempTable ? getBcdRawFileInfo(conn) : getBcdRawFileInfo(ids, conn);
                    } else if (dt.equals(DataType.CAL)) {
                        fi = useTempTable ? getBcdCalFileInfo(conn) : getBcdCalFileInfo(ids, conn);
                    } else throw new UnsupportedOperationException("Unsupported data type for bcd download");

                    fgs.add(getFileGroup(fi, baseDir, dt.toString()));
                }
                if (includeQAReadme) {
                    fi = useTempTable ? getBcdQAFileInfo(conn) : getBcdQAFileInfo(ids, conn);
                    fgs.add(getFileGroup(fi, baseDir, "QA"));
                }
            } else if (request instanceof PackageRequest.PBCD) {
                if (useTempTable) loadPbcdIds(conn, ids);
                for (DataType dt : dataTypes) {
                    if (dt.equals(DataType.PBCD)) {
                        fi = useTempTable ? getPbcdFileInfo(conn) : getPbcdFileInfo(ids, conn);
                    } else if (dt.equals(DataType.PBCD_ANCIL)) {
                        fi = useTempTable ? getPbcdAncilFileInfo(conn) : getPbcdAncilFileInfo(ids, conn);
                    } else if (dt.equals(DataType.BCD)) {
                        fi = useTempTable ? getPbcdBcdFileInfo(conn) : getPbcdBcdFileInfo(ids, conn);
                    } else if (dt.equals(DataType.BCD_ANCIL)) {
                        fi = useTempTable ? getPbcdBcdAncilFileInfo(conn) : getPbcdBcdAncilFileInfo(ids, conn);
                    } else if (dt.equals(DataType.RAW)) {
                        fi = useTempTable ? getPbcdRawFileInfo(conn) : getPbcdRawFileInfo(ids, conn);
                    } else if (dt.equals(DataType.CAL)) {
                        fi = useTempTable ? getPbcdCalFileInfo(conn) : getPbcdCalFileInfo(ids, conn);
                    } else throw new UnsupportedOperationException("Unsupported data type for pbcd download: "+dt);

                    fgs.add(getFileGroup(fi, baseDir, dt.toString()));
                }
                if (includeQAReadme) {
                    fi = useTempTable ? getPbcdQAFileInfo(conn) : getPbcdQAFileInfo(ids, conn);
                    fgs.add(getFileGroup(fi, baseDir, "QA"));
                }

            } else if (request instanceof PackageRequest.SM) {
                if (useTempTable) loadSmpIds(conn, ids);
                for (DataType dt : dataTypes) {
                    if (dt.equals(DataType.SM)) {
                        fi = useTempTable ? getSmFileInfo(conn) : getSmFileInfo(ids, conn);
                    } else if (dt.equals(DataType.SM_ANCIL)) {
                        fi = useTempTable ? getSmAncilFileInfo(conn) : getSmAncilFileInfo(ids, conn);
                    } else if (dt.equals(DataType.BCD)) {
                        fi = useTempTable ? getSmBcdFileInfo(conn) : getSmBcdFileInfo(ids, conn);
                    } else if (dt.equals(DataType.BCD_ANCIL)) {
                        fi = useTempTable ? getSmBcdAncilFileInfo(conn) : getSmBcdAncilFileInfo(ids, conn);
                    } else throw new UnsupportedOperationException("Unsupported data type for pbcd download: "+dt);

                    fgs.add(getFileGroup(fi, baseDir, dt.toString()));
                }

            } else if (request instanceof PackageRequest.AOR) {
                PackageRequest.AorPackageUnit [] punits = ((PackageRequest.AOR)request).getPackageUnits();
                // create HashMap
                HashMap<Integer, List<Short>> reqChannums = new HashMap<Integer, List<Short>>(10);
                int reqkey;
                short channum;
                int numReqsWithFilter=0;
                List<Short> al;
                for (PackageRequest.AorPackageUnit pu : punits) {
                    reqkey = pu.getReqkey();
                    channum = pu.getChannum();
                    if (reqChannums.containsKey(reqkey)) {
                        // there might be duplicate reqkeys (ex. target list search)                        
                        if (reqChannums.get(reqkey) != null) {
                            // duplicate channels will be removed later (when loading into db)
                            reqChannums.get(reqkey).add(channum);
                        }
                    } else {
                        if (channum == -1) {
                            // all channels will be used for matching
                            reqChannums.put(reqkey, null);
                        } else {
                            al = new ArrayList<Short>(3);
                            al.add(channum);
                            reqChannums.put(reqkey, al);
                            numReqsWithFilter++;
                        }
                    }
                }
                if (numReqsWithFilter == 0 && !useTempTable) {
                    // use select in
                    for (DataType dt : dataTypes) {
                        if (dt.equals(DataType.BCD)) {
                            fi = getAorBcdFileInfo(ids, conn);
                        } else if (dt.equals(DataType.BCD_ANCIL)) {
                            fi = getAorBcdAncilFileInfo(ids, conn);
                        } else if (dt.equals(DataType.PBCD)) {
                            fi = getAorPbcdFileInfo(ids, conn);
                        } else if (dt.equals(DataType.PBCD_ANCIL)) {
                            fi = getAorPbcdAncilFileInfo(ids, conn);
                        } else if (dt.equals(DataType.CAL)) {
                            fi = getAorCalFileInfo(ids, conn);
                        } else if (dt.equals(DataType.RAW)) {
                            fi = getAorRawFileInfo(ids, conn);
                        }
                        else throw new UnsupportedOperationException("Unsupported data type for AOR download: "+dt);

                        fgs.add(getFileGroup(fi, baseDir, dt.toString()));
                    }
                    if (includeQAReadme) {
                        fi = getAorQAFileInfo(ids, conn);
                        fgs.add(getFileGroup(fi, baseDir, "QA"));
                    }

                } else {
                    // use temp table
                    loadAorIds(conn, reqChannums);
                    for (DataType dt : dataTypes) {
                        if (dt.equals(DataType.BCD)) {
                            fi = getAorBcdFileInfo(conn);
                        } else if (dt.equals(DataType.BCD_ANCIL)) {
                            fi = getAorBcdAncilFileInfo(conn);
                        } else if (dt.equals(DataType.PBCD)) {
                            fi = getAorPbcdFileInfo(conn);
                        } else if (dt.equals(DataType.PBCD_ANCIL)) {
                            fi = getAorPbcdAncilFileInfo(conn);
                        } else if (dt.equals(DataType.CAL)) {
                            fi = getAorCalFileInfo(conn);
                        } else if (dt.equals(DataType.RAW)) {
                            fi = getAorRawFileInfo(conn);
                        } else throw new UnsupportedOperationException("Unsupported data type for bcd download: "+dt);

                        fgs.add(getFileGroup(fi, baseDir, dt.toString()));
                    }
                    if (includeQAReadme) {
                        fi = getAorQAFileInfo(conn);
                        fgs.add(getFileGroup(fi, baseDir, "QA"));
                    }

                }
            }

            // log statistics by group and by instrument/channel
            logStatistics(fgs);

            // AR8656: keep all the files from one AOR in one zip
            if (getTotalSize(fgs) > request.getMaxBundleSize()) {
                fgs = regroupByReqkey(fgs, baseDir);
            }
            _log.briefDebug("computeFileGroup took "+(System.currentTimeMillis()-cTime)+"ms");

        } catch (SQLException e) {
            _log.warn(e, e.getMessage());
            if (translator == null) {
                translator = new SQLErrorCodeSQLExceptionTranslator("Informix");
            }
            throw translator.translate("computeFileGroup", "", e);
        } finally {
            if (conn != null) DataSourceUtils.releaseConnection(conn, ds);
        }


        return fgs;
    }

    private static void logStatistics(List<FileGroup> fgs) {
        Logger.LoggerImpl logger= Logger.getLogger(Logger.DOWNLOAD_LOGGER);
        String preAmble = "datatype";
        String rawLog = "";
        HashMap<String, Long> sizeByWavelength = new HashMap<String, Long>();
        HashMap<String, Integer> filesByWavelength = new HashMap<String, Integer>();
        int numFiles;
        rawLog += "type: nfiles,size(B)";
        for (FileGroup fg : fgs) {
            numFiles = 0;
            for (FileInfo fi : fg) {
                numFiles++;
                // sum statistics by wavelength
                Object extra = fi.getExtraData();
                String wl = (String) extra;
                Long size;
                Integer files;
                if (extra != null && extra instanceof String)  {
                    size = sizeByWavelength.get(wl);
                    files = filesByWavelength.get(wl);
                    if (size == null) {
                        size = 0l;
                        files = 0;
                    }
                    sizeByWavelength.put(wl, size+fi.getSizeInBytes());
                    filesByWavelength.put(wl, (files+1));
                }
            }

            // statistics by data type
            rawLog += " | "+fg.getDesc()+": "+numFiles+","+fg.getSizeInBytes();

        }
        // log datatypes statistics
        logger.stats(preAmble, rawLog);

        preAmble = "wavelength";
        rawLog = "";
        rawLog += "wl: nfiles,size(B)";
        String [] wlKeys = sizeByWavelength.keySet().toArray(new String[sizeByWavelength.size()]);
        Arrays.sort(wlKeys);
        for (String wl : wlKeys) {
            rawLog += " | "+wl+": "+filesByWavelength.get(wl)+","+sizeByWavelength.get(wl);
        }
        logger.stats(preAmble, rawLog);
    }

    private static List<FileGroup> regroupByReqkey(List<FileGroup> fgs, File baseDir) {
        long cTime = System.currentTimeMillis();
        HashMap<String, ArrayList<FileInfo>> byReqkey = new HashMap<String, ArrayList<FileInfo>>();
        ArrayList<FileInfo> fiList;
        for (FileGroup fg : fgs) {
            for (FileInfo fi : fg) {
                String reqkey = getReqkey(fi.getExternalName());
                fiList = byReqkey.get(reqkey);
                if (fiList == null) {
                    fiList = new ArrayList<FileInfo>();
                    byReqkey.put(reqkey, fiList);
                }
                fiList.add(fi);
            }
        }
        String [] keys = byReqkey.keySet().toArray(new String [1]);
        Arrays.sort(keys);
        List<FileGroup> fgList  = new ArrayList<FileGroup>(keys.length);
        for (String reqkey : keys) {
            FileGroup newFG = getFileGroup(byReqkey.get(reqkey), baseDir, reqkey);
            newFG.setPackageTogether(true);
            fgList.add(newFG);
        }
        _log.briefDebug("regroupByReqkey took "+(System.currentTimeMillis()-cTime)+"ms");
        return fgList;
    }

    private static String getReqkey(String externalName) {
        //assuming external name format like r15712256/ch0/bcd/SPITZER_S0_15712256_0000_0000_3_bcdb.fits
        int endIdx = externalName.indexOf('/');
        if (endIdx < 2) { throw new IllegalArgumentException("Can not parse reqkey from external name "+externalName); }
        return externalName.substring(1, endIdx);
    }

    private static long getTotalSize(List<FileGroup> fgs) {
        long totalSize = 0;
        for (FileGroup fg : fgs) {
            totalSize += fg.getSizeInBytes();
        }
        return totalSize;
    }

    static boolean includeQAReadme(DataType [] dataTypes) {
        for (DataType dt : dataTypes) {
            if (dt.equals(DataType.BCD) || dt.equals(DataType.PBCD) || dt.equals(DataType.RAW)) {
                return true;
            }
        }
        return false;
    }



    static FileGroup getFileGroup(Collection<FileInfo> fi, File baseDir, String desc) {
        long sizeInBytes = 0;
        for (FileInfo i : fi) {sizeInBytes += i.getSizeInBytes(); }
        return new FileGroup(fi, baseDir, sizeInBytes, desc);
    }

    /**
     * convert primitive int array to list of Integer objects
     * @param arr array of primitives
     * @return list of objects
     */
    static List<Integer> p2o(int [] arr) {
        Integer[] objArr = new Integer[arr.length];
        for (int i=0; i<arr.length; i++) { objArr[i] = arr[i]; }
        return Arrays.asList(objArr);
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