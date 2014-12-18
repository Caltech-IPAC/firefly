package edu.caltech.ipac.util;
/**
 * User: roby
 * Date: 2/7/13
 * Time: 3:44 PM
 */


import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.astro.CoordUtil;
import edu.caltech.ipac.util.dd.Global;
import edu.caltech.ipac.util.dd.Region;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class RegionParser {

    private IOException readException= null;

    static {
        RegionFactory.setCoordConverter(new RegionFactory.CoordConverter() {
            public double convertStringToLon(String hms) throws CoordException {
                return CoordUtil.sex2dd(hms, false, true);
            }

            public double convertStringToLat(String dms) throws CoordException {
               return CoordUtil.sex2dd(dms, true, true);
            }
        });
    }

    public  RegionFactory.ParseRet processFile(File f) throws IOException {
        return processFile(new BufferedReader(new FileReader(f), 5000));
    }

    public  RegionFactory.ParseRet processFile(final BufferedReader reader) throws IOException {
        RegionFactory.ParseRet retval= RegionFactory.processInput(new RegionFactory.LineGetter() {
            public String getLine() {
                try {
                    return reader.readLine();
                } catch (IOException e) {
                    readException= e;
                    return null;
                }
            }
        });
        if (readException!=null) throw readException;
        return retval;
    }

    public void saveFile(File f, List<String> regStringList, String titleComment) throws IOException {
        PrintWriter writer= new PrintWriter(new BufferedWriter(new FileWriter(f), 5000));
        List<Region> regList= new ArrayList<Region>(regStringList.size());
        Region r;
        for(String s : regStringList) {
            r= Region.parse(s);
            if (s!=null) regList.add(r);
        }
        Global global= RegionFactory.createGlobal(regList);
        String outStr;
        if (!StringUtils.isEmpty(titleComment))writer.println("# "+titleComment);
        writer.println(RegionFactory.serialize(global,false));
        for(Region reg : regList) {
            outStr= RegionFactory.serialize(reg,global,false);
            writer.println(outStr);
        }
        writer.close();
    }


    public static void main(String args[]) {
        File f= new File("/Users/roby/fits/region-files/test.reg");
        try {
            System.out.println("Type enter");
            System.in.read();
            RegionParser parser= new RegionParser();
            RegionFactory.ParseRet result= parser.processFile(f);
            for(Region r : result.getRegionList())   System.out.println(r.toString());
            for(String s : result.getMsgList())  System.out.println(s);

            System.out.println("Output:");
            System.out.println("------");
            for(Region r : result.getRegionList())   System.out.println(r.serialize());

        } catch (Exception e) {
            System.out.println("Test Failed: "+e.toString());
            e.printStackTrace();
        }

    }

}

