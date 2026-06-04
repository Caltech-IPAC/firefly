import Flare from '@mui/icons-material/Flare';
import ManageSearch from '@mui/icons-material/ManageSearch';
import UploadFile from '@mui/icons-material/UploadFile';
import {Box, Chip, Divider, Link, Sheet, Stack, Typography, SvgIcon} from '@mui/joy';
import {cloneDeep, defaultsDeep} from 'lodash';
import React, {useContext, useState} from 'react';
import {arrayOf, func, node, number, object, shape, string} from 'prop-types';

import BG_IMAGE from 'images/Background_Firefly.jpg';
import IpacLogo from 'html/images/logos/IPAC_logo.svg';
import CaltechLogo from 'html/images/logos/Caltech_logo.svg';
import NasaLogo from 'html/images/logos/NASA_logo.svg';
import NsfLogo from 'html/images/logos/NSF_logo.svg';
import {getHintAnchorNodes} from '../../core/LayoutCntlr.js';
import {joyVarColorWithAlpha} from 'firefly/util/Color.js';
import {LandingPage} from './LandingPage.jsx';
import {AppPropertiesCtx} from 'firefly/ui/AppPropertiesCtx.jsx';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {GuidedHint} from 'firefly/ui/AppHint';
import {showFullVersionInfoDialog} from 'firefly/ui/VersionInfo.jsx';


// ── Configurable text & data ─────────────────────────────────────────────────

const DEFAULT_TITLE      = 'Firefly';
const DEFAULT_TAGLINE    = 'Explore Astronomy Data across missions and archives, including your own data';

const FEATURE_BADGES = [
    // keep same length (~2 words)
    'FITS/HiPS Images',
    'DS9/MOC Overlays',
    'Catalogs at Scale',
    'Interlinked Views',
    'Spectra & Charts',
];

// Base config for action cards — onClick is added inside StandaloneFireflyLanding to wire up GuidedHints
const ACTION_CARD_CONFIG = [
    {
        icon:      <Flare sx={{fontSize: '1.5rem'}}/>,
        title:     'Search for data',
        desc:      'TAP, SIAv2, HiPS & more',
        sub:       'using the tabs above',
        hintId:    'search',
        hintText:  'Choose a tab to search for data',
    },
    {
        icon:      <ManageSearch sx={{fontSize: '1.75rem'}}/>,
        title:     'Find more search options',
        desc:      'SCS, IRSA, NED & more',
        sub:       'in the side menu ☰',
        hintId:    'sideMenu',
        hintText:  'Click on the menu to find more search options',
        placement: 'bottom-start',
    },
    {
        icon:      <UploadFile sx={{fontSize: '1.625rem'}}/>,
        title:     'Upload a file',
        desc:      'FITS, VOTable, Parquet & more',
        sub:       'drag & drop anywhere on screen',
        hintId:    'upload',
        hintText:  'Choose this tab to upload a file from your device or URL',
    },
];

const DEFAULT_CHIPS = [
    {
        label: '🔍 AllWISE cone search at M101 →',
        url: '?api=tap&service=https://irsa.ipac.caltech.edu/TAP&schema=wise&table=allwise_p3as_psd&ra=210.8022671&dec=54.34895&sr=300s',
    },
    {
        // TODO: api=image loads image directly, need to create api=sia and use that (also because image&service is IRSA-dependent)
        label: '🌌 Spitzer SEIP image of M101 →',
        // url: '?api=image&service=TWOMASS&SurveyKey=mosaic&SurveyKeyBand=j&WorldPt=210.80227;54.34895;EQ_J2000&sr=500s',
        url: '?api=image&service=SEIP&SurveyKey=spitzer.seip_science&SurveyKeyBand=IRAC4&WorldPt=210.80227;54.34895;EQ_J2000&sr=500s',
    },
    {
        label: '🔭 GAIA DR3 cone search at M101 →',
        url: '?api=tap&service=https://gea.esac.esa.int/tap-server/tap&schema=gaiadr3&table=gaiadr3.gaia_source&WorldPt=210.80227;54.34895;EQ_J2000&sr=500s',
    },
    {
        // TODO: api=hipsPanel doesn't allowing selecting a HiPS server without executing the search, need to make "uri" not execute
        label: '🌐 SDSS HiPS view of M81 →',
        uri: '?api=hipsPanel&showPanel=true&ra=148.88822&dec=69.06529&sr=40m&uri=ivo://CDS/P/SDSS9/color',
    },
    {
        label: '📈 Euclid Q1 spectrum from cloud →',
        // contains spectrum for object id -649007186467842564 in hdu 709
        url: '?api=load&url=s3://nasa-irsa-euclid-q1/q1/SIR/102022482/EUC_SIR_W-COMBSPEC_102022482_2024-11-05T13:04:22.738022Z.fits',
    },
];

