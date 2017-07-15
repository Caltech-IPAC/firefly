import {PureComponent} from 'react';
import { getColNames,getColumnTypes,getStringColNames} from '../../tables/TableUtil.js';
import {getLayouInfo} from '../../core/LayoutCntlr.js';
import {flux} from '../../Firefly.js';
import {get} from 'lodash';


export class SettingBox extends PureComponent {
    constructor(props) {
        super(props);

        this.getNextState = () => {
            return Object.assign({}, {tblColumns: get(getLayouInfo(), 'rawTableColumns', [])});
        };

        const {tblColumns} = this.getNextState();
        const numericColTypes = getColumnTypes(tblColumns, 'numeric');
        const numColumns = getColNames(tblColumns, numericColTypes);
        const charColumns = getStringColNames(tblColumns);//(tblColumns, ['char', 'c', 's', 'str']);
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
                const numericColTypes = getColumnTypes(tblColumns, 'numeric');
                const numColumns = getColNames(tblColumns, numericColTypes);
                const charColumns = getStringColNames(tblColumns);
                ;//getColNames(tblColumns, ['char', 'c', 's', 'str']);
                this.setState({tblColumns, charColumns, numColumns});
            }
        }
    }
}