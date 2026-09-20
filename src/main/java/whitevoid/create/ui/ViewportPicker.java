package whitevoid.create.ui;

import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.TransformMath;
import whitevoid.create.model.CubeGeometry;

public final class ViewportPicker {
    public ModelNode pick(Model model, ViewportContext viewport, double mouseX, double mouseY, int width, int height) {
        int left=16, top=16, right=width-16, bottom=height-16;
        int cx=(left+right)/2, cy=(top+bottom)/2;
        ViewportProjector projector=new ViewportProjector(viewport.viewport().camera());
        ModelNode best=null;
        double bestDistance=Double.MAX_VALUE;

        for(ModelNode node:model.allNodes()) {
            CubeGeometry g=node.geometry();
            if(g==null) continue;
            double hx=g.width()/2, hy=g.height()/2, hz=g.depth()/2;
            TransformMath.Point center=TransformMath.applyHierarchy(new TransformMath.Point(0,0,0),node);
            ViewportProjector.Point p=projector.project(center.x(),center.y(),center.z(),cx,cy,300);
            if(p==null) continue;
            double dx=p.x()-mouseX, dy=p.y()-mouseY;
            double distance=Math.sqrt(dx*dx+dy*dy);
            double radius=Math.max(10, Math.min(80, 300.0/Math.max(0.1,p.depth()) * Math.max(hx,Math.max(hy,hz))));
            if(distance<=radius && distance<bestDistance) {
                best=node;
                bestDistance=distance;
            }
        }
        return best;
    }
}
