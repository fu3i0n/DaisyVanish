package me.quantiom.advancedvanish.util

import me.quantiom.advancedvanish.AdvancedVanish
import org.bukkit.Bukkit
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner
import java.util.function.Consumer

object UpdateChecker {
    private const val REPO_OWNER = "fu3i0n"
    private const val REPO_NAME = "AdvancedVanish"

    fun getVersion(consumer: Consumer<String?>) {
        Bukkit.getScheduler().runTaskAsynchronously(
            AdvancedVanish.instance!!,
            Runnable {
                try {
                    // Fetch the latest release dynamically
                    val url = URL("https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connect()
                    val responseCode = connection.responseCode

                    if (responseCode == 200) {
                        connection.inputStream.use { inputStream ->
                            Scanner(inputStream).use { scanner ->
                                val response = scanner.useDelimiter("\\A").next()
                                val parser = JSONParser()
                                val json = parser.parse(response) as JSONObject
                                val latestVersion = json["tag_name"] as String
                                consumer.accept(latestVersion)
                                checkForUpdate(latestVersion)
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

    private fun checkForUpdate(latestVersion: String) {
        val currentVersion = AdvancedVanish.instance?.description?.version ?: return

        if (isUpdateAvailable(currentVersion, latestVersion)) {
            AdvancedVanish.instance!!.logger.info("Update available! Current version: $currentVersion, Latest version: $latestVersion")
        } else {
            AdvancedVanish.instance!!.logger.info("You are running the latest version ($currentVersion).")
        }
    }

    private fun isUpdateAvailable(
        currentVersion: String,
        latestVersion: String,
    ): Boolean = compareVersions(currentVersion, latestVersion) < 0

    private fun compareVersions(
        version1: String,
        version2: String,
    ): Int {
        val versionParts1 = version1.split(".").map { it.toInt() }
        val versionParts2 = version2.split(".").map { it.toInt() }
        val maxLength = maxOf(versionParts1.size, versionParts2.size)

        for (i in 0 until maxLength) {
            val part1 = versionParts1.getOrElse(i) { 0 }
            val part2 = versionParts2.getOrElse(i) { 0 }
            if (part1 != part2) return part1 - part2
        }
        return 0
    }
}
