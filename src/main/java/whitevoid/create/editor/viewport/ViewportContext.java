package whitevoid.create.editor.viewport;

import whitevoid.create.editor.selection.SelectionController;
import whitevoid.create.editor.transform.TransformController;

public final class ViewportContext {
    private final ViewportState viewport = new ViewportState();
    private final SelectionController selection = new SelectionController();
    private final TransformController transform = new TransformController();

    public ViewportState viewport() { return viewport; }
    public SelectionController selection() { return selection; }
    public TransformController transform() { return transform; }

    public void reset() {
        viewport.reset();
        selection.clear();
        transform.setMode(whitevoid.create.editor.transform.TransformMode.SELECT);
    }
}
