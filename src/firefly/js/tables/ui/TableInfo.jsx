import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';

import {get} from 'lodash';
import {getTblById} from '../TableUtil.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {copyToClipboard} from '../../util/WebUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';

import CLIP from 'html/images/icons-2014/16x16_Clipboard.png';

const TT_CLIP = 'Copy link to Clipboard';

export class TableInfo extends PureComponent {
    constructor(props) {
        super(props);
    }

    render() {
        const {tbl_id} = this.props;
        if (!tbl_id) {
            return <div>No additional Information</div>;
        }

        const tableModel = getTblById(tbl_id);
        const title = get(tableModel, 'title', '');
        const links = get(tableModel, 'links');

        return (
            <div style={{padding: 10}}>
                <h3>{title}</h3>
                {links && links.map((link, idx) => renderLink(link, idx))}
            </div>
        );
    }
}

TableInfo.propTypes = {
    tbl_id: PropTypes.string
};

function renderLink(link, idx) {
    const {ID, href, role, type, title} = link;
    if (href) {
        const linkTitle = title || role || type || ID || 'Table link';
        const doCopy = () => {
            copyToClipboard(href);
            showInfoPopup('Copy to Clipboard completed.');
        };
        return (
            <div key={`link${idx}`}>
                <div style={{display: 'inline-block', cursor:'pointer', marginRight: 10, verticalAlign: 'middle'}}
                     title={TT_CLIP}
                     onClick={doCopy}>
                    <ToolbarButton icon={CLIP}/>
                </div>
                <a className='ff-href' style={{verticalAlign: 'middle'}} href={href} target='Links'>{linkTitle}</a>
            </div>
        );
    }
}