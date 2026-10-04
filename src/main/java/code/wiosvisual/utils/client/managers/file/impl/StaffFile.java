/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonIOException
 *  com.google.gson.JsonSyntaxException
 *  com.google.gson.reflect.TypeToken
 */
package code.wiosvisual.utils.client.managers.file.impl;

import code.wiosvisual.common.repository.staff.StaffRepository;
import code.wiosvisual.utils.client.managers.file.ClientFile;
import code.wiosvisual.utils.client.managers.file.exception.FileLoadException;
import code.wiosvisual.utils.client.managers.file.exception.FileSaveException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class StaffFile
extends ClientFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public StaffFile() {
        super("staff");
    }

    @Override
    public void loadFromFile(File directory) throws FileLoadException {
        File file = new File(directory, this.getName() + ".json");
        if (!file.exists()) {
            return;
        }
        try {
            String content = Files.readString(file.toPath());
            if (content.isEmpty()) {
                return;
            }
            List<StaffRepository.Staff> staff = GSON.fromJson(content, new TypeToken<List<StaffRepository.Staff>>(){}.getType());
            if (staff == null) {
                return;
            }
            StaffRepository.getStaff().clear();
            StaffRepository.getStaff().addAll(staff);
        }
        catch (IOException e) {
            throw new FileLoadException("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u0442\u044c \u0444\u0430\u0439\u043b \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u043b\u0430", e);
        }
        catch (JsonSyntaxException e) {
            throw new FileLoadException("JSON syntax error, staff config cannot be loaded", e);
        }
        catch (JsonIOException e) {
            throw new FileLoadException("JSON IO error, staff config cannot be loaded", e);
        }
    }

    @Override
    public void saveToFile(File directory) throws FileSaveException {
        File file = new File(directory, this.getName() + ".json");
        try {
            Files.writeString(file.toPath(), (CharSequence)GSON.toJson(StaffRepository.getStaff()), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException e) {
            throw new FileSaveException("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0444\u0430\u0439\u043b \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u043b\u0430", e);
        }
    }
}
