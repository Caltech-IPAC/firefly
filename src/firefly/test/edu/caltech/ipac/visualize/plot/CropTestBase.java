package edu.caltech.ipac.visualize.plot;

import nom.tam.fits.*;
import nom.tam.util.Cursor;
import org.junit.Assert;

/**
 * Created by zhang on 11/15/16.
 */
public class CropTestBase {
    private  double  delta =0.1E-10;

    /**
     * Validate the header card one by one
     * @param expectedFits
     * @param calculatedFits
     * @throws FitsException
     */
   void validHeader (Fits expectedFits, Fits calculatedFits) throws FitsException {
        BasicHDU[] expectedHDUs =  expectedFits.read();

        BasicHDU[] outHDUs =calculatedFits.read();
        for (int i=0; i<expectedHDUs.length; i++){
            Header expectedHeader = expectedHDUs[i].getHeader();
            Header calculatedHeader = outHDUs[i].getHeader();
            Assert.assertEquals(expectedHeader.getNumberOfCards(), calculatedHeader.getNumberOfCards());
            Cursor expectedIter = 	expectedHeader.iterator();
            Cursor  calculatedIter = 	calculatedHeader.iterator();
            while(expectedIter.hasNext() && calculatedIter.hasNext()){
                HeaderCard expectedCard = (HeaderCard) expectedIter.next();
                HeaderCard calculatedCard = (HeaderCard) calculatedIter.next();
                Assert.assertEquals( expectedCard.getKey(), calculatedCard.getKey());
                Assert.assertEquals( expectedCard.getValue(), calculatedCard.getValue());
            }
        }
    }

    /**
     * Validate the data stored in the FITS file
     * @param expectedFits
     * @param calculatedFits
     * @throws FitsException
     */
    void validateData(Fits expectedFits, Fits calculatedFits) throws FitsException {
        FitsRead[] fitsReads = FitsRead.createFitsReadArray(calculatedFits);
        FitsRead[] expectedFitsRead = FitsRead.createFitsReadArray(expectedFits);
        for (int i=0;  i<fitsReads.length; i++){
            Assert.assertArrayEquals(fitsReads[i].getDataFloat(), expectedFitsRead[i].getDataFloat(), (float) delta);
        }

    }
}
