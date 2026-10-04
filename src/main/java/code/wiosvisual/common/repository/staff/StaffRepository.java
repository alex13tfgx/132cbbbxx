/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.common.repository.staff;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.utils.client.managers.file.FileController;
import code.wiosvisual.utils.client.managers.file.impl.StaffFile;
import java.util.ArrayList;
import java.util.List;

public class StaffRepository {
    private static final List<Staff> staffList = new ArrayList<Staff>();

    public static void addStaff(String name) {
        if (!StaffRepository.isStaff(name)) {
            staffList.add(new Staff(name));
        }
    }

    public static void removeStaff(String name) {
        staffList.removeIf(staff -> staff.getName().equalsIgnoreCase(name));
    }

    public static boolean isStaff(String name) {
        return staffList.stream().anyMatch(staff -> staff.getName().equalsIgnoreCase(name));
    }

    public static void clear() {
        staffList.clear();
    }

    public static List<Staff> getStaff() {
        return staffList;
    }

    private static void save() {
        FileController fileController = Wiosvisual.getInstance().getFileController();
        if (fileController != null) {
            fileController.saveFile(StaffFile.class);
        }
    }

    public record Staff(String name) {
        public String getName() {
            return this.name;
        }
    }
}
