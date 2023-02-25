/**
 * Created by loi on 6/8/16.
 */

import React from 'react';
import './DropDownContainer.css';

import IPAC_ICO from 'html/images/footer/icon_ipac-white-78x60.png';
import JPL_ICO from 'html/images/footer/icon_jpl-white-91x60.png';
import NASA_ICO from 'html/images/footer/icon_nasa-white-59x60.png';
import CALTECH_ICO from 'html/images/footer/icon_caltech-new.png';
const iconHeight=30;

export function IrsaFooterSmall() {
    return (
        <div style={{display:'flex', justifyContent:'space-around', alignItems:'center'}}>
            <div className='DD-ToolBar__footer--links' style={{display:'flex', justifyContent:'space-around',
                alignItems:'center', width:450, marginRight:90}}>
                    <a href='https://irsasupport.ipac.caltech.edu/' target='helpdesk'>Contact</a>
                    <a href='https://irsa.ipac.caltech.edu/privacy.html' target='privacy'>Privacy Policy</a>
                    <a href='https://irsa.ipac.caltech.edu/ack.html' target='ack'>Acknowledge IRSA</a>
            </div>
            <div className='DD-ToolBar__footer--icons'>
                <a href='http://www.ipac.caltech.edu/'
                   alt='Infrared Processing and Analysis Center' target='ipac'
                   title='Infrared Processing and Analysis Center'>
                    <img alt='Icon_ipac' src={IPAC_ICO} height={iconHeight}/></a>
                <a href='http://www.caltech.edu/'
                   alt='California Institute of Technology'
                   target='caltech' title='California Institute of Technology'>
                    <img alt='Icon_caltech' src={CALTECH_ICO} height={iconHeight}/></a>
                <a href='http://www.jpl.nasa.gov/' alt='Jet Propulsion Laboratory'
                   target='jpl' title='Jet Propulsion Laboratory'>
                    <img alt='Icon_jpl' src={JPL_ICO} height={iconHeight}/></a>
                <a href='http://www.nasa.gov/'
                   alt='National Aeronautics and Space Administration' target='nasa'
                   title='National Aeronautics and Space Administration'>
                    <img alt='Icon_nasa' src={NASA_ICO} height={iconHeight}/></a>
            </div>
        </div>
    );
};

export function IrsaFooter() {
    return (
        <div>
            <div className='DD-ToolBar__footer--links'>
                <ul>
                    <li><a href='https://irsasupport.ipac.caltech.edu/' target='helpdesk'>Contact</a></li>
                    <li><a href='https://irsa.ipac.caltech.edu/privacy.html' target='privacy'>Privacy Policy</a></li>
                    <li><a href='https://irsa.ipac.caltech.edu/ack.html' target='ack'>Acknowledge IRSA</a></li>
                </ul>
            </div>
            <div className='DD-ToolBar__footer--icons'>
                <a href='http://www.ipac.caltech.edu/'
                   alt='Infrared Processing and Analysis Center' target='ipac'
                   title='Infrared Processing and Analysis Center'><img alt='Icon_ipac' src={IPAC_ICO}/></a>
                <a href='http://www.caltech.edu/'
                   alt='California Institute of Technology'
                   target='caltech' title='California Institute of Technology'><img alt='Icon_caltech' src={CALTECH_ICO}/></a>
                <a href='http://www.jpl.nasa.gov/' alt='Jet Propulsion Laboratory'
                   target='jpl' title='Jet Propulsion Laboratory'><img alt='Icon_jpl' src={JPL_ICO}/></a>
                <a href='http://www.nasa.gov/'
                   alt='National Aeronautics and Space Administration' target='nasa'
                   title='National Aeronautics and Space Administration'><img alt='Icon_nasa' src={NASA_ICO}/></a>
            </div>
        </div>
    );
};
