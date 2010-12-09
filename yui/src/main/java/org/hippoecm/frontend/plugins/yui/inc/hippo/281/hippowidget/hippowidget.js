/**
 * @description
 * <p>
 * Hippowidgets register with their ancestor layout units for rendering, resizing en destroying
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, functionqueue, layoutmanager, hippoajax
 * @module hippowidget
 * @beta
 */

YAHOO.namespace('hippo');
(function() {
    if (!YAHOO.hippo.Widget) {

        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.WidgetManagerImpl = function() {
            this.NAME = 'HippoWidget';
            this.queue = new YAHOO.hippo.FunctionQueue();
        };

        YAHOO.hippo.WidgetManagerImpl.prototype = {

            register : function(id, config, instance) {
                var name = this.NAME;
                this.queue.registerFunction(function() {
                    var widget = Dom.get(id);
                    if(widget == null) {
                        return;
                    }
                    if(Lang.isUndefined(widget[name])) {
                        try {
                            widget[name] = new instance(id, config);
                        } catch(e) {
                            YAHOO.log('Could not instantiate widget of type ' + instance, 'error');
                            return;
                        }
                    }
                    widget[name].render();
                });
            },

            render : function() {
                this.queue.handleQueue();
            },

            update : function(id) {
                var widget = Dom.get(id);
                if(widget == null) {
                    return;
                }
                if(!Lang.isUndefined(widget[this.NAME])) {
                   widget[this.NAME].update();
                }
            }
        };

        YAHOO.hippo.Widget = function(id, config) {
            this.id = id;
            this.config = config;

            this.el = Dom.get(id);
            this.unit = null;
            this.helper = new YAHOO.hippo.DomHelper();

            if(Lang.isFunction(this.config.calculateWidthAndHeight)) {
                this.calculateWidthAndHeight = this.config.calculateWidthAndHeight;
            }

            var self = this;
            YAHOO.hippo.LayoutManager.registerResizeListener(this.el, this, function(sizes) {
                self.resize(sizes);
            }, false);
            YAHOO.hippo.HippoAjax.registerDestroyFunction(this.el, function() {
                self.destroy();
            }, this);
        };

        YAHOO.hippo.Widget.prototype = {

            resize: function(sizes) {
                var dim = this.calculateWidthAndHeight(sizes);
                this.performResize(dim);
            },

            performResize : function(dim) {
                this._setWidth(dim.width);
                this._setHeight(dim.height);
            },

            _setWidth : function(w) {
                Dom.setStyle(this.el, 'width', w + 'px');
            },

            _setHeight : function(h) {
                Dom.setStyle(this.el, 'height', h + 'px');
            },

            render : function() {
                this.unit = YAHOO.hippo.LayoutManager.findLayoutUnit(this.el);
                if(this.unit != null) {
                    this.resize(this.unit.getSizes());
                } else {
                    //We're not inside a layout unit to provide us with dimension details, thus the
                    //resize event will never be called. For providing an initial size, the first ancestor
                    //with a classname is used.
                    var parent = Dom.getAncestorBy(el, function(node) {
                       return Lang.isValue(node.className) && Lang.trim(node.className).length > 0;
                    });
                    if(parent != null) {
                        var reg = Dom.getRegion(parent);
                        var margin = this.helper.getMargin(parent);
                        this.resize({wrap: {w: reg.width, h: reg.height}});
                    }
                }
            },

            update : function() {
                this.render();
            },

            destroy : function() {
                YAHOO.hippo.LayoutManager.unregisterResizeListener(this.el, this);
            },

            calculateWidthAndHeight : function(sizes) {
                return {width: sizes.wrap.w, height: sizes.wrap.h};
            }
        }

        YAHOO.hippo.WidgetManager = new YAHOO.hippo.WidgetManagerImpl();

        YAHOO.register("hippowidget", YAHOO.hippo.WidgetManager, {
            version: "2.8.1", build: "19"
        });
    }
})();

