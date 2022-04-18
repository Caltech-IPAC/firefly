package edu.caltech.ipac.firefly.util;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;
import org.junit.Assert;

import java.util.ArrayList;

/**
 * Created by zhang on 12/13/16.
 * This is a base class to validate FITS file for the unit test
 */
public class FitsValidation extends ConfigTest{
    private  double  delta =0.1e-10;
    /**
     * Validate the header card one by one
     * @param expectedFits
     * @param calculatedFits
     * @throws FitsException
     */
    public void validateHDUs(Fits expectedFits, Fits calculatedFits) throws FitsException {
        BasicHDU[] expectedHDUs =  expectedFits.read();

        BasicHDU[] outHDUs =calculatedFits.read();
        for (int i=0; i<expectedHDUs.length; i++){
            validateSingleHDU(expectedHDUs[i],  outHDUs[i]);

        }
    }

    /**
     * Validate the data stored in the FITS file
     * @param expectedFits
     * @param calculatedFits
     * @throws FitsException
     */
   public void validateData(Fits expectedFits, Fits calculatedFits) throws FitsException {
        FitsRead[] fitsReads = FitsReadFactory.createFitsReadArray(calculatedFits);
        FitsRead[] expectedFitsRead = FitsReadFactory.createFitsReadArray(expectedFits);
       for (int i=0;  i<fitsReads.length; i++){
            Assert.assertArrayEquals(fitsReads[i].getDataFloat(), expectedFitsRead[i].getDataFloat(), (float) delta);
        }

    }

    public void validateFits(Fits expectedFits, Fits fits) throws  FitsException  {
        validateHDUs(expectedFits,fits);
        validateData(expectedFits,fits);
    }

    public void validateSingleHDU(BasicHDU expectedHdu, BasicHDU outHdu) throws FitsException {

         Header expectedHeader = expectedHdu.getHeader();
         Header calculatedHeader = outHdu.getHeader();

        //validate expected and calculated headers have the same keys and same values
        String[] keys = getExpectedKeys(expectedHeader);
        String[] calcKeys = getExpectedKeys(calculatedHeader);

         //validate expected and calculated headers have the same number of card
        int calcHeadCardCnt= calcKeys.length;
        int expHeadCardCnt= keys.length;


         Assert.assertEquals(expHeadCardCnt, calcHeadCardCnt);


         for (int i=0; i<keys.length; i++){
             HeaderCard expectedCard =expectedHeader.findCard(keys[i]);
             HeaderCard calculatedCard =calculatedHeader.findCard(keys[i]);
//             if (keys[i].startsWith("SPOT")) {
//                 continue;
//             }
             if (isBlank(expectedCard.getValue())){
                 LOG.debug("expectedCard.getValue() is null, key = "+keys[i]+", skipping test");
                 continue;
             }
             //OK for both are null
             if (expectedCard==null && calculatedCard==null) continue;

             //Not OK if only one is null
             if (expectedCard==null  && calculatedCard !=null ||
                     expectedCard!=null && calculatedCard==null  ){
                 Assert.fail("The calculated Header is not the same as expected header.");
             }

             //compare the header values in each HeaderCard
             String expectedValue = expectedCard.getValue().trim();
             String calculatedValue = calculatedCard.getValue().trim();
             //Do the numerical comparison  in case there are scientific notations, or precision difference
             if (!isBlank(expectedValue) && isOnlyNumber(expectedValue)){
                 double expectedDoubleNumber = 0;
                 double calculatedDoubleNumber = 0;
                 try {
                     expectedDoubleNumber = Double.parseDouble(expectedValue);
                     calculatedDoubleNumber = Double.parseDouble(calculatedValue);
                 } catch (NumberFormatException ignore) { }
                 //compare double numbers
                 Assert.assertEquals(expectedDoubleNumber, calculatedDoubleNumber,delta);
             }
             else {
                 //compare strings
                 Assert.assertEquals(expectedValue, calculatedValue);
             }
         }

    }

    public boolean isBlank(String value) {
        return (value == null || value.equals("") || value.equals("null") || value.trim().equals(""));
    }


    public boolean isOnlyNumber(String value) {
         try{
             Double.parseDouble(value);
             return true;
         }  catch(NumberFormatException e)
         {
             return false;
         }

    }

    private String[] getExpectedKeys( Header expectedHeader){
        Cursor expectedIter = 	expectedHeader.iterator();

        ArrayList<String> keyArrays = new ArrayList<>();
        int i=0;
        while(expectedIter.hasNext() ){
            HeaderCard expectedCard = (HeaderCard) expectedIter.next();

            if (!expectedCard.getKey().startsWith("SPOT") && expectedCard.getKey().trim().length()!=0) {
                keyArrays.add(expectedCard.getKey());
                i++;
            }
        }
        return keyArrays.toArray(new String[0]);
    }
}
