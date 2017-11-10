import React from 'react';
import { DragSource } from 'react-dnd';

const IMAGE_EXTENSIONS = [
  'jpg',
  'png',
  'bmp'
];

class BaseFile extends React.Component {
  constructor(props) {
    super(props);

    this.handleFileClick = this.handleFileClick.bind(this);
    this.handleItemClick = this.handleItemClick.bind(this);
    this.handleItemDoubleClick = this.handleItemDoubleClick.bind(this);
    this.handleRenameClick = this.handleRenameClick.bind(this);
    this.handleNewNameChange = this.handleNewNameChange.bind(this);
    this.handleRenameSubmit = this.handleRenameSubmit.bind(this);
    this.handleDeleteClick = this.handleDeleteClick.bind(this);
    this.handleDeleteSubmit = this.handleDeleteSubmit.bind(this);
    this.handleCancelEdit = this.handleCancelEdit.bind(this);

    this.connectDND = this.connectDND.bind(this);

    this.state = {
      ...this.state,

      newName: this.getName()
    };
  }

  componentDidUpdate(oldProps, oldState) {
    if (!oldProps.isRenaming && this.props.isRenaming) {
      window.requestAnimationFrame(() => {
        var currentName = this.refs.newName.value;
        var pointIndex = currentName.lastIndexOf('.');
        this.refs.newName.setSelectionRange(0, pointIndex || currentName.length);
        this.refs.newName.focus();
      });
    }
  }

  getName() {
    var name = this.props.newKey || this.props.fileKey;
    var slashIndex = name.lastIndexOf('/');
    if (slashIndex !== -1) {
      name = name.substr(slashIndex + 1);
    }
    return name;
  }
  getExtension() {
    var blobs = this.props.fileKey.split('.');
    return blobs[blobs.length - 1].toLowerCase().trim();
  }
  isImage() {
    var extension = this.getExtension();
    for (var extensionIndex = 0; extensionIndex < IMAGE_EXTENSIONS.length; extensionIndex++) {
      var imageExtension = IMAGE_EXTENSIONS[extensionIndex];
      if (extension === imageExtension) {
        return true;
      }
    }
    return false;
  }
  isPdf() {
    var extension = this.getExtension();
    return (extension === 'pdf');
  }

  handleFileClick(event) {
    if (event) {
      event.preventDefault();
    }
    this.props.browserProps.preview({
      url: this.props.url,
      name: this.getName(),
      key: this.props.fileKey,
      extension: this.getExtension()
    });
  }
  handleItemClick(event) {
    event.stopPropagation();
    this.props.browserProps.select(this.props.fileKey);
  }
  handleItemDoubleClick(event) {
    event.stopPropagation();
    this.handleFileClick();
  }

  handleRenameClick(event) {
    if (!this.props.browserProps.renameFile) {
      return;
    }
    this.props.browserProps.beginAction('rename', this.props.fileKey);
  }
  handleNewNameChange(event) {
    var newName = this.refs.newName.value;
    this.setState((state) => {
      state.newName = newName;
      return state;
    });
  }
  handleRenameSubmit(event) {
    if (event) {
      event.preventDefault();
    }
    var newName = this.state.newName.trim();
    if (newName.length === 0) {
      // todo: move to props handler
      // window.notify({
      //   style: 'error',
      //   title: 'Invalid new file name',
      //   body: 'File name cannot be blank',
      // });
      return;
    }
    if (newName.indexOf('/') !== -1) {
      // todo: move to props handler
      // window.notify({
      //   style: 'error',
      //   title: 'Invalid new file name',
      //   body: 'File names cannot contain forward slashes.',
      // });
      return;
    }
    var newKey = this.props.fileKey.substr(0, this.props.fileKey.lastIndexOf('/'));
    newKey += '/';
    newKey += newName;
    this.props.browserProps.renameFile(this.props.fileKey, newKey);
  }

  handleDeleteClick(event) {
    if (!this.props.browserProps.deleteFile) {
      return;
    }
    this.props.browserProps.beginAction('delete', this.props.fileKey);
  }
  handleDeleteSubmit(event) {
    event.preventDefault();
    if (!this.props.browserProps.deleteFile) {
      return;
    }
    this.props.browserProps.deleteFile(this.props.fileKey);
  }

  handleCancelEdit(event) {
    this.props.browserProps.endAction();
  }

  connectDND(render) {
    var inAction = (this.props.isDragging || this.props.action);
    if (
      typeof this.props.browserProps.moveFile === 'function'
      && !inAction
      && !this.props.isRenaming
    ) {
      render = this.props.connectDragSource(render);
    }
    if (
      typeof this.props.browserProps.createFiles === 'function'
      || typeof this.props.browserProps.moveFile === 'function'
      || typeof this.props.browserProps.moveFolder === 'function'
    ) {
      render = this.props.connectDropTarget(render);
    }
    return render;
  }
}

const dragSource = {
  beginDrag(props) {
    props.browserProps.select(props.fileKey);
    return {
      key: props.fileKey
    };
  },

  endDrag(props, monitor, component) {
    if (!monitor.didDrop()) {
      return;
    }

    const item = monitor.getItem();
    const dropResult = monitor.getDropResult();
    var fileNameParts = props.fileKey.split('/');
    var fileName = fileNameParts[fileNameParts.length - 1];
    var newKey = `${dropResult.path ? dropResult.path + '/' : ''}${fileName}`;
    if (newKey !== props.fileKey && props.browserProps.renameFile) {
      props.browserProps.openFolder(dropResult.path + '/');
      props.browserProps.renameFile(props.fileKey, newKey);
    }
  }
};

function dragCollect(connect, monitor) {
  return {
    connectDragPreview: connect.dragPreview(),
    connectDragSource: connect.dragSource(),
    isDragging: monitor.isDragging()
  };
}

const targetSource = {
  drop(props, monitor) {
    if (monitor.didDrop()) {
      return;
    }
    var key = props.newKey || props.fileKey;
    var path = key.substr(0, key.lastIndexOf('/') || key.length);
    var item = monitor.getItem();
    if (item.files && props.browserProps.createFiles) {
      props.browserProps.createFiles(item.files, path + '/');
    }
    return {
      path: path
    };
  }
};

function targetCollect(connect, monitor) {
  return {
    connectDropTarget: connect.dropTarget(),
    isOver: monitor.isOver({shallow: true})
  };
}

var BaseFileConnectors = {
  dragSource,
  dragCollect,
  targetSource,
  targetCollect
};

export default BaseFile;
export {
  BaseFileConnectors
};
