/*
 * Copyright 2013 Next Century Corporation 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define([
    './BaseView',
    'jquery',
    'underscore'
], function(BaseView, $, _) {
    return BaseView.extend({

        // backbone view for a list item
        CollectionItemView: null,

        // class to add when item is selected or focused
        selectedClassName: 'selected',

        // currently selected item (backbone view)
        selectedItem: null,

        //configurable static parameters to be passed in to the
        //child views when they are constructed
        subViewParams: null,

        //flag to track whether to allow individual add operations.  By default, this coincides
        //with render being called
        allowAdd: false,

        events: {
            'click > *': 'onItemClick',
            'focusin > *': '_selectFocusedItem',
            'focusout > *': 'clearSelection'
        },

        modelEvents: {
            'add': 'addOneFromEvent',
            'reset': 'addAll'
        },

        initialize: function () {
            this.CollectionItemView = this.CollectionItemView || this.options.CollectionItemView;
            BaseView.prototype.initialize.apply(this, arguments);
        },

        render: function () {
            BaseView.prototype.render.apply(this, arguments);
            this.allowAdd = true;

            this.addAll();
            return this;
        },

        /**
         * Creates and renders a view for one child element.
         * @param item The model to base the child view on
         * @param params Extra parameters to pass to the child view constructor
         */
        createOne: function (item, params) {
            var view = new this.CollectionItemView(_.extend({ model: item },
                        _.extend({}, this.subViewParams, params)));

            view.render();
            return view;
        },

        /**
         * @protected
         * @param item The model add to this view
         * @param $container (Optional) A jquery element to add the new child element to.
         *  Defaults to this.$el
         */
        addOne: function(item, $container) {
            if (!this.allowAdd) { return false; }

            var view = this.createOne( item );

            $container = $container || this.$el;

            $container.append(view.el);
            return view;
        },

        /**
         * Wrapper function to discard the extra parameters that an add
         * event may be carrying which do not match the extra parameters to the
         * addOne function
         */
        addOneFromEvent: function(model) {
            this.addOne(model);
        },

        addAll: function() {
            var me = this;

            this.collection.each(function(item) {
                me.addOne(item);
            });
        },

        // handle item clicks
        // selects clicked item
        onItemClick: function (evt) {
            this.selectItem($(evt.currentTarget));
        },

        selectItem: function ($el) {
            this.clearSelection();

            this.selectedItem = $el.data('view');
            $el.addClass( this.selectedClassName );
        },

        clearSelection: function () {
            if(this.selectedItem) {
                this.selectedItem.$el.removeClass( this.selectedClassName );
                delete this.selectedItem;
            }
        },

        _selectFocusedItem: function (evt) {
            this.selectItem($(evt.target));
        }
    });
});
