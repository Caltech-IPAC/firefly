package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
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


            int calcCardCnt= calculatedHeader.getNumberOfCards();
            Cursor<String, HeaderCard> j= calculatedHeader.iterator();

            while (j.hasNext()) {
                HeaderCard c= j.next();
                if (c.getKey().startsWith("SPOT")) calcCardCnt--;
            }


            Assert.assertEquals(expectedHeader.getNumberOfCards(), calcCardCnt);
            Cursor expectedIter = 	expectedHeader.iterator();
            Cursor  calculatedIter = 	calculatedHeader.iterator();
            while(expectedIter.hasNext() && calculatedIter.hasNext()){
                HeaderCard expectedCard = (HeaderCard) expectedIter.next();
                Assert.assertEquals( expectedHeader.getStringValue(expectedCard.getKey()),
                                    calculatedHeader.getStringValue(expectedCard.getKey()) );
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
        FitsRead[] fitsReads = FitsReadFactory.createFitsReadArray(calculatedFits);
        FitsRead[] expectedFitsRead = FitsReadFactory.createFitsReadArray(expectedFits);
        for (int i=0;  i<fitsReads.length; i++){
            Assert.assertArrayEquals(fitsReads[i].getDataFloat(), expectedFitsRead[i].getDataFloat(), (float) delta);
        }

    }
}
