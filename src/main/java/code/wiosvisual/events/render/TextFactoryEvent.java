/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.events.render;

import code.wiosvisual.utils.client.managers.event.events.Event;

public class TextFactoryEvent
implements Event {
    private String text;

    public void replaceText(String protect, String replaced) {
        if (this.text == null || this.text.isEmpty()) {
            return;
        }
        if (this.text.contains(protect) && (this.text.equalsIgnoreCase(protect) || this.text.contains(protect + " ") || this.text.contains(" " + protect) || this.text.contains("\u23cf" + protect) || this.text.contains(protect + "\u00a7"))) {
            this.text = this.text.replace(protect, replaced);
        }
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public TextFactoryEvent(String text) {
        this.text = text;
    }
}
