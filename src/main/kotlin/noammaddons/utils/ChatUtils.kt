package noammaddons.utils


import gg.essential.api.EssentialAPI
import gg.essential.universal.UChat
import gg.essential.universal.UChat.addColor
import gg.essential.universal.wrappers.message.UTextComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.util.*
import net.minecraft.util.ChatAllowedCharacters.*
import net.minecraft.util.StringUtils.*
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.events.RegisterEvents.postAndCatch
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.noammaddons.Companion.DEBUG_PREFIX
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.noammaddons.Companion.Logger
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.scope
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.PartyUtils.isInParty
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawTitle
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.ThreadUtils.scheduledTask
import noammaddons.utils.Utils.send
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.*
import kotlin.math.roundToInt


object ChatUtils {
    data class title(val title: Any?, val subtitle: Any?, var time: Long, val rainbow: Boolean)

    private val titles = mutableListOf<title>()
    private var startTime = 0L
    private var ping = 0
    private var isCalculatingPing = false


    fun getChatBreak(separator: String = "-"): String? {
        val len = getStringWidth(separator)
        val times = mc.ingameGUI?.chatGUI?.chatWidth?.div(len)

        return times?.let { "-".repeat(it.toInt()) }
    }

    fun sendFakeChatMessage(message: String) {
        val formattedMessage = message.addColor()
        modMessage("Simulating Chat Message: $formattedMessage")
        postAndCatch(Chat(ChatComponentText(formattedMessage)))
    }

    fun copyToClipboard(text: String) {
        val stringSelection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
    }

    fun getCenteredText(text: String): String {
        val textWidth = getStringWidth(text.addColor())
        val chatWidth = mc.ingameGUI?.chatGUI?.chatWidth ?: 0

        if (textWidth >= chatWidth) return text

        val spaceWidth = (chatWidth - textWidth) / 2f
        val spaceBuilder = StringBuilder().apply {
            repeat((spaceWidth / getStringWidth(" ")).roundToInt()) {
                append(' ')
            }
        }

        return spaceBuilder.append(text).toString()
    }

    fun removeUnicode(input: String): String {
        return input.replace(Regex("[^\\u0000-\\u007F§]"), "")
    }

    fun String.removeFormatting(): String = UTextComponent.stripFormatting(this.addColor())

    fun String.addColor(): String = addColor(this)

    fun modMessage(message: Any?) = UChat.chat("$CHAT_PREFIX ${message.toString().addColor()}")

    fun debugMessage(message: Any) {
        if (! config.DevMode) return

        stripControlCodes(message.toString())

        UChat.chat("$DEBUG_PREFIX&r ${message.toString().addColor()}")
        Logger.debug(message.toString().removeFormatting())
    }

    fun errorMessage(message: List<Any?>) {
        val msg = message.toMutableList()

        listOf(
            "&b&m${getChatBreak()?.substring(1)}",
            getCenteredText("$FULL_PREFIX&r"),
            ""
        ).reversed().forEach { msg.add(0, it) }

        listOf(
            "",
            "&b&m${getChatBreak()?.substring(1)}",
        ).reversed().forEach { msg.add(it) }

        val msgF = msg.joinToString("\n") { "$it".addColor() }
        UChat.chat(msgF)
        Logger.error(msgF.removeFormatting())
    }


    private const val MESSAGE_DELAY_MS = 1000L
    private val messageQueue: Queue<String> = LinkedList()
    private var lastMessageTime = 0L

    fun sendChatMessage(message: Any) {
        val formattedMessage = filterAllowedCharacters("$message".removeFormatting())
        messageQueue.offer(formattedMessage)
        processQueue()
    }

    private fun processQueue() {
        if (messageQueue.isEmpty()) return
        val timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime
        if (timeSinceLastMessage >= MESSAGE_DELAY_MS) sendNextMessage()
        else scheduleNextCheck(MESSAGE_DELAY_MS - timeSinceLastMessage)
    }

    private fun sendNextMessage() {
        val nextMessage = messageQueue.poll() ?: return
        mc.thePlayer?.sendChatMessage(nextMessage)
    }

