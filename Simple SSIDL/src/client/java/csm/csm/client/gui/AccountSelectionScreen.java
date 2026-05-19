package csm.csm.client.gui;

import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;
import csm.csm.client.SessionAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccountSelectionScreen extends Screen {

    private static final Pattern TOKEN_REGEX = Pattern.compile("(?:accessToken:\"|token:)?([A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)");
    private static User overriddenUser = null;
    private final Screen parent;
    private EditBox sessionField;
    private String feedbackMessage = "";
    private int feedbackColor = 0xFFFFFFFF;
    private String currentAccountName = "";
    private int centerX, centerY;

    public AccountSelectionScreen(Screen parent) {
        super(Component.literal("Choose Account"));
        this.parent = parent;
        updateCurrentAccountName();
    }

    private void updateCurrentAccountName() {
        User user = Minecraft.getInstance().getUser();
        currentAccountName = (user != null && user.getName() != null) ? user.getName() : "Unknown";
    }

    private long getWindowHandle() {
        try {
            Object windowObj = Minecraft.getInstance().getWindow();
            Class<?> clazz = windowObj.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getType() == long.class) {
                    f.setAccessible(true);
                    long handle = f.getLong(windowObj);
                    if (handle != 0L) return handle;
                }
            }
            String[] possibleNames = {"handle", "windowHandle", "glfwWindow", "field_1643"};
            for (String name : possibleNames) {
                try {
                    Field f = clazz.getDeclaredField(name);
                    f.setAccessible(true);
                    long handle = f.getLong(windowObj);
                    if (handle != 0L) return handle;
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
    }

    private void copyToClipboard(String text) {
        if (text == null) text = "";
        long win = getWindowHandle();
        if (win != 0L) {
            GLFW.glfwSetClipboardString(win, text);
            feedbackMessage = "Copied!";
            feedbackColor = 0xFF00FF00;
        } else {
            feedbackMessage = "Copy failed";
            feedbackColor = 0xFFFF0000;
        }
    }

    private String getClipboardText() {
        long win = getWindowHandle();
        if (win != 0L) {
            String text = GLFW.glfwGetClipboardString(win);
            return text != null ? text : "";
        }
        return "";
    }

    private void login() {
        if (sessionField.getValue().isBlank()) {
            feedbackMessage = "Please enter an SSID!";
            feedbackColor = 0xFF8f0000;
            return;
        }

        String ssidText = parseToken(sessionField.getValue().trim());
        String[] info = null;

        for (int i = 0; i < 10; i++) {
            try {
                info = SessionAPI.getProfileInfo(ssidText);
                break;
            } catch (MalformedJsonException | JsonSyntaxException json) {
                feedbackMessage = "Ran out of retries, network error!";
                feedbackColor = 0xFF8f0000;
                System.err.println("Failed to parse json! Retries left: " + i);
            } catch (IOException e) {
                feedbackMessage = "Failed to poll API for username and UUID!";
                feedbackColor = 0xFF8f0000;
                return;
            } catch (Exception e) {
                feedbackMessage = "Invalid SSID!";
                e.printStackTrace();
                feedbackColor = 0xFF8f0000;
                return;
            }
        }

        if (info == null) return;

        try {
            overriddenUser = new User(info[0], SessionAPI.undashedToUUID(info[1]), ssidText, Optional.empty(), Optional.empty());
        } catch (Exception e) {
            feedbackMessage = "Failed to parse UUID from string!";
            feedbackColor = 0xFF8f0000;
            return;
        }

        updateCurrentAccountName();
        feedbackMessage = "Valid SSID!";
        feedbackColor = 0xFF009405;
    }

    private void reset() {
        overriddenUser = null;
        updateCurrentAccountName();
        feedbackMessage = "Reset SSID.";
        feedbackColor = 0xFFFFFF00;
    }

    private void copySSID() {
        String token = Minecraft.getInstance().getUser().getAccessToken();
        copyToClipboard(token);
    }

    private String parseToken(String input) {
        if (input == null || input.isEmpty()) return "";
        Matcher matcher = TOKEN_REGEX.matcher(input);
        if (matcher.find()) return matcher.group(1);
        return "";
    }

    public static User getOverriddenUser() {
        return overriddenUser;
    }

    @Override
    protected void init() {
        centerX = width / 2;
        centerY = height / 2;

        int totalWidth = 300;
        int textFieldWidth = 240;
        int runButtonWidth = 60;
        int runButtonHeight = 22;
        int gap = 10;
        int yTextField = centerY - 10;
        int yRunButton = yTextField - 1;
        int leftEdge = centerX - totalWidth / 2;

        sessionField = new EditBox(font, leftEdge, yTextField, textFieldWidth, 20,
                                   Component.literal("Paste or enter SSID..."));
        sessionField.setMaxLength(10000);
        this.addRenderableWidget(sessionField);

        this.addRenderableWidget(Button.builder(Component.literal("Run"), btn -> login())
                .bounds(leftEdge + textFieldWidth, yRunButton, runButtonWidth, runButtonHeight).build());

        int buttonWidth = (totalWidth - gap) / 2;
        int yButtons = yTextField + 25;

        this.addRenderableWidget(Button.builder(Component.literal("Copy"), btn -> copySSID())
                .bounds(leftEdge, yButtons, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Paste"), btn -> {
            String clip = getClipboardText();
            if (!clip.isEmpty()) {
                sessionField.setValue(clip);
                feedbackMessage = "Pasted";
                feedbackColor = 0xFF00FF00;
            } else {
                feedbackMessage = "Clipboard empty";
                feedbackColor = 0xFFFF0000;
            }
        }).bounds(leftEdge + buttonWidth + gap, yButtons, buttonWidth, 20).build());

        int yReset = yButtons + 25;
        int resetWidth = buttonWidth;
        this.addRenderableWidget(Button.builder(Component.literal("Reset"), btn -> reset())
                .bounds(centerX - resetWidth / 2, yReset, resetWidth, 20).build());

        int yBack = height - 40;
        this.addRenderableWidget(Button.builder(Component.literal("Return to Home"), btn -> onClose())
                .bounds(centerX - resetWidth / 2, yBack, resetWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        guiGraphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int totalWidth = 300;
        int leftEdge = width / 2 - totalWidth / 2;
        int textFieldY = height / 2 - 10;
        int aboveY = textFieldY - 15;

        guiGraphics.drawString(font, "Current: " + currentAccountName, leftEdge, aboveY, 0xFFFFFFFF);
        int statusX = leftEdge + totalWidth - font.width(feedbackMessage);
        guiGraphics.drawString(font, feedbackMessage, statusX, aboveY, feedbackColor);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}