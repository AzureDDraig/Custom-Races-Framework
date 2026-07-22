package ddraig.net.customraces.pack;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ZIP Exporter and Importer manager for sharing race packs across servers and singleplayer worlds.
 */
public class PackManager {

    public static File getExportsDir() {
        File dir = new File("config/custom_races/exports");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getImportsDir() {
        File dir = new File("config/custom_races/imports");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static boolean exportPack(String packName) {
        File exportFile = new File(getExportsDir(), packName.endsWith(".zip") ? packName : packName + ".zip");
        File racesFile = new File("config/custom_races/races.json");
        if (!racesFile.exists()) return false;

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(exportFile))) {
            // Add races.json
            zos.putNextEntry(new ZipEntry("races.json"));
            try (FileInputStream fis = new FileInputStream(racesFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
            zos.closeEntry();
            return true;
        } catch (Exception e) {
            System.err.println("[CustomRaces] Failed to export pack: " + e.getMessage());
            return false;
        }
    }

    public static boolean importPack(String fileName) {
        File importFile = new File(getImportsDir(), fileName.endsWith(".zip") ? fileName : fileName + ".zip");
        if (!importFile.exists()) return false;

        File targetDir = new File("config/custom_races");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(importFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(targetDir, entry.getName());
                if (!entry.isDirectory()) {
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
            return true;
        } catch (Exception e) {
            System.err.println("[CustomRaces] Failed to import pack: " + e.getMessage());
            return false;
        }
    }
}
