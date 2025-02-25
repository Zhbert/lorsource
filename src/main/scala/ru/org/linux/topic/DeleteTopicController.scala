/*
 * Copyright 1998-2022 Linux.org.ru
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

package ru.org.linux.topic

import com.typesafe.scalalogging.StrictLogging
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.auth.{AccessViolationException, AuthUtil}
import ru.org.linux.group.GroupPermissionService
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.section.SectionService
import ru.org.linux.user.{User, UserDao, UserErrorException}

import scala.jdk.CollectionConverters._

@Controller
class DeleteTopicController(searchQueueSender: SearchQueueSender, sectionService: SectionService,
                                         messageDao: TopicDao, topicService: TopicService,
                                         prepareService: TopicPrepareService,
                                         permissionService: GroupPermissionService,
                                         userDao: UserDao) extends StrictLogging {
  private def checkUndeletable(topic: Topic, currentUser: User): Unit = {
    if (!permissionService.isUndeletable(topic, currentUser)) {
      throw new AccessViolationException("это сообщение нельзя восстановить")
    }
  }

  @RequestMapping(value = Array("/delete.jsp"), method = Array(RequestMethod.GET))
  def showForm(@RequestParam("msgid") msgid: Int): ModelAndView = AuthorizedOnly { currentUser =>
    val msg = messageDao.getById(msgid)

    if (msg.isDeleted) {
      throw new UserErrorException("Сообщение уже удалено")
    }

    if (!permissionService.isDeletable(msg, currentUser.user)) {
      throw new AccessViolationException("Вы не можете удалить это сообщение")
    }

    val section = sectionService.getSection(msg.getSectionId)

    new ModelAndView("delete", Map[String, Any](
      "bonus" -> (!section.isPremoderated && !msg.isDraft && !msg.isExpired),
      "author" -> userDao.getUser(msg.getAuthorUserId),
      "msgid" -> msgid,
      "draft" -> msg.isDraft,
      "uncommited" -> (section.isPremoderated && !msg.isCommited)
    ).asJava)
  }

  @RequestMapping(value = Array("/delete.jsp"), method = Array(RequestMethod.POST))
  def deleteMessage(@RequestParam("msgid") msgid: Int, @RequestParam("reason") reason: String,
                    @RequestParam(value = "bonus", defaultValue = "0") bonus: Int): ModelAndView = AuthorizedOnly { currentUser =>
    val user = currentUser.user

    val message = messageDao.getById(msgid)
    if (message.isDeleted) {
      throw new UserErrorException("Сообщение уже удалено")
    }

    if (!permissionService.isDeletable(message, user)) {
      throw new AccessViolationException("Вы не можете удалить это сообщение")
    }

    topicService.deleteWithBonus(message, user, reason, bonus)
    logger.info(s"Удалено сообщение $msgid пользователем ${user.getNick} по причине `$reason'")

    searchQueueSender.updateMessage(msgid, true)

    new ModelAndView("action-done", "message", "Сообщение удалено")
  }

  @RequestMapping(value = Array("/undelete"), method = Array(RequestMethod.GET))
  def undeleteForm(@RequestParam msgid: Int): ModelAndView = AuthorizedOnly { currentUser =>
    val message = messageDao.getById(msgid)
    checkUndeletable(message, currentUser.user)

    new ModelAndView("undelete", Map(
      "message" -> message,
      "preparedMessage" -> prepareService.prepareTopic(message, currentUser.user)
    ).asJava)
  }

  @RequestMapping(value = Array("/undelete"), method = Array(RequestMethod.POST))
  def undelete(@RequestParam msgid: Int): ModelAndView = AuthorizedOnly { currentUser =>
    val message = messageDao.getById(msgid)
    checkUndeletable(message, AuthUtil.getCurrentUser)

    if (message.isDeleted) {
      messageDao.undelete(message)

      logger.info(s"Восстановлено сообщение $msgid пользователем ${currentUser.user.getNick}")

      searchQueueSender.updateMessage(msgid, true)
    }

    new ModelAndView("action-done", "message", "Сообщение восстановлено")
  }
}