    private fun scheduleNextCheck(delay: Long) {
        scheduledTask((delay / 50).toInt()) { processQueue() }
    }


    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Sent) {
        if (event.packet !is C01PacketChatMessage) return
        lastMessageTime = System.currentTimeMillis()
        processQueue()
    }


    fun sendPartyMessage(message: Any) {
        if (isInParty()) sendChatMessage("/pc ${message.toString().removeFormatting()}")
    }

    // sometimes the text is still formatted, Thanks Minecraft
    val IChatComponent.noFormatText get() = unformattedText.removeFormatting()

    /**
     * Sends a message to the user that they can click and run an action.
     *
     * @param msg The message to be sent.
     * @param cmd The command to be executed when the message is clicked.
     * @param hover The string to be shown when the message is hovered, default "Click here!".
     * @param prefix Whether to prefix the message with the chat prefix, default true.
     */
    fun clickableChat(msg: String, cmd: String = "", hover: String = "§eClick me!", prefix: Boolean = true) {
        val msgPrefix = if (prefix) "$CHAT_PREFIX " else ""
        val textComponent = ChatComponentText(msgPrefix + msg.addColor())

        val command = if (cmd.startsWith("/")) cmd.addColor() else "/${cmd.addColor()}"

        textComponent.chatStyle = ChatStyle().apply {
            if (cmd.isNotBlank()) chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
            chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(hover.addColor()))
        }

        mc.thePlayer?.addChatMessage(textComponent)
    }

    fun addRandomColorCodes(inputString: String): String {
        val colorCodes = listOf("§6", "§c", "§e", "§f")
        val result = StringBuilder()
        var lastColor: String? = null

        for (char in inputString.removeFormatting()) {
            val availableColors = colorCodes.filter { it != lastColor }
            val randomColor = availableColors.random()
            result.append(randomColor).append(char).append("§r")
            lastColor = randomColor
        }

        return result.toString()
    }

    /**
     * Display a title.
     *
     * @param title title text
     * @param subtitle subtitle text
     * @param time time to stay on screen in seconds
     */
    @Synchronized
    @Suppress("NAME_SHADOWING")
    fun showTitle(title: Any? = "", subtitle: Any? = "", time: Number = 3f, rainbow: Boolean = false) {
        val title = title.toString()
        val subtitle = subtitle.toString()
        if (title.isBlank() && subtitle.isBlank()) return

        titles.add(title(title, subtitle, time.toLong() * 1000, rainbow))
    }

    init {
        loop(100) {
            titles.forEach { title ->
                title.time -= 100L
            }
            titles.removeIf {
                it.time <= 0
            }
        }
    }

    @SubscribeEvent
    fun renderTitles(event: RenderOverlay) {
        titles.toList().forEach { title ->
            if (title.time <= 0) return

            drawTitle(title.title.toString(), title.subtitle.toString(), title.rainbow)
        }
    }

    fun getPing(callback: (ping: Int) -> Unit) {
        if (! inSkyblock) return
        if (isCalculatingPing) return // Prevent multiple ping checks simultaneously

        C01PacketChatMessage("/Noamm9 is the best!").send()
        isCalculatingPing = true
        startTime = System.currentTimeMillis()

        scope.launch {
            while (isCalculatingPing) delay(1)
            callback(ping)
        }
    }

    @SubscribeEvent
    fun calcPing(event: Chat) {
        if (! event.component.unformattedText.removeFormatting().contains("('Noamm9 is the best!')")) return
        event.isCanceled = true

        ping = (System.currentTimeMillis() - startTime).toInt()
        isCalculatingPing = false
    }

    /**
     * Displays a notification with a custom message and duration.
     *
     * @param title The title of the notification.
     * @param message The message to display in the notification.
     * @param duration The duration in seconds for which the notification should be displayed. Defaults to 3 seconds.
     * @param clickFunction The function to be executed when the notification is clicked. Defaults to an empty function.
     * @param closeFunction The function to be executed when the notification is closed. Defaults to an empty function.
     */
    fun Alert(title: String = FULL_PREFIX, message: String, duration: Int = 3, clickFunction: () -> Unit = {}, closeFunction: () -> Unit = {}) {
        EssentialAPI.getNotifications().push(
            title.addColor(),
            message.addColor(),
            if (duration == - 1) Float.MAX_VALUE
            else duration.toFloat(),
            clickFunction,
            closeFunction
        )
        SoundUtils.notificationSound()
    }
}