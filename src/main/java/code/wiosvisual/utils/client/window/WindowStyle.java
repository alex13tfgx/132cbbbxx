/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.platform.win32.WinDef$HWND
 *  org.lwjgl.glfw.GLFWNativeWin32
 */
package code.wiosvisual.utils.client.window;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import org.lwjgl.glfw.GLFWNativeWin32;

public class WindowStyle {
    public static void setDarkMode(long windowHandle) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            return;
        }
        long hwnd = GLFWNativeWin32.glfwGetWin32Window((long)windowHandle);
        WinDef.HWND hwndJna = new WinDef.HWND(new Pointer(hwnd));
        int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
        Memory darkModeEnabled = new Memory(4L);
        darkModeEnabled.setInt(0L, 1);
        DwmApi.INSTANCE.DwmSetWindowAttribute(hwndJna, DWMWA_USE_IMMERSIVE_DARK_MODE, darkModeEnabled, 4);
    }

    public static interface DwmApi
    extends StdCallLibrary {
        public static final DwmApi INSTANCE = Native.loadLibrary("dwmapi", DwmApi.class);

        public int DwmSetWindowAttribute(WinDef.HWND var1, int var2, Pointer var3, int var4);
    }
}
