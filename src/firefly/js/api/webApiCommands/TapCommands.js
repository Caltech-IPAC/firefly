import {makeExamples, ReservedParams} from '../WebApi';
import {dispatchActiveTarget} from '../../core/AppDataCntlr';
import {dispatchShowDropDown} from '../../core/LayoutCntlr';


const tapPanelOverview= {
    overview: [
        'open tap panel'
    ],
    parameters: {
        service: {desc:'tap service url', isRequired:true},
        schema : 'tap schema',
        table : 'tap table to search',
        WorldPt : 'coordinates of the image (semi-colon separated) - example-   10.68479;41.26906;EQ_J2000',
        MAXREC : 'max number of rows to return',
        adql: 'the ADQL select statement',
        [ReservedParams.POSITION.name]: ['coordinates of the search',...ReservedParams.POSITION.desc],
        [ReservedParams.SR.name]: ['radius of search  (optional)',...ReservedParams.SR.desc],
        execute: 'true or false - if true execute the tap search'
    },
};

const tapPanelExamples= [
    {
        desc:'Open tap panel - with 2MASS PSC search of M5, 2 arcmin',
        params:{
            service: 'https://irsa.ipac.caltech.edu/TAP',
            schema:'fp_2mass',
            table:'fp_psc',
            ra: '229.64',
            dec: '2.08',
            sr: '2m',
        }
    },
    {
        desc:'Execute search above but return only first 10 rows',
        params:{
            service: 'https://irsa.ipac.caltech.edu/TAP',
            schema:'fp_2mass',
            table:'fp_psc',
            ra: '229.64',
            dec: '2.08',
            sr: '2m',
            MAXREC: '10',
            execute: true,
        }
    },
    {
        desc:'Open tap panel- setup ivoa obscore search',
        params:{
            service: 'https://vao.stsci.edu/CAOMTAP/TapService.aspx',
            schema:'ivoa',
            table:'ivoa.obscore',
            WorldPt: '202.48417;47.23056;EQ_J2000',
            sr: '20s'
        }
    },
    {
        desc:'Execute search above',
        params:{
            service: 'https://vao.stsci.edu/CAOMTAP/TapService.aspx',
            schema:'ivoa',
            table:'ivoa.obscore',
            WorldPt: '202.48417;47.23056;EQ_J2000',
            sr: '20s',
            execute: 'true'
        }
    },
    {
        desc:'Show tap tables for CADC',
        params:{
            service: 'https://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/tap',
            schema:'tap_schema',
            table:'tap_schema.tables',
            execute: 'true'
        }
    },
    {
        desc:'ADQL: search from IRSA',
        params:{
            service: 'https://irsa.ipac.caltech.edu/TAP',
            adql:
`SELECT ra,dec,sigra,sigdec,sigradec,w1mpro,w1sigmpro,w1snr,w1rchi2,w1mpro_allwise,w1sigmpro_allwise,,w4mpro_allwise,w4sigmpro_allwise \
FROM neowiser_p1bs_psd WHERE CONTAINS(POINT('ICRS', ra, dec), CIRCLE(\'ICRS\', 10.68479, 41.26906, 0.013))=1`,
        }
    },
    {
        desc:'ADQL: Execute similar search as above',
        params:{
            service: 'https://irsa.ipac.caltech.edu/TAP',
            adql:
`SELECT ra,dec,sigra,sigdec,sigradec,w2mpro,w2sigmpro,w2snr,w2rchi2,w2mpro_allwise,w2sigmpro_allwise,w3mpro_allwise,w3sigmpro_allwise \
FROM neowiser_p1bs_psd WHERE CONTAINS(POINT('ICRS', ra, dec), CIRCLE(\'ICRS\', 10.68479, 41.26906, 0.013))=1`,
            execute: 'true',
        }
    }
];


function validateTap(params) {
    return {valid:true};
}

function showTapPanel(cmd,inParams) {
    const params= {...inParams};
    if (params[ReservedParams.POSITION.name]) dispatchActiveTarget(params[ReservedParams.POSITION.name]);
    if (params[ReservedParams.SR.name]) {
        params.radiusInArcSec= params[ReservedParams.SR.name] * 3600;
        Reflect.deleteProperty(params, ReservedParams.SR.name);
    }
    dispatchShowDropDown({view:'TAPSearch', initArgs:params});
}

export function getTapCommands() {
    return [
        {
            cmd : 'tap',
            validate : validateTap,
            execute:  showTapPanel,
            ...tapPanelOverview,
            examples: makeExamples('tap', tapPanelExamples),
        },
    ];
}