const DEFAULT_RESULT_HINT = 'Results from your searches will appear here or try one of the examples above';


// ── Sub-components ───────────────────────────────────────────────────────────

function FireflyAppBranding({title, tagline}) {
    const textShadow = '0 1px 2px rgba(0,0,0,0.9), 0 2px 8px rgba(0,0,0,0.7)';
    return (
        <Stack spacing={1} alignItems='center'>
            <Typography level='h1' sx={(theme) => ({
                color: theme.colorSchemes.dark.palette.text.primary,
                letterSpacing: '0.04em',
                // add a glow micro-animation
                animation: 'fireflyGlow 5s ease-in-out infinite',
                '@keyframes fireflyGlow': {
                    '0%':   { opacity: 1,    textShadow: '0px -1px 2px rgba(255,255,255,0.1)' },
                    '50%':  { opacity: 0.85, textShadow: '0px -1px 8px rgba(96,184,224,1), 1px -2px 16px rgba(96,184,224,0.8)' },
                    '100%': { opacity: 1,    textShadow: '0px -1px 2px rgba(255,255,255,0.1)' },
                },
            })}>
                {title}
            </Typography>
            {tagline && (
                <Typography level='body-lg' sx={(theme) => ({
                    color: theme.colorSchemes.dark.palette.text.secondary,
                    textShadow,
                    textAlign: 'center',
                })}>
                    {tagline}
                </Typography>
            )}
            <Typography level='body-sm' sx={(theme) => ({
                color: theme.colorSchemes.dark.palette.text.tertiary,
                textShadow,
                display: 'flex',
                flexWrap: 'wrap',
                justifyContent: 'center',
                rowGap: 0,
                columnGap: '0.75em',
                letterSpacing: '0.03em',
            })}>
                {FEATURE_BADGES.map((badge, idx) => (
                    <React.Fragment key={idx}>
                        {idx > 0 && <Typography>✦</Typography>}
                        <Typography sx={{whiteSpace: 'nowrap'}}>{badge}</Typography>
                    </React.Fragment>
                ))}
            </Typography>
        </Stack>
    );
}

FireflyAppBranding.propTypes = {
    title: string,
    tagline: string,
    trustLine: string,
};


function ActionCard({icon, title, desc, sub, onClick}) {
    return (
        <Box onClick={onClick} sx={(theme) => {
            const dp = theme.colorSchemes.dark.palette;
            return {
                flex: 1,
                p: 1.5,
                textAlign: 'center',
                cursor: onClick ? 'pointer' : 'default',
                borderRadius: '8px',
                transition: 'background 0.15s',
                ...(onClick && {'&:hover': {background: joyVarColorWithAlpha(dp.text.primary, 0.1)}}),
            };
        }}>
            {icon && (
                <Box sx={(theme) => ({
                    display: 'flex', alignItems: 'center', justifyContent: 'center', mb: 1,
                    color: theme.colorSchemes.dark.palette.text.icon,
                    '.MuiSvgIcon-root': {color: theme.colorSchemes.dark.palette.text.icon}
                })}>
                    {icon}
                </Box>
            )}
            <Typography level='title-sm' sx={(theme) => ({fontWeight: 500, color: theme.colorSchemes.dark.palette.text.primary, mb: 0.5})}>
                {title}
            </Typography>
            {desc && (
                <Typography level='body-xs' sx={(theme) => ({color: theme.colorSchemes.dark.palette.text.secondary, lineHeight: 1.5})}>
                    {desc}
                </Typography>
            )}
            {sub && (
                <Typography level='body-xs' sx={(theme) => ({color: theme.colorSchemes.dark.palette.text.tertiary, mt: 0.25})}>
                    {sub}
                </Typography>
            )}
        </Box>
    );
}

ActionCard.propTypes = {
    icon: node,
    title: string,
    desc: string,
    sub: string,
    onClick: func,
};


