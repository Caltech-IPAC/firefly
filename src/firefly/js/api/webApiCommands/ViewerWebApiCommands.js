import {isEmpty} from 'lodash';
import {getImageCommands} from './ImageCommands';
import {getLsdbCommands} from './LsdbCommands';
import {getTabCommands} from './TabCommands';
import {getTableCommands} from './TableCommands';
import {getTapCommands} from './TapCommands';


/**
 * @param {Array.<string>|undefined} [cmdNameList] - a array of command names that will filter the returned command array
 * @return Array.<WebApiCommand>
 */
export function getFireflyViewerWebApiCommands(cmdNameList, tapPanelList=[]) {
    const allCommands= [
        ...getTabCommands(),
        ...getImageCommands(),
        ...getTableCommands(),
        ...getTapCommands(tapPanelList),
        ...getLsdbCommands()
    ];

    if (isEmpty(cmdNameList)) return allCommands;

    return allCommands.filter( (c) => cmdNameList.includes(c.cmd));
}
