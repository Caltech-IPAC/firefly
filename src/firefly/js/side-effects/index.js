
// code taken from https://github.com/gregwebs/redux-side-effect
// modified to work correctly.

//'use strict';

//Object.defineProperty(exports, "__esModule", {
//  value: true
//});
//exports.mkSideEffect = mkSideEffect;

export var actionSideEffectMiddleware = actionSideEffectMiddleware;

function mkSideEffectCollector(sideEffects) {
  return function sideEffect() {
    for (var _len = arguments.length, effects = Array(_len), _key = 0; _key < _len; _key++) {
      effects[_key] = arguments[_key];
    }

    for (var i in effects) {
      sideEffects.push(effects[i]);
    }
  };
}

function mkSideEffectTimeout(sideEffect) {
  return function sideEffectTimeout(timeout) {
    for (var _len2 = arguments.length, effects = Array(_len2 > 1 ? _len2 - 1 : 0), _key2 = 1; _key2 < _len2; _key2++) {
      effects[_key2 - 1] = arguments[_key2];
    }

    return sideEffect(effects.map(function (eff) {
      return function (dispatch, getState) {
        setTimeout(eff(dispatch, getState), timeout);
      };
    }));
  };
}

function mkDrainSideEffects(sideEffects, dispatch, getState) {
  return function drainSideEffects() {
    while (sideEffects.length > 0) {
      sideEffects.shift()(dispatch, getState);
    }
  };
}

//function mkSideEffect() {
//  var sideEffects = [];
//  var sideEffect = mkSideEffectCollector(sideEffects);
//
//  function sideEffectMiddleware(_ref) {
//    var dispatch = _ref.dispatch;
//    var getState = _ref.getState;
//
//    var drainSideEffects = mkDrainSideEffects(sideEffects, dispatch, getState);
//    return function (next) {
//      return function (action) {
//        var result = next(action);
//        drainSideEffects();
//        return result;
//      };
//    };
//  }
//
//  return { sideEffect: sideEffect,
//    sideEffectTimeout: mkSideEffectTimeout(sideEffect),
//    sideEffectMiddleware: sideEffectMiddleware
//  };
//}

function actionSideEffectMiddleware(_ref2) {
  var dispatch = _ref2.dispatch;
  var getState = _ref2.getState;

  var sideEffects = [];
  var sideEffect = mkSideEffectCollector(sideEffects);
  var sideEffectTimeout = mkSideEffectTimeout(sideEffects);
  var drainSideEffects = mkDrainSideEffects(sideEffects, dispatch, getState);
  return function (next) {
    return function (action) {
      action.sideEffect = sideEffect;
      action.sideEffectTimeout = sideEffectTimeout;
      var result = next(action);
      drainSideEffects();
      return result;
    };
  };
}
