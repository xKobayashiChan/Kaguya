package com.github.kaguya.auth;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * ログイン画面。
 * 起動時にHWID自動ログインを試みる。
 * 自動ログイン失敗時のみユーザーID・パスワード入力欄を表示する。
 */
public class GuiLogin extends GuiScreen {

    private GuiTextField userIdField;
    private GuiTextField passwordField;
    private GuiButton loginButton;
    private String status = "";
    private boolean loggingIn = false;
    private boolean autoLoginDone = false;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        userIdField = new GuiTextField(0, this.fontRendererObj, centerX - 100, centerY - 30, 200, 20);
        userIdField.setMaxStringLength(64);

        passwordField = new PasswordTextField(1, this.fontRendererObj, centerX - 100, centerY + 5, 200, 20);
        passwordField.setMaxStringLength(128);

        this.buttonList.clear();
        loginButton = new GuiButton(0, centerX - 100, centerY + 40, 200, 20, "Login");
        this.buttonList.add(loginButton);

        setInputEnabled(false);
        status = "\u00a7eConnecting...";

        // HWID自動ログインを別スレッドで試行
        new Thread(() -> {
            boolean success = AuthManager.autoLogin();
            Minecraft.getMinecraft().addScheduledTask(() -> {
                autoLoginDone = true;
                if (success) {
                    Minecraft.getMinecraft().displayGuiScreen(null);
                } else {
                    // 自動ログイン失敗 → 入力欄を有効化
                    status = "";
                    setInputEnabled(true);
                    userIdField.setFocused(true);
                }
            });
        }, "KaguyaAutoLogin").start();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == loginButton && !loggingIn) {
            attemptLogin();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (!autoLoginDone) return;

        if (userIdField.isFocused()) userIdField.textboxKeyTyped(typedChar, keyCode);
        if (passwordField.isFocused()) passwordField.textboxKeyTyped(typedChar, keyCode);

        if (keyCode == Keyboard.KEY_TAB) {
            if (userIdField.isFocused()) {
                userIdField.setFocused(false);
                passwordField.setFocused(true);
            } else {
                passwordField.setFocused(false);
                userIdField.setFocused(true);
            }
        }

        if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) && !loggingIn) {
            attemptLogin();
        }
        // ESCは無効（ログインを強制）
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        try {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (autoLoginDone) {
            userIdField.mouseClicked(mouseX, mouseY, mouseButton);
            passwordField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        drawCenteredString(fontRendererObj, "\u00a76\u00a7lKaguya Client", width / 2, height / 2 - 70, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "\u00a77Authentication Required", width / 2, height / 2 - 55, 0xAAAAAA);

        if (autoLoginDone) {
            drawString(fontRendererObj, "User ID:", width / 2 - 100, height / 2 - 42, 0xBBBBBB);
            drawString(fontRendererObj, "Password:", width / 2 - 100, height / 2 - 7, 0xBBBBBB);
            userIdField.drawTextBox();
            passwordField.drawTextBox();
        }

        if (!status.isEmpty()) {
            drawCenteredString(fontRendererObj, status, width / 2, height / 2 + 67, 0xFFFFFF);
        }

        String hwid = HWIDUtil.getHWID();
        drawString(fontRendererObj, "\u00a78HWID: " + hwid.substring(0, Math.min(16, hwid.length())) + "...", 2, height - 12, 0x555555);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen() {
        userIdField.updateCursorCounter();
        passwordField.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void setInputEnabled(boolean enabled) {
        userIdField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginButton.enabled = enabled;
        loginButton.visible = enabled;
    }

    private void attemptLogin() {
        String userId = userIdField.getText().trim();
        String password = passwordField.getText();

        if (userId.isEmpty()) {
            status = "\u00a7cPlease enter your User ID.";
            return;
        }
        if (password.isEmpty()) {
            status = "\u00a7cPlease enter your password.";
            return;
        }

        loggingIn = true;
        setInputEnabled(false);
        status = "\u00a7eLogging in...";

        new Thread(() -> {
            boolean success = AuthManager.login(userId, password);
            Minecraft.getMinecraft().addScheduledTask(() -> {
                loggingIn = false;
                status = AuthManager.getStatusMessage();
                if (success) {
                    Minecraft.getMinecraft().displayGuiScreen(null);
                } else {
                    setInputEnabled(true);
                }
            });
        }, "KaguyaAuth").start();
    }

    /**
     * パスワード入力欄（文字を * でマスクする）
     */
    private static class PasswordTextField extends GuiTextField {
        private String actualText = "";

        public PasswordTextField(int id, FontRenderer fontRenderer, int x, int y, int width, int height) {
            super(id, fontRenderer, x, y, width, height);
        }

        @Override
        public boolean textboxKeyTyped(char typedChar, int keyCode) {
            String before = super.getText();
            boolean result = super.textboxKeyTyped(typedChar, keyCode);
            String after = super.getText();

            if (!before.equals(after)) {
                int lenDiff = after.length() - before.length();
                int cursor = getCursorPosition();

                if (lenDiff > 0) {
                    String added = "";
                    for (int i = 0; i < lenDiff; i++) added += typedChar;
                    actualText = actualText.substring(0, Math.min(cursor - lenDiff, actualText.length()))
                            + added
                            + actualText.substring(Math.min(cursor - lenDiff, actualText.length()));
                } else if (lenDiff < 0) {
                    int deleteStart = Math.max(0, cursor);
                    int deleteEnd = Math.min(deleteStart - lenDiff, actualText.length());
                    if (deleteStart <= actualText.length() && deleteEnd <= actualText.length()) {
                        actualText = actualText.substring(0, deleteStart) + actualText.substring(deleteEnd);
                    }
                }

                String masked = repeatChar('*', after.length());
                int savedCursor = getCursorPosition();
                super.setText(masked);
                setCursorPosition(savedCursor);
            }

            return result;
        }

        @Override
        public void setText(String text) {
            actualText = text;
            super.setText(repeatChar('*', text.length()));
        }

        @Override
        public String getText() {
            return actualText;
        }

        private static String repeatChar(char c, int count) {
            StringBuilder sb = new StringBuilder(count);
            for (int i = 0; i < count; i++) sb.append(c);
            return sb.toString();
        }
    }
}