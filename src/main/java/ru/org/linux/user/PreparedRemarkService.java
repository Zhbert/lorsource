/*
 * Copyright 1998-2015 Linux.org.ru
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

package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PreparedRemarkService {

  @Autowired
  private UserDao userDao;

  public List<PreparedRemark> prepareRemarkList(List<Remark> list) throws Exception {
    if (list.isEmpty()) {
      return ImmutableList.of();
    }

    List<PreparedRemark> remarksPrepared = new ArrayList<>(list.size());
    for (Remark remark : list) {
      User refUser = userDao.getUserCached(remark.getRefUserId());
      remarksPrepared.add(new PreparedRemark(remark,refUser));
    }

    return remarksPrepared;
  }

}
