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

import code.wiosvisual.features.impl.misc.Globals;
import code.wiosvisual.features.impl.render.ClickGui;
import code.wiosvisual.features.impl.render.NoRender;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleRepository;
import code.wiosvisual.features.module.setting.Setting;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.GroupSetting;
import code.wiosvisual.features.module.setting.implement.MultiSelectSetting;
import code.wiosvisual.features.module.setting.implement.RadioSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.features.module.setting.implement.TextSetting;
import code.wiosvisual.utils.client.managers.api.draggable.DraggableRepository;
import code.wiosvisual.utils.client.managers.file.ClientFile;
import code.wiosvisual.utils.client.managers.file.exception.FileLoadException;
import code.wiosvisual.utils.client.managers.file.exception.FileSaveException;
import code.wiosvisual.utils.client.server.ServerRestrictions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModuleFile
extends ClientFile {
    private static final Logger LOGGER = LogManager.getLogger("Wiosvisual");

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final ModuleRepository moduleRepository;

    public ModuleFile(ModuleRepository moduleRepository, DraggableRepository draggableRepository) {
        super("autoCfg");
        this.moduleRepository = moduleRepository;
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        this.saveToFile(path, this.getName() + ".json");
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        this.loadFromFile(path, this.getName() + ".json");
    }

    @Override
    public void saveToFile(File path, String fileName) throws FileSaveException {
        JsonObject functionObject = this.createJsonObjectFromModules();
        File file = new File(path, fileName);
        this.writeJsonToFile(functionObject, file);
        super.saveToFile(path, fileName);
    }

    @Override
    public void loadFromFile(File path, String fileName) throws FileLoadException {
        File file = new File(path, fileName);
        JsonObject functionObject = this.readJsonFromFile(file);
        if (functionObject != null) {
            this.updateModulesFromJsonObject(functionObject);
        }
        ClickGui.enforceOpenKey();
        Globals.enforceEnabled();
        NoRender.enforceDefaults();
        super.loadFromFile(path, fileName);
    }

    private JsonObject createJsonObjectFromModules() {
        JsonObject functionObject = new JsonObject();
        for (Module module : this.moduleRepository.modules()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.addProperty("bind", (Number)module.getKey());
            if (module.getSecondKey() != -1 && module.getSecondKey() != -1) {
                moduleObject.addProperty("secondBind", (Number)module.getSecondKey());
            }
            // Выключенное запретом сервера сохраняем как включённое: иначе fail-closed LiteAPI затирал бы конфиг.
            moduleObject.addProperty("state", Boolean.valueOf(module.isState() || ServerRestrictions.isSuppressed(module)));
            module.settings().forEach(setting -> this.addSettingToJsonObject(moduleObject, (Setting)setting));
            functionObject.add(module.getName().toLowerCase(), (JsonElement)moduleObject);
        }
        return functionObject;
    }

    private void addSettingToJsonObject(JsonObject moduleObject, Setting setting) {
        if (setting instanceof BooleanSetting) {
            BooleanSetting booleanSetting = (BooleanSetting)setting;
            moduleObject.addProperty(setting.getName(), Boolean.valueOf(booleanSetting.isValue()));
        }
        if (setting instanceof SliderSettings) {
            SliderSettings valueSetting = (SliderSettings)setting;
            moduleObject.addProperty(setting.getName(), (Number)Float.valueOf(valueSetting.getValue()));
        }
        if (setting instanceof ColorSetting) {
            ColorSetting colorSetting = (ColorSetting)setting;
            moduleObject.addProperty(setting.getName(), (Number)colorSetting.getColor());
        }
        if (setting instanceof BindSetting) {
            BindSetting bindSetting = (BindSetting)setting;
            moduleObject.addProperty(setting.getName(), (Number)bindSetting.getKey());
            if (bindSetting.isCombo()) {
                moduleObject.addProperty(setting.getName() + "_second", (Number)bindSetting.getSecondKey());
            }
        }
        if (setting instanceof TextSetting) {
            TextSetting textSetting = (TextSetting)setting;
            moduleObject.addProperty(setting.getName(), textSetting.getText());
        }
        if (setting instanceof SelectSetting) {
            SelectSetting selectSetting = (SelectSetting)setting;
            moduleObject.addProperty(setting.getName(), selectSetting.getSelected());
        }
        if (setting instanceof RadioSetting) {
            RadioSetting radioSetting = (RadioSetting)setting;
            moduleObject.addProperty(setting.getName(), radioSetting.get());
        }
        if (setting instanceof MultiSelectSetting) {
            MultiSelectSetting multiSelectSetting = (MultiSelectSetting)setting;
            List<String> selected = multiSelectSetting.getSelected();
            String selectedAsString = String.join((CharSequence)",", selected);
            moduleObject.addProperty(setting.getName(), selectedAsString);
        }
        if (setting instanceof GroupSetting) {
            GroupSetting groupSetting = (GroupSetting)setting;
            JsonObject groupObject = new JsonObject();
            groupObject.addProperty("state", Boolean.valueOf(groupSetting.isValue()));
            for (Setting subSetting : groupSetting.getSubSettings()) {
                this.addSettingToJsonObject(groupObject, subSetting);
            }
            moduleObject.add(setting.getName(), (JsonElement)groupObject);
        }
    }

    private void writeJsonToFile(JsonObject functionObject, File file) throws FileSaveException {
        try (FileWriter writer = new FileWriter(file);){
            this.GSON.toJson((JsonElement)functionObject, (Appendable)writer);
        }
        catch (IOException e) {
            throw new FileSaveException("Failed to save module to file", e);
        }
    }

    private JsonObject readJsonFromFile(File file) throws FileLoadException {
        JsonObject jsonObject;
        try (FileReader reader = new FileReader(file)) {
            jsonObject = JsonParser.parseReader((Reader)reader).getAsJsonObject();
        }
        catch (IOException e) {
            throw new FileLoadException("Failed to load module from file", e);
        }
        catch (JsonIOException | JsonSyntaxException e) {
            throw new FileLoadException("Failed to parse JSON from file", e);
        }
        return jsonObject;
    }

    public void resetConfiguration(File configDirectory) throws FileSaveException, FileLoadException {
        JsonObject resetJson;
        File defaultFile = new File(configDirectory, "default.json");
        for (Module module : this.moduleRepository.modules()) {
            if (module instanceof ClickGui || module instanceof Globals) continue;
            if (module.isState()) {
                module.setState(false);
            }
            module.setKey(-1);
            module.setSecondKey(-1);
            for (Setting setting : module.settings()) {
                this.resetSettingToDefault(setting);
            }
        }
        JsonObject clickGuiSnapshot = this.createJsonObjectFromModules().getAsJsonObject("clickgui");
        if (defaultFile.exists()) {
            resetJson = this.readJsonFromFile(defaultFile);
            for (String moduleKey : resetJson.keySet()) {
                if (!resetJson.get(moduleKey).isJsonObject() || "clickgui".equals(moduleKey)) continue;
                if ("globals".equals(moduleKey)) {
                    JsonObject globalsObject = resetJson.getAsJsonObject(moduleKey);
                    globalsObject.addProperty("state", Boolean.valueOf(true));
                    continue;
                }
                JsonObject moduleObject = resetJson.getAsJsonObject(moduleKey);
                moduleObject.addProperty("state", Boolean.valueOf(false));
                moduleObject.addProperty("bind", -1);
                moduleObject.remove("secondBind");
            }
            if (resetJson.has("attackaura")) {
                resetJson.getAsJsonObject("attackaura").addProperty("\u0420\u043e\u0442\u0430\u0446\u0438\u044f", "Smooth");
            }
            if (resetJson.has("aura")) {
                JsonObject auraConfig = resetJson.getAsJsonObject("aura");
                resetJson.add("attackaura", (JsonElement)auraConfig);
                resetJson.remove("aura");
            }
        } else {
            resetJson = this.createJsonObjectFromModules();
        }
        if (clickGuiSnapshot != null) {
            clickGuiSnapshot.addProperty("bind", (Number)344);
            clickGuiSnapshot.addProperty("state", Boolean.valueOf(true));
            clickGuiSnapshot.remove("secondBind");
            resetJson.add("clickgui", (JsonElement)clickGuiSnapshot);
        }
        this.updateModulesFromJsonObject(resetJson);
        ClickGui.enforceOpenKey();
        Globals.enforceEnabled();
        NoRender.enforceDefaults();
        this.writeJsonToFile(resetJson, new File(configDirectory, "autoCfg.json"));
    }

    private void resetSettingToDefault(Setting setting) {
    }

    private void deleteConfigFiles(File configDirectory) {
        File[] files = configDirectory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".json")) continue;
            file.delete();
        }
    }

    private void updateModulesFromJsonObject(JsonObject functionObject) {
        for (Module module : this.moduleRepository.modules()) {
            JsonObject moduleObject = functionObject.getAsJsonObject(module.getName().toLowerCase());
            if (moduleObject == null) continue;
            if (module instanceof ClickGui) {
                if (moduleObject.has("state")) {
                    module.setState(moduleObject.get("state").getAsBoolean());
                }
            } else if (module instanceof Globals) {
                if (moduleObject.has("bind")) {
                    module.setKey(moduleObject.get("bind").getAsInt());
                    if (moduleObject.has("secondBind")) {
                        module.setSecondKey(moduleObject.get("secondBind").getAsInt());
                    } else {
                        module.setSecondKey(-1);
                    }
                }
                module.setState(true);
            } else if (moduleObject.has("bind") && moduleObject.has("state")) {
                module.setKey(moduleObject.get("bind").getAsInt());
                if (moduleObject.has("secondBind")) {
                    module.setSecondKey(moduleObject.get("secondBind").getAsInt());
                } else {
                    module.setSecondKey(-1);
                }
                module.setState(moduleObject.get("state").getAsBoolean());
            }
            module.settings().forEach(setting -> this.updateSettingFromJsonObject(moduleObject, (Setting)setting));
        }
        ClickGui.enforceOpenKey();
        Globals.enforceEnabled();
    }

    /**
     * Настройки сопоставляются с конфигом по имени, а тип настройки со временем меняется
     * (галочка становится слайдером и наоборот). Старый конфиг тогда отдаёт значение
     * чужого типа, поэтому перед чтением проверяем, что в JSON лежит именно то, что мы
     * ждём: не подошло — оставляем значение по умолчанию, и при следующем сохранении
     * конфиг сам себя починит. Раньше такой конфиг ронял клиент на старте.
     */
    private void updateSettingFromJsonObject(JsonObject moduleObject, Setting setting) {
        try {
            this.applySettingFromJsonObject(moduleObject, setting);
        }
        catch (RuntimeException broken) {
            // Ни одна испорченная строка конфига не стоит падения всего клиента.
            LOGGER.warn("Настройка '{}' пропущена: значение в конфиге не подходит по типу", setting.getName());
        }
    }

    /** Лежит ли в элементе число: строки и булевы значения к числу не приводим. */
    private static boolean isNumber(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
    }

    /** Лежит ли в элементе строка. */
    private static boolean isString(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    private void applySettingFromJsonObject(JsonObject moduleObject, Setting setting) {
        JsonElement settingElement = moduleObject.get(setting.getName());
        if (settingElement == null || settingElement.isJsonNull()) {
            return;
        }
        if (setting instanceof BooleanSetting) {
            BooleanSetting booleanSetting = (BooleanSetting)setting;
            if (settingElement.isJsonPrimitive() && settingElement.getAsJsonPrimitive().isBoolean()) {
                booleanSetting.setValue(settingElement.getAsBoolean());
            }
        }
        if (setting instanceof SliderSettings) {
            SliderSettings valueSetting = (SliderSettings)setting;
            if (ModuleFile.isNumber(settingElement)) {
                valueSetting.setValue(settingElement.getAsFloat());
            }
        }
        if (setting instanceof ColorSetting) {
            ColorSetting colorSetting = (ColorSetting)setting;
            if (ModuleFile.isNumber(settingElement)) {
                colorSetting.setColor(settingElement.getAsInt());
            }
        }
        if (setting instanceof BindSetting) {
            BindSetting bindSetting = (BindSetting)setting;
            try {
                if (settingElement.isJsonPrimitive() && settingElement.getAsJsonPrimitive().isBoolean()) {
                    bindSetting.setKey(-1);
                } else {
                    bindSetting.setKey(settingElement.getAsInt());
                }
                JsonElement secondKeyElement = moduleObject.get(setting.getName() + "_second");
                if (secondKeyElement != null && !secondKeyElement.isJsonNull()) {
                    bindSetting.setSecondKey(secondKeyElement.getAsInt());
                }
            }
            catch (NumberFormatException e) {
                bindSetting.setKey(-1);
            }
        }
        if (setting instanceof TextSetting) {
            TextSetting textSetting = (TextSetting)setting;
            if (ModuleFile.isString(settingElement)) {
                textSetting.setText(settingElement.getAsString());
            }
        }
        if (setting instanceof SelectSetting) {
            SelectSetting selectSetting = (SelectSetting)setting;
            if (ModuleFile.isString(settingElement)) {
                selectSetting.setSelected(settingElement.getAsString());
            }
        }
        if (setting instanceof RadioSetting) {
            RadioSetting radioSetting = (RadioSetting)setting;
            if (!ModuleFile.isString(settingElement)) {
                return;
            }
            String value = settingElement.getAsString();
            if ("\u0420\u043e\u0442\u0430\u0446\u0438\u044f".equals(setting.getName())) {
                value = this.migrateAuraRotation(value, moduleObject);
            }
            for (String option : radioSetting.getOptions()) {
                if (!option.equals(value)) continue;
                radioSetting.set(value);
                break;
            }
        }
        if (setting instanceof MultiSelectSetting) {
            MultiSelectSetting multiSelectSetting = (MultiSelectSetting)setting;
            String asString = settingElement.getAsString();
            ArrayList<String> selectedList = new ArrayList<String>(Arrays.asList(asString.split(",")));
            selectedList.removeIf(s -> !multiSelectSetting.getList().contains(s));
            multiSelectSetting.setSelected(selectedList);
        }
        if (setting instanceof GroupSetting) {
            GroupSetting groupSetting = (GroupSetting)setting;
            JsonObject groupObject = settingElement.getAsJsonObject();
            if (groupObject.has("state")) {
                groupSetting.setValue(groupObject.get("state").getAsBoolean());
            }
            for (Setting subSetting : groupSetting.getSubSettings()) {
                this.updateSettingFromJsonObject(groupObject, subSetting);
            }
        }
    }

    private String migrateAuraRotation(String value, JsonObject moduleObject) {
        if ("NoRotate".equals(value)) {
            return "Smooth";
        }
        if ("Sloth 2".equals(value) || "Astrum".equals(value)) {
            return "Sloth";
        }
        return value;
    }
}
