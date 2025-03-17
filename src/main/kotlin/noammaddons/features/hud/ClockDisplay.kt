package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ClockDisplay: Feature() {
    private object ClockDisplayElement: GuiElement(hudData.getData().ClockDisplay) {
        override val enabled get() = config.ClockDisplay
        private val text get() = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f

        override fun draw() = drawText(text, getX(), getY(), getScale(), config.ClockDisplayColor)
    }

    @SubscribeEvent
    fun draw(event: RenderOverlay) {
        if (! ClockDisplayElement.enabled) return
        ClockDisplayElement.draw()
    }
}
