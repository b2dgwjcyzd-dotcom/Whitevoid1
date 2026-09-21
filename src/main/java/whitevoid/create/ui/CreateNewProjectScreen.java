package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import whitevoid.create.core.CreateCore;
import whitevoid.create.project.ProjectType;

public final class CreateNewProjectScreen extends Screen {
    private static final int PANEL_WIDTH = 420;

    private final CreateCore core;
    private final Screen parent;
    private String name = "Untitled";
    private boolean focused = true;
    private String error;

    public CreateNewProjectScreen(CreateCore core, Screen parent) {
        super(Text.literal("New CREATE Project"));
        this.core = core;
        this.parent = parent;
    }

    @Override
    protected void init() {
        focused = true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!focused || name.length() >= 48) return false;
        if (chr >= 32 && chr != 127) {
            name += chr;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            close();
            return true;
        }
        if (focused && keyCode == 259) {
            if (!name.isEmpty()) name = name.substring(0, name.length() - 1);
            return true;
        }
        if (focused && keyCode == 257) {
            createProject();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void createProject() {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            error = "Project name cannot be empty";
            return;
        }

        try {
            core.projectManager().create(trimmed, ProjectType.MODEL);
            close();
        } catch (Exception exception) {
            error = "Unable to create project";
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int left = (width - PANEL_WIDTH) / 2;
        int top = 90;

        if (mouseX >= left && mouseX <= left + PANEL_WIDTH
                && mouseY >= top && mouseY <= top + 32) {
            focused = true;
            return true;
        }

        if (mouseX >= left && mouseX <= left + PANEL_WIDTH
                && mouseY >= top + 50 && mouseY <= top + 80) {
            createProject();
            return true;
        }

        if (mouseX >= left && mouseX <= left + PANEL_WIDTH
                && mouseY >= top + 90 && mouseY <= top + 120) {
            close();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int left = (width - PANEL_WIDTH) / 2;
        int top = 90;

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("New CREATE Project"),
                width / 2, 48, 0xFFFFFF);

        context.fill(left, top, left + PANEL_WIDTH, top + 32, 0xFF202020);
        context.drawTextWithShadow(textRenderer,
                Text.literal(name + (focused ? "|" : "")),
                left + 10, top + 10, 0xFFFFFF);

        drawButton(context, left, top + 50, "Create", mouseX, mouseY);
        drawButton(context, left, top + 90, "Cancel", mouseX, mouseY);

        if (error != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(error),
                    width / 2, top + 132, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawButton(DrawContext context, int left, int top, String label, int mouseX, int mouseY) {
        boolean hovered = mouseX >= left && mouseX <= left + PANEL_WIDTH
                && mouseY >= top && mouseY <= top + 30;
        context.fill(left, top, left + PANEL_WIDTH, top + 30, hovered ? 0xFF303030 : 0xFF202020);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(label),
                left + PANEL_WIDTH / 2, top + 10, 0xFFFFFF);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
