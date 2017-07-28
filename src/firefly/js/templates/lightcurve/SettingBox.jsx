import {PureComponent} from 'react';
import { getColsByType, COL_TYPE} from '../../tables/TableUtil.js';
import {getLayouInfo} from '../../core/LayoutCntlr.js';
import {flux} from '../../Firefly.js';
import {get} from 'lodash';

/**
 * @desc This is the base class that is used by DefaultMissionOption and BasicMissionOption.
 */

export class SettingBox extends PureComponent {
    constructor(props) {
        super(props);

        this.getNextState = () => {
            return Object.assign({}, {tblColumns: get(getLayouInfo(), 'rawTableColumns', [])});
        };

        const {tblColumns} = this.getNextState();
        const numColumns = getColsByType(tblColumns, COL_TYPE.NUMBER).map((c) => c.name);
        const charColumns = getColsByType(tblColumns, COL_TYPE.TEXT).map((c) => c.name);//(tblColumns, ['char', 'c', 's', 'str']);
        this.state = {tblColumns, charColumns, numColumns};
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        this.removeListener && this.removeListener();
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const {tblColumns} = this.getNextState();

            if (tblColumns !== this.state.tblColumns) {
                const numColumns = getColsByType(tblColumns, COL_TYPE.NUMBER).map((c) => c.name);
                const charColumns = getColsByType(tblColumns, COL_TYPE.TEXT).map((c) => c.name);
                ;//getColNames(tblColumns, ['char', 'c', 's', 'str']);
                this.setState({tblColumns, charColumns, numColumns});
            }
        }
    }
}