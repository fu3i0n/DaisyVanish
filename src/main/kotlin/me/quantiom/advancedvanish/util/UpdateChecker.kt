package me.quantiom.advancedvanish.util

import me.quantiom.advancedvanish.AdvancedVanish
import org.bukkit.Bukkit
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner
import java.util.function.Consumer

object UpdateChecker {
    private const val REPO_OWNER = "fu3i0n"
    private const val REPO_NAME = "AdvancedVanish"
    private const val RELEASE_TAG = "1.2.8"

    fun getVersion(consumer: Consumer<String?>) {
        Bukkit.getScheduler().runTaskAsynchronously(
            AdvancedVanish.instance!!,
            Runnable {
                try {
                    val url = URL("https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/tags/$RELEASE_TAG")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connect()

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        connection.inputStream.use { inputStream ->
                            Scanner(inputStream).use { scanner ->
                                val response = scanner.useDelimiter("\\A").next()
                                val json = JSONObject(response)
                                val latestVersion = json.getString("tag_name")
                                consumer.accept(latestVersion)
                            }
                        }
                    } else {
                        AdvancedVanish.instance!!.logger.info("Unable to check for updates: HTTP $responseCode")
                    }
                } catch (exception: IOException) {
                    AdvancedVanish.instance!!.logger.info("Unable to check for updates: " + exception.message)
                }
            },
        )
    }
}