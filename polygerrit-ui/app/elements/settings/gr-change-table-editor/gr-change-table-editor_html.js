/**
 * @license
 * Copyright (C) 2020 The Android Open Source Project
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
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

export const htmlTemplate = html`
    <style include="shared-styles">
      /* Workaround for empty style block - see https://github.com/Polymer/tools/issues/408 */
    </style>
    <style include="gr-form-styles">
      #changeCols {
        width: auto;
      }
      #changeCols .visibleHeader {
        text-align: center;
      }
      .checkboxContainer {
        cursor: pointer;
        text-align: center;
      }
      .checkboxContainer input {
        cursor: pointer;
      }
      .checkboxContainer:hover {
        outline: 1px solid var(--border-color);
      }
    </style>
    <div class="gr-form-styles">
      <table id="changeCols">
        <thead>
          <tr>
            <th class="nameHeader">Column</th>
            <th class="visibleHeader">Visible</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Number</td>
            <td class="checkboxContainer" on-click="_handleCheckboxContainerClick">
              <input type="checkbox" name="number" on-click="_handleNumberCheckboxClick" checked\$="[[showNumber]]">
            </td>
          </tr>
          <template is="dom-repeat" items="[[columnNames]]">
            <tr>
              <td>[[item]]</td>
              <td class="checkboxContainer" on-click="_handleCheckboxContainerClick">
                <input type="checkbox" name="[[item]]" on-click="_handleTargetClick" checked\$="[[!isColumnHidden(item, displayedColumns)]]">
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
`;
