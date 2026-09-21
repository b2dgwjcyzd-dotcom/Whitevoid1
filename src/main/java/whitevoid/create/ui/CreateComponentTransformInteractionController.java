package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;

public final class CreateComponentTransformInteractionController {
    private final ComponentTransformMouseController mouse;
    private final ComponentTransformInputController input;
    private final ComponentTransformController transform;

    public CreateComponentTransformInteractionController(
            ComponentTransformMouseController mouse,
            ComponentTransformInputController input,
            ComponentTransformController transform) {
        this.mouse = mouse;
        this.input = input;
        this.transform = transform;
    }

    public boolean beginKeyboardArmed(
            ModelNode node,
            ViewportContext viewport,
            double mouseX,
            double mouseY
    ) {
        if (!mouse.beginKeyboardArmed(node, viewport, mouseX, mouseY)) {
            return false;
        }
        input.disarm();
        return true;
    }

    public boolean beginFromGizmo(
            ModelNode node,
            ViewportContext viewport,
            ViewportProjector projector,
            double mouseX,
            double mouseY,
            int centerX,
            int centerY
    ) {
        return mouse.beginFromGizmo(node, viewport, projector, mouseX, mouseY, centerX, centerY);
    }

    public void update(
            ModelNode node,
            ViewportContext viewport,
            double mouseX,
            double mouseY,
            int width,
            int height,
            boolean proportional,
            double proportionalRadius,
            boolean snap
    ) {
        mouse.update(
                node,
                viewport,
                new ViewportProjector(viewport.viewport().camera()),
                mouseX,
                mouseY,
                width,
                height,
                proportional,
                proportionalRadius,
                snap
        );
    }

    public boolean finish(
            CreateCore core,
            CreateViewportInteractionState interaction,
            ModelNode node,
            TransformMode mode
    ) {
        if (!interaction.componentDragging) {
            return false;
        }

        mouse.finish(core, node);
        interaction.componentDragging = false;
        input.disarm();
        transform.setAxis(ComponentTransformGizmo.Axis.NONE);
        interaction.hoveredComponentAxis = ComponentTransformGizmo.Axis.NONE;
        return true;
    }
}
