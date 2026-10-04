/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1799
 *  net.minecraft.class_2561
 *  net.minecraft.class_476
 *  net.minecraft.class_9290
 *  net.minecraft.class_9323
 *  net.minecraft.class_9334
 *  org.apache.commons.lang3.StringUtils
 */
package code.wiosvisual.utils.features.price;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;

public class PriceParser {
    private final Pattern funTimePricePattern = Pattern.compile("\\$\\s*(\\d+(?:[,\\s]\\d{3})*(?:\\.\\d{2})?)");

    public int getPrice(ItemStack stack) {
        LoreComponent lore;
        ComponentMap tag = stack.getComponents();
        if (tag == null) {
            return -1;
        }
        String componentString = tag.toString();
        String price = StringUtils.substringBetween((String)componentString, (String)"literal{ $", (String)"}[style={color=green}]");
        if ((price == null || price.isEmpty()) && (lore = (LoreComponent)stack.get(DataComponentTypes.LORE)) != null && !lore.lines().isEmpty()) {
            String lastFoundPrice = null;
            for (Text line : lore.lines()) {
                String text = line.getString();
                Matcher matcher = this.funTimePricePattern.matcher(text);
                while (matcher.find()) {
                    lastFoundPrice = matcher.group(1);
                }
            }
            if (lastFoundPrice != null && !lastFoundPrice.isEmpty()) {
                price = lastFoundPrice;
            }
        }
        if (price == null || price.isEmpty()) {
            return -1;
        }
        try {
            price = price.replaceAll("[\\s,]", "");
            return Integer.parseInt(price);
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }

    public int getSpookyTimePrice(ItemStack stack, GenericContainerScreen screen) {
        if (screen == null) {
            return -1;
        }
        String title = screen.getTitle().getString();
        if (!title.contains("\u0410\u0443\u043a\u0446\u0438\u043e\u043d") && !title.contains("\u041f\u043e\u0438\u0441\u043a:")) {
            return -1;
        }
        LoreComponent lore = (LoreComponent)stack.get(DataComponentTypes.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            for (Text line : lore.lines()) {
                String text = line.getString();
                if (!text.contains("\u0426\u0435\u043d\u0430:") && !text.contains("Price:")) continue;
                try {
                    int priceIndex = Math.max(text.indexOf("\u0426\u0435\u043d\u0430:"), text.indexOf("Price:"));
                    if (priceIndex != -1) {
                        String priceText = text.substring(priceIndex);
                        String priceStr = priceText.replaceAll("[^0-9.]", "").trim();
                        try {
                            return (int)Double.parseDouble(priceStr);
                        }
                        catch (NumberFormatException e) {
                            try {
                                return Integer.parseInt(priceStr);
                            }
                            catch (NumberFormatException e2) {
                                return -1;
                            }
                        }
                    }
                    return -1;
                }
                catch (Exception e) {
                    return -1;
                }
            }
        }
        return -1;
    }
}
