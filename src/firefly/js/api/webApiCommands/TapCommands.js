import {makeExamples, processSpatialReservedParams, ReservedParams} from '../WebApi';
import {dispatchShowDropDown} from '../../core/LayoutCntlr';
import {SpatialRegOp, SpatialRegOpType} from 'firefly/ui/tap/SpatialSearch';
import {BASE_UNIT} from 'firefly/ui/WavelengthInputField';
import {WAVELENGTH_UNITS} from 'firefly/visualize/VisUtil';

const wvlUnit = WAVELENGTH_UNITS[BASE_UNIT].symbol;
export const tapPanelOverview= {
    overview: [
        'open tap panel'
    ],
    parameters: {
        service: {desc:'tap service url'},
        schema : 'tap schema',
        table : 'tap table to search',
        WorldPt : 'coordinates of the image (semi-colon separated) - example-   10.68479;41.26906;EQ_J2000',
        MAXREC : 'max number of rows to return',
        adql: 'the ADQL select statement',
        selectBy: 'The way to select by',
        exposureLengthMin : 'Minimum duration of exposure (in seconds)',
        exposureLengthMax : 'Maximum duration of exposure (in seconds)',
        exposureMin : 'Time when exposures must end by (requires `exposureRangeType=range`, optionally specify `exposureTimeMode=mjd` if in mjd)',
        exposureMax : 'Time when exposures must start by (requires `exposureRangeType=range`, optionally specify `exposureTimeMode=mjd` if in mjd)',
        exposureTimeMode : 'Time mode (iso, mjd) that `exposureMin` and `exposureMax` are specified in',
        exposureRangeType : '`range` or `since` (`since` if not specified)',
        exposureSinceValue : 'The quantity of time (n) when looking for completed observations. Requires `exposureRangeType=since`',
        exposureSinceOptions : 'The unit of time for when looking for completed observations: `minutes`, `hours`, `days`, or `years`. Requires `exposureRangeType=since`',
        obsCoreCalibrationLevel : 'List of ObsCore calibration levels, from 0-4 (e.g. `obsCoreCalibrationLevel=3,4`)',
        obsCoreTypeSelection : 'List of options of ObsCore Data Product types (`image`, `cube`,  `spectrum`, `sed`, `timeseries`, `visibility`, `event`, `measurements`)',
        obsCoreInstrumentName : 'Name of ObsCore instrument',
        obsCoreCollection : 'ObsCore collection',
        obsCoreSubType : 'ObsCore subtype. Only considered if the table contains the appropriate column.',
        obsCoreWavelengthRangeType : 'Type of Wavelength search for Observations. `contains` or `overlaps`',
        obsCoreWavelengthContains : `Wavelength value (in ${wvlUnit}) when selecting \`obsCoreWavelengthRangeType=contains\``,
        obsCoreWavelengthMinRange : `Upper limit of an observation's wavelength coverage (in ${wvlUnit}). Requires \`obsCoreWavelengthRangeType=overlaps\``,
        obsCoreWavelengthMaxRange : `Lower limit of an observation\'s wavelength coverage (in ${wvlUnit}). Requires \`obsCoreWavelengthRangeType=overlaps\``,
        [ReservedParams.POSITION.name]: ['coordinates of the search',...ReservedParams.POSITION.desc],
        [ReservedParams.SR.name]: ['radius of search  (optional)',...ReservedParams.SR.desc],
        polygon: 'polygon as a list of ra dec strings, example: 269.3 68.2, 272.85 68.2, 272.7 66.7, 269.3 66.7',
        [SpatialRegOp]: `Query Type for ObsCore searches, one of [${Object.values(SpatialRegOpType).join(', ')}]`,
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
        desc:'Open tap panel - with 2MASS PSC search of a polygon enclosing M5',
        params:{
            service: 'https://irsa.ipac.caltech.edu/TAP',
            schema:'fp_2mass',
            table:'fp_psc',
            polygon: '202.584218 47.312657, 202.583636 47.087652, 202.365233 47.087705, 202.364887 47.312710',
        }
    },
    {
        desc:'Open tap panel- setup an IVOA ObsCore search',
        params:{
            service: 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/argus',
            schema:'ivoa',
            table:'ivoa.ObsCore',
            WorldPt: '202.48417;47.23056;EQ_J2000',
            sr: '20s',
            [SpatialRegOp]: SpatialRegOpType.INTERSECTS,
        }
    },
    {
        desc:'Execute search above',
        params:{
            service: 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/argus',
            schema:'ivoa',
            table:'ivoa.ObsCore',
            WorldPt: '202.48417;47.23056;EQ_J2000',
            sr: '20s',
            [SpatialRegOp]: SpatialRegOpType.INTERSECTS,
            execute: 'true'
        }
    },
    {
        desc:'Open tap panel - setup GAIA search for data release 2',
        params:{
            service: 'https://gea.esac.esa.int/tap-server/tap',
            schema:'gaiadr2',
            table:'gaiadr2.gaia_source',
            WorldPt: '83.63321237;22.01446012;EQ_J2000',
            sr: '20s',
        }
    },
    {
        desc:'Execute adql GAIA search on sources for data release 2',
        params:{
            service: 'https://gea.esac.esa.int/tap-server/tap',
            adql: ` SELECT * 
FROM gaiadr2.gaia_source 
WHERE CONTAINS(POINT('ICRS', ra, dec),CIRCLE('ICRS', 83.63321237, 22.01446012, 0.027777777777777776))=1 `,
            execute: 'true'
        }
    },
    {
        desc:'Show tap tables for CADC',
        params:{
            service: 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/argus/',
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
`SELECT ra,dec,sigra,sigdec,sigradec,w1mpro,w1sigmpro,w1snr,w1rchi2,w1mpro_allwise,w1sigmpro_allwise,w4mpro_allwise,w4sigmpro_allwise \
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

function showTapPanel(cmd, inParams) {
    const params = processSpatialReservedParams(inParams);
    const view= inParams?.view ?? 'TAPSearch';

    dispatchShowDropDown({view, initArgs:{defaultSelectedId:'tap', urlApi:{...params}}});
}

export function getTapCommands(tapPanelList) {
    if (tapPanelList?.length) {
        tapPanelOverview.parameters.view=
            'a constrained tap panel, may be: ' + tapPanelList.map( ({name}) => name).join(', ');
    }

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


