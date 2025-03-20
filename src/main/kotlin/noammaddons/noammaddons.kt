package noammaddons

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.commands.CommandManager.registerCommands
import noammaddons.config.*
import noammaddons.events.PreKeyInputEvent
import noammaddons.events.RegisterEvents
import noammaddons.features.FeatureManager.registerFeatures
import noammaddons.features.misc.ClientBranding
import noammaddons.features.misc.Cosmetics.CosmeticRendering
import noammaddons.utils.*
import org.apache.logging.log4j.LogManager


@Mod(
    modid = noammaddons.MOD_ID,
    name = noammaddons.MOD_NAME,
    version = noammaddons.MOD_VERSION,
    clientSideOnly = true
)
class noammaddons {
    private var loadTime = 0L

    companion object {
        const val MOD_NAME = "@NAME@"
        const val MOD_ID = "@MODID@"
        const val MOD_VERSION = "@VER@"
        const val CHAT_PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"
        const val FULL_PREFIX = "§d§l§nNo§lamm§b§l§nAddons"
        const val DEBUG_PREFIX = "§8[§b§lN§d§lA §7DEBUG§8]"

        @JvmField
        val Logger = LogManager.getLogger(MOD_NAME)

        @JvmStatic
        val mc = Minecraft.getMinecraft()

        val scope = CoroutineScope(Dispatchers.Default)

        @JvmField
        val config = Config
        val hudData = PogObject("hudData", DataClasses.HudElementConfig())
        private val firstLoad = PogObject("firstLoad", true)
        val personalBests = PogObject("PersonalBests", DataClasses.PersonalBestData())

        @JvmField
        val ahData = mutableMapOf<String, Double>()

        @JvmField
        val bzData = mutableMapOf<String, DataClasses.bzitem>()

        @JvmField
        val npcData = mutableMapOf<String, Double>()

        @JvmField
        val itemIdToNameLookup = mutableMapOf<String, String>()
        var mayorData: DataClasses.Mayor? = null
    }

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {
        Logger.info("Initializing NoammAddons")
        loadTime = System.currentTimeMillis()
        config.initialize()

        SoundUtils.initSounds()
        ClientBranding.setCustomIcon()
        ClientBranding.setCustomTitle()

        KeyBinds.allBindings.forEach(ClientRegistry::registerKeyBinding)

        listOf(
            this, RegisterEvents, ThreadUtils,
            TestGround, GuiUtils, ScanUtils,
            LocationUtils, DungeonUtils,
            ActionBarParser, PartyUtils,
            ChatUtils, EspUtils, ActionUtils,
            TablistListener
        ).forEach(MinecraftForge.EVENT_BUS::register)

        mc.renderManager.skinMap.let {
            it["slim"]?.run { addLayer(CosmeticRendering(this)) }
            it["default"]?.run { addLayer(CosmeticRendering(this)) }
        }

        registerFeatures()
        registerCommands()
        this.init()

        Logger.info("Finished Initializing NoammAddons")
        Logger.info("Load Time: ${(System.currentTimeMillis() - loadTime) / 1000.0} seconds")
    }

    @SubscribeEvent
    fun onKey(event: PreKeyInputEvent) {
        if (! firstLoad.getData()) return
        firstLoad.setData(false)
        Utils.playFirstLoadMessage()
        ThreadUtils.setTimeout(11_000) {
            GuiUtils.openScreen(config.gui())
            config.openDiscordLink()
        }
    }

    fun init() {
        ThreadUtils.loop(600_000) {
            UpdateUtils.update()

            JsonUtils.fetchJsonWithRetry<Map<String, Double>>("https://moulberry.codes/lowestbin.json") {
                it ?: return@fetchJsonWithRetry
                ahData.clear()
                ahData.putAll(it)
            }

            JsonUtils.fetchJsonWithRetry<Map<String, DataClasses.bzitem>>("https://sky.shiiyu.moe/api/v2/bazaar") {
                it ?: return@fetchJsonWithRetry
                bzData.clear()
                bzData.putAll(it)
            }

            JsonUtils.get("https://api.hypixel.net/resources/skyblock/items") { obj ->
                if (obj["success"]?.jsonPrimitive?.booleanOrNull != true) return@get

                val items = JsonUtils.json.decodeFromJsonElement(
                    ListSerializer(DataClasses.APISBItem.serializer()),
                    obj["items"] !!
                )

                val sellPrices = items.filter {
                    it.npcSellPrice != null
                }.associate { it.id to it.npcSellPrice !! }.toMutableMap()

                val idToName = items.associate { it.id to it.name }.toMutableMap()

                npcData.clear()
                npcData.putAll(sellPrices)
                itemIdToNameLookup.clear()
                itemIdToNameLookup.putAll(idToName)
            }

            JsonUtils.get("https://soopy.dev/api/v2/mayor") { jsonObject ->
                if (jsonObject["success"]?.jsonPrimitive?.booleanOrNull != true) return@get
                val dataElement = jsonObject["data"] ?: return@get
                val data = JsonUtils.json.decodeFromJsonElement(DataClasses.APIMayor.serializer(), dataElement)
                val perks = data.perks?.map { it["name"] !! } ?: emptyList()
                mayorData = DataClasses.Mayor(data.name, perks)
            }
        }
    }
}