package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.viewport.ViewportContext;

/** Minimal first CREATE screen. Rendering is intentionally isolated in ViewportRenderer. */
public final class CreateScreen extends Screen {
    private final CreateCore core;
    private final ViewportRenderer viewportRenderer;

    public CreateScreen(CreateCore core) {
        super(Text.literal("CREATE"));
        this.core = core;
        this.viewportRenderer = new ViewportRenderer();
    }

    @Override
    protected void init() {
        core.editorContext().setEditing(true);
    }

    @Override
    public void close() {
        core.editorContext().setEditing(false);
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        ViewportContext viewport = core.editorContext().viewport();
        viewportRenderer.render(context, width, height, viewport);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
