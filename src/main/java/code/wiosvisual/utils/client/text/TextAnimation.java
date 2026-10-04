/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.text;

import code.wiosvisual.Wiosvisual;
import java.util.List;

public class TextAnimation {
    private String currentText = "";
    private int currentMessageIndex = 0;
    private int animationTick = 0;
    private boolean isRemoving = false;
    private boolean showUnderscore = true;
    private int underscoreTick = 0;
    private final int delayTicks = 2;
    private final int pauseTicksMax = 60;
    private int pauseTicks = 0;
    private final int underscoreBlinkTicks = 10;

    private List<String> buildMessages() {
        String username = "Unknown";
        try {
            if (Wiosvisual.getInstance() != null && Wiosvisual.getInstance().getNativeUsername() != null) {
                username = Wiosvisual.getInstance().getNativeUsername();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return List.of("Glad to see you again, " + username + "! Come on in, everything's ready for you", "Welcome back, " + username + "! Ready to dive into the adventure?", "Hey " + username + ", let's make some epic moments today!", username + ", the game awaits your legendary skills!");
    }

    public void updateText() {
        if (this.pauseTicks > 0) {
            --this.pauseTicks;
            this.updateUnderscore();
            return;
        }
        if (this.animationTick >= 2) {
            List<String> messages = this.buildMessages();
            String fullText = messages.get(this.currentMessageIndex);
            if (this.isRemoving) {
                if (this.currentText.length() > 0) {
                    this.currentText = this.currentText.substring(0, this.currentText.length() - 1);
                } else {
                    this.isRemoving = false;
                    this.currentMessageIndex = (this.currentMessageIndex + 1) % messages.size();
                    this.pauseTicks = 60;
                }
            } else if (this.currentText.length() < fullText.length()) {
                this.currentText = fullText.substring(0, this.currentText.length() + 1);
            } else {
                this.isRemoving = true;
                this.pauseTicks = 60;
            }
            this.animationTick = 0;
        }
        ++this.animationTick;
        this.updateUnderscore();
    }

    private void updateUnderscore() {
        ++this.underscoreTick;
        if (this.underscoreTick >= 10) {
            this.showUnderscore = !this.showUnderscore;
            this.underscoreTick = 0;
        }
    }

    public String getCurrentText() {
        return this.currentText + (this.showUnderscore ? "_" : "");
    }
}
