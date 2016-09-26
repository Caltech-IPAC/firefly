/*eslint-env node, mocha */
import {expect,assert} from 'chai';
import {doConv, doConvPM} from '../CoordConv.js';
const  EQUATORIAL_J = 0;
const EQUATORIAL_B = 1;
const  GALACTIC = 2;
const  ECLIPTIC_B = 3;
const SUPERGALACTIC = 4;
const ECLIPTIC_J = 13;
const EQINOX2000=2000.0;
const EQINOX1950=1950.0;
const precision=10;
describe('A test suite for CoordConv.js', function () {


    it('should be equal when convert from inCoord = EQUATORIAL_J, to outCoord = coordianteSys where inEquirox=outEquinox=2000', function () {
         const coordianteSys = [
             //EQUATORIAL_J,
             EQUATORIAL_B,
             GALACTIC,
             ECLIPTIC_B,
             SUPERGALACTIC
         ];
         const expectedLons = [
             124.53450295504638,
             254.14759117810968,
             140.5026330845168,
             167.8940515712723
         ];
         const expectedLats =[
              -35.95580982205028,
              -0.0946854772394013,
              -53.509857120978175,
              -62.562888161876515
         ];
         const  inLon =  124.534666;
         const inLat =  -35.955853;
         var lons = [];
         var lats = [];
         var ret;
         const tobs=0.0;
         const inCoord = EQUATORIAL_J;
         for (let i = 0; i < coordianteSys.length; i++){
            ret  =   doConv(inCoord, EQINOX2000,
                 inLon, inLat,
                 coordianteSys[i], EQINOX2000, tobs);
             lons[i]=ret.lon;
             lats[i]=ret.lat;
         }
        for (let i=0; i<lons.length; i++){
            expect(lons[i].toFixed( precision), `${i} th lon  not equal` ).to.equal(expectedLons[i].toFixed( precision));
            expect(lats[i].toFixed( precision), `${i} th lat not equal`).to.equal(expectedLats[i].toFixed( precision));
        }

     });


    it('should be equal when convert from inCoord = EQUATORIAL_B, to outCoord = coordianteSys where inEquirox=outEquinox=2000', function () {
        const coordianteSys = [
            EQUATORIAL_J,
            //EQUATORIAL_B,
            GALACTIC,
            ECLIPTIC_B,
            SUPERGALACTIC
        ];
        const expectedLons = [124.53482902154403,  254.14770106291908, 140.5028659746326, 167.89392878562498];
        const expectedLats =[-35.95589617435087,   -0.0946006066378949, -53.50984702816197, -62.56276136778953];
        const  inLon =  124.534666;
        const inLat =  -35.955853;
        // LonLat[] lonLat= new LonLat[coordianteSys.length];
        var lons = [];
        var lats = [];
        var ret;
        const tobs=0.0;
        const inCoord = EQUATORIAL_B;
        for (let i = 0; i < coordianteSys.length; i++){
            ret  =   doConv(inCoord, EQINOX2000,
                inLon, inLat,
                coordianteSys[i], EQINOX2000, tobs);
            lons[i]=ret.lon;
            lats[i]=ret.lat;
        }

        for (let i=0; i<lons.length; i++){
            expect(lons[i].toFixed(precision), `${i} th lon  not equal` ).to.equal(expectedLons[i].toFixed(precision));
            expect(lats[i].toFixed(precision), `${i} th lat not equal`).to.equal(expectedLats[i].toFixed(precision));
        }

    });



    it('should be equal when convert from inCoord = GALACTIC, to outCoord = coordianteSys where inEquirox=outEquinox=2000', function () {
        const coordianteSys = [
            EQUATORIAL_J,
            EQUATORIAL_B,
            //GALACTIC,
            ECLIPTIC_B,
            SUPERGALACTIC
        ];
        const expectedLons = [
             14.314192367676567,
             14.313891114646578,
             23.862153926328073,
             322.6015293667469
        ];
        const expectedLats =[
            26.897737748051213,
            26.897721362584967,
            19.108830668361076,
            6.55143894512179
        ];
        const  inLon =  124.534666;
        const inLat =  -35.955853;
        // LonLat[] lonLat= new LonLat[coordianteSys.length];
        var lons = [];
        var lats = [];
        var ret;
        const tobs=0.0;
        const inCoord = GALACTIC;
        for (let i = 0; i < coordianteSys.length; i++){
            ret  =   doConv(inCoord, EQINOX2000,
                inLon, inLat,
                coordianteSys[i], EQINOX2000, tobs);
            lons[i]=ret.lon;
            lats[i]=ret.lat;
        }

        for (let i=0; i<lons.length; i++){
            expect(lons[i].toFixed(precision), `${i} th lon  not equal` ).to.equal(expectedLons[i].toFixed(precision));
            expect(lats[i].toFixed(precision), `${i} th lat not equal`).to.equal(expectedLats[i].toFixed(precision));
        }

    });



    it('should be equal when convert from inCoord = ECLIPTIC_B, to outCoord = coordianteSys where inEquirox=outEquinox=2000', function () {
        const coordianteSys = [
            EQUATORIAL_J,
            EQUATORIAL_B,
            GALACTIC,
            //ECLIPTIC_B,
            SUPERGALACTIC
        ];
        const expectedLons = [
             118.49488604196446,
             118.49466903172939,
             234.2557377542504,
             119.11472767964223
        ];
        const expectedLats =[
            -15.870385824487494,
            -15.87036211628557,
            6.086441566001859,
            -75.81735095264665
        ];
        const  inLon =  124.534666;
        const inLat =  -35.955853;
        // LonLat[] lonLat= new LonLat[coordianteSys.length];
        var lons = [];
        var lats = [];
        var ret;
        const tobs=0.0;
        const inCoord = ECLIPTIC_B;
        for (let i = 0; i < coordianteSys.length; i++){
            ret  =   doConv(inCoord, EQINOX2000,
                inLon, inLat,
                coordianteSys[i], EQINOX2000, tobs);
            lons[i]=ret.lon;
            lats[i]=ret.lat;
        }

        for (let i=0; i<lons.length; i++){
            expect(lons[i].toFixed(precision), `${i} th lon  not equal` ).to.equal(expectedLons[i].toFixed(precision));
            expect(lats[i].toFixed(precision), `${i} th lat not equal`).to.equal(expectedLats[i].toFixed(precision));
        }

   });

    it('should convert from inCoord = SUPERGALACTIC, to outCoord = coordianteSys where inEquirox=outEquinox=2000', function () {
        // Test implementation goes here
        const coordianteSys = [
            EQUATORIAL_J,
            EQUATORIAL_B,
            GALACTIC,
            ECLIPTIC_B,
            //SUPERGALACTIC
        ];
        const expectedLons = [
            160.05774114616887,
            160.05747714105252,
            262.30305234915625,
            167.87051071206182
        ];
        const expectedLats =[
            -15.578527433407034,
            -15.578455616476347,
            36.73676677794771,
            -22.153262855160243
        ];
        const  inLon =  124.534666;
        const inLat =  -35.955853;
        // LonLat[] lonLat= new LonLat[coordianteSys.length];
        var lons = [];
        var lats = [];
        var ret;
        const tobs=0.0;
        const inCoord = SUPERGALACTIC;
        for (let i = 0; i < coordianteSys.length; i++){
            ret  =   doConv(inCoord, EQINOX2000,
                inLon, inLat,
                coordianteSys[i], EQINOX2000, tobs);
            lons[i]=ret.lon;
            lats[i]=ret.lat;
        }

        for (let i=0; i<lons.length; i++){
            expect(lons[i].toFixed(precision), `${i} th lon  not equal` ).to.equal(expectedLons[i].toFixed(precision));
            expect(lats[i].toFixed(precision), `${i} th lat not equal`).to.equal(expectedLats[i].toFixed(precision));
        }

    });


    it('should be equal when convert from inCoord = ECLIPTIC_B, to outCoord =ECLIPTIC_J where inEquirox=outEquinox=1950', function () {
        const expectedLon = 124.5347532192079;
        const expectedLat =-35.95583152511536;
        const  inLon =  124.534666;
        const inLat =  -35.955853;

        const tobs=0.0;
        const inCoord = ECLIPTIC_B;
        const outCoord = ECLIPTIC_J;


        var ret  = doConv(inCoord, EQINOX1950,inLon, inLat,outCoord, EQINOX1950, tobs);

        var lon =ret.lon;
        var lat=ret.lat;


        expect(lon.toFixed(7), 'lon  not equal').to.equal(expectedLon.toFixed(7));
        expect(lat.toFixed(7), 'lat not equal' ).to.equal(expectedLat.toFixed(7));

    });


    it('should be equal when convert from inCoord = ECLIPTIC_J, to outCoord =ECLIPTIC_B where inEquirox=outEquinox=1950', function () {
        const expectedLon = 124.53457878080559;
        const expectedLat =-35.9558744748764;
        const  inLon =  124.534666;
        const inLat =  -35.955853;

        const tobs=0.0;
        const inCoord = ECLIPTIC_J;
        const outCoord = ECLIPTIC_B;


        var ret  = doConv(inCoord, EQINOX1950, inLon, inLat,outCoord, EQINOX1950, tobs);

        var lon =ret.lon;
        var lat=ret.lat;


        expect(lon.toFixed(precision), 'lon  not equal').to.equal(expectedLon.toFixed(precision));
        expect(lat.toFixed(precision), 'lat not equal' ).to.equal(expectedLat.toFixed(precision));

    });



    it('should be equal when convert from inCoord = EQUATORIAL_B,inEquirox=20000 , to outCoord array where outEquinox=1950', function () {
        const coordianteSys = [
            EQUATORIAL_J,
            // EQUATORIAL_B,
            GALACTIC,
            ECLIPTIC_B,
            SUPERGALACTIC,
            ECLIPTIC_B,
            ECLIPTIC_J
        ];
        const expectedLons = [
             189.36453921014683,
             289.97256953016654,
             183.72633259856838,
             103.6528039980443,
             183.72633259856838,
             183.72661635258902
        ];
        const expectedLats =[
            12.06512707543329,
            74.58398219233717,
            14.777604604170802,
            -0.8857277432637922,
            14.777604604170802,
            14.777729481820373
        ];

        const inLon =   188.733333;//124.534666;
        const inLat =   12.34;//-35.955853;

        var lons = [];
        var lats = [];
        var ret;
        const tobs=1983.5;
        const inCoord = EQUATORIAL_B;
        for (let i = 0; i < coordianteSys.length; i++){
            ret  =   doConv(inCoord, EQINOX1950,
                inLon, inLat,
                coordianteSys[i], EQINOX2000, tobs);
            lons[i]=ret.lon;
            lats[i]=ret.lat;
        }

        for (let i=0; i<lons.length; i++){
            expect(lons[i].toFixed(precision), `${i} th lon  not equal` ).to.equal(expectedLons[i].toFixed(precision));
            expect(lats[i].toFixed(precision), `${i} th lat not equal`).to.equal(expectedLats[i].toFixed(precision));
        }


    });

    it('should be equal when handle the Proper Motion conversion from Equatorial B1950 to J2000', function () {

        const expected={
            ra:189.37874706190132,
            dec: 12.065158672309735,
            raPM: 1.0012468839571316,
            decPM:0.005252980797938754
        };
        const in_lon = 188.733333;
        const  in_lat = 12.34;
        const  in_pmlon = 1.0;
        const  in_pmlat = 0.0;


        var fromB1950ToJ2000=true;
        var result = doConvPM( fromB1950ToJ2000 ,in_lon,  in_lat,in_pmlon, in_pmlat) ;


        Object.keys(result).forEach(function(key) {
            expect(result[key].toFixed(precision), 'lon  not equal').to.equal(expected[key].toFixed(precision) );
        });


    });


    it('should be equal when handle the Proper Motion conversion from Equatorial J2000 to B1950', function () {

        const expected={
            ra:188.08745733910888,
            dec:  12.615389043008733,
            raPM: 0.998835388671965,
            decPM:-0.0052536804247012465
        };

        const in_lon = 188.733333;
        const  in_lat = 12.34;
        const  in_pmlon = 1.0;
        const  in_pmlat = 0.0;


        var fromB1950ToJ2000=false;
        var result =doConvPM( fromB1950ToJ2000 ,in_lon,  in_lat,in_pmlon, in_pmlat) ;



        Object.keys(result).forEach(function(key) {
            expect(result[key].toFixed(precision), 'lon  not equal').to.equal(expected[key].toFixed(precision) );
        });


    });



    it('should be equal when convert from inCoord = EQUATORIAL_J,inEquirox=20000 , to outCoord=EQUATORIAL_B outEquinox=1950 lat=-90,',  function () {

        const expectedLons = [
            179.69493905911276,
            179.6949390591126,
            179.69493905911244,
            179.69493905911233,
            179.69493905911222,
            179.69493905911213,
            179.69493905911207,
            179.69493905911202,
            179.69493905911202,
            179.69493905911204,
             179.6949390591121,
             179.6949390591122,
             179.6949390591123,
             179.69493905911244,
             179.69493905911258,
             179.69493905911273,
             179.6949390591129,
             179.69493905911304,
             179.69493905911318,
             179.69493905911327,
             179.69493905911335,
             179.6949390591134,
             179.69493905911347,
             179.69493905911347,
             179.69493905911344,
             179.69493905911338,
             179.6949390591133,
             179.69493905911318,
             179.69493905911304,
             179.6949390591129,
             179.69493905911276
        ];
        const expectedLats =[
            -89.72157064600971
            -89.72157064600971
            -89.72157064600971
            -89.72157064600971
            -89.72157064600971
            -89.72157064600971
            -89.72157064600971
            -89.72157064600971
            -89.72157064600971
            -89.72157064600968
             -89.7215706460096,
             -89.7215706460096,
             -89.7215706460096,
             -89.7215706460096,
             -89.7215706460096,
             -89.7215706460096,
             -89.7215706460096,
             -89.7215706460096,
             -89.7215706460096,
             -89.7215706460096,
             -89.7215706460096,
             -89.7215706460096,
             -89.7215706460097,
             -89.7215706460097,
             -89.7215706460097,
             -89.7215706460097,
             -89.7215706460097,
             -89.7215706460097,
             -89.7215706460097,
             -89.7215706460097,
             -89.72157064600971
        ];

        const deltaLon=12.0;

        var lons =[];


        for (let i=0; i<31; i++){
            lons[i] = deltaLon*i;
        }

        const inLat =  -90;//-35.955853;

        var lons = [];
        var lats = [];
        var ret;
        const tobs=1983.5;
        const inCoord = EQUATORIAL_J;
        const outCoord = EQUATORIAL_B;
        for (let i = 0; i < lons.length; i++){
            ret  =   doConv(inCoord, EQINOX2000,
                lons[i], inLat,
                outCoord, EQINOX1950, tobs);
            lons[i]=ret.lon;
            lats[i]=ret.lat;
        }

        for (let i=0; i<lons.length; i++){
            expect(lons[i].toFixed(precision), `${i} th lon  not equal` ).to.equal(expectedLons[i].toFixed(precision));
            expect(lats[i].toFixed(precision), `${i} th lat not equal`).to.equal(expectedLats[i].toFixed(precision));
        }


    });

    it('should be equal when convert from inCoord = EQUATORIAL_J,inEquirox=20000 , to outCoord=EQUATORIAL_B outEquinox=1950 lat=0.0',  function () {

        const expectedLons = [
            359.3592849828098,
            11.359438904263449,
            23.359566843235793,
            35.359650057376456,
            47.359677830602095,
            59.35964940454754,
            71.35956484441512,
            83.35944478971054,
            95.35931135011296,
            107.35918709318142,
             119.35908986175045,
             131.3590421568453,
             143.35904600332142,
             155.3591007329216,
             167.3591963210104,
             179.35931461851578,
             191.35943370129027,
             203.35953056942128,
             215.35958518331051,
             227.35958610780577,
             239.35953390722406,
             251.35942969109652,
             263.35929009885723,
             275.359141930767,
             287.3590120840817,
             299.35892108133567,
             311.3588895795302,
             323.3589192601622,
             335.359005963513,
             347.3591355634421,
             359.3592849828098
        ];
        const expectedLats =[
           -0.2783452467691817,
           -0.27258880994923246,
           -0.2549168566135561,
           -0.22610388924970531,
           -0.18741017409840746,
           -0.140524933281613,
           -0.0874966364959342,
           -0.030644779011098463,
           0.027545426167679507,
           0.084531511433299,
            0.13782588020254463,
            0.18509716721558142,
            0.2242802023550557,
            0.25366068042758827,
            0.2719535488741306,
            0.27836006972495225,
            0.2726021733065438,
            0.2549288290606657,
            0.22611358589813,
            0.18741641466087752,
            0.14052771512116813,
            0.08749733253305987,
            0.030643405348049723,
            -0.027548326428057907,
            -0.08453481107285257,
            -0.13782788666203,
            -0.1850936532441476,
            -0.22427098142081875,
            -0.25364740528097,
            -0.27193864159257625,
            -0.2783452467691817
        ];

        const deltaLon=12.0;

        var lons =[];


        for (let i=0; i<31; i++){
            lons[i] = deltaLon*i;
        }

        const inLat = 0.0;

        var lons = [];
        var lats = [];
        var ret;
        const tobs=1983.5;
        const inCoord = EQUATORIAL_J;
        const outCoord = EQUATORIAL_B;
        for (let i = 0; i < lons.length; i++){
            ret  =   doConv(inCoord, EQINOX2000,
                lons[i], inLat,
                outCoord, EQINOX1950, tobs);
            lons[i]=ret.lon;
            lats[i]=ret.lat;
        }

        for (let i=0; i<lons.length; i++){
            expect(lons[i].toFixed(precision), `${i} th lon  not equal` ).to.equal(expectedLons[i].toFixed(precision));
            expect(lats[i].toFixed(precision), `${i} th lat not equal`).to.equal(expectedLats[i].toFixed(precision));
        }


    });


    it('should be equal when convert from inCoord = EQUATORIAL_J,inEquirox=20000 , to outCoord=ECLIPTIC_B outEquinox=1950 lat=-90.0',  function () {

        const expectedLons = [
            359.68340855555186,
            359.68340855555203,
            359.68340855555215,
            359.6834085555523,
            359.6834085555524,
            359.6834085555525,
            359.68340855555255,
            359.6834085555526,
            359.6834085555526,
            359.68340855555255,
             359.6834085555525,
             359.6834085555524,
             359.6834085555523,
             359.68340855555215,
             359.68340855555203,
             359.68340855555186,
             359.68340855555175,
             359.6834085555516,
             359.68340855555147,
             359.68340855555135,
             359.68340855555124,
             359.6834085555512,
             359.6834085555511,
             359.6834085555511,
             359.6834085555512,
             359.68340855555124,
             359.68340855555135,
             359.6834085555514,
             359.6834085555516,
             359.6834085555517,
             359.68340855555186
        ];
        const expectedLats =[
           89.72175645220416,
           89.72175645220416,
           89.72175645220416,
           89.72175645220416,
           89.72175645220416,
           89.72175645220416,
           89.72175645220416,
           89.72175645220416,
           89.72175645220416,
           89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416,
            89.72175645220416
        ];

        const deltaLon=12.0;

        var lons =[];


        for (let i=0; i<31; i++){
            lons[i] = deltaLon*i;
        }

        const inLat =90.0;

        var lons = [];
        var lats = [];
        var ret;
        const tobs=1983.5;
        const inCoord = EQUATORIAL_J;
        const outCoord = EQUATORIAL_B;
        for (let i = 0; i < lons.length; i++){
            ret  =   doConv(inCoord, EQINOX2000,
                lons[i], inLat,
                outCoord, EQINOX1950, tobs);
            lons[i]=ret.lon;
            lats[i]=ret.lat;
        }

        for (let i=0; i<lons.length; i++){
            expect(lons[i].toFixed(precision), `${i} th lon  not equal` ).to.equal(expectedLons[i].toFixed(precision));
            expect(lats[i].toFixed(precision), `${i} th lat not equal`).to.equal(expectedLats[i].toFixed(precision));
        }


    });


    it('should be equal when convert from inCoord = EQUATORIAL_J,inEquirox=20000 , to outCoord=EQUATORIAL_B outEquinox=1950 lat=90.0',  function () {

        const expectedLons = [
           69.3002173418388,
           69.3002173418388,
           69.3002173418388,
           69.3002173418388,
           69.3002173418388,
           69.3002173418388,
           69.3002173418388,
           69.3002173418388,
           69.3002173418388,
           69.3002173418387,
           269.3002173418387,
           269.3002173418387,
           269.3002173418387,
           269.3002173418387,
           269.3002173418387,
           269.3002173418387,
           269.3002173418387,
           269.3002173418387,
           269.3002173418387,
           269.3002173418387,
           269.3002173418387,
           269.3002173418387,
           269.3002173418388,
           269.3002173418388,
           269.3002173418388,
           269.3002173418388,
           269.3002173418388,
           269.3002173418388,
           269.3002173418388,
           269.3002173418388,
           269.3002173418388
        ];
        const expectedLats =[
           -66.55413363542428,
           -66.55413363542428,
           -66.55413363542428,
           -66.55413363542428,
           -66.55413363542428,
           -66.55413363542428,
           -66.55413363542428,
           -66.55413363542428,
           -66.55413363542428,
           -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428,
            -66.55413363542428
        ];

        const deltaLon=12.0;

        var lons =[];


        for (let i=0; i<31; i++){
            lons[i] = deltaLon*i;
        }

        const inLat =90.0;

        var lons = [];
        var lats = [];
        var ret;
        const tobs=1983.5;
        const inCoord = EQUATORIAL_J;
        const outCoord =  ECLIPTIC_B;
        for (let i = 0; i < lons.length; i++){
            ret  =   doConv(inCoord, EQINOX2000,
                lons[i], inLat,
                outCoord, EQINOX1950, tobs);
            lons[i]=ret.lon;
            lats[i]=ret.lat;
        }

        for (let i=0; i<lons.length; i++){
            expect(lons[i].toFixed(precision), `${i} th lon  not equal` ).to.equal(expectedLons[i].toFixed(precision));
            expect(lats[i].toFixed(precision), `${i} th lat not equal`).to.equal(expectedLats[i].toFixed(precision));
        }


    });


});