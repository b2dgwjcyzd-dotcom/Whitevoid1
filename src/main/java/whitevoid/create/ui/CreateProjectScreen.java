package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import whitevoid.create.core.CreateCore;
import whitevoid.create.project.ProjectMetadata;

import java.io.IOException;
import java.util.List;

public final class CreateProjectScreen extends Screen {
    private static final int ROW_HEIGHT = 28;
    private static final int PANEL_WIDTH = 420;

    private final CreateCore core;
    private final Screen parent;
    private List<ProjectMetadata> projects = List.of();
    private String error;

    public CreateProjectScreen(CreateCore core, Screen parent) {
        super(Text.literal("CREATE Projects"));
        this.core = core;
        this.parent = parent;
    }

    @Override
    protected void init() {
        reloadProjects();
    }

    private void reloadProjects() {
        try {
            projects = core.projectManager().listMetadata();
            error = null;
        } catch (IOException | RuntimeException exception) {
            projects = List.of();
            error = exception.getMessage();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int left = (width - PANEL_WIDTH) / 2;
        int top = 55;

        if (mouseX >= left && mouseX <= left + PANEL_WIDTH
                && mouseY >= top && mouseY <= top + projects.size() * ROW_HEIGHT) {
            int index = (int) ((mouseY - top) / ROW_HEIGHT);
            if (index >= 0 && index < projects.size()) {
                ProjectMetadata metadata = projects.get(index);
                core.openProject(metadata.id());
                close();
                return true;
            }
        }

        if (mouseX >= left && mouseX <= left + PANEL_WIDTH
                && mouseY >= top + projects.size() * ROW_HEIGHT + 12
                && mouseY <= top + projects.size() * ROW_HEIGHT + 38) {
            close();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int left = (width - PANEL_WIDTH) / 2;
        int top = 55;

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("CREATE Projects"),
                width / 2, 28, 0xFFFFFF);

        for (int i = 0; i < projects.size(); i++) {
            ProjectMetadata metadata = projects.get(i);
            int rowTop = top + i * ROW_HEIGHT;
            boolean hovered = mouseX >= left && mouseX <= left + PANEL_WIDTH
                    && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;

            int background = hovered ? 0xFF303030 : 0xFF202020;
            context.fill(left, rowTop, left + PANEL_WIDTH, rowTop + ROW_HEIGHT - 2, background);
            context.drawTextWithShadow(textRenderer, Text.literal(metadata.name()),
                    left + 10, rowTop + 8, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, Text.literal(metadata.type().name()),
                    left + PANEL_WIDTH - 90, rowTop + 8, 0x888888);
        }

        int buttonTop = top + projects.size() * ROW_HEIGHT + 12;
        context.fill(left, buttonTop, left + PANEL_WIDTH, buttonTop + 26, 0xFF202020);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Back"),
                width / 2, buttonTop + 8, 0xFFFFFF);

        if (projects.isEmpty() && error == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No saved projects"),
                    width / 2, top + 12, 0xAAAAAA);
        } else if (error != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Unable to load projects"),
                    width / 2, top + 12, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
