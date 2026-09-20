package whitevoid.create.editor.viewport;

import whitevoid.create.editor.geometry.GeometryFaceSelection;
import whitevoid.create.editor.geometry.MeshComponentSelection;
import whitevoid.create.editor.selection.SelectionController;
import whitevoid.create.editor.transform.TransformController;

public final class ViewportContext {
    private final ViewportState viewport = new ViewportState();
    private final SelectionController selection = new SelectionController();
    private final TransformController transform = new TransformController();
    private final GeometryFaceSelection geometryFaceSelection = new GeometryFaceSelection();
    private final MeshComponentSelection meshComponentSelection = new MeshComponentSelection();

    public ViewportState viewport() { return viewport; }
    public SelectionController selection() { return selection; }
    public TransformController transform() { return transform; }
    public GeometryFaceSelection geometryFaceSelection() { return geometryFaceSelection; }
    public MeshComponentSelection meshComponentSelection() { return meshComponentSelection; }

    public void reset() {
        viewport.reset();
        selection.clear();
        geometryFaceSelection.clear();
        meshComponentSelection.clear();
        transform.setMode(whitevoid.create.editor.transform.TransformMode.SELECT);
    }
}
