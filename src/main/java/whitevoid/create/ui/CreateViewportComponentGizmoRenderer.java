package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.TransformMath;
import whitevoid.create.ui.ComponentTransformGizmo.Operation;
import whitevoid.create.ui.ViewportProjector.Point;
import whitevoid.create.editor.viewport.ViewportContext;

public final class CreateViewportComponentGizmoRenderer {
    public void render(DrawContext context, ViewportProjector projector, ModelNode node,
                       ViewportContext viewport, int cx, int cy, int left, int top, int right, int bottom,
                       ComponentTransformGizmo.Axis hoveredAxis, Operation operation,
                       ComponentTransformGizmo.PivotMode pivotMode) {
        var selection=viewport.meshComponentSelection();
        if(!selection.matches(node)||selection.size()==0)return;
        ComponentTransformGizmo gizmo=new ComponentTransformGizmo();
        TransformMath.Point localPivot=gizmo.localPivot(node,selection.mode(),selection.vertexIndices(),
                selection.edgeIndices(),selection.faceIndices(),pivotMode,selection);
        TransformMath.Point p3=TransformMath.applyHierarchy(localPivot,node);
        Point o=projector.project(p3.x(),p3.y(),p3.z(),cx,cy,300);
        if(o==null)return;
        double r=ComponentTransformGizmo.gizmoRadius(projector);
        double[][] dirs={{r,0,0},{0,r,0},{0,0,r}};
        int[] colors={0xFFE06B6B,0xFF70C878,0xFF6B8EDC};
        if(operation==Operation.ROTATE){
            drawRotationRings(context,projector,localPivot,node,cx,cy,left,top,right,bottom,colors,hoveredAxis);
        }else{
            ComponentTransformGizmo.Axis[] axes={ComponentTransformGizmo.Axis.X,ComponentTransformGizmo.Axis.Y,ComponentTransformGizmo.Axis.Z};
            for(int i=0;i<3;i++){
                TransformMath.Point end=TransformMath.applyHierarchy(
                        new TransformMath.Point(localPivot.x()+dirs[i][0],localPivot.y()+dirs[i][1],localPivot.z()+dirs[i][2]),node);
                Point p=projector.project(end.x(),end.y(),end.z(),cx,cy,300);
                if(p==null)continue;
                int color=hoveredAxis==axes[i]?0xFFFFFFFF:colors[i];
                drawLine(context,o,p,left,top,right,bottom,color);
                if(operation==Operation.SCALE)drawHandle(context,p,color,left,top,right,bottom);
                else drawArrowHead(context,p,o,color,left,top,right,bottom);
            }
        }
        context.fill((int)o.x()-4,(int)o.y()-4,(int)o.x()+5,(int)o.y()+5,0xFFFFFFFF);
    }

    private void drawRotationRings(DrawContext context,ViewportProjector projector,TransformMath.Point pivot,
                                   ModelNode node,int cx,int cy,int left,int top,int right,int bottom,
                                   int[] colors,ComponentTransformGizmo.Axis hoveredAxis){
        int[][] planes={{1,2},{0,2},{0,1}};
        ComponentTransformGizmo.Axis[] axes={ComponentTransformGizmo.Axis.X,ComponentTransformGizmo.Axis.Y,ComponentTransformGizmo.Axis.Z};
        for(int axis=0;axis<3;axis++){
            Point previous=null;int color=hoveredAxis==axes[axis]?0xFFFFFFFF:colors[axis];
            for(int i=0;i<=64;i++){
                double angle=Math.PI*2*i/64.0;
                double[] o={0,0,0};
                o[planes[axis][0]]=Math.cos(angle)*2.0;
                o[planes[axis][1]]=Math.sin(angle)*2.0;
                TransformMath.Point world=TransformMath.applyHierarchy(
                        new TransformMath.Point(pivot.x()+o[0],pivot.y()+o[1],pivot.z()+o[2]),node);
                Point current=projector.project(world.x(),world.y(),world.z(),cx,cy,300);
                if(current==null){previous=null;continue;}
                if(previous!=null)drawLine(context,previous,current,left,top,right,bottom,color);
                previous=current;
            }
        }
    }

    private void drawArrowHead(DrawContext context,Point tip,Point origin,int color,int left,int top,int right,int bottom){
        double dx=tip.x()-origin.x(),dy=tip.y()-origin.y(),len=Math.hypot(dx,dy);if(len<1)return;
        dx/=len;dy/=len;double px=-dy,py=dx;
        drawLine(context,tip,new Point(tip.x()-dx*10+px*4,tip.y()-dy*10+py*4,tip.depth()),left,top,right,bottom,color);
        drawLine(context,tip,new Point(tip.x()-dx*10-px*4,tip.y()-dy*10-py*4,tip.depth()),left,top,right,bottom,color);
    }
    private void drawHandle(DrawContext context,Point p,int color,int left,int top,int right,int bottom){
        int x=(int)Math.round(p.x()),y=(int)Math.round(p.y());
        context.fill(x-4,y-4,x+5,y+5,0xFF111216);context.fill(x-3,y-3,x+4,y+4,color);
    }
    private void drawLine(DrawContext context,Point a,Point b,int left,int top,int right,int bottom,int color){
        int x0=(int)Math.round(a.x()),y0=(int)Math.round(a.y()),x1=(int)Math.round(b.x()),y1=(int)Math.round(b.y());
        int dx=Math.abs(x1-x0),dy=Math.abs(y1-y0),sx=x0<x1?1:-1,sy=y0<y1?1:-1,err=dx-dy;
        while(true){
            if(x0>=left&&x0<right&&y0>=top&&y0<bottom)context.fill(x0,y0,x0+1,y0+1,color);
            if(x0==x1&&y0==y1)break;int e2=2*err;if(e2>-dy){err-=dy;x0+=sx;}if(e2<dx){err+=dx;y0+=sy;}
        }
    }
}
