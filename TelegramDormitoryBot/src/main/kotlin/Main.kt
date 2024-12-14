package org.example

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

enum class BotCommand(val command: String) {
    START("/start"),
    STOP("/stop"),
    DEVELOPER_INFO("/developer_info"),
    NEWCHAT("/newchat"),
    DELETEBOT("/deletebot"),
    HELP("/help")
}

enum class BannedUsers(val userId: String) {
    USER_1("USER_ID_1"),
    USER_2("USER_ID_2")
}

class SecurityBot : TelegramLongPollingBot() {

    private val logger: Logger = LoggerFactory.getLogger(SecurityBot::class.java)
    private val lastMessageTimes = ConcurrentHashMap<String, LocalDateTime>()

    override fun getBotUsername(): String {
        return "BOT_NAME"
    }

    override fun getBotToken(): String {
        return "YOUR_BOT_TOKEN"
    }

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val message = update.message
            val chatId = message.chatId.toString()
            val text = message.text
            val userName = message.from.userName ?: message.from.firstName
            val userId = message.from.id.toString()
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            val securityChatId = "SECURITY_CHAT_ID"
            val loggingChatId = "LOGGING_CHAT_ID"

            if (chatId != securityChatId && chatId != loggingChatId) {

                if (!BotCommand.values().any { it.command == text }) {

                    if (!BannedUsers.values().any { it.userId == userId }) {

                        val lastMessageTime = lastMessageTimes[userId]
                        if (lastMessageTime == null || lastMessageTime.plusMinutes(1).isBefore(LocalDateTime.now())) {

                            val forwardedMessage = SendMessage(securityChatId, "\uD83D\uDEA8 Анонимная жалоба: $text")


                            val loggingMessage = SendMessage(loggingChatId, "\uD83D\uDEA8 Лог жалобы: $text\nДата и время: $timestamp\nID пользователя: $userId\nИмя пользователя: $userName")

                            try {
                                execute(forwardedMessage)
                                logger.info("Сообщение успешно переслано в чат Стажёры (СООП): $text")

                                execute(loggingMessage)
                                logger.info("Сообщение успешно переслано в чат LogMessageTestBot: $text")

                                lastMessageTimes[userId] = LocalDateTime.now()
                            } catch (e: TelegramApiException) {
                                logger.error("Ошибка при пересылке сообщения", e)
                            }
                        } else {
                            logger.warn("Сообщение от пользователя $userId игнорировано из-за спама")
                        }
                    } else {
                        logger.warn("Сообщение от забаненного пользователя $userId игнорировано")
                    }
                }
            }
        }
    }
}

fun main() {
    val botsApi = TelegramBotsApi(DefaultBotSession::class.java)

    try {
        botsApi.registerBot(SecurityBot())
    } catch (e: TelegramApiException) {
        e.printStackTrace()
    }
}