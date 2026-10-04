/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.main;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FontGenerator {
    public static final String CHARSET = "\"!\\\"#$%&\\\\()*+,-./ 0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0401\u0410\u0411\u0412\u0413\u0414\u0415\u0416\u0417\u0418\u0419\u041a\u041b\u041c\u041d\u041e\u041f\u0420\u0421\u0422\u0423\u0424\u0425\u0426\u0427\u0428\u0429\u042a\u042b\u042c\u042d\u042e\u042f\u0430\u0431\u0432\u0433\u0434\u0435\u0436\u0437\u0438\u0439\u043a\u043b\u043c\u043d\u043e\u043f\u0440\u0441\u0442\u0443\u0444\u0445\u0446\u0447\u0448\u0449\u044a\u044b\u044c\u044d\u044e\u044f\u0451\"";

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream((OutputStream)System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream((OutputStream)System.err, true, StandardCharsets.UTF_8));
        }
        catch (Exception exception) {
            // empty catch block
        }
        String fontFolderPath = "mtsdf-font/";
        Path outputPath = FontGenerator.initFile(fontFolderPath);
        if (outputPath == null) {
            System.err.println("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0438\u043d\u0438\u0446\u0438\u0430\u043b\u0438\u0437\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u043f\u0430\u043f\u043a\u0443 \u0432\u044b\u0432\u043e\u0434\u0430.");
            return;
        }
        FontGenerator.generate(fontFolderPath, outputPath, "ghost");
    }

    private static void generate(String fontFolderPath, Path outputPath, String fontPath) {
        File fontFolder = new File(fontFolderPath + fontPath);
        File[] fontFiles = fontFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".otf"));
        if (fontFiles != null && fontFiles.length > 0) {
            File atlasGenExe = new File(fontFolderPath + "atlas-gen.exe");
            if (!atlasGenExe.exists()) {
                System.err.println("\u041d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d atlas-gen.exe \u0432 \u043f\u0430\u043f\u043a\u0435: " + atlasGenExe.getAbsolutePath());
                return;
            }
            ExecutorService executor = Executors.newFixedThreadPool(fontFiles.length);
            ArrayList<Process> processes = new ArrayList<Process>();
            for (File fontFile : fontFiles) {
                executor.execute(() -> {
                    try {
                        String fontFileName = fontFile.getName().replaceFirst("[.][^.]+$", "");
                        Path charsetPath = Path.of(fontFolderPath + "charset.txt", new String[0]).toAbsolutePath();
                        Path imageOutPath = outputPath.resolve(fontFileName.toLowerCase().replaceAll("-", "_") + ".png").toAbsolutePath();
                        Path jsonOutPath = outputPath.resolve(fontFileName.toLowerCase().replaceAll("-", "_") + ".json").toAbsolutePath();
                        ProcessBuilder processBuilder = new ProcessBuilder(atlasGenExe.getAbsolutePath(), "-font", fontFile.getAbsolutePath(), "-charset", charsetPath.toString(), "-type", "mtsdf", "-format", "png", "-imageout", imageOutPath.toString(), "-json", jsonOutPath.toString(), "-size", "64", "-pxrange", "10");
                        Process process = processBuilder.start();
                        processes.add(process);
                        int exitCode = process.waitFor();
                        if (exitCode == 0) {
                            System.out.println("\u0410\u0442\u043b\u0430\u0441 \u0434\u043b\u044f \u0448\u0440\u0438\u0444\u0442\u0430 " + fontFileName + " \u0443\u0441\u043f\u0435\u0448\u043d\u043e \u0441\u043e\u0437\u0434\u0430\u043d.");
                        } else {
                            System.out.println("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u0441\u043e\u0437\u0434\u0430\u043d\u0438\u0438 \u0430\u0442\u043b\u0430\u0441\u0430 \u0434\u043b\u044f \u0448\u0440\u0438\u0444\u0442\u0430 " + fontFileName);
                        }
                    }
                    catch (IOException | InterruptedException e) {
                        System.err.println("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u0438 \u043a\u043e\u043c\u0430\u043d\u0434\u044b \u0434\u043b\u044f \u0448\u0440\u0438\u0444\u0442\u0430 " + fontFile.getName() + ": " + e.getMessage());
                    }
                });
            }
            executor.shutdown();
            try {
                if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    System.out.println("\u041f\u0440\u043e\u0446\u0435\u0441\u0441 \u0437\u0430\u0432\u0435\u0440\u0448\u0451\u043d.");
                }
            }
            catch (InterruptedException e) {
                System.err.println("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u043e\u0436\u0438\u0434\u0430\u043d\u0438\u0438 \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u0438\u044f \u043f\u043e\u0442\u043e\u043a\u043e\u0432: " + e.getMessage());
            }
            for (Process process : processes) {
                process.destroy();
            }
        } else {
            System.out.println("\u041d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u044b \u0444\u0430\u0439\u043b\u044b \u0448\u0440\u0438\u0444\u0442\u043e\u0432 \u0432 \u0443\u043a\u0430\u0437\u0430\u043d\u043d\u043e\u0439 \u043f\u0430\u043f\u043a\u0435.");
        }
    }

    private static Path initFile(String fontFolderPath) {
        Path charsetPath;
        Path outputPath = Path.of(fontFolderPath + "out", new String[0]);
        if (Files.notExists(outputPath, new LinkOption[0])) {
            try {
                Files.createDirectories(outputPath, new FileAttribute[0]);
            }
            catch (IOException e) {
                System.err.println("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u0441\u043e\u0437\u0434\u0430\u043d\u0438\u0438 \u043f\u0430\u043f\u043a\u0438: " + e.getMessage());
                return null;
            }
        }
        if (Files.notExists(charsetPath = Path.of(fontFolderPath + "charset.txt", new String[0]), new LinkOption[0])) {
            try {
                Files.createFile(charsetPath, new FileAttribute[0]);
                Files.write(charsetPath, CHARSET.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
            }
            catch (IOException e) {
                System.err.println("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u0441\u043e\u0437\u0434\u0430\u043d\u0438\u0438 charset.txt: " + e.getMessage());
                return null;
            }
        }
        return outputPath;
    }
}
