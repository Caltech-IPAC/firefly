import {makeExamples} from 'firefly/api/WebApi';
import {dispatchShowDropDown, dispatchUpdateMenu} from 'firefly/core/LayoutCntlr';
import {isUndefined} from 'lodash';
import {getMenu} from '../../core/AppDataCntlr';


const lsdbOverview= {
    overview: [
        'Enable Experimental LSDB Search Panel'
    ],
    allowAdditionalParameters: false,
    parameters: {
        enable: 'if defined panel is enabled'
    }
};

const lsdbExample= [
    {
        desc:'Enable the LSDB panel',
        params:{ enable: undefined, }
    },
];

function enablePanel(cmd,inParams) {

    const params= {...inParams};
    if (isUndefined(params.enable)) return;
    setTimeout(() => {

        const lsdbMenuItem= {
            label: 'LSDB Searches',
            action: 'GatorProtocolRootPanel',
            primary: true,
            category:'Archive Searches'
        };

        const {menuItems,selected,showBgMonitor}= getMenu();
        if (!menuItems?.find(({action}) => action===lsdbMenuItem.action)) { // add the toolbar option
            const newMenuItems= [...menuItems];
            const dlDrop= lsdbMenuItem;
            newMenuItems.splice(1,0,dlDrop);
            dispatchUpdateMenu({selected,showBgMonitor,menuItems:newMenuItems});
        }
        dispatchShowDropDown({view:'GatorProtocolRootPanel'});
    },10);
}

export function getLsdbCommands() {
    return [
        {
            cmd: 'lsdb',
            validate: () => ({valid:true}),
            execute: enablePanel,
            ...lsdbOverview,
            examples: makeExamples('lsdb', lsdbExample),
        },
    ];
}