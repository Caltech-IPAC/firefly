/*eslint-env node, mocha */
import {expect,assert} from 'chai';
import {Projection, makeProjection} from '../Projection.js';
import CoordinateSys from '../../CoordSys.js';
//import {FileUpload} from '../../../ui/FileUpload.jsx';

var fs = require('fs');

const JAVA_TEST_DATA_PATH='/test/edu/caltech/ipac/visualize/plot/projection/';
const precision=10;


var projectionJson={};



function getImageHeader( imageHeaderStr){
   // function getImageHeader( jsonHeaderFile){


        // var fits = FileUpload('f3.fits');
   // console.log(fits);
   // var fitsHeader = require(jsonHeaderFile);
    //make the header in json syntax
    //console.log(imageHeaderStr);
    var header = imageHeaderStr.replace(/\s[=]\s/g, ':');
   // console.log(header);
    //TODO test img should be imageHeader
   // var img = JSON.parse(header);
    //console.log('parsed header is:'+img );
    var hArr = header.trim().split(/\s/g);
    var imageHeader = {};
    var  pair=[];
   // String.prototype.isNumber = function(){return /\d/.test(this);}
    for (let i=0; i<hArr.length; i++){
        pair = hArr[i].split(':');
        if (pair.length==2) {

          if ( /\d/.test(pair[1]) ) {

              if (pair[1].indexOf('.') > -1) {// integer
                  imageHeader[[pair[0]]] = parseFloat(pair[1]);
              }
              else {

                  imageHeader[[pair[0]]] = parseInt(pair[1]);
              }
          }
          else {
               imageHeader[ [pair[0]] ]= pair[1];
           }
        }
    }
    //console.log(imageHeader);
    return imageHeader;

}
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


    describe('This is to test the ORTHOGRAPHIC projection using the same input FITS:twomass-j-SIN.fits which is used by java unit test', function () {

        it('should be equal to javaProject with the same input FITS file: twomass-j-SIN.fits, and same coordinate=CoordinateSys.EQ_J2000', function () {
            //build up a header using the same header information from twomass-j-SIN.fits in the java unit test directory
            var header =
            {
                cdelt1 : -2.777777845E-4 ,
                cdelt2 : 2.777777845E-4 ,
                crota1: 0,
                crota2: 0.0127251805,
                crpix1:  161.5,
                crpix2:484.5,
                crval1:  10.71769366,
                crval2: 41.33390546,
                ctype1: 'RA---SIN        ',
                ctype2: 'DEC--SIN        ',
                datamax: 112.425,
                datamin: 1.40037,
                file_equinox: 2000,
                map_distortion: false,
                maptype: 1002

            };


            projectionJson['header'] = header;
            projectionJson['coorindateSys'] = 'EQ_J2000';

            var jsProjection = makeProjection(projectionJson);
            console.log( projectionJson);
            if (jsProjection){
                console.log('the jsProject is:'+jsProjection.getProjectionName());
            }
            else {
                console.log('jsProject is null');
            }

            var  input_ra =  10.6395000;
             var input_dec = 41.2638333;

            var image_pt = jsProjection.getImageCoords( input_ra, input_dec);
            var expectedImagePt = {x:372.15189358590766, y:231.38265329482758 }; //from the java's calculation for the same input
            expect( expectedImagePt.x.toFixed(precision)).to.equals(image_pt.x.toFixed(precision));
            expect( expectedImagePt.y.toFixed(precision)).to.equals(image_pt.y.toFixed(precision));

        });

      } );

    describe('This is to test the GNOMONIC projection using the same input FITS:iris-25-GNOMONIC.fits which is used by java unit test', function () {

        it('should be equal to javaProject with the same input FITS file: twomass-j-SIN.fits, and same coordinate=CoordinateSys.EQ_J2000', function () {
            //build up a header using the same header information from twomass-j-SIN.fits in the java unit test directory
            var header =
            {
                crpix1 : -13.0,
                crpix2 : 122.0,
                crval1 : 156.0,
                crval2 : 70.0,
                cdelt1 : -0.025,
                cdelt2 : 0.025,
                crota2 : 0.0,
                crota1 : 0.0,
                file_equinox : 1950.0,
                ctype1 :'RA---TAN ,      ',
                ctype2 :'DEC--TAN ,     ',
                maptype: 1001

            };

            projectionJson['header'] = header;
            projectionJson['coorindateSys'] = 'EQ_J2000';

            var jsProjection = makeProjection(projectionJson);
            /* describe('This is to test all json files stored in the testing directory', function () {

             //var path ='/Users/zhang/lsstDev/testingData/projectionTestingData/';
             var path = require('path');
             var scriptDirString = path.dirname(fs.realpathSync(__filename));
             // console.log(scriptDirString);
             var rootPath = scriptDirString.split('js')[0];
             //console.log(rootPath);
             var dataPath = rootPath+JAVA_TEST_DATA_PATH;

             var jsonFiles = getJsonFiles(dataPath);
             //console.log(dataPath);

             var imageHeader, imageHeaderStr;
             var expectedProjectPt;
             var expectedWorldPt;
             //for (let i=0; i<jsonFiles.length; i++){

             for (let i=0; i<1; i++){

             it('this is to test:'+jsonFiles[i], function (){
             var jsonStr = require(jsonFiles[i]);
             imageHeaderStr = jsonStr.header;//.replace(/\s[=]\s/g, ':');

             // console.log(imageHeaderStr);
             imageHeader = getImageHeader( imageHeaderStr);
             //console.log(imageHeader);
             projectionJson['header'] = imageHeader;
             projectionJson['coorindateSys'] = 'EQ_J2000';
             console.log(projectionJson);
             var jsProjection = makeProjection(projectionJson);
             console.log(jsProjection.getProjectionName());

             if (jsProjection){
             console.log('the jsProject is:'+jsProjection.getProjectionName());
             }
             else {
             console.log('jsProject is null');
             }

             console.log(imageHeader.crval1);
             console.log(imageHeader.crval2);
             console.log(imageHeader.maptype);

             var image_pt = jsProjection.getImageCoords( imageHeader.crval1, imageHeader.crval2);//RA and DEC at the center of the image

             //the expected value is achieved when the test is written.  If the Java code changes and
             //the Assert is falling, the changes introduce the problem.
             var expectedProjectPtStr = jsonStr['expectedImagePt'];//replace(/[=]/g, ':');
             expectedProjectPt = getXYObjFromString(expectedProjectPtStr );
             //console.log(expectedProjectPt);
             //console.log(expectedProjectPt.x);
             console.log(image_pt);
             expect( expectedProjectPt.x.toFixed(precision)).to.equals(image_pt.x.toFixed(precision));
             expect( expectedProjectPt.y.toFixed(precision)).to.equals(image_pt.y.toFixed(precision));

             // assert.equal(expectedProjectPt ,image_pt );
             var expectedWorldPtStr = jsonStr['expectedWorldPt'];//.replace(/[=]/g, ':');

             expectedWorldPt=getXYObjFromString(expectedWorldPtStr );
             //console.log(expectedWorldPt);
             //var expectedWorldPt = JSON.parse(expectedWorldPtStr);
             var  world_pt = jsProjection.getWorldCoords(image_pt.x, image_pt.y);
             expect( expectedWorldPt.x.toFixed(precision)).to.equals(world_pt.x.toFixed(precision));
             expect(  expectedWorldPt.y.toFixed(precision)).to.equals(world_pt.y.toFixed(precision));



             });
             }

             });*/
            if (jsProjection){
                console.log('the jsProjection is:'+jsProjection.getProjectionName());
            }
            else {
                console.log('jsProjection is null');
            }

            var  input_ra = 8.1759583;
            var   input_dec =41.9102500;


            var image_pt = jsProjection.getImageCoords( input_ra, input_dec);
            var expectedImagePt = {x:2189.175045953085, y:4681.739079415915}; //from the java's calculation for the same input
            expect( expectedImagePt.x.toFixed(precision)).to.equals(image_pt.x.toFixed(precision));
            expect( expectedImagePt.y.toFixed(precision)).to.equals(image_pt.y.toFixed(precision));


            var  world_pt = jsProjection.getWorldCoords(image_pt.x, image_pt.y);
            const expectedWorldPt = {x:8.17595829999999,y:41.91024999999999};

            expect( expectedWorldPt.x.toFixed(precision)).to.equals(world_pt.x.toFixed(precision));
            expect(  expectedWorldPt.y.toFixed(precision)).to.equals(world_pt.y.toFixed(precision));


        });

    } );


   /* describe('This is to test GNOMONIC for the input "f3.fits  header" stored in  f3Header.json ', function () {

        var jasonHeaderFile = '/Users/zhang/lsstDev/testingData/projectionTestingData/f3Header.json';
        var  imageHeader = getImageHeader(jasonHeaderFile);


        it ('should load the fits header into a jason object', function () {

            console.log(imageHeader);
            projectionJson['header'] = imageHeader;
            projectionJson['coorindateSys'] = 'EQ_J2000';
            var jsProjection = makeProjection(projectionJson);

            if (jsProjection){
                console.log('the jsProject is:'+jsProjection.getProjectionName());
            }
            else {
                console.log('jsProject is null');
            }

        });



    });
*/
    function getXYObjFromString(xyString){

      var xyArray=xyString.split(' ');
      var xArray=xyArray[0].split('=');
      var  yArray=xyArray[1].split('=');
      var xyObj = { x: parseFloat(xArray[1]), y: parseFloat(yArray[1]) };
      return xyObj;


    }
    describe('This is to test all json files stored in the testing directory', function () {

        //var path ='/Users/zhang/lsstDev/testingData/projectionTestingData/';
        var path = require('path');
        var scriptDirString = path.dirname(fs.realpathSync(__filename));
       // console.log(scriptDirString);
        var rootPath = scriptDirString.split('js')[0];
        //console.log(rootPath);
        var dataPath = rootPath+JAVA_TEST_DATA_PATH;

        var jsonFiles = getJsonFiles(dataPath);
        //console.log(dataPath);

        var imageHeader, imageHeaderStr;
        var expectedProjectPt;
        var expectedWorldPt;

        for (let i=0; i<jsonFiles.length; i++){

        //for (let i=0; i<1; i++){

            it('this is to test:'+jsonFiles[i], function (){
                var jsonStr = require(jsonFiles[i]);
                imageHeaderStr = jsonStr.header;//.replace(/\s[=]\s/g, ':');

                imageHeader = getImageHeader( imageHeaderStr);
               // console.log(imageHeader);

                if (projectionJson['amd_x_coeff']){
                    imageHeader['amd_x_coeff']=jsonStr['amd_x_coeff'];
                }
                if (projectionJson['amd_y_coeff']){
                    console.log(jsonStr['amd_y_coeff']);
                    imageHeader['amd_y_coeff']=jsonStr['amd_y_coeff'];
                }
                if (projectionJson['ppo_coeff']){
                    imageHeader['ppo']=jsonStr['ppocoeff'];
                }
                projectionJson['header'] = imageHeader;
                projectionJson['coorindateSys'] = 'EQ_J2000';
                console.log(projectionJson);

                var jsProjection = makeProjection(projectionJson);
                //console.log(jsProjection.getProjectionName());

                if (jsProjection){
                    console.log('the jsProject is:'+jsProjection.getProjectionName());
                }
                else {
                    console.log('jsProject is null');
                }


                var image_pt = jsProjection.getImageCoords( imageHeader.crval1, imageHeader.crval2);//RA and DEC at the center of the image

                //the expected value is achieved when the test is written.  If the Java code changes and
                //the Assert is falling, the changes introduce the problem.
                var expectedProjectPtStr = jsonStr['expectedImagePt'];//replace(/[=]/g, ':');
                expectedProjectPt = getXYObjFromString(expectedProjectPtStr );
                //console.log(expectedProjectPt);
                //console.log(expectedProjectPt.x);
                //console.log(image_pt);
                expect( expectedProjectPt.x.toFixed(precision)).to.equals(image_pt.x.toFixed(precision));
                expect( expectedProjectPt.y.toFixed(precision)).to.equals(image_pt.y.toFixed(precision));

               // assert.equal(expectedProjectPt ,image_pt );
                var expectedWorldPtStr = jsonStr['expectedWorldPt'];//.replace(/[=]/g, ':');

                expectedWorldPt=getXYObjFromString(expectedWorldPtStr );
                //console.log(expectedWorldPt);
                //var expectedWorldPt = JSON.parse(expectedWorldPtStr);
                var  world_pt = jsProjection.getWorldCoords(image_pt.x, image_pt.y);
                expect( expectedWorldPt.x.toFixed(precision)).to.equals(world_pt.x.toFixed(precision));
                expect(  expectedWorldPt.y.toFixed(precision)).to.equals(world_pt.y.toFixed(precision));



            });
        }

    });

});

