/*eslint-env node, mocha */
import {expect,assert} from 'chai';
import {Projection, makeProjection} from '../Projection.js';
import CoordinateSys from '../../CoordSys.js';

/**
 * LZ finalized on 8/30/16
 * The javascript unit test uses the same data in the java unit test tree.
 *
 */
var fs = require('fs');

const JAVA_TEST_DATA_PATH='firefly_test_data/edu/caltech/ipac/visualize/plot/projection/';

const precision=10;

var projectionJson={};


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
    UNRECOGNIZED : 1999
};

function getJsonFiles(dir){
    var fileList = [];

    var files = fs.readdirSync(dir);
    for(var i in files){
        if (!files.hasOwnProperty(i)) continue;
        var name = dir+'/'+files[i];
        if (!fs.statSync(name).isDirectory() && name.endsWith('json') ){
            fileList.push(name);
        }
    }
    return fileList;
}

describe('A test suite for projection.js', function () {



    describe('This is to test all json files stored in the testing directory', function () {


        var path = require('path');
        //__filename returns absolute path to file where it is placed
        var scriptDirString = path.dirname(fs.realpathSync(__filename));

       //the abosolute root path to js/ not include js
       // var rootPath = scriptDirString.split('js')[0];
        var rootPath = scriptDirString.split('firefly')[0];
        var dataPath = rootPath+JAVA_TEST_DATA_PATH;

        var jsonFiles = getJsonFiles(dataPath);
        console.log(dataPath);

        var imageHeader;
        var expectedImagePt;
        var expectedWorldPt;
        var imagePt, worldPt;


        for (let i=0; i<jsonFiles.length; i++){

            it('this is to test:'+jsonFiles[i], function (){
                var jsonStr =require(jsonFiles[i]);
                imageHeader = jsonStr.header;
                //console.log(imageHeader);
                projectionJson['header'] = imageHeader;
                projectionJson['coorindateSys'] = 'EQ_J2000';
               // console.log(projectionJson);
                var jsProjection = makeProjection(projectionJson);

                if (jsProjection){
                    console.log('the jsProject is:'+jsProjection.getProjectionName());
                    var projectionName = jsProjection.getProjectionName();
                    var maptype = imageHeader.maptype;
                    expect(projTypes[projectionName]).to.equals(maptype);

                }
                else {
                    console.log('jsProject is null');
                }


                imagePt = jsProjection.getImageCoords( imageHeader.crval1, imageHeader.crval2);//RA and DEC at the center of the image

                //the expected value is achieved when the test is written.  If the Java code changes and
                //the Assert is falling, the changes introduce the problem.
                expectedImagePt = jsonStr['expectedImagePt'];//replace(/[=]/g, ':');

                //console.log(image_pt);
                expect( expectedImagePt.x.toFixed(precision)).to.equals(imagePt.x.toFixed(precision));
                expect( expectedImagePt.y.toFixed(precision)).to.equals(imagePt.y.toFixed(precision));

                // assert.equal(expectedImagePt ,image_pt );
                expectedWorldPt = jsonStr['expectedWorldPt'];//.replace(/[=]/g, ':');

                // console.log(expectedWorldPt);
                //var expectedWorldPt = JSON.parse(expectedWorldPtStr);
                worldPt = jsProjection.getWorldCoords(imagePt.x, imagePt.y);
                expect( expectedWorldPt.x.toFixed(precision)).to.equals(worldPt.x.toFixed(precision));
                expect(  expectedWorldPt.y.toFixed(precision)).to.equals(worldPt.y.toFixed(precision));



            });
        }

    });

});

