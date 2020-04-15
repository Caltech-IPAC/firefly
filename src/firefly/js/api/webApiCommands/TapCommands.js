import {findExtraParams, makeExamples} from '../WebApi';
import {isEmpty} from 'lodash';
import {parseWorldPt} from '../../visualize/Point';
import {dispatchActiveTarget} from '../../core/AppDataCntlr';
import {dispatchShowDropDown} from '../../core/LayoutCntlr';


const tapPanelOverview= {
    overview: [
        'open tap panel'
    ],
    parameters: {
        service : 'tap service',
        schema : 'tap scheme',
        table : 'tap table to search',
        WorldPt : 'coordinates of the image (semi-colon separated) - example-   10.68479;41.26906;EQ_J2000',
        radiusInArcSec: 'radius of search in arcsec',
        execute: 'true or false - if true execute the tap search'
    },
};

const tapPanelExamples= [
    {
        desc:'Open tap panel',
        params:{
            service: 'https://vao.stsci.edu/CAOMTAP/TapService.aspx',
            schema:'ivoa',
            table:'ivoa.obscore',
            WorldPt: '202.48417;47.23056;EQ_J2000',
            radiusInArcSec: '20'
        }
    },
    {
        desc:'Same query as above but executes it',
        params:{
            service: 'https://vao.stsci.edu/CAOMTAP/TapService.aspx',
            schema:'ivoa',
            table:'ivoa.obscore',
            WorldPt: '202.48417;47.23056;EQ_J2000',
            radiusInArcSec: '20',
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
    }
];


function validateTap(params) {
    const badParams= findExtraParams(Object.keys(tapPanelOverview.parameters), params);
    if (!isEmpty(badParams) || isEmpty(params)) {
        return {valid:false,msg:`url contains unsupported params: ${badParams?badParams.join():''}`, badParams};
    }
    return {valid:true};
}

function showTapPanel(cmd,params) {
    if (params.WorldPt) {
        const wp= parseWorldPt(params.WorldPt);
        if (wp) dispatchActiveTarget(wp);
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


