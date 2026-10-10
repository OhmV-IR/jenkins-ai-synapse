package io.ohmvir.plugins.jenkinsaisynapse.configuration;

import hudson.XmlFile;
import hudson.util.XStream2;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.jspecify.annotations.Nullable;

/**
 * Reads the configuration files that the management links used to write themselves (for example
 * {@code JENKINS_HOME/models.xml}), so that existing settings carry over to the {@link jenkins.model.GlobalConfiguration}
 * classes that replaced them.
 */
final class LegacyConfigFile {
    private static final Logger LOGGER = Logger.getLogger(LegacyConfigFile.class.getName());

    private LegacyConfigFile() {}

    /**
     * @param fileName The name of the legacy file in {@code JENKINS_HOME}
     * @param legacyType The class that used to be serialized as the root element of the file
     * @param holderType A class with the same field names as {@code legacyType} to read the data into
     * @return The legacy data, or null if there is no legacy file or it could not be read
     */
    static <T> @Nullable T read(String fileName, Class<?> legacyType, Class<T> holderType) {
        File file = getFile(fileName);
        if (!file.isFile()) {
            return null;
        }
        XStream2 xstream = new XStream2();
        xstream.alias(legacyType.getName(), holderType);
        try {
            Object data = new XmlFile(xstream, file).read();
            if (holderType.isInstance(data)) {
                return holderType.cast(data);
            }
            LOGGER.warning("Ignoring legacy configuration file " + file + " with unexpected content");
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Failed to read legacy configuration file " + file, e);
        }
        return null;
    }

    /**
     * Renames the legacy file once its data has been saved in the new location, so it is not migrated again.
     */
    static void markMigrated(String fileName) {
        File file = getFile(fileName);
        File migrated = new File(file.getParentFile(), fileName + ".migrated");
        try {
            Files.move(file.toPath(), migrated.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Migrated " + file + " (the old file was kept as " + migrated.getName() + ")");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to rename migrated configuration file " + file, e);
        }
    }

    private static File getFile(String fileName) {
        return new File(Jenkins.get().getRootDir(), fileName);
    }
}
