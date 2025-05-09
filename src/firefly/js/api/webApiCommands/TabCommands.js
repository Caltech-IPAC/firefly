import {makeExamples} from 'firefly/api/WebApi';
import {dispatchShowDropDown} from 'firefly/core/LayoutCntlr';
import {getAppOptions, getMenu} from '../../core/AppDataCntlr';


const tabOverview= {
    overview: [
        `Go to a tab. You may also use partial names (e.g. 'mon' instead of 'Job Monitor')`,
        'An unrecognized tab name will just go to the results view.'
    ],
    allowAdditionalParameters: false,
    parameters: {
        tab: {desc:'the landing tab', isRequired:true}
    }
};


function makeExampleInput(menu, backgroundMonitor) {
    const examples= menu.map( (mi) => {
        return {
            desc:`${mi.label}`,
            params:{ tab: mi.label}
        };
    });
    if (backgroundMonitor) {
        const bgMonTxt= 'Job Monitor';
        examples.push( { desc: bgMonTxt, params:{ tab: bgMonTxt} } );
    }

    return (
        [{
           sectionDesc: 'Go to one of the following tab names',
           examples,
        }]
    );
}

function enablePanel(cmd,params) {
    const tabRequest= params.tab?.toLowerCase();
    setTimeout(() => {
        const {menuItems=[]} = getMenu();
        const mi= menuItems.find( (mi) => {
            if (mi.action?.toLowerCase()===tabRequest) return true;
            const label= mi.label?.toLowerCase() ?? '';
            return label===tabRequest || label.includes(tabRequest);
        });
        if (mi) dispatchShowDropDown({view:mi.action});
    });
}

export function getTabCommands() {
    return [
        {
            cmd: 'menu',
            validate: (params) => {
                if (params.tab==='xxx') return {valid:false,msg: 'xxx not supported'};
                return {valid:true};
            },
            execute: enablePanel,
            ...tabOverview,
            examples: (appProps) => {
                const {backgroundMonitor= true}= getAppOptions()??{};
                return makeExamples('menu', makeExampleInput(appProps.menu, backgroundMonitor));
            },
        },
    ];
}