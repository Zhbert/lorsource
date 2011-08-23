/*
 * Copyright 1998-2010 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.site;

import org.springframework.validation.Errors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DupeProtector {
  private static final int THRESHOLD = 30000;
  private static final int THRESHOLD_TRUSTED = 3000;
  
  private static final DupeProtector instance = new DupeProtector();

  private final Map<String,Long> hash = new HashMap<String,Long>();

  private DupeProtector() {
  }

  private synchronized boolean check(String ip, boolean trusted) {
    cleanup();

    long current = System.currentTimeMillis();

    if (hash.containsKey(ip)) {
      long date = hash.get(ip);

      if ((current-date)<(trusted?THRESHOLD_TRUSTED:THRESHOLD)) {
        return false;
      }
    }

    hash.put(ip, current);

    return true;
  }

  public void checkDuplication(String ip) throws DuplicationException {
    if (!check(ip,false)) {
      throw new DuplicationException();
    }
  }

  public void checkDuplication(String ip,boolean trusted, Errors errors) throws DuplicationException {
    if (!check(ip,trusted)) {
      errors.reject(null, DuplicationException.MESSAGE);
    }
  }

  private synchronized void cleanup() {
    long current = System.currentTimeMillis();

    for (Iterator<Long> i = hash.values().iterator(); i.hasNext(); ) {
      long date = i.next();

      if ((current-date)>THRESHOLD) {
        i.remove();
      }
    }
  }

  public static DupeProtector getInstance() {
    return instance;
  }
}