import {PureComponent} from 'react';
import {get} from 'lodash';

import {flux} from '../../Firefly.js';
import {getBackgroundInfo, isActive} from './BackgroundUtil.js';
import {BG_MONITOR_SHOW} from './BackgroundCntlr.js';
import {makeMenuItem} from '../../ui/Menu.jsx';


export class BgMonitorButton extends PureComponent {
    constructor(props) {
        super(props);
        this.bgAction = { type:'COMMAND',
            action: BG_MONITOR_SHOW,
            label: 'Background Monitor',
            desc: 'Watch and retrieve background tasks for packaging and catalogs'
        };
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
        this.isUnmounted = true;
    }

    storeUpdate() {
        if (!this.isUnmounted) {
            const jobs = get(getBackgroundInfo(), 'jobs', {});
            const jobsCnt = Object.keys(jobs).length;
            const working = Object.entries(jobs).reduce( (rval, [k,v]) => {
                        return rval || isActive(get(v, 'STATE'));
                    }, false);
            this.setState({jobsCnt, working});
        }
    }

    render() {
        const {jobsCnt=0, working=false} = this.state || {};
        return makeMenuItem(this.bgAction, false, working, jobsCnt);
    }
}

