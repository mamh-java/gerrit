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
import '../test/common-test-setup-karma.js';
import {isValidDate, parseDate, fromNow, isWithinDay, isWithinHalfYear, formatDate} from './date-util.js';

suite('date-util tests', () => {
  suite('parseDate', () => {
    test('parseDate server date', () => {
      const parsed = parseDate('2015-09-15 20:34:00.000000000');
      assert.equal('2015-09-15T20:34:00.000Z', parsed.toISOString());
    });
  });

  suite('isValidDate', () => {
    test('date is valid', () => {
      assert.isTrue(isValidDate(new Date()));
    });
    test('broken date is invalid', () => {
      assert.isFalse(isValidDate(new Date('xxx')));
    });
  });

  suite('fromNow', () => {
    test('test all variants', () => {
      const fakeNow = new Date('May 08 2020 12:00:00');
      sinon.useFakeTimers(fakeNow.getTime());
      assert.equal('just now', fromNow(new Date('May 08 2020 11:59:30')));
      assert.equal('a minute ago', fromNow(new Date('May 08 2020 11:59:00')));
      assert.equal('5 minutes ago', fromNow(new Date('May 08 2020 11:55:00')));
      assert.equal('an hour ago', fromNow(new Date('May 08 2020 11:00:00')));
      assert.equal('3 hours ago', fromNow(new Date('May 08 2020 9:00:00')));
      assert.equal('a day ago', fromNow(new Date('May 07 2020 12:00:00')));
      assert.equal('3 days ago', fromNow(new Date('May 05 2020 12:00:00')));
      assert.equal('a month ago', fromNow(new Date('Apr 05 2020 12:00:00')));
      assert.equal('2 months ago', fromNow(new Date('Mar 05 2020 12:00:00')));
      assert.equal('a year ago', fromNow(new Date('May 05 2019 12:00:00')));
      assert.equal('10 years ago', fromNow(new Date('May 05 2010 12:00:00')));
    });
  });

  suite('isWithinDay', () => {
    test('basics works', () => {
      assert.isTrue(isWithinDay(new Date('May 08 2020 12:00:00'),
          new Date('May 08 2020 02:00:00')));
      assert.isFalse(isWithinDay(new Date('May 08 2020 12:00:00'),
          new Date('May 07 2020 12:00:00')));
    });
  });

  suite('isWithinHalfYear', () => {
    test('basics works', () => {
      assert.isTrue(isWithinHalfYear(new Date('May 08 2020 12:00:00'),
          new Date('Feb 08 2020 12:00:00')));
      assert.isFalse(isWithinHalfYear(new Date('May 08 2020 12:00:00'),
          new Date('Nov 07 2019 12:00:00')));
    });
  });

  suite('formatDate', () => {
    test('works for standard format', () => {
      const stdFormat = 'MMM DD, YYYY';
      assert.equal('May 08, 2020',
          formatDate(new Date('May 08 2020 12:00:00'), stdFormat));
      assert.equal('Feb 28, 2020',
          formatDate(new Date('Feb 28 2020 12:00:00'), stdFormat));

      const time24Format = 'HH:mm:ss';
      assert.equal('Feb 28, 2020 12:01:12',
          formatDate(new Date('Feb 28 2020 12:01:12'), stdFormat + ' '
          + time24Format));
    });
    test('works for euro format', () => {
      const euroFormat = 'DD.MM.YYYY';
      assert.equal('01.12.2019',
          formatDate(new Date('Dec 01 2019 12:00:00'), euroFormat));
      assert.equal('20.01.2002',
          formatDate(new Date('Jan 20 2002 12:00:00'), euroFormat));

      const time24Format = 'HH:mm:ss';
      assert.equal('28.02.2020 00:01:12',
          formatDate(new Date('Feb 28 2020 00:01:12'), euroFormat + ' '
          + time24Format));
    });
    test('works for iso format', () => {
      const isoFormat = 'YYYY-MM-DD';
      assert.equal('2015-01-01',
          formatDate(new Date('Jan 01 2015 12:00:00'), isoFormat));
      assert.equal('2013-07-03',
          formatDate(new Date('Jul 03 2013 12:00:00'), isoFormat));

      const timeFormat = 'h:mm:ss A';
      assert.equal('2013-07-03 5:00:00 AM',
          formatDate(new Date('Jul 03 2013 05:00:00'), isoFormat + ' '
          + timeFormat));
      assert.equal('2013-07-03 5:00:00 PM',
          formatDate(new Date('Jul 03 2013 17:00:00'), isoFormat + ' '
          + timeFormat));
    });
    test('h:mm:ss A shows correctly midnight and midday', () => {
      const timeFormat = 'h:mm A';
      assert.equal('12:14 PM',
          formatDate(new Date('Jul 03 2013 12:14:00'), timeFormat));
      assert.equal('12:15 AM',
          formatDate(new Date('Jul 03 2013 00:15:00'), timeFormat));
    });
  });
});