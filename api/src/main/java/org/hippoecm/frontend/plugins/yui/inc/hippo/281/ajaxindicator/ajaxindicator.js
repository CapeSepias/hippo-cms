/*
 * Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @description <p>TODO</p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom
 * @module ajaxindicator
 */

 //TODO: might register to a custom layout-processing event to extend the loading indication
 //until after the layout has processed, instead of just the postAjaxEvent

(function() {
  var Dom = YAHOO.util.Dom,
  Lang = YAHOO.util.Lang;

  YAHOO.namespace('hippo');

  YAHOO.hippo.AjaxIndicator = function(_elId) {
    this.elementId = _elId;

    Wicket.Event.subscribe(Wicket.Event.Topic.AJAX_CALL_BEFORE_SEND, Wicket.bind(this.show, this));
    Wicket.Event.subscribe(Wicket.Event.Topic.AJAX_CALL_COMPLETE, Wicket.bind(this.hide, this));

    if (Ext && Ext.Ajax) {
      Ext.Ajax.on('beforerequest', this.show, this);
      Ext.Ajax.on('requestcomplete', this.hide, this);
      Ext.Ajax.on('requestexception', this.hide, this);
    }
  };

  YAHOO.hippo.AjaxIndicator.prototype = {
    elementId: null,
    calls: 0,
    timerID: 0,
    active: true,

    getElement: function() {
        return Dom.get(this.elementId);
    },

    setActive: function(active) {
        if (this.active && !active) {
            document.body.style.cursor = 'default';
            Dom.setStyle(this.getElement(), 'display', 'none');
        }
        if (!this.active && active && this.calls > 0) {
            Dom.setStyle(this.getElement(), 'display', 'block');
        }
        this.active = active;
    },

    show: function() {
        if (this.active && this.calls === 0) {
            this.timerID = self.setTimeout("document.body.style.cursor = 'wait';", 750);
        }
        this.calls++;
        if (this.active) {
            Dom.setStyle(this.getElement(), 'display', 'block');
        }
    },

    hide: function() {
        if (this.calls > 0) {
            this.calls--;
        }
        if (this.active && this.calls === 0) {
            self.clearTimeout(this.timerID);
            document.body.style.cursor = 'default';
            Dom.setStyle(this.getElement(),'display', 'none');
        }
    },

    setCursor: function(win, cursor) {
        if (!Lang.isNull(win.document.body)) {
            Dom.setStyle(win.document.body, 'cursor', cursor);
        }
    }
  };
}());

YAHOO.register("ajaxindicator", YAHOO.hippo.AjaxIndicator, {version: "2.8.1", build: "19"});
