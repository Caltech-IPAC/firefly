# key-tree-store

Simple tool for storing/retrieving objects events based hierarchical keypaths.

It lets you store and retrive objects that are at an equal or deeper key path than what you give it.

## install

```
npm install key-tree-store
```

## example

Assume you've got a structure like this:

```js
{
    'first': [ {obj: 1}, {obj: 2} ],
    'first.stuff': [ {obj: 3} ],
    'first.something.other': [ {obj: 4}, {obj: 5} ]
}
```

Then you can retrive it by key. Where it returns anything at or deeper than level supplied. 

```javascript
var KeyTree = require('key-tree-store');

var tree = new KeyTree();

tree.add('first', {id: 'one'});
tree.add('first.second', {id: 'two'});
tree.add('first.second', {id: 'three'});
tree.add('first.second.third', {id: 'four'});

// now we can retrieve them by key
tree.get('first'); // returns all of them
tree.get('first.second'); // returns array of objects two, three and four
tree.get('first.second.third'); // returns array of object four;

// that's all there is to it

```

removing items:

```javascript
var KeyTree = require('key-tree-store');

var tree = new KeyTree();
var obj1 = {obj: '1'};

tree.add('key.path', obj1);

// removes it no matter what key
tree.remove(obj1);
```

## credits

If you like this follow [@HenrikJoreteg](http://twitter.com/henrikjoreteg) on twitter.

## license

MIT

