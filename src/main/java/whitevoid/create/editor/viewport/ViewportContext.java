package whitevoid.create.editor.viewport;

import whitevoid.create.editor.selection.SelectionController;

/** Shared state consumed by the future CREATE viewport renderer and input layer. */
public final class ViewportContext {
    private final ViewportState viewport = new ViewportState();
    private final SelectionController selection = new SelectionController();

    public ViewportState viewport() { return viewport; }
    public SelectionController selection() { return selection; }

    public void reset() {
        viewport.camera().reset();
        viewport.setGridVisible(true);
        viewport.setAxesVisible(true);
        viewport.setSelectionOutlineVisible(true);
        selection.clear();
    }
}
