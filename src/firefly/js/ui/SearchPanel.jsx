/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {flux} from '../Firefly.js';
import './SearchPanel.css';


function showFooters() {
    return (
        <div id='region-footer' className='footer'>
            <div className='footer_wrapper'>
                <div className='footer-container'>
                    <div className='footer-panel'>
                        <div className='footer-row'>
                            <div className='footer-contact'>
                                <ul>
                                    <li>
                                        <a href='https://irsasupport.ipac.caltech.edu/' target='helpdesk'>Contact</a>
                                    </li>
                                    <li>
                                        <a href='http://irsa.ipac.caltech.edu/privacy.html' target='privacy'>Privacy
                                            Policy</a>
                                    </li>
                                    <li>
                                        <a href='http://irsa.ipac.caltech.edu/ack.html' target='ack'>Acknowledge
                                            IRSA</a>
                                    </li>
                                </ul>
                            </div>
                            <div className='footer-span5'>
                                <div className='affiliates right'>
                                    <a href='http://www.ipac.caltech.edu/'
                                       alt='Infrared Processing and Analysis Center' target='ipac'
                                       title='Infrared Processing and Analysis Center'><img alt='Icon_ipac'
                                                                                            src='footer/icon_ipac-white-78x60.png'/></a>
                                    <a href='http://www.caltech.edu/'
                                       alt='California Institute of Technology'
                                       target='caltech' title='California Institute of Technology'><img
                                        alt='Icon_caltech' src='footer/icon_caltech-new.png'/></a>
                                    <a href='http://www.jpl.nasa.gov/' alt='Jet Propulsion Laboratory'
                                       target='jpl' title='Jet Propulsion Laboratory'><img alt='Icon_jpl'
                                                                                           src='footer/icon_jpl-white-91x60.png'/></a>
                                    <a href='http://www.nasa.gov/'
                                       alt='National Aeronautics and Space Administration' target='nasa'
                                       title='National Aeronautics and Space Administration'><img
                                        alt='Icon_nasa' src='footer/icon_nasa-white-59x60.png'/></a>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

function showAlerts() {
    return (
        <div id="region-alerts" aria-hidden="true" style="width: 100%; height: 100%; display: none;">
            <div align="left" style="width: 100%; height: 100%;"></div>
        </div>
    );
}


const SearchPanel = function (props) {
    var {show, children} = props;
    const visi = show ? 'block' : 'none';

    return (
        <div className='DropDownToolBar'
             style={{display: visi, zIndex: 10, position: 'absolute', width: '100%', height: 'calc(100% - 90px)'}}>
            <div className='content'>
                <div style={{display: 'table', margin: '0 auto'}}>
                    <div className='shadow'>
                        {children}
                    </div>
                </div>

                {showFooters()}
            </div>

        </div>
    );
};

SearchPanel.propTypes = {
    show: React.PropTypes.bool
};


export default SearchPanel;


