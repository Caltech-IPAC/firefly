/**
 * Created by loi on 6/8/16.
 */

import FOOTER_BG from 'images/ipac_bar.jpg';
import React from 'react';
import {Stack, Link, Sheet} from '@mui/joy';

import IPAC_ICO from 'html/images/footer/icon_ipac-white-78x60.png';
import JPL_ICO from 'html/images/footer/icon_jpl-white-91x60.png';
import NASA_ICO from 'html/images/footer/icon_nasa-white-59x60.png';
import CALTECH_ICO from 'html/images/footer/icon_caltech-new.png';

export function IrsaFooterSmall() {
    const iconHeight=30;
    return (
        <Stack direction='row' justifyContent='space-around' width={1} alignItems='center' spacing={2} py={1}>
            <Stack direction='row' spacing={4}>
                <Link sx={{color:'white'}} underline='hover' href='https://irsasupport.ipac.caltech.edu/' target='helpdesk'>Contact</Link>
                <Link sx={{color:'white'}} underline='hover' href='https://irsa.ipac.caltech.edu/privacy.html' target='privacy'>Privacy Policy</Link>
                <Link sx={{color:'white'}} underline='hover' href='https://irsa.ipac.caltech.edu/ack.html' target='ack'>Acknowledge IRSA</Link>
            </Stack>
            <Stack direction='row' spacing={4}>
                <Link href='http://www.ipac.caltech.edu/'
                      alt='Infrared Processing and Analysis Center' target='ipac'
                      title='Infrared Processing and Analysis Center'>
                    <img alt='Icon_ipac' src={IPAC_ICO} height={iconHeight}/>
                </Link>
                <Link href='http://www.caltech.edu/'
                      alt='California Institute of Technology'
                      target='caltech' title='California Institute of Technology'>
                    <img alt='Icon_caltech' src={CALTECH_ICO} height={iconHeight}/>
                </Link>
                <Link href='http://www.jpl.nasa.gov/' alt='Jet Propulsion Laboratory'
                      target='jpl' title='Jet Propulsion Laboratory'>
                    <img alt='Icon_jpl' src={JPL_ICO} height={iconHeight}/></Link>
                <Link href='http://www.nasa.gov/'
                      alt='National Aeronautics and Space Administration' target='nasa'
                      title='National Aeronautics and Space Administration'>
                    <img alt='Icon_nasa' src={NASA_ICO} height={iconHeight}/>
                </Link>
            </Stack>
        </Stack>
    );
};

export function IrsaFooterSmallLanding() {
    const iconHeight=30;
    return (
        <Sheet color='neutral' variant='solid'>
            <Stack alignItems='center'>
                <IrsaFooterSmall/>
            </Stack>
        </Sheet>
    );
}

export function IrsaFooterLanding() {
    return (
        <Stack alignItems='center' sx={{background:`url(${FOOTER_BG}) repeat center center transparent`}} >
            <IrsaFooter/>
        </Stack>
    );
}

export function IrsaFooter() {
    return (
        <Stack direction='row' justifyContent='space-around' width={1} alignItems='center' spacing={2} py={1}>
            <Stack>
                <Link sx={{color:'white'}} underline='hover' href='https://irsasupport.ipac.caltech.edu/' target='helpdesk'>Contact</Link>
                <Link sx={{color:'white'}} underline='hover' href='https://irsa.ipac.caltech.edu/privacy.html' target='privacy'>Privacy Policy</Link>
                <Link sx={{color:'white'}} underline='hover' href='https://irsa.ipac.caltech.edu/ack.html' target='ack'>Acknowledge IRSA</Link>
            </Stack>
            <Stack direction='row' spacing={1}>
                <Link href='http://www.ipac.caltech.edu/'
                   alt='Infrared Processing and Analysis Center' target='ipac'
                   title='Infrared Processing and Analysis Center'>
                    <img alt='Icon_ipac' src={IPAC_ICO}/>
                </Link>
                <Link href='http://www.caltech.edu/'
                   alt='California Institute of Technology'
                   target='caltech' title='California Institute of Technology'>
                    <img alt='Icon_caltech' src={CALTECH_ICO}/>
                </Link>
                <Link href='http://www.jpl.nasa.gov/' alt='Jet Propulsion Laboratory'
                   target='jpl' title='Jet Propulsion Laboratory'>
                    <img alt='Icon_jpl' src={JPL_ICO}/>
                </Link>
                <Link href='http://www.nasa.gov/'
                   alt='National Aeronautics and Space Administration' target='nasa'
                   title='National Aeronautics and Space Administration'>
                    <img alt='Icon_nasa' src={NASA_ICO}/>
                </Link>
            </Stack>
        </Stack>
    );
};
