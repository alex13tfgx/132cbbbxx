/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonIOException
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.google.gson.JsonSyntaxException
 */
package code.wiosvisual.utils.client.managers.file.impl;

import code.wiosvisual.display.hud.TargetHud;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.client.managers.api.draggable.DraggableRepository;
import code.wiosvisual.utils.client.managers.file.ClientFile;
import code.wiosvisual.utils.client.managers.file.exception.FileLoadException;
import code.wiosvisual.utils.client.managers.file.exception.FileSaveException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

public class ElementsFile
extends ClientFile {
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final DraggableRepository draggableRepository;

    public ElementsFile(DraggableRepository draggableRepository) {
        super("elements");
        this.draggableRepository = draggableRepository;
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        JsonObject elementsObject = new JsonObject();
        for (AbstractDraggable draggable : this.draggableRepository.draggable()) {
            JsonObject draggableObject = new JsonObject();
            int posX = draggable.getX();
            int posY = draggable.getY();
            float scaleFactor = draggable.getScaleFactor();
            draggableObject.addProperty("posX", (Number)posX);
            draggableObject.addProperty("posY", (Number)posY);
            draggableObject.addProperty("scaleFactor", (Number)Float.valueOf(scaleFactor));
            if (draggable instanceof TargetHud) {
                TargetHud targetHud = (TargetHud)draggable;
                draggableObject.addProperty("showOnHover", Boolean.valueOf(targetHud.isShowOnHover()));
            }
            String name = draggable.getName().toLowerCase().replace(" ", "");
            elementsObject.add(name, (JsonElement)draggableObject);
        }
        File file = new File(path, this.getName() + ".json");
        this.writeJsonToFile(elementsObject, file);
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        File file = new File(path, this.getName() + ".json");
        JsonObject elementsObject = this.readJsonFromFile(file);
        if (elementsObject != null) {
            this.updateElementsFromJsonObject(elementsObject);
        }
    }

    private void updateElementsFromJsonObject(JsonObject elementsObject) {
        for (AbstractDraggable draggable : this.draggableRepository.draggable()) {
            String name = draggable.getName().toLowerCase().replace(" ", "");
            JsonObject draggableObject = elementsObject.getAsJsonObject(name);
            if (draggableObject == null) continue;
            if (draggableObject.has("posX") && draggableObject.has("posY")) {
                int posX = draggableObject.get("posX").getAsInt();
                int posY = draggableObject.get("posY").getAsInt();
                draggable.setX(posX);
                draggable.setY(posY);
            }
            if (draggableObject.has("scaleFactor")) {
                float scaleFactor = draggableObject.get("scaleFactor").getAsFloat();
                draggable.setScaleFactor(scaleFactor);
            } else {
                draggable.setScaleFactor(1.0f);
            }
            if (!(draggable instanceof TargetHud)) continue;
            TargetHud targetHud = (TargetHud)draggable;
            if (!draggableObject.has("showOnHover")) continue;
            boolean showOnHover = draggableObject.get("showOnHover").getAsBoolean();
            targetHud.setShowOnHover(showOnHover);
        }
    }

    private void writeJsonToFile(JsonObject jsonObject, File file) throws FileSaveException {
        try (FileWriter writer = new FileWriter(file);){
            this.GSON.toJson((JsonElement)jsonObject, (Appendable)writer);
        }
        catch (IOException e) {
            throw new FileSaveException("Failed to save elements to file", e);
        }
    }

    private JsonObject readJsonFromFile(File file) throws FileLoadException {
        JsonObject jsonObject;
        if (!file.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(file)) {
            jsonObject = JsonParser.parseReader((Reader)reader).getAsJsonObject();
        }
        catch (IOException e) {
            throw new FileLoadException("Failed to load elements from file", e);
        }
        catch (JsonIOException | JsonSyntaxException e) {
            throw new FileLoadException("Failed to parse JSON from file", e);
        }
        return jsonObject;
    }
}