function FireflyActionsPanel({actionItems, chips, resultHint = DEFAULT_RESULT_HINT, slotProps}) {
    // actionItems may arrive merged with LandingPage's old-format defaults (which use `text`/`subtext`);
    // fall back to built-in defaults when none of the items carry the new `title` field.
    const cardItems = actionItems?.some((i) => i.title) ? actionItems : ACTION_CARD_CONFIG;
    const cardChips = Array.isArray(chips) ? chips : DEFAULT_CHIPS;
    const hasFooter = cardChips.length > 0 || Boolean(resultHint);

    return (
        <Sheet sx={(theme) => {
            const dp = theme.colorSchemes.dark.palette;
            return {
                background: joyVarColorWithAlpha(dp.neutral[500], 0.25),
                backdropFilter: 'blur(8px)',
                border: `0.5px solid ${joyVarColorWithAlpha(dp.neutral[100], 0.15)}`,
                borderRadius: '12px',
                p: 3,
                width: '100%',
            };
        }} {...slotProps?.root}>
            <Stack direction='row' sx={{mb: hasFooter ? 3 : 0}}>
                {cardItems.map((item, idx) => (
                    <React.Fragment key={idx}>
                        {idx > 0 && (
                            <Divider orientation='vertical' sx={(theme) => ({
                                background: joyVarColorWithAlpha(theme.colorSchemes.dark.palette.neutral[300], 0.1),
                                my: 1,
                            })}/>
                        )}
                        <ActionCard {...item}/>
                    </React.Fragment>
                ))}
            </Stack>
            {cardChips.length > 0 && (
                <>
                    <Divider sx={(theme) => ({
                        background: joyVarColorWithAlpha(theme.colorSchemes.dark.palette.neutral[300], 0.1),
                        mb: 2
                    })}/>
                    <Stack direction='row' flexWrap='wrap' gap={1} justifyContent='center' sx={{mb: resultHint ? 2 : 0}}>
                        {cardChips.map((chip, idx) => (
                            <Chip key={idx} variant='outlined'
                                onClick={() => { window.location.href = chip.url; }}
                                sx={(theme) => {
                                    const dp = theme.colorSchemes.dark.palette;
                                    return {
                                        color: dp.text.secondary,
                                        fontSize: 'xs',
                                        cursor: 'pointer',
                                        py: .5, px: 1.5,
                                        '& .MuiChip-action': {
                                            borderColor: joyVarColorWithAlpha(dp.neutral[300], 0.3),
                                            backgroundColor: joyVarColorWithAlpha(dp.neutral[200], 0.15),
                                            '&:hover': {
                                                borderColor: joyVarColorWithAlpha(dp.neutral[200], 0.3),
                                                backgroundColor: joyVarColorWithAlpha(dp.neutral[100], 0.3),
                                            },
                                        },
                                        '&:hover': {
                                            color: dp.text.primary,
                                            textDecoration: 'underline',
                                            textUnderlineOffset: '2px',
                                        },
                                    };
                                }}>
                                {chip.label}
                            </Chip>
                        ))}
                    </Stack>
                </>
            )}
            {resultHint && (
                <Typography sx={(theme) => ({fontSize: 'xs', color: theme.colorSchemes.dark.palette.text.tertiary, textAlign: 'center'})}>
                    {resultHint}
                </Typography>
            )}
        </Sheet>
    );
}

FireflyActionsPanel.propTypes = {
    actionItems: arrayOf(shape({
        icon: node,
        title: string,
        desc: string,
        sub: string,
        onClick: func,
    })),
    chips: arrayOf(shape({label: string, url: string})),
    resultHint: string,
    slotProps: object,
};

// ── Main component ───────────────────────────────────────────────────────────

/**
 * Landing page for the standalone Firefly web application (Firefly.js defAppProps).
 * Renders a full-bleed dark hero section with an astronomy background image, feature badges,
 * action cards, and clickable example query chips.
 *
 * Wraps {@link LandingPage} via the slot system to override visual elements (bgContainer, contentSection, topSection,
 * bottomSection) while keeping the store connector logic, FileDropZone, and AppHints.
 *
 * @param {object}  props
 * @param {object}  [props.bgImage=BG_IMAGE]   - URL/import of the hero background image.
 * @param {number}  [props.bgDimOpacity=0.2]   - Opacity (0–1) of the dark overlay on the bg image to dim it. Higher = darker.
 * @param {object}  [props.slotProps={}]       - Per-slot overrides forwarded to LandingPage.
 *                                               User-provided values take precedence; defaults fill in missing keys.
 */
