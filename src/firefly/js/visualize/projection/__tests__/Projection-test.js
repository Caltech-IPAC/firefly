import assert from 'assert';

import { makeProjection} from '../Projection.js';

/**
 * LZ finalized on 8/30/16
 * The javascript unit test uses the same data in the java unit test tree.
 *
 *
 */
const fs = require('fs');

const JAVA_TEST_DATA_PATH='firefly_test_data/edu/caltech/ipac/visualize/plot/projection/';

const precision=10;

const projTypes = {
    GNOMONIC   : 1001,
    ORTHOGRAPHIC : 1002,
    NCP          : 1003,
    AITOFF       : 1004,
    CAR          : 1005,
    LINEAR       : 1006,
    PLATE        : 1007,
    ARC          : 1008,
    SFL          : 1009,
    CEA          : 1010,
    UNSPECIFIED  : 1998,
    UNRECOGNIZED : 1999,
    TPV          : 1011
};

function getJsonFiles(dir){
    const fileList = [];

    const files = fs.readdirSync(dir);
    for(let i in files){
        if (!files.hasOwnProperty(i)) continue;
        const name = dir+'/'+files[i];
        if (!fs.statSync(name).isDirectory() && name.endsWith('json') ){
            fileList.push(name);
        }
    }
    return fileList;
}
describe('A test suite for projection.js', function () {



    describe('This is to test all json files stored in the testing directory', function () {

        const path = require('path');
        //__filename returns absolute path to file where it is placed
        const scriptDirString = path.dirname(fs.realpathSync(__filename));
        const rootPath = scriptDirString.split('firefly')[0];
        const dataPath =rootPath+JAVA_TEST_DATA_PATH;

        //read out all test files stored in json format
        const jsonFiles = getJsonFiles(dataPath);

        for (let i=0; i<jsonFiles.length; i++){

            it('this is to test:'+jsonFiles[i], function (){
                const jsonStr =require(jsonFiles[i]);
                const imageHeader = jsonStr.header;
                const jsProjection = makeProjection({'header':imageHeader, 'coorindateSys':'EQ_J2000'});

                if (jsProjection) {
                    console.log('the jsProject is:' + jsProjection.getProjectionName());
                    console.log(jsonFiles[i]);
                    const projectionName = jsProjection.getProjectionName();
                    const maptype = imageHeader.maptype;
                    assert.strictEqual(projTypes[projectionName], maptype);
                }
                else {
                    console.log('jsProject is null');
                }


                const imagePt = jsProjection.getImageCoords( imageHeader.crval1, imageHeader.crval2);//RA and DEC at the center of the image

                //the expected value is achieved when the test is written.  If the Java code changes and
                //the Assert is falling, the changes introduce the problem.
                const expectedImagePt = jsonStr['expectedImagePt'];//replace(/[=]/g, ':');

                //console.log(image_pt);
               assert.equal( expectedImagePt.x.toFixed(precision), imagePt.x.toFixed(precision));
               assert.equal( expectedImagePt.y.toFixed(precision), imagePt.y.toFixed(precision));


                // assert.equal(expectedImagePt ,image_pt );
                const expectedWorldPt = jsonStr['expectedWorldPt'];//.replace(/[=]/g, ':');

                // console.log(expectedWorldPt);
                //var expectedWorldPt = JSON.parse(expectedWorldPtStr);
               const  worldPt = jsProjection.getWorldCoords(imagePt.x, imagePt.y);
               assert.equal( expectedWorldPt.x.toFixed(precision),  worldPt.x.toFixed(precision));
               assert.equal(  expectedWorldPt.y.toFixed(precision), worldPt.y.toFixed(precision));

            });
        }

    });

});

