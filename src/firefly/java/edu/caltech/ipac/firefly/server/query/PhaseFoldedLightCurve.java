package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.astro.IpacTableWriter;
import java.io.File;
import java.io.IOException;

/**
 * Created by ymei on 8/19/16.
 * To convert a Light Curve DataGroup into a phase folded Light Curve DataGroup.
 */
public class PhaseFoldedLightCurve {

    //Empty constructor
    public PhaseFoldedLightCurve(){};

    /**
     * This method adds a new column "phase" to the input data group.
     * @param dg
     * @param period
     * @param timeColName
     * @throws IpacTableException
     */
    public void addPhaseCol (DataGroup dg, float period, String timeColName)
            throws IpacTableException {

        //Check if the time column is in the data:
        if (!dg.containsKey(timeColName)) {
            throw new IpacTableException("The data does not contain the column: " + timeColName);
        }

        //Add a new data type and colunm: phase
        DataType phaseType = new DataType("phase", "phase", Double.class, DataType.Importance.HIGH, null, false);
        //DataType phaseType = new DataType("phase", Double.class);
        dg.addDataDefinition(phaseType);

        //Find the minimum time:
        double tzero = Double.MAX_VALUE;
        for (int i = 0; i < dg.size(); i++) {
            double mjd = (double)dg.get(i).getDataElement(timeColName);
            if (mjd < tzero) tzero = mjd;
        }

        /*Add the data of phase, row by row: */
        for (int i = 0; i < dg.size(); i++) {
            double mjd = (double)dg.get(i).getDataElement(timeColName);
            double phaseC = (mjd-tzero)/period - Math.floor((mjd-tzero)/period);
            System.out.println(phaseC);
            DataObject row = dg.get(i);
            row.setDataElement(phaseType, phaseC);
        }
    }

    /**
     * This method splits the input file to path and file name.
     * @param inputFileName
     * @return  the path (or directory)  of the inputFileName is from.
     */
    private static String[] getInputFilePath(String inputFileName){
        String[] dirs= inputFileName.split("/");
        String name = dirs[dirs.length-1];
        String path = inputFileName.substring(0, inputFileName.length()-name.length());
        String[] ret={path, name};
        return ret;
    }


    public static void main(String args[])  throws IOException{

        if (args.length > 0) {
            String path = getInputFilePath(args[0])[0];
            String inFileName = getInputFilePath(args[0])[1];
            String timeColName = "mjd";
            String phaseColName = "phase";
            float period = 0.140630f;

            try {
                File inFile = new File(args[0]);
                //Get a datagroup from the IPAC table file:
                DataGroup dataGroup = DataGroupReader.readAnyFormat(inFile);

                //Add the new phase column:
                PhaseFoldedLightCurve pflc = new PhaseFoldedLightCurve();
                pflc.addPhaseCol(dataGroup, period, timeColName);

                //Check sum:
                double sum = 0;
                for (int i = 0; i < dataGroup.size(); i++) {
                    sum += (double)dataGroup.get(i).getDataElement(phaseColName);
                }
                System.out.println("sum = " + sum);

                //Write out:
                String outFileName = path+"phaseFolded_output_"+inFileName;
                File outFile = new File(outFileName);
                IpacTableWriter.save(outFile, dataGroup);

            } catch (IpacTableException e) {
                e.printStackTrace();
            }
        }
    }

}