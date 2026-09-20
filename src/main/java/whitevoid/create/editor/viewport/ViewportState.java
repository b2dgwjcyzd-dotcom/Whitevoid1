package whitevoid.create.editor.viewport;

public final class ViewportState {
    private final ViewportCamera camera = new ViewportCamera();
    private boolean gridVisible = true;
    private boolean axesVisible = true;
    private boolean selectionOutlineVisible = true;

    public ViewportCamera camera() { return camera; }
    public boolean gridVisible() { return gridVisible; }
    public boolean axesVisible() { return axesVisible; }
    public boolean selectionOutlineVisible() { return selectionOutlineVisible; }
    public void setGridVisible(boolean value) { gridVisible = value; }
    public void setAxesVisible(boolean value) { axesVisible = value; }
    public void setSelectionOutlineVisible(boolean value) { selectionOutlineVisible = value; }
}
