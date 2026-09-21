package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;
import whitevoid.create.editor.selection.SelectionMode;

final class CreateViewportNodeSelectionController {
    private final ViewportPicker picker = new ViewportPicker();

    boolean selectAt(
            CreateCore core,
            ViewportContext viewport,
            double mouseX,
            double mouseY,
            int width,
            int height
    ) {
        ModelNode hit = picker.pick(
                core.editorContext().model(), viewport, mouseX, mouseY, width, height);

        if (hit != null) {
            viewport.selection().select(hit, SelectionMode.SINGLE);
        } else {
            viewport.selection().clear();
        }
        viewport.geometryFaceSelection().clear();
        return true;
    }
}
