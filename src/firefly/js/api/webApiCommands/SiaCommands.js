


import {makeExamples, processSpatialReservedParams, ReservedParams} from '../WebApi';
import {dispatchShowDropDown} from '../../core/LayoutCntlr';
import {BASE_UNIT} from 'firefly/ui/WavelengthInputField';
import {WAVELENGTH_UNITS} from 'firefly/visualize/VisUtil';



const wvlUnit = WAVELENGTH_UNITS[BASE_UNIT].symbol;

export const siaPanelOverview= {
    overview: [
        'open SIAv2 panel'
    ],
    parameters: {
        service: {desc:'SIAv2 service url'},
        WorldPt : 'coordinates of the image (semi-colon separated) - example-   10.68479;41.26906;EQ_J2000',
        MAXREC : 'max number of rows to return',

        exposureLengthMin : 'Minimum duration of exposure (in seconds)',
        exposureLengthMax : 'Maximum duration of exposure (in seconds)',
        exposureMin : 'Time when exposures must end by (requires `exposureRangeType=range`, optionally specify `exposureTimeMode=mjd` if in mjd)',
        exposureMax : 'Time when exposures must start by (requires `exposureRangeType=range`, optionally specify `exposureTimeMode=mjd` if in mjd)',
        exposureTimeMode : 'Time mode (iso, mjd) that `exposureMin` and `exposureMax` are specified in',
        obsCoreCalibrationLevel : 'List of ObsCore calibration levels, from 0-4 (e.g. `obsCoreCalibrationLevel=3,4`)',
        obsCoreTypeSelection : 'List of options of ObsCore Data Product types (`image`, `cube`,  `spectrum`, `sed`, `timeseries`, `visibility`, `event`, `measurements`)',
        obsCoreInstrumentName : 'Name of ObsCore instrument',
        obsCoreCollection : 'ObsCore collection',
        facility : 'facility',
        obsCoreSubType : 'ObsCore subtype. Only considered if the table contains the appropriate column.',
        obsCoreWavelengthRangeType : 'Type of Wavelength search for Observations. `contains` or `overlaps`',
        obsCoreWavelengthContains : `Wavelength value (in ${wvlUnit}) when selecting \`obsCoreWavelengthRangeType=contains\``,
        obsCoreWavelengthMinRange : `Upper limit of an observation's wavelength coverage (in ${wvlUnit}). Requires \`obsCoreWavelengthRangeType=overlaps\``,
        obsCoreWavelengthMaxRange : `Lower limit of an observation\'s wavelength coverage (in ${wvlUnit}). Requires \`obsCoreWavelengthRangeType=overlaps\``,

        [ReservedParams.POSITION.name]: ['coordinates of the search',...ReservedParams.POSITION.desc],
        [ReservedParams.SR.name]: ['radius of search  (optional)',...ReservedParams.SR.desc],
        
        polygon: 'polygon as a list of ra dec strings, example: 269.3 68.2, 272.85 68.2, 272.7 66.7, 269.3 66.7',
        execute: 'true or false - if true execute the SIA search'
    },
};
const siaPanelExamples= [
    {
        desc: 'Open the SIAv2 panel - with 2MASS PSC search of M5, 2 arcmin',
        params: {
            service: 'https://irsa.ipac.caltech.edu/SIA',
            ra: '229.64',
            dec: '2.08',
            sr: '2m',
            exposureLengthMin: '5',
            exposureLengthMax: '200',
        }
    },
    {
        desc: 'do the above search immediately and only return the first 20 lines',
        params: {
            service: 'https://irsa.ipac.caltech.edu/SIA',
            ra: '229.64',
            dec: '2.08',
            sr: '2m',
            exposureLengthMin: '5',
            exposureLengthMax: '200',
            MAXREC: 20,
            execute: true,
        }
    },
    {
        desc: 'search Spitzer SEIP, IRAC',
        params: {
            service: 'https://irsa.ipac.caltech.edu/SIA',
            ra: '210.8',
            dec: '54.34',
            sr: '2m',
            obsCoreInstrumentName: 'IRAC',
            obsCoreCollection: 'spitzer_seip',
        }
    },
];
function validateSia(params) {
    return {valid:true};
}

function showSiaPanel(cmd, inParams) {
    const params = processSpatialReservedParams(inParams);
    const view= inParams?.view ?? 'SIAv2Search';

    dispatchShowDropDown({view, initArgs:{urlApi:{...params}}});
}

export function getSiaCommands() {
    return [
        {
            cmd : 'sia',
            validate : validateSia,
            execute:  showSiaPanel,
            ...siaPanelOverview,
            examples: makeExamples('sia', siaPanelExamples),
        },
    ];
}


