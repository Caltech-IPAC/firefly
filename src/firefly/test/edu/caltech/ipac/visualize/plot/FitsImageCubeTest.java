//public class FitsImageCubeTest {
//
//    private FitsImageCube fitsCube;
//    private String  inCubeFitsFileName = "cube1Linear.fits";//"cube1.fits";
//    private Fits inCubeFits;
//    private String dgName= "cube1LinearDg.fits";
//    private DataGroup exDg;
//    @Before
//    /**
//     * An one dimensional array is created and it is used to run the unit test for Histogram's public methods
//     */
//    public void setUp() throws FitsException, ClassNotFoundException, IOException {
//        //this FITS file has three extensions.  Using it as expected value to get if the FitsRead can get all extensions
//        inCubeFits =  FileLoader.loadFits(FitsImageCubeTest.class, inCubeFitsFileName);
//
//       exDg = FITSTableReader.convertFitsToDataGroup(
//               FileLoader.getDataPath(FitsImageCubeTest.class)+"/"+dgName,
//                null,
//                null,
//                FITSTableReader.EXPAND_BEST_FIT, 1);
//
//        fitsCube = FitsReadFactory.createFitsImageCube(inCubeFits);
//
//
//
//    }
//
//    @After
//    /**
//     * Release the memories
//     */
//    public void tearDown() {
//        fitsCube  =null;
//    }
//
//    @Test
//    public void testGetDataGroup() throws DataFormatException, FitsException, PixelValueException, IOException {
//
//        String[] keys =fitsCube.getMapKeys();
//        ImagePt imagePt = new ImagePt(0, 0);
//        //verify one data group
//        DataGroup dg = fitsCube.getDataGroup(keys[0], imagePt);
//        DataType[] types = dg.getDataDefinitions();
//        DataType[] expTypes = exDg.getDataDefinitions();
//
//        for (int i=0; i<types.length; i++){
//            Assert.assertEquals(types[i].getKeyName(), expTypes[i].getKeyName());
//
//            Assert.assertArrayEquals(dg.get(i).getData(), exDg.get(i).getData());
//        }
//
//        //assert the wavelength is calculated when CTYPE3 is WAVE
//        for (int i=0; i<keys.length; i++){
//            FitsRead[] fr = fitsCube.getFitsReadMap().get( keys[i]);
//            DataGroup dgs = fitsCube.getDataGroup(keys[i],  imagePt);
//            for (int j=0; j<fr.length; j++){
//                if (fr[j].getHeader().getStringValue("CTYPE3").toUpperCase().startsWith("WAVE")
//                         || fr[j].getHeader().getStringValue("CTYPE3").toUpperCase().startsWith("AWAV") ){
//                    //check if the wavelength is in the data group
//                    DataObject obj = dgs.get(i);
//                    Assert.assertNotNull("The wavelength can not be null", obj.getDataElement("wavelength"));
//                }
//
//
//            }
//        }
//
//
//    }
//}
