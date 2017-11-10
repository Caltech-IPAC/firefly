import React from 'react';
import ClassNames from 'classnames';
import { DragSource, DropTarget } from 'react-dnd';
import { NativeTypes } from 'react-dnd-html5-backend';

import BaseFolder from './../base-folder.js';
import { BaseFolderConnectors } from './../base-folder.js';
import { BaseFileConnectors } from './../base-file.js';

class ListFolder extends BaseFolder {
  render() {
    var icon;
    if (this.props.isOpen) {
      icon = (<i className='fa fa-folder-open-o' aria-hidden='true'></i>);
    }
    else {
      icon = (<i className='fa fa-folder-o' aria-hidden='true'></i>);
    }

    var inAction = (this.props.isDragging || this.props.action);

    var name;
    if (!inAction && this.props.isRenaming) {
      name = (
        <div>
          <form className='renaming' onSubmit={this.handleRenameSubmit}>
            <input
              type='text'
              ref='newName'
              className='form-control input-sm'
              value={this.state.newName}
              onChange={this.handleNewNameChange}
              onBlur={this.handleCancelEdit}
            />
            <div className='actions'>
              <a
                className='cancel btn btn-secondary btn-sm'
                onClick={this.handleCancelEdit}
              >
                Cancel
              </a>
            </div>
          </form>
        </div>
      );
    }
    else {
      name = (
        <div>
          <a onClick={this.toggleFolder}>
            {this.getName()}
          </a>
        </div>
      );
    }

    var children;
    if (this.props.isOpen && this.props.browserProps.nestChildren) {
      children = [];
      for (var childIndex = 0; childIndex < this.props.children.length; childIndex++) {
        var file = this.props.children[childIndex];

        var thisItemProps = {
          ...this.props.browserProps.getItemProps(file, this.props.browserProps),
          depth: this.props.depth + 1
        };

        if (file.size) {
          children.push(
            <this.props.browserProps.fileRenderer
              {...file}
              {...thisItemProps}
              browserProps={this.props.browserProps}
            />
          );
        }
        else {
          children.push(
            <this.props.browserProps.folderRenderer
              {...file}
              {...thisItemProps}
              browserProps={this.props.browserProps}
            />
          );
        }
      }
      if (children.length) {
        children = (<ul style={{padding: '0 8px', paddingLeft: '16px'}}>{children}</ul>);
      }
      else {
        children = (<p className='text-muted'>No items in this folder</p>);
      }
    }

    var folder = (
      <li
        className={ClassNames('folder', {
          expanded: (this.props.isOpen && this.props.browserProps.nestChildren),
          pending: (this.props.action),
          dragging: (this.props.isDragging),
          dragover: this.props.isOver,
          selected: this.props.isSelected
        })}
        onClick={this.handleFolderClick}
        onDoubleClick={this.handleFolderDoubleClick}
      >
        <div className='item'>
          <span className='thumb'>{icon}</span>
          <span className='name'>{name}</span>
        </div>
        {children}
      </li>
    );
    if (this.props.browserProps.canMoveFolders && this.props.keyDerived) {
      folder = this.props.connectDragPreview(folder);
    }

    return this.connectDND(folder);
  }
}

export default DragSource(
  'folder',
  BaseFolderConnectors.dragSource,
  BaseFolderConnectors.dragCollect
)(
  DropTarget(
    ['file', 'folder', NativeTypes.FILE],
    BaseFileConnectors.targetSource,
    BaseFileConnectors.targetCollect
  )(
    ListFolder
  )
);
