/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Convert a single file-input element into a 'multiple' input list
 *
 * Usage:
 *
 *   1. Create a file input element (no name)
 *      eg. <input type="file" id="first_file_element">
 *
 *   2. Create a DIV for the output to be written to
 *      eg. <div id="files_list"></div>
 *
 *   3. Instantiate a MultiSelector object, passing in the DIV and an (optional) maximum number of files
 *      eg. var multi_selector = new MultiSelector( document.getElementById( 'files_list' ), 3 );
 *
 *   4. Add the first element
 *      eg. multi_selector.addElement( document.getElementById( 'first_file_element' ) );
 *
 *   5. That's it.
 *
 *   You might (will) want to play around with the addListRow() method to make the output prettier.
 *
 *   You might also want to change the line
 *       element.name = 'file_' + this.count;
 *   ...to a naming convention that makes more sense to you.
 *
 * Licence:
 *   Use this however/wherever you like, just don't blame me if it breaks anything.
 *
 * Credit:
 *   If you're nice, you'll leave this bit:
 *
 *   Class by Stickman -- http://www.the-stickman.com
 *      with thanks to:
 *      [for Safari fixes]
 *         Luis Torrefranca -- http://www.law.pitt.edu
 *         and
 *         Shawn Parker & John Pennypacker -- http://www.fuzzycoconut.com
 *      [for duplicate name bug]
 *         'neal'
 */

/**
 * Had to fork this to customize layout.
 *
 * - added wrapper element around list
 * - added meaningfull classNames
 * - added fileName stripping (IE) and maxLength handling
 * - added scrollbars if list get's to big for dialog (very hard-coded atm)
 */

/*global wicketSubmitForm, YAHOO*/

