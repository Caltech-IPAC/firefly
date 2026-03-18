/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Logger} from 'firefly/util/Logger.js';



const defEntry= (serviceId, label,url) => ({ serviceId, label, value: url});

/**
 * @typedef {Object} TapService
 * @prop {String} serviceId
 * @prop {String} label
 * @prop {String} value the url
 * @prop {Array.<TapExample>} examples
 * @prop {Number} fovDeg
 * @prop {String} hipsUrl
 * @prop {String} centerWP - world point serialized string
 * @prop {String} schemaLabel - label to show instead of "Schema'
 * @prop {boolean} schemaLoadManual - label to show instead of "Schema'
 */

/**
 * @typedef {Object} TapExample
 * @prop {String} description -
 * @prop {String} statement
 */


export function makeTAPDefaultServicesByName(nameList) {
    const services= makeServices();
    if (!nameList) return services;

    return nameList.map( (name) => {
            const foundService= services.find( (s) => s.label===name || s.serviceId===name);
            if (!foundService) Logger('TapKnownServices').warn(`TAP Service: '${name}' was not found`);
            return foundService;
        })
        .filter( (v) => v);
}

/**
 *
 * @return {Array.<TapService>}
 */
function makeServices() {
    return [
        {...defEntry('IRSA', 'IRSA', 'https://irsa.ipac.caltech.edu/TAP'), schemaLabel: 'Project', examples:irsaExamples()},
        {...defEntry('NED', 'NED', 'https://ned.ipac.caltech.edu/tap/'), examples:nedExamples()},
        defEntry('ExoplanetArchive', 'NASA Exoplanet Archive', 'https://exoplanetarchive.ipac.caltech.edu/TAP/'),
        defEntry('KOA', 'KOA', 'https://koa.ipac.caltech.edu/TAP/'),
        {...defEntry('HEASARC', 'HEASARC', 'https://heasarc.gsfc.nasa.gov/xamin/vo/tap'), schemaLoadManual:true},
        defEntry('MASTImages', 'MAST Images', 'https://mast.stsci.edu/vo-tap/api/v0.1/caom'),
        defEntry('CADC', 'CADC', 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/argus/'),
        defEntry('CANFARyoucat', 'CANFAR youcat', 'https://ws-uv.canfar.net/youcat'),
        // CDS???
        {...defEntry('VizieR', 'VizieR (CDS)', 'https://tapvizier.u-strasbg.fr/TAPVizieR/tap/'), schemaLoadManual:true},
        defEntry('Simbad', 'Simbad (CDS)', 'https://simbad.u-strasbg.fr/simbad/sim-tap'),
        // more ESA??
        {...defEntry('Gaia', 'Gaia', 'https://gea.esac.esa.int/tap-server/tap'), examples:gaiaExamples()},
        defEntry('GAVO', 'GAVO', 'https://dc.g-vo.org/tap'),
        defEntry('HSA', 'HSA',  'https://archives.esac.esa.int/hsa/whsa-tap-server/tap'),
        defEntry('NOIRLab', 'NOIR Lab',  'https://datalab.noirlab.edu/tap'),
    ];
}


/**
 * @return {Array.<TapExample>}
 */
const irsaExamples= () => [
    {
        description: 'From the IRSA TAP service, a 1 degree cone search of the 2MASS point source catalog around M101 would be:',
        statement:
            `SELECT * FROM fp_psc 
WHERE CONTAINS(POINT('J2000', ra, dec), CIRCLE('J2000', 210.80225, 54.34894, 1.0)) = 1`
    },
    {
        description: 'From the IRSA TAP service, a .25 degree cone search of the 2MASS point source catalog around M31 would be:',
        statement:
            `SELECT * FROM fp_psc 
WHERE CONTAINS(POINT('ICRS', ra, dec), CIRCLE('ICRS', 10.684, 41.269, .25))=1`
    },
    {
        description: 'From the IRSA TAP service, a triangle search of the AllWISE point source catalog around M101 would be:',
        statement:
            `SELECT designation, ra, dec, w2mpro 
FROM allwise_p3as_psd 
WHERE CONTAINS (POINT('J2000' , ra , dec) , POLYGON('J2000' , 209.80225 , 54.34894 , 209.80225 , 55.34894 , 210.80225 , 54.34894))=1`,
    }
];

/**
 * @return {Array.<TapExample>}
 */
const nedExamples= () => [
    {
        description: 'From the NED TAP service, a 100 arcsec radius around m16:',
        statement:
            `SELECT *
FROM NEDTAP.objdir
WHERE CONTAINS(POINT('ICRS', ra, dec), CIRCLE('ICRS', 274.7007274, -13.80722822, 0.02777)) = 1
`
    },
    {
        description: 'From the NED TAP service, a 10 arcsec radius around m31, with only ra, dec and redshift in z',
        statement:
            `SELECT ra,dec,z 
FROM NEDTAP.objdir
WHERE CONTAINS(POINT('ICRS', ra, dec),CIRCLE('ICRS', 10.68479292, 41.269065, 0.00277))=1
`
    },
    {
        description: 'From the NED TAP service, a 100 arcsec radius around m31, and has a redshift in z',
        statement:
            `SELECT * 
FROM NEDTAP.objdir 
WHERE CONTAINS(POINT('ICRS', ra, dec),CIRCLE('ICRS', 10.68479292, 41.269065, 0.027777777777777776))=1
AND z < 0
`
    },
];

/**
 * @return {Array.<TapExample>}
 */
const gaiaExamples= () => [
    {
        description: 'From the Gaia TAP service, a .25 degree cone search Gaia data release 3 point source catalog around M31 would be:',
        statement:
            `SELECT * FROM gaiaedr3.gaia_source 
WHERE CONTAINS(POINT('ICRS', ra, dec), CIRCLE('ICRS', 10.684, 41.269, .25))=1`
    },
    {
        description: 'From the Gaia TAP service, a 1 degree by 1 degree box of the Gaia data release 3 point source catalog around M101 would be:',
        statement:
            `SELECT * FROM gaiaedr3.gaia_source 
WHERE CONTAINS(POINT('ICRS', ra, dec), BOX('ICRS', 210.80225, 54.34894, 1.0, 1.0))=1`
    },
    {
        description: 'From the Gaia TAP service, a triangle search of of the Gaia data release 3 point source catalog around M101 would be:',
        statement:
            `SELECT source_id, designation, ra, dec, phot_g_mean_mag FROM gaiaedr3.gaia_source 
WHERE CONTAINS (POINT('ICRS' , ra , dec) , POLYGON('ICRS' , 209.80225 , 54.34894 , 209.80225 , 55.34894 , 210.80225 , 54.34894))=1`,
    }
];

export const defaultADQLExamples= ()=> [
    {
        description: 'From the IRSA TAP service, a 1 degree cone search of the 2MASS point source catalog around M101 would be:',
        statement:
            `SELECT * FROM fp_psc 
WHERE CONTAINS(POINT('J2000', ra, dec), CIRCLE('J2000', 210.80225, 54.34894, 1.0)) = 1`
    },
    {
        description: 'From the Gaia TAP service, a 1 degree by 1 degree box of the Gaia data release 3 point source catalog around M101 would be:',
        statement:
            `SELECT * FROM gaiaedr3.gaia_source 
WHERE CONTAINS(POINT('ICRS', ra, dec), BOX('ICRS', 210.80225, 54.34894, 1.0, 1.0))=1`
    },
    {
        description: 'From the IRSA TAP service, a triangle search of the AllWISE point source catalog around M101 would be:',
        statement:
            `SELECT designation, ra, dec, w2mpro 
FROM allwise_p3as_psd 
WHERE CONTAINS (POINT('J2000' , ra , dec), POLYGON('J2000' , 209.80225 , 54.34894 , 209.80225 , 55.34894 , 210.80225 , 54.34894))=1`,
    }
];
