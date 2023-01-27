/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/* eslint-disable  quotes */
import {Logger} from 'firefly/util/Logger.js';

const tapEntry= (label,url,examples,hipsUrl, fovDeg,centerWP) =>
    ({ label, value: url, examples, fovDeg, hipsUrl, centerWP});

export function getTAPServices(nameList) {
    const services= makeServices();
    if (!nameList) return services;

    return nameList.map( (name) => {
            const foundService= services.find( (s) => s.label===name);
            if (!foundService) Logger('TapKnownServices').warn(`TAP Service: '${name}' was not found`);
            return foundService;
        })
        .filter( (v) => v);
}

function makeServices() {
    return [
        tapEntry('IRSA', 'https://irsa.ipac.caltech.edu/TAP',
            [
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
            ]
        ),
        tapEntry('NED', 'https://ned.ipac.caltech.edu/tap/'),
        tapEntry('NASA Exoplanet Archive', 'https://exoplanetarchive.ipac.caltech.edu/TAP/'),
        tapEntry('KOA', 'https://koa.ipac.caltech.edu/TAP/'),
        tapEntry('HEASARC', 'https://heasarc.gsfc.nasa.gov/xamin/vo/tap'),
        tapEntry('MAST Images', 'https://vao.stsci.edu/CAOMTAP/TapService.aspx'),
        tapEntry('CADC', 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/argus/'),
        // CDS???
        tapEntry('VizieR (CDS)', 'http://tapvizier.u-strasbg.fr/TAPVizieR/tap/'),
        tapEntry('Simbad (CDS)', 'https://simbad.u-strasbg.fr/simbad/sim-tap'),
        // more ESA??
        tapEntry('Gaia', 'https://gea.esac.esa.int/tap-server/tap',
            [
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
            ]
        ),
        tapEntry('GAVO', 'http://dc.g-vo.org/tap'),
        tapEntry('HSA',  'https://archives.esac.esa.int/hsa/whsa-tap-server/tap'),
    ];

}

