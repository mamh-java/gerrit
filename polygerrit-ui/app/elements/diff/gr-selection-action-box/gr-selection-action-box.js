/**
 * @license
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
import '../../../scripts/bundled-polymer.js';

import '../../../behaviors/fire-behavior/fire-behavior.js';
import '../../../behaviors/keyboard-shortcut-behavior/keyboard-shortcut-behavior.js';
import '../../../styles/shared-styles.js';
import '../../shared/gr-tooltip/gr-tooltip.js';
import {flush} from '@polymer/polymer/lib/legacy/polymer.dom.js';
import {mixinBehaviors} from '@polymer/polymer/lib/legacy/class.js';
import {GestureEventListeners} from '@polymer/polymer/lib/mixins/gesture-event-listeners.js';
import {LegacyElementMixin} from '@polymer/polymer/lib/legacy/legacy-element-mixin.js';
import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {htmlTemplate} from './gr-selection-action-box_html.js';

/**
 * @appliesMixin Gerrit.FireMixin
 * @extends Polymer.Element
 */
class GrSelectionActionBox extends mixinBehaviors( [
  Gerrit.FireBehavior,
], GestureEventListeners(
    LegacyElementMixin(
        PolymerElement))) {
  static get template() { return htmlTemplate; }

  static get is() { return 'gr-selection-action-box'; }
  /**
   * Fired when the comment creation action was taken (click).
   *
   * @event create-comment-requested
   */

  static get properties() {
    return {
      keyEventTarget: {
        type: Object,
        value() { return document.body; },
      },
      positionBelow: Boolean,
    };
  }

  /** @override */
  created() {
    super.created();

    // See https://crbug.com/gerrit/4767
    this.addEventListener('mousedown',
        e => this._handleMouseDown(e));
  }

  placeAbove(el) {
    flush();
    const rect = this._getTargetBoundingRect(el);
    const boxRect = this.$.tooltip.getBoundingClientRect();
    const parentRect = this._getParentBoundingClientRect();
    this.style.top =
        rect.top - parentRect.top - boxRect.height - 6 + 'px';
    this.style.left =
        rect.left - parentRect.left + (rect.width - boxRect.width) / 2 + 'px';
  }

  placeBelow(el) {
    flush();
    const rect = this._getTargetBoundingRect(el);
    const boxRect = this.$.tooltip.getBoundingClientRect();
    const parentRect = this._getParentBoundingClientRect();
    this.style.top =
    rect.top - parentRect.top + boxRect.height - 6 + 'px';
    this.style.left =
    rect.left - parentRect.left + (rect.width - boxRect.width) / 2 + 'px';
  }

  _getParentBoundingClientRect() {
    // With native shadow DOM, the parent is the shadow root, not the gr-diff
    // element
    const parent = this.parentElement || this.parentNode.host;
    return parent.getBoundingClientRect();
  }

  _getTargetBoundingRect(el) {
    let rect;
    if (el instanceof Text) {
      const range = document.createRange();
      range.selectNode(el);
      rect = range.getBoundingClientRect();
      range.detach();
    } else {
      rect = el.getBoundingClientRect();
    }
    return rect;
  }

  _handleMouseDown(e) {
    if (e.button !== 0) { return; } // 0 = main button
    e.preventDefault();
    e.stopPropagation();
    this.fire('create-comment-requested');
  }
}

customElements.define(GrSelectionActionBox.is, GrSelectionActionBox);
