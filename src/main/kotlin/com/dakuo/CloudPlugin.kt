package com.dakuo


import io.papermc.paper.plugin.entrypoint.Entrypoint
import io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader
import io.papermc.paper.plugin.manager.PaperPluginManagerImpl
import io.papermc.paper.plugin.provider.classloader.PaperClassLoaderStorage
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta
import io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProvider
import org.bukkit.Bukkit
import org.bukkit.plugin.PluginDescriptionFile
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.info
import taboolib.platform.BukkitPlugin
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import java.security.ProtectionDomain
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

object CloudPlugin : Plugin() {

    var plugin: org.bukkit.plugin.Plugin? = null

    override fun onEnable() {
        val classLoader = this.javaClass.classLoader

        val url = URLClassLoader(arrayOf(URL("https://souts.cn/upload/ExampleCloudPlugin-1.0-SNAPSHOT.jar")))
        val pluginYaml = url.getResourceAsStream("plugin.yml")
        val jarOutputStream = JarOutputStream(File(getDataFolder(), "ExampleCloudPlugin.jar").outputStream())
        val jarEntry = JarEntry("plugin.yml")
        jarOutputStream.putNextEntry(jarEntry)
        val readBytes = pluginYaml.readBytes()
        jarOutputStream.write(readBytes)
        jarOutputStream.finish()
        jarOutputStream.flush()
        jarOutputStream.close()

        val pluginDescriptionFile = PluginDescriptionFile(readBytes.inputStream())
        val mainClazz = url.getResourceAsStream(pluginDescriptionFile.main.replace(".","/").plus(".class"))
        val readAllBytes = mainClazz.readAllBytes()

        val providingPlugin = BukkitPlugin.getProvidingPlugin(this::class.java)

        val paperPluginMeta = PaperPluginMeta.create(readBytes.inputStream().bufferedReader())


        PaperPluginClassLoader(
            Bukkit.getLogger(),
            Paths.get(getDataFolder().toURI()),
            JarFile(File(getDataFolder(),"ExampleCloudPlugin.jar")),
            paperPluginMeta,
            classLoader.parent,
            classLoader.parent as URLClassLoader
        ).also {
            PaperClassLoaderStorage.instance().registerOpenGroup(it)
            val clazz = defineClass(it as ClassLoader, pluginDescriptionFile.main, readAllBytes, 0, readAllBytes.size)
            val jarFile = JarFile(File(getDataFolder(), "ExampleCloudPlugin.jar"))
            val create = SpigotPluginProvider.FACTORY.build(jarFile,pluginDescriptionFile,Paths.get(getDataFolder().toURI()))
            create.setContext(PaperPluginManagerImpl.getInstance())
            val newInstance = create.createInstance()
            LaunchEntryPointHandler.INSTANCE.register(Entrypoint.PLUGIN,create)
            PaperPluginManagerImpl.getInstance().loadPlugin(newInstance as org.bukkit.plugin.Plugin)
            PaperPluginManagerImpl.getInstance().enablePlugin(newInstance as org.bukkit.plugin.Plugin)
        }


        info("云插件加载程序已运行!")
    }

    override fun onDisable() {
        plugin?.let { Bukkit.getPluginManager().disablePlugin(it) }
    }

    fun defineClass(
        classLoader: ClassLoader?,
        name: String?,
        data: ByteArray?,
        offset: Int,
        len: Int,
        domain: ProtectionDomain? = null
    ): Class<*> {
        val method = ClassLoader::class.java.getDeclaredMethod(
            "defineClass",
            String::class.java,
            ByteArray::class.java, Integer.TYPE, Integer.TYPE,
            ProtectionDomain::class.java
        )
        method.isAccessible = true
        val clazz = method.invoke(classLoader, name, data, offset, len, domain) as Class<*>
        method.isAccessible = false
        return clazz
    }
}