export function StandaloneFireflyLanding({bgImage = BG_IMAGE, bgDimOpacity = 0.2, slotProps = {}, ...rest}) {
    const [activeHint, setActiveHint] = useState(null);
    const {first: searchAnchor, upload: uploadAnchor, sideMenuBtn: sideMenuAnchor} = useStoreConnector(getHintAnchorNodes);
    const anchorMap = {search: searchAnchor, sideMenu: sideMenuAnchor, upload: uploadAnchor};

    const mSlotProps = cloneDeep(slotProps);
    defaultsDeep(mSlotProps, {
        bgContainer: {
            sx: (theme) => {
                const bodyColor = theme.colorSchemes.dark.palette.background.body;
                const dimColor  = joyVarColorWithAlpha(bodyColor, bgDimOpacity);
                // in light mode, we need to blend hard edge of the dark bg image into light banner and footer (primary.softBg)
                const insetShadowColor = joyVarColorWithAlpha(theme.palette.primary.softBg, 0.36);
                return {
                    display: 'flex',
                    alignItems: 'center',
                    flexGrow: 1,
                    backgroundColor: bodyColor,
                    ...(bgImage && {
                        background: `linear-gradient(${dimColor}, ${dimColor}), url(${bgImage}) center/cover`,
                    }),
                    ...(theme.palette.mode === 'light' && {
                        boxShadow: `inset 0 48px 36px -32px ${insetShadowColor}, inset 0 -42px 36px -32px ${insetShadowColor}`
                    }),
                };
            },
        },
        contentSection: {
            alignItems: 'center',
            spacing: 4,
            sx: {
                maxWidth: '52rem',
                mx: 'auto',
            },
        },
        topSection: {
            component: FireflyAppBranding,
            title: DEFAULT_TITLE,
            tagline: DEFAULT_TAGLINE,
        },
        bottomSection: {
            component: FireflyActionsPanel,
            resultHint: DEFAULT_RESULT_HINT,
            actionItems: ACTION_CARD_CONFIG.map((item) => ({
                ...item,
                onClick: () => setActiveHint(item.hintId),
            })),
            // chips intentionally omitted — FireflyActionsPanel handles its own defaults
            // so that callers can supply the array without defaultsDeep merging by index
        },
    });
    return (
        <>
            <LandingPage slotProps={mSlotProps} {...rest}/>
            {/* GuidedHints render into a Popper portal (appended to <body>), outside LandingPage's DOM tree */}
            {ACTION_CARD_CONFIG.map(({hintId, hintText, placement='bottom'}) =>
                anchorMap[hintId] && (
                    <GuidedHint key={hintId} open={activeHint===hintId} onClose={() => setActiveHint(null)}
                                anchorEl={anchorMap[hintId]} hintText={hintText} placement={placement}/>
                    )
            )}
        </>
    );
}

StandaloneFireflyLanding.propTypes = {
    bgImage: string,
    bgDimOpacity: number,
    ...LandingPage.propTypes,
};


// ── Footer ───────────────────────────────────────────────────────────────────

export function StandaloneFireflyFooter() {
    const {appTitle} = useContext(AppPropertiesCtx);
    return (
        <Sheet color='primary' variant='soft' sx={{
            width: 1,
            height: '3rem',
            display: 'flex',
            border: '1px solid',
            borderColor: 'neutral.outlinedBorder',
        }}>
            <Stack direction='row' spacing={2}
                   sx={{
                       flexGrow: 1,
                       px: 4.5,
                       alignItems: 'center',
                       justifyContent: 'space-between',
                       '& .MuiLink-root': {color: 'primary.softColor'}
            }}>
                <Stack direction='row' spacing={4}>
                    <Link level='title-sm' underline='hover' href='https://github.com/Caltech-IPAC/firefly' target='github'>GitHub</Link>
                    <Link level='title-sm' underline='hover' href='https://hub.docker.com/r/ipac/firefly' target='dockerhub'>DockerHub</Link>
                    <Link level='title-sm' underline='hover' component='button' onClick={() => showFullVersionInfoDialog(appTitle)}>Version Info</Link>
                </Stack>
                <Stack direction='row' spacing={4} alignItems='center'>
                    <Link href='http://www.ipac.caltech.edu/' target='ipac' title='Science & Data Center for Astrophysics & Planetary Sciences'>
                        <SvgIcon component={IpacLogo} inheritViewBox={true} sx={{height: '1.75rem', width: 'auto'}}/>
                    </Link>
                    <Link href='http://www.caltech.edu/' target='caltech' title='California Institute of Technology'>
                        <SvgIcon component={CaltechLogo} inheritViewBox={true} sx={{height: '1rem', width: 'auto'}}/>
                    </Link>
                    <Link href='http://www.nasa.gov/' target='nasa' title='National Aeronautics and Space Administration'>
                        <SvgIcon component={NasaLogo} inheritViewBox={true} sx={{height: '2.25rem', width: 'auto'}}/>
                    </Link>
                    <Link href='https://www.nsf.gov/' target='nsf' title='U.S. National Science Foundation'>
                        <SvgIcon component={NsfLogo} inheritViewBox={true} sx={{height: '2.25rem', width: 'auto', ml: -1.5}}/>
                    </Link>
                </Stack>
            </Stack>
        </Sheet>
    );
}
