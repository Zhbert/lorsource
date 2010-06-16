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

package ru.org.linux.spring.boxlets;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.PollNotFoundException;
import ru.org.linux.spring.CacheableController;
import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.spring.dao.PollDTO;
import ru.org.linux.spring.dao.PollDaoImpl;
import ru.org.linux.spring.dao.VoteDTO;

@Controller
public class PollBoxletImpl extends SpringBoxlet implements CacheableController {
  private CacheProvider cacheProvider;
  private PollDaoImpl pollDao;

  public PollDaoImpl getPollDao() {
    return pollDao;
  }

  @Autowired
  public void setPollDao(PollDaoImpl pollDao) {
    this.pollDao = pollDao;
  }

  @Autowired
  public void setCacheProvider(CacheProvider cacheProvider) {
    this.cacheProvider = cacheProvider;
  }

  @Override
  @RequestMapping("/poll.boxlet")
  protected ModelAndView getData(HttpServletRequest request, HttpServletResponse response) {
    final PollDTO poll = getFromCache(cacheProvider, getCacheKey() + "poll", new GetCommand<PollDTO>() {
      @Override
      public PollDTO get() {
        try {
          return pollDao.getCurrentPoll();
        } catch (PollNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    });

    List<VoteDTO> votes = getFromCache(cacheProvider, getCacheKey() + "votes", new GetCommand<List<VoteDTO>>() {
      @Override
      public List<VoteDTO> get() {
        return pollDao.getVoteDTO(poll.getId());
      }
    });

    Integer count = getFromCache(cacheProvider, getCacheKey() + "count", new GetCommand<Integer>() {
      @Override
      public Integer get() {
        return pollDao.getVotersCount(poll.getId());
      }
    });

    ModelAndView result = new ModelAndView("boxlets/poll");
    result.addObject("poll", poll);
    result.addObject("votes", votes);
    result.addObject("count", count);
    return result;
  }

  @Override
  public int getExpiryTime() {
    return super.getExpiryTime() * 2;
  }
}
