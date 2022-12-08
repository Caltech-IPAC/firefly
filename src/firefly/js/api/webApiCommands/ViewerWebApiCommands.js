import {isEmpty} from 'lodash';
import {getDatalinkUICommands} from './DatalinkUICommands.js';
import {getImageCommands} from './ImageCommands';
import {getTableCommands} from './TableCommands';
import {getTapCommands} from './TapCommands';


/**
 * @param {Array.<string>|undefined} [cmdNameList] - a array of command names that will filter the returned command array
 * @return Array.<WebApiCommand>
 */
export function getFireflyViewerWebApiCommands(cmdNameList) {
    const allCommands= [
        ...getImageCommands(),
        ...getTableCommands(),
        ...getTapCommands(),
    ];

    if (isEmpty(cmdNameList)) return allCommands;

    return allCommands.filter( (c) => cmdNameList.includes(c.cmd));
}
