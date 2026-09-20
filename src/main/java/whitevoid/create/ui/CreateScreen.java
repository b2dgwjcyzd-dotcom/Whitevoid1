package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.selection.SelectionMode;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;

public final class CreateScreen extends Screen {
    private final CreateCore core;
    private final ViewportRenderer viewportRenderer = new ViewportRenderer();
    private final CreateViewportInput viewportInput;

    public CreateScreen(CreateCore core) {
        super(Text.literal("CREATE"));
        this.core = core;
        this.viewportInput = new CreateViewportInput(core.editorContext().viewport().viewport().camera());
    }

    @Override protected void init() {
        core.editorContext().setEditing(true);
    }

    @Override public void close() {
        viewportInput.cancelDrag();
        core.editorContext().setEditing(false);
        super.close();
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 71) { core.editorContext().viewport().transform().setMode(TransformMode.MOVE); return true; }
        if (keyCode == 82) { core.editorContext().viewport().transform().setMode(TransformMode.ROTATE); return true; }
        if (keyCode == 83) { core.editorContext().viewport().transform().setMode(TransformMode.SCALE); return true; }
        if (keyCode == 27) { core.editorContext().viewport().transform().setMode(TransformMode.SELECT); }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (viewportInput.mouseClicked(mouseX, mouseY, button)) return true;

        if (button == 0) {
            ModelNode hit = new ViewportPicker().pick(core.editorContext().model(),
                    core.editorContext().viewport(), mouseX, mouseY, width, height);
            if (hit != null) {
                core.editorContext().viewport().selection().select(hit, SelectionMode.SINGLE);
            } else {
                core.editorContext().viewport().selection().clear();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (viewportInput.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (viewportInput.mouseDragged(mouseX, mouseY, button, hasShiftDown())) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (viewportInput.mouseScrolled(verticalAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        ViewportContext viewport = core.editorContext().viewport();
        viewportRenderer.render(context, width, height, viewport, core.editorContext().model());
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
