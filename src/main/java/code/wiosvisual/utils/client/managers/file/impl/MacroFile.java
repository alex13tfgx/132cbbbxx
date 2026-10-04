/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonIOException
 *  com.google.gson.JsonSyntaxException
 */
package code.wiosvisual.utils.client.managers.file.impl;

import code.wiosvisual.common.repository.macro.Macro;
import code.wiosvisual.common.repository.macro.MacroRepository;
import code.wiosvisual.utils.client.managers.file.ClientFile;
import code.wiosvisual.utils.client.managers.file.exception.FileLoadException;
import code.wiosvisual.utils.client.managers.file.exception.FileSaveException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

public class MacroFile
extends ClientFile {
    private final MacroRepository macroRepository;

    public MacroFile(MacroRepository macroRepository) {
        super("macro");
        this.macroRepository = macroRepository;
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(path, this.getName() + ".json");
        try (FileWriter writer = new FileWriter(file);){
            gson.toJson(this.macroRepository.macroList, (Appendable)writer);
        }
        catch (JsonIOException | IOException e) {
            throw new FileSaveException(String.format("Failed to save %s to file", this.getName()), e);
        }
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        Gson gson = new Gson();
        File file = new File(path, this.getName() + ".json");
        try (FileReader reader = new FileReader(file);){
            Macro[] macros = (Macro[])gson.fromJson((Reader)reader, Macro[].class);
            this.macroRepository.macroList.clear();
            this.macroRepository.macroList.addAll(Arrays.asList(macros));
        }
        catch (IOException e) {
            throw new FileLoadException(String.format("Failed to load %s from file", this.getName()), e);
        }
        catch (JsonSyntaxException e) {
            throw new FileLoadException(String.format("JSON syntax error, %s config cannot be loaded", this.getName()), e);
        }
        catch (JsonIOException e) {
            throw new FileLoadException(String.format("JSON IO error, %s config cannot be loaded", this.getName()), e);
        }
    }
}
