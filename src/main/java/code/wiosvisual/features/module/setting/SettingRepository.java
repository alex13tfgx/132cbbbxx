/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 */
package code.wiosvisual.features.module.setting;

import code.wiosvisual.features.module.setting.Setting;
import code.wiosvisual.utils.client.interfaces.Setupable;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;

public class SettingRepository
implements Setupable {
    private final List<Setting> settings = Lists.newArrayList();

    @Override
    public final void setup(Setting ... setting) {
        this.settings.addAll(Arrays.asList(setting));
    }

    public Setting get(String name) {
        return this.settings.stream().filter(setting -> setting.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public List<Setting> settings() {
        return this.settings;
    }
}