function MultiSelector(prefix, listId, listLabel, deleteLabel, maxNumberOfFiles,
                       submitAfterSelect, clearAfterSubmit, clearTimeout) {
    "use strict";

    // How many elements?
    this.count = 0;
    // Input[file] id count
    this.id = 0;

    this.element_name_prefix = prefix;

    // Where to write the list
    this.list_target = document.getElementById(listId);

    this.list_label = null;
    this.list_container = null;
    this.selected_items = [];
    this.form = null;

    this.delete_label = deleteLabel;
    this.listLabel = listLabel;

    this.maxNumberOfFiles = maxNumberOfFiles || 1;
    this.submitAfterSelect = submitAfterSelect || false;
    this.clearAfterSubmit = clearAfterSubmit || false;
    this.clearTimeout = clearTimeout || 500;
    this.maxLengthFilename = 70;

    /**
     * Add a new file input element
     */
    this.addElement = function(element) {
        var new_element, multiSelector;

        // Make sure it's a file input element
        if (element.tagName.toLowerCase() === 'input' && element.type.toLowerCase() === 'file') {

            // Element name -- what number am I?
            element.name = this.element_name_prefix + "_mf_" + this.id++;

            // Add reference to this object
            element.multi_selector = multiSelector = this;

            if (this.form === null) {
                this.form = this.getForm(element);
            }

            // What to do when a file is selected
            element.onchange = function() {
                var filename,
                    submit = function() {
                        var form = multiSelector.form;
                        if (form.hasAttribute('action')) {
                            // the action should be the form url
                            wicketSubmitForm(form, form.getAttribute('action'), null, function() {
                                //success
                                if (multiSelector.clearAfterSubmit) {
                                    window.setTimeout(function() {
                                        multiSelector.reset();
                                    }, multiSelector.clearTimeout);

                                }
                            }, function() {
                                //failure
                                multiSelector.reset();
                            });
                        }
                    };

                if (multiSelector.submitAfterSelect && multiSelector.maxNumberOfFiles === 1) {
                    submit();
                } else {
                    // Workaround for issue CMS7-6415: IE8: Extra empty files...
                    if (YAHOO.env.ua.ie === '8') {
                        filename = multiSelector.parseFilename(this.value);
                        if (filename === '' || YAHOO.lang.trim(filename) === '') {
                            return;
                        }
                    }

                    multiSelector.selected_items.push(this);

                    // New file input
                    new_element = document.createElement('input');
                    new_element.type = 'file';

                    // Add new element
                    this.parentNode.insertBefore(new_element, this);

                    // Apply 'update' to element
                    multiSelector.addElement(new_element);
                    multiSelector.addListRow(this);

                    // If we've reached maximum number, disable input element
                    if (multiSelector.count === multiSelector.maxNumberOfFiles) {
                        new_element.disabled = true;
                        if (multiSelector.submitAfterSelect) {
                            submit();
                        }
                    }

                    // Hide this: we can't use display:none because Safari doesn't like it
                    this.style.position = 'absolute';
                    this.style.left = '-3000px';
                }
            };

            // Most recent element
            this.current_element = element;

        } else {
            // This can only be applied to file input elements!
            alert('Error: not a file input element');
        }

    };

    this.getForm = function(element) {
        while (element !== document.body) {
            if (element.tagName.toLowerCase() === 'form') {
                return element;
            }
            element = element.parentNode;
        }
        return null;
    };

    /**
     * Add a new row to the list of files
     */
    this.addListRow = function(element) {
        var new_row, new_row_button, img, label;

        if (this.count >= 8) {
            //turn container element into a scrollable unit with set height.
            this.list_container.className = 'wicket-mfu-row-container-scrollable';
        }

        // Row div
        new_row = document.createElement('div');
        new_row.className = 'wicket-mfu-row';

        // Delete button
        new_row_button = document.createElement('a');
        new_row_button.title = this.delete_label;
        new_row_button.className = 'wicket-mfu-delete-button';

        img = document.createElement('span');
        img.className = 'wicket-mfu-delete-button-inner';
        new_row_button.appendChild(img);

        // References
        new_row.element = element;

        // Delete function
        new_row_button.onclick = function() {
            var selector = this.parentNode.element.multi_selector;

            selector.removeElement(this.parentNode);

            // Decrement counter
            selector.count--;

            if (selector.count < 8) {
                selector.list_container.className = 'wicket-mfu-row-container';
            }

            // Re-enable input element (if it's disabled)
            selector.current_element.disabled = false;

            //remove empty list and label form DOM if count == 0
            if (selector.count === 0) {
                selector.reset();
            }

            // Appease Safari
            //    without it Safari wants to reload the browser window
            //    which nixes your already queued uploads
            return false;
        };

        // Set row value
        label = document.createElement("span");
        label.innerHTML = this.parseFilename(element.value);
        label.className = 'wicket-mfu-row-label';
        new_row.appendChild(label);

        // Add button
        new_row.appendChild(new_row_button);

        // Add it to the list
        this.appendToList(new_row);

        this.count++;
    };

    this.appendToList = function(row) {
        if (this.list_container === null || this.list_container === undefined) {
            this.list_label = document.createElement("div");
            this.list_label.className = 'wicket-mfu-row-container-label';
            this.list_target.appendChild(this.list_label);
            this.list_label.innerHTML = this.listLabel;

            this.list_container = document.createElement("div");
            this.list_container.className = 'wicket-mfu-row-container';
            this.list_target.appendChild(this.list_container);
        }
        this.list_container.appendChild(row);
    };

    this.reset = function() {
        var i;

        if (this.list_label !== null) {
            this.list_target.removeChild(this.list_label);
            this.list_label = null;
        }

        if (this.list_container !== null) {
            this.list_target.removeChild(this.list_container);
            this.list_container = null;
        }

        for (i = 0; i < this.selected_items.length; i++) {
            this.selected_items[i].parentNode.removeChild(this.selected_items[i]);
        }
        this.selected_items = [];
        this.count = 0;

        //Reset input field. In IE the property is readonly and requires cloning the node which introduces issues
        //with the custom onchange handler.
        this.current_element.value = '';
        if (!YAHOO.env.ua.ie) {
            this.current_element.value = '';
        }
        // Re-enable input element (if it's disabled)
        this.current_element.disabled = false;
    };

    this.removeElement = function(parent) {
        var i;
        // Remove from selected_items
        for (i = 0; i < this.selected_items.length; i++) {
            if (this.selected_items[i] === parent.element) {
                this.selected_items.splice(i, 1);
                break;
            }
        }

        // Remove element from form
        parent.element.parentNode.removeChild(parent.element);

        // Remove this row from the list
        parent.parentNode.removeChild(parent);

    };

    this.parseFilename = function(filename) {
        if (YAHOO.env.ua.ie) {
            //IE passes in the whole filepath, strip it.
            var s = filename.split('\\');
            if (s.length > 0) {
                filename = s[s.length - 1];
            }
        }
        if (filename.length > this.maxLengthFilename) {
            filename = filename.substr(filename.length - this.maxLengthFilename, this.maxLengthFilename);
        }
        return filename;
    };

}
