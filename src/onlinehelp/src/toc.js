import {get, set, omit, has, clone} from 'lodash';


/*
    This file contains reusable parts used to construct a table of contents.
    Below are key parts you should be aware of:

    Table of contents:  A tree-like structure consisting of HelpItems rendered as the left navigation panel of the app.

    Topic:      A HelpItem with sub-items.  Because HelpItem may contain sub-items of other HelpItems, you can create a tree
                with unlimited branches and leaves.

    HelpItem:   One entry of help that can be referenced by the app or other help pages.
                HelpItem contains properties that's defined below: @typedef {object} HelpItem


    Suggested Usage:

    For greater reusability, a topic should be written as 2 parts; overview and details.
    Overview is a project specific information.  The details part should be sub-items and written in a generic way
    so that other projects can easily include it into their onlinehelp.

    For example:
        export const toc_a_topic = {
            id: 'a_topic',
            title: 'My topic',
            href: 'a_topic.html',           // points to html file in the public folder
            items: [
                {
                    id: 'a_topic.item1',
                    title: 'Item 1',
                    href: 'share/a_topic_details.html#item1'         // points to html file in the public/share folder
                },
                {
                    id: 'a_topic.item2',
                    title: 'Item 2',
                    href: 'share/a_topic_details_item2.html'         // can be part of a shared file or separate files
                }
            ]
        };


    This way, other project can reuse this topic like this:

        import {toc_a_topic} from 'firelfyHelp/toc';
        const my_topic = {...toc_a_topic, title: 'custom title if needed', href: 'my_topic.html'};

        or, insert an item into an existing topic:

        const my_topic = add{toc_a_topic, 'items.2', {id: 'new_id', title: 'New Item', href: 'new_topic.html'};


*/


/**
 * @typedef {object} HelpItem
 * @prop {string}   id      unique ID of the help item
 * @prop {string}   title   title of this item
 * @prop {string}   href    link to html content for this help item
 * @prop {string}   hidden  default false.  When true, entry will not be shown in the navigation tree.
 * @prop {object}   style   additional style to apply to this item
 * @prop [HelpItem] items   array of help items.  This is used to build the table of contents
 *
 * @global
 * @public
 */


//-------------------------------------------------------------------------------------------------
//  Below is a set of predefined topics.  Use this to construct your table of contents
//-------------------------------------------------------------------------------------------------

export const toc_about = {
    id: 'about',
    title: 'About Firefly',
    href: 'about.html'
};

export const toc_user = {
    id: 'user',
    title: 'User Registration',
    href: 'user.html'
};

//export const toc_faq = {
//    id: 'faq',
//    title: 'FAQ',
//    href: 'share/faq.html'
//};

export const toc_privacy = {
    id: 'privacy',
    title: 'IRSA Privacy Notice',
    href: 'privacy.html'
};

