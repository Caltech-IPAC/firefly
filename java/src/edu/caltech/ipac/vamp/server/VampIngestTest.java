package edu.caltech.ipac.vamp.server;

import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Mar 31, 2010
 * Time: 1:06:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class VampIngestTest {
    static {
        AppProperties.loadClassPropertiesFromFileToPdb(new File("/hydra/server/config/vamp/db.prop"), null);
    }
    private static JdbcTemplate _jdbc = JdbcFactory.getTemplate(new DbInstance("vamp"));
    private static final String SUB_ITEM_REGEX="<[0-9]+>[^<]*";
    private static final String[] UNIQUE_KEYS={"avm_id","publisher_id"};
    private static final String CSV_DELIMITER="\\|";
    private static final String primaryDictionary = "avm_meta";
    private static final String ENTRY_DELIMITER = ";";
    private HashMap<String, HashMap<String,String>> _avmDictionaries
                                = new HashMap<String, HashMap<String,String>>(3);

    //---------------------------------------------------
    //---------- private static methods -----------------
    //---------------------------------------------------
    private static HashMap<String, String> convertStringArraysToMap(String[] keys, String[] values) {
        HashMap<String, String> result= new HashMap<String, String>(3);
        String value;
        int i=0;
        for (String key: keys) {
            if (i < values.length)
                value = values[i++];
            else
                value = "";
            result.put(key.trim(), value.trim());
        }

        return result;
    }

    private static boolean isUniqueKey(String key) {
        boolean result = false;
        for (String unique: UNIQUE_KEYS) {
            if (key.equals(unique)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static void printDebugInfo(String sql, final String[] valuesArray) {
        String dispStr;
        int idx = 0;
        System.out.println(sql);
        try {
            for (String value: valuesArray) {
                if (value==null)
                    dispStr = "{null}";
                else if (value.length()==0)
                    dispStr = "{empty}";
                else if (value.length() > 64)
                    dispStr = value.substring(0, 60)+"...";
                else
                    dispStr = value;
                System.out.println("value["+(++idx)+"]"+dispStr);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static int CharOccurrence(String host, String token) {
        int result = 0;

        if (host.indexOf(token) > -1)
            result = host.length() - (host.replace(token,"")).length();
        return result;
    }

    //---------------------------------------------------
    //---------------- private  methods -----------------
    //---------------------------------------------------
    private int findForeignKey(String fKey, String fTableName, HashMap<String, String> cvsMap) {
        int result = 0;
        StringBuffer sqlBuf = new StringBuffer("SELECT ");
        ArrayList <String> valuesList = new ArrayList <String> (0);
        HashMap<String, String> pDict = _avmDictionaries.get(fTableName);
        sqlBuf.append(fKey);
        sqlBuf.append(" FROM ");
        sqlBuf.append(fTableName);
        sqlBuf.append(" WHERE ");

        int u = 0;
        for (String uKey: UNIQUE_KEYS) {
            if (u>0) sqlBuf.append(" AND ");
            sqlBuf.append(uKey);
            sqlBuf.append(" = ? ");
            valuesList.add(cvsMap.get(pDict.get(uKey))==null?"":cvsMap.get(pDict.get(uKey)));
            u++;
        }
        result = _jdbc.queryForInt(sqlBuf.toString(), valuesList.toArray(new String[valuesList.size()]));
        return result;
    }

    private int findMaximumSpectralRows(HashMap<String, String> cvsMap, String tableName) {
        int result =0, subTotal=0;
        HashMap<String, String> dict = _avmDictionaries.get(tableName);

        for (String key: dict.keySet()) {
            if (dict.get(key).equals("#")) continue;

            if (!dict.get(key).equals("@")) {
                String value = cvsMap.get(dict.get(key));
                if (value==null || value.length()==0) {
                    subTotal= 0;
                } else {
                    subTotal = CharOccurrence(value,ENTRY_DELIMITER)+1;
                }
                result = subTotal > result? subTotal: result;
            }
        }

        return result;
    }

    private String prepareInsertString(HashMap<String, String> cvsMap, String tableName, ArrayList <String> valuesList) {
        HashMap<String, String> dict = _avmDictionaries.get(tableName);
        //HashMap<String, String> pDict = _avmDictionaries.get(primaryDictionary);
        StringBuffer sqlBuf = new StringBuffer("INSERT INTO "+tableName+" (");
        StringBuffer valuesBuf = new StringBuffer("VALUES (");
        int index = 0;

        for (String key: dict.keySet()) {
            if (dict.get(key).equals("#")) continue;
            if (index>0) {
                sqlBuf.append(",");
                valuesBuf.append(",");
            }
            sqlBuf.append(key);
            if (dict.get(key).equals("@")) {
                valuesList.add(index++, Integer.toString(findForeignKey(key, primaryDictionary, cvsMap)));
                valuesBuf.append("?");
            } else {
                valuesList.add(index++, cvsMap.get(dict.get(key))==null?"":cvsMap.get(dict.get(key)));
                valuesBuf.append("?");
            }
        }
        valuesBuf.append(")");
        sqlBuf.append(") ");
        sqlBuf.append(valuesBuf);

        return sqlBuf.toString();
    }

    private String prepareInsertString(HashMap<String, String> cvsMap, String tableName,
                                       ArrayList <String> valuesList, int idx) {
        HashMap<String, String> dict = _avmDictionaries.get(tableName);
        //HashMap<String, String> pDict = _avmDictionaries.get(primaryDictionary);
        StringBuffer sqlBuf = new StringBuffer("INSERT INTO "+tableName+" (");
        StringBuffer valuesBuf = new StringBuffer("VALUES (");
        int index = 0;

        for (String key: dict.keySet()) {
            if (dict.get(key).equals("#")) continue;
            if (index>0) {
                sqlBuf.append(",");
                valuesBuf.append(",");
            }
            sqlBuf.append(key);
            if (dict.get(key).equals("@")) {
                valuesList.add(index++, Integer.toString(findForeignKey(key, primaryDictionary, cvsMap)));
                valuesBuf.append("?");
            } else {
                String value=cvsMap.get(dict.get(key));
                if (value==null) {
                    value = "";
                } else {
                    int occurrence = CharOccurrence(value,ENTRY_DELIMITER);
                    value = value.split(String.valueOf(ENTRY_DELIMITER))[idx<=occurrence?idx:occurrence];
                }
                valuesList.add(index++, value);
                valuesBuf.append("?");
            }
        }
        valuesBuf.append(")");
        sqlBuf.append(") ");
        sqlBuf.append(valuesBuf);

        return sqlBuf.toString();
    }

    private String prepareUpdateString(HashMap<String, String> cvsMap, String tableName,
                                       ArrayList <String> valuesList, int idx) {
        HashMap<String, String> dict = _avmDictionaries.get(tableName);
        StringBuffer sqlBuf = new StringBuffer("UPDATE "+tableName+" SET ");
        int index = 0;
        int where = 0;
        for (String key: dict.keySet()) {
            if (dict.get(key).equals("#")) continue;
            if (!isUniqueKey(key)) {
                if (index>0) {
                    sqlBuf.append(", ");
                }
                String value=cvsMap.get(dict.get(key));
                if (value==null) {
                    value = "";
                } else {
                    int occurrence = CharOccurrence(value,ENTRY_DELIMITER);
                    value = value.split(String.valueOf(ENTRY_DELIMITER))[idx<=occurrence?idx:occurrence];
                }
                valuesList.add(index++, cvsMap.get(dict.get(key)));
                sqlBuf.append(key);
                sqlBuf.append("= ?");
            }
        }
        sqlBuf.append(" WHERE ");
        for (String key: UNIQUE_KEYS) {
            if (where>0) {
                sqlBuf.append("AND ");
            }
            sqlBuf.append(key);
            sqlBuf.append("= ? ");
            valuesList.add(index++, cvsMap.get(dict.get(key)));
            where++;
        }

        return sqlBuf.toString();
    }

    private String prepareUpdateString(HashMap<String, String> cvsMap, String tableName, ArrayList <String> valuesList) {
        HashMap<String, String> dict = _avmDictionaries.get(tableName);
        StringBuffer sqlBuf = new StringBuffer("UPDATE "+tableName+" SET ");
        int idx = 0;
        int where = 0;
        for (String key: dict.keySet()) {
            if (dict.get(key).equals("#")) continue;
            if (!isUniqueKey(key)) {
                if (idx>0) {
                    sqlBuf.append(", ");
                }
                sqlBuf.append(key);
                sqlBuf.append("= ?");
                valuesList.add(idx++, cvsMap.get(dict.get(key)));
            }
        }
        sqlBuf.append(" WHERE ");
        for (String key: UNIQUE_KEYS) {
            if (where>0) {
                sqlBuf.append("AND ");
            }
            sqlBuf.append(key);
            sqlBuf.append("= ? ");
            valuesList.add(idx++, cvsMap.get(dict.get(key)));
            where++;
        }

        return sqlBuf.toString();
    }

    private void jdbcUpdate(String sql, String args[]) {
        jdbcUpdate(sql, args, false);
    }

    private void jdbcUpdate(String sql, String args[], boolean showArgs) {
        int status = 0;
        System.out.println("Processing statement: "+sql);
        if (showArgs) {
            System.out.print("Values:");
            for (String arg: args) {
                System.out.print("["+arg+"]");
            }
            System.out.println();
        }

        status = _jdbc.update(sql, args);
        System.out.println("status = "+status);
    }

    private void useCvsMapToUpdateAvmMetaTable(final HashMap<String, String> cvsMap,
                                                String tableName) throws Exception {
        ArrayList <String> valuesList = new ArrayList<String>(3);
        String sql = prepareInsertString(cvsMap, tableName, valuesList);

        try {
            jdbcUpdate(sql, valuesList.toArray(new String[valuesList.size()]));
        } catch (DataAccessException e) {
            if (e instanceof DataIntegrityViolationException) {
                valuesList = new ArrayList<String>(3);
                sql = prepareUpdateString(cvsMap, tableName, valuesList);
                jdbcUpdate(sql, valuesList.toArray(new String[valuesList.size()]));
            } else {
                e.printStackTrace();
            }
        }
    }

    private void useCvsMapToUpdateAvmFileTable(final HashMap<String, String> cvsMap,
                                                    String tableName) throws Exception {
        ArrayList <String> valuesList = new ArrayList<String>(3);
        String sql = prepareInsertString(cvsMap, tableName, valuesList);
        try {
            jdbcUpdate(sql, valuesList.toArray(new String[valuesList.size()]));
        } catch (DataAccessException e) {
            if (e instanceof DataIntegrityViolationException) {
                valuesList = new ArrayList<String>(3);
                sql = prepareUpdateString(cvsMap, tableName, valuesList);
                jdbcUpdate(sql, valuesList.toArray(new String[valuesList.size()]));
            } else {
                e.printStackTrace();
            }
        }
    }

    private void useCvsMapToUpdateAvmSpectralTable(final HashMap<String, String> cvsMap,
                                                String tableName) throws Exception {
        ArrayList <String> valuesList = new ArrayList<String>(3);
        int rows = findMaximumSpectralRows(cvsMap, tableName);
        String sql;
        for (int i = 0; i<rows; i++) {
            sql = prepareInsertString(cvsMap, tableName, valuesList, i);
            try {
                jdbcUpdate(sql, valuesList.toArray(new String[valuesList.size()]), true);
            } catch (DataAccessException e) {
                if (e instanceof DataIntegrityViolationException) {
                    valuesList = new ArrayList<String>(3);
                    sql = prepareUpdateString(cvsMap, tableName, valuesList, i);
                    jdbcUpdate(sql, valuesList.toArray(new String[valuesList.size()]), true);
                } else {
                    e.printStackTrace();
                }
            }
            valuesList.clear();
        }
    }

    //---------------------------------------------------
    //----------------- public  methods -----------------
    //---------------------------------------------------
    public File resizeImage(File file) {
        String newFilename = "";
        File newFile = null;
        String extension = FileUtil.getExtension(file);
        try {
            float scale = 1.0F;
            BufferedImage img = ImageIO.read(file);
            float ratio;
            if (img != null)
                ratio = (float)img.getWidth() / (float)img.getHeight();
            else {
                throw new IOException ("ImageIO.read() cannot process "+FileUtil.getFilename(file)+".");
            }
            int thumbW, thumbH;
            if (ratio > 1.0F) {
                thumbW = 128;
                thumbH = (int)(128.0F / ratio);
            } else {
                thumbW = (int)(128.0F * ratio);
                thumbH = 128;
            }
            BufferedImage ret = new BufferedImage(thumbW, thumbH, BufferedImage.TYPE_INT_RGB);
            ret.getGraphics().drawImage(img, 0, 0, thumbW, thumbH, null);

            newFilename = FileUtil.getBase(file).replace("_Ti","")+".jpg";
            File thumbnailDirectory = new File (file.getParent(),"_thumbnail");
            if (!thumbnailDirectory.exists()) {
                if (!thumbnailDirectory.mkdir())
                    throw new IOException ("Unable to create "+thumbnailDirectory.getAbsolutePath()+".");    
            }
            newFile = new File (thumbnailDirectory, newFilename);
            ImageIO.write(ret, "jpeg", newFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newFile;
    }

    public void readFileAndIngestDB(String filename) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        int count = 0;
        String[] keys = null;
        String[] values = null;
        HashMap<String,String> map;
        while ((line = reader.readLine())!=null) {
            line = line.trim();
            //First line: create list of keys
            if (count==0) {
                keys = line.split(CSV_DELIMITER);
                if (keys == null || keys.length == 0) {
                    throw new Exception("Unable to read keys from CSV file.");
                }
            } else {
                values = line.split(CSV_DELIMITER);
                if (keys.length >= values.length) {
                    map = convertStringArraysToMap(keys, values);
                    File thumbnail = resizeImage(new File(map.get("ParentURL"), map.get("Filename")));
                    if (thumbnail != null)
                        map.put("thumbnail_path", thumbnail.getAbsolutePath());
                    useCvsMapToUpdateAvmMetaTable(map, "avm_meta");
                    useCvsMapToUpdateAvmFileTable(map, "avm_resources");
                    useCvsMapToUpdateAvmSpectralTable(map, "avm_spectral");
                } else {
                    throw new Exception("Unable to read line "+(count+1)+
                            ": keys="+keys.length+" values="+values.length);
                }
            }
            count++;
        }

    }

    public void initAVMDictionaries(String fileName, String tableName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        ArrayList<String> keys = new ArrayList<String>(3);
        ArrayList<String> values = new ArrayList<String>(3);
        int count = 0;
        String[] tokens;

        while ((line = reader.readLine())!=null) {
            tokens = line.split(CSV_DELIMITER);
            keys.add(tokens[0].trim());
            values.add(tokens[1].trim());
            count++;
        }

        _avmDictionaries.put(tableName,
                convertStringArraysToMap(keys.toArray(new String[count]), values.toArray(new String[count])));
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        try {
            VampIngestTest test = new VampIngestTest();
            String resourcePath = "/Users/tlau/Documents/VAMP/";
            System.out.println("Starting IngestTest... ");
            //test.readFileAndExecute(args[1]);
            String csvFilename = "/Users/tlau/vamp_files/output.csv";
            //if (args[1]!=null) csvFilename = args[1];

            test.initAVMDictionaries(resourcePath+"avm_meta_dictionary.csv", "avm_meta");
            test.initAVMDictionaries(resourcePath+"avm_resources_dictionary.csv", "avm_resources");
            test.initAVMDictionaries(resourcePath+"avm_spectral_dictionary.csv", "avm_spectral");

            test.readFileAndIngestDB(csvFilename);
            System.out.println("--- Completed ---");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
