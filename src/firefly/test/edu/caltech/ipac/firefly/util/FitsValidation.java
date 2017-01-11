package edu.caltech.ipac.firefly.util;

import edu.caltech.ipac.visualize.plot.FitsRead;
import nom.tam.fits.*;
import nom.tam.util.Cursor;
import org.junit.Assert;

import java.util.ArrayList;

/**
 * Created by zhang on 12/13/16.
 * This is a base class to validate FITS file for the unit test
 */
public class FitsValidation {
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
        FitsRead[] fitsReads = FitsRead.createFitsReadArray(calculatedFits);
        FitsRead[] expectedFitsRead = FitsRead.createFitsReadArray(expectedFits);
        for (int i=0;  i<fitsReads.length; i++){
            Assert.assertArrayEquals(fitsReads[i].getDataFloat(), expectedFitsRead[i].getDataFloat(), (float) delta);
        }

    }

    public void validateFits(Fits expectedFits, Fits fits) throws FitsException {
        validateHDUs(expectedFits,fits);
        validateData(expectedFits,fits);
    }

    public void validateSingleHDU(BasicHDU expectedHdu, BasicHDU outHdu) throws FitsException{

         Header expectedHeader = expectedHdu.getHeader();
         Header calculatedHeader = outHdu.getHeader();
         //validate expected and calculated headers have the same number of card
         Assert.assertEquals(expectedHeader.getNumberOfCards(), calculatedHeader.getNumberOfCards());

         //validate expected and calculated headers have the same keys and same values
         String[] keys = getExpectedKeys(expectedHeader);
         for (int i=0; i<keys.length; i++){
             HeaderCard expectedCard =expectedHeader.findCard(keys[i]);
             HeaderCard calculatedCard =calculatedHeader.findCard(keys[i]);
             if (expectedCard==null && calculatedCard==null) continue;
             if (expectedCard!=null &&   calculatedCard!=null ){
                  Assert.assertEquals(expectedCard.getValue(), calculatedCard.getValue());
             }
            else {
                 Assert.fail("The calculated Fits Header is not the same as expected header");
                 break;
             }

         }

   }

    private String[] getExpectedKeys( Header expectedHeader){
        Cursor expectedIter = 	expectedHeader.iterator();

        ArrayList<String> keyArrays = new ArrayList<>();
        int i=0;
        while(expectedIter.hasNext() ){
            HeaderCard expectedCard = (HeaderCard) expectedIter.next();
            if (expectedCard.getKey().trim().length()!=0) {
                keyArrays.add(expectedCard.getKey());
                i++;
            }
        }
        return keyArrays.toArray(new String[0]);
    }
}