const toc_visualization = {
    id: 'visualization',
    title: 'Visualization',
    href: 'visualization.html',
    items: [
        {
            id: 'visualization.imageoptions',
            title: 'Image Toolbar',
            href: 'visualization.html#toolbar',
            hidden: true,
        },
        {
            id: 'visualization.Rotate',
            title: 'Rotation',
            href: 'visualization.html#rotateImage',
            hidden: true,
        },
        {
            id: 'visualization.selectregion',
            title: 'Select Region',
            href: 'visualization.html#selectregion',
            hidden: true,
        },
        {
            id: 'visualization.layers',
            title: 'Layers',
            href: 'visualization.html#layers',
            hidden: true,
        },
        {
            id: 'visualization.ds9regions',
            title: 'ds9 Regions',
            href: 'visualization.html#ds9regions',
            hidden: true,
        },
        {
            id: 'visualization.fitsViewer',
            title: 'The FITS/HiPS viewer',
            href: 'visualization.html#fitsViewer',
        },
        {
            id: 'visualization.imageinfo',
            title: 'Image Information',
            href: 'visualization.html#imageinfo',
        },
        {
            id: 'visualization.toolbar',
            title: 'Image Toolbar',
            href: 'visualization.html#toolbar',
        },
        {
            id: 'visualization.stretches',
            title: 'Color Stretches',
            href: 'visualization.html#stretches',
        },
        {
            id: 'visualization.hipsViewer',
            title: 'Specific HiPS Features',
            href: 'visualization.html#hipsViewer',
        },
        {
            id: 'visualization.footprints',
            title: 'Footprints',
            href: 'visualization.html#footprints',
        },
        {
            id: 'visualization.breakingout',
            title: 'Breaking out of the pane',
            href: 'visualization.html#breakingout',
        },
        {
            id: 'visualization.loaded-images',
            title: 'Image Navigation',
            href: 'visualization.html#imagenavigation',
             hidden: true,
        },
        {
            id: 'visualization.navigation',
            title: 'Image Navigation',
            href: 'visualization.html#imagenavigation',
        },
        {
            id: 'visualization.wcs',
            title: 'WCS Alignment',
            href: 'visualization.html#wcs',
        },
        {
            id: 'visualization.coverage',
            title: 'Coverage Image',
            href: 'visualization.html#coverage',
        },
        {
            id: 'visualization.fitships',
            title: 'Automatic FITS-HiPS-Aitoff Transitions',
            href: 'visualization.html#autozoom',
        },
        {
            id: 'visualization.changehips',
            title: 'Change HiPS',
            href: 'visualization.html#changehips',
              hidden: true,
       },
    ]
};

export const toc_tables = {
    id: 'tables',
    title: 'Tables',
    href: 'tables.html',
    items: [
        {
            id: 'tables.tableoptions',
            title: 'Table Options',
            href: 'tables.html#tableoptions',
            hidden: true,
        },
        {
            id: 'tables.header',
            title: 'Table Header',
            href: 'tables.html#header',
        },
        {
            id: 'tables.columns',
            title: 'Table Columns',
            href: 'tables.html#columns',
        },
        {
            id: 'tables.filters',
            title: 'Table Filters',
            href: 'tables.html#filters',
        },
        {
            id: 'tables.save',
            title: 'Saving Tables',
            href: 'tables.html#save',
        },
        {
            id: 'tables.catalogs',
            title: 'Catalogs',
            href: 'tables.html#catalogs',
        },
        {
            id: 'basics.catalogs',
            title: 'Catalogs',
            href: 'tables.html#catalogs',
             hidden: true,
        },
    ]
};


export const toc_plots = {
    id: 'plots',
    title: 'Plots',
    href: 'plots.html',
    items: [
        {
            id: 'plots.default',
            title: 'Default Plot',
            href: 'plots.html#default'
        },
        {
            id: 'plots.firstlook',
            title: 'Plot Format: A First Look',
            href: 'plots.html#firstlook'
        },
        {
            id: 'plots.linking',
            title: 'Plot Linking',
            href: 'plots.html#linking'
        },
        {
            id: 'plots.changing',
            title: 'Changing What is Plotted',
            href: 'plots.html#changing'
        },
        {
            id: 'plots.manipulating',
            title: 'Plotting Manipulated Columns',
            href: 'plots.html#manipulating'
        },
        {
            id: 'plots.restricting',
            title: 'Restricting What is Plotted',
            href: 'plots.html#restricting'
        },
        {
            id: 'plots.saving',
            title: 'Saving Plots',
            href: 'plots.html#saving'
        },
        {
            id: 'plots.adding',
            title: 'Adding Plots',
            href: 'plots.html#adding'
        },
        {
            id: 'plots.example',
            title: 'Example Plots',
            href: 'plots.html#example'
        },
    ]
};

//-------------------------------------------------------------------------------------------------
//  Below is a set of predefined table of contents.  Or, simply create one from the above topics.
//-------------------------------------------------------------------------------------------------


/**
 * Default table of contents used by Firefly
 */
export const fireflyToc = [
    toc_about,
    toc_visualization,
    toc_tables,
    toc_plots,
    toc_user,
    toc_privacy
];



// export it as toc
export const toc = fireflyToc;
