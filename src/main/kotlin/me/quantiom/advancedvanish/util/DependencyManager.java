package me.quantiom.advancedvanish.util;

import com.google.common.collect.Lists;
import me.quantiom.advancedvanish.AdvancedVanishPlugin;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;

import java.util.List;
import java.util.logging.Level;

public final class DependencyManager {
    private final AdvancedVanishPlugin plugin;
    private final BukkitLibraryManager libraryManager;

    public DependencyManager(AdvancedVanishPlugin plugin) {
        this.plugin = plugin;
        this.libraryManager = new BukkitLibraryManager(plugin);
        this.libraryManager.addMavenCentral();
    }

    private Library getLibrary(String groupId, String artifactId, String version, String pattern, String relocatePattern) {
        Library.Builder builder = Library.builder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version);

        if (!relocatePattern.isEmpty()) {
            builder.relocate(pattern, relocatePattern);
        }

        return builder.build();
    }

    public void loadDependencies() {
        this.plugin.getLogger().log(Level.INFO, "Loading dependencies...");

        // this is very weird, but I think without a way to exclude specific files/classes from
        // being replaced by the shade plugin, this would be the only way to not have the string "kotlin" replaced
        String kotlinStr = "ko";
        kotlinStr += "tlin";

        List<Library> libraries = Lists.newArrayList(
                // redis
                this.getLibrary("redis{}clients", "jedis", "4.2.0", "", ""),
                this.getLibrary("org{}apache{}commons", "commons-pool2", "2.11.1", "", ""),
                this.getLibrary("org{}json", "json", "20211205", "", ""),
                // kotlin
                this.getLibrary("org{}jetbrains{}" + kotlinStr, kotlinStr + "-stdlib", "1.7.10", kotlinStr, "me.quantiom.advancedvanish.shaded.kotlin")
        );

        for (Library library : libraries) {
            this.libraryManager.loadLibrary(library);
        }

        this.plugin.getLogger().log(Level.INFO, "Successfully loaded dependencies.");
    }
}
