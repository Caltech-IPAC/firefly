import React from 'react';
import PropTypes from 'prop-types';
import FileBrowser from './react-keyed-file-browser/browser.js';

import './react-keyed-file-browser/react-keyed-file-browser.css';

export function FilePicker({engine, files, selectedItem, keepSelect, openFolders,...eventHandler}) {
    if (!engine || engine === 'react-keyed-file-browser') {
        return (
            <FileBrowser
                files={files}
                selectedItem={selectedItem}
                keepSelect={keepSelect}
                openFolders={openFolders}
                {...eventHandler} />
        );
    }
}

FilePicker.propTypes = {
    engine: PropTypes.string,
    onCreateFolder: PropTypes.func,
    onCreateFiles: PropTypes.func,
    onMoveFolder: PropTypes.func,
    onMoveFile: PropTypes.func,
    onRenameFolder: PropTypes.func,
    onRenameFile: PropTypes.func,
    onDeleteFolder: PropTypes.func,
    onDeleteFile: PropTypes.func,
    onClickItem: PropTypes.func,
    files: PropTypes.arrayOf(PropTypes.object),
    selectedItem: PropTypes.string,
    keepSelect: PropTypes.bool
};



