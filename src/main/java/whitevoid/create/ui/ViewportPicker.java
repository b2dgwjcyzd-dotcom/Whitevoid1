package whitevoid.create.ui;

import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.CubeGeometry;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.Transform;

/**
 * Ray-based picking for CREATE model geometry.
 * Rays are generated from the same projection math used by the viewport,
 * then tested against each node's local cube.
 */
public final class ViewportPicker {
    public ModelNode pick(Model model, ViewportContext viewport, double mouseX, double mouseY, int width, int height) {
        int left=16, top=16, right=width-16, bottom=height-16;
        int cx=(left+right)/2, cy=(top+bottom)/2;
        ViewportProjector projector=new ViewportProjector(viewport.viewport().camera());

        Ray ray=createRay(projector, mouseX, mouseY, cx, cy, 300.0);
        ModelNode best=null;
        double bestDistance=Double.POSITIVE_INFINITY;

        for(ModelNode node:model.allNodes()) {
            CubeGeometry geometry=node.geometry();
            if(geometry==null) continue;

            Ray localRay=toLocalRay(ray,node);
            double hit=intersectCube(localRay,geometry);
            if(hit>=0 && hit<bestDistance) {
                best=node;
                bestDistance=hit;
            }
        }
        return best;
    }

    private Ray createRay(ViewportProjector projector, double mouseX, double mouseY,
                           int cx, int cy, double focalLength) {
        double yaw=Math.toRadians(projector.cameraYaw());
        double pitch=Math.toRadians(projector.cameraPitch());

        double screenX=(mouseX-cx-projectorPanX(projector))/focalLength;
        double screenY=-(mouseY-cy-projectorPanY(projector))/focalLength;

        // Camera space: +Z is forward, matching ViewportProjector's depth equation.
        Vec3 direction=new Vec3(screenX,screenY,1).normalize();
        Vec3 originCamera=new Vec3(0,0,-cameraDistance(projector));

        return new Ray(inverseRotate(inverseRotate(originCamera,-pitch),-yaw),
                inverseRotate(inverseRotate(direction,-pitch),-yaw));
    }

    private double projectorPanX(ViewportProjector projector) {
        return projector.cameraPanX();
    }

    private double projectorPanY(ViewportProjector projector) {
        return projector.cameraPanY();
    }

    private double cameraDistance(ViewportProjector projector) {
        return projector.cameraDistance();
    }

    private Ray toLocalRay(Ray worldRay, ModelNode node) {
        java.util.ArrayList<ModelNode> chain=new java.util.ArrayList<>();
        for(ModelNode current=node; current!=null; current=current.parent()) chain.add(current);

        Vec3 origin=worldRay.origin;
        Vec3 direction=worldRay.direction;

        for(int i=chain.size()-1;i>=0;i--) {
            origin=inverseTransformPoint(origin,chain.get(i).transform());
            direction=inverseTransformDirection(direction,chain.get(i).transform());
        }
        return new Ray(origin,direction);
    }

    private Vec3 inverseTransformPoint(Vec3 point, Transform t) {
        Vec3 p=new Vec3(point.x-t.x(),point.y-t.y(),point.z-t.z());
        p=inverseRotate(p,Math.toRadians(t.rotationZ()));
        p=inverseRotateY(p,Math.toRadians(t.rotationY()));
        p=inverseRotateX(p,Math.toRadians(t.rotationX()));
        return new Vec3(p.x/t.scaleX(),p.y/t.scaleY(),p.z/t.scaleZ());
    }

    private Vec3 inverseTransformDirection(Vec3 direction, Transform t) {
        Vec3 d=inverseRotate(direction,Math.toRadians(t.rotationZ()));
        d=inverseRotateY(d,Math.toRadians(t.rotationY()));
        d=inverseRotateX(d,Math.toRadians(t.rotationX()));
        return new Vec3(d.x/t.scaleX(),d.y/t.scaleY(),d.z/t.scaleZ()).normalize();
    }

    private double intersectCube(Ray ray, CubeGeometry g) {
        double hx=g.width()/2, hy=g.height()/2, hz=g.depth()/2;
        double tMin=0, tMax=Double.POSITIVE_INFINITY;
        double[] origin={ray.origin.x,ray.origin.y,ray.origin.z};
        double[] direction={ray.direction.x,ray.direction.y,ray.direction.z};
        double[] min={-hx,-hy,-hz}, max={hx,hy,hz};

        for(int i=0;i<3;i++) {
            if(Math.abs(direction[i])<1e-9) {
                if(origin[i]<min[i] || origin[i]>max[i]) return -1;
                continue;
            }
            double t1=(min[i]-origin[i])/direction[i];
            double t2=(max[i]-origin[i])/direction[i];
            if(t1>t2){ double tmp=t1;t1=t2;t2=tmp; }
            tMin=Math.max(tMin,t1);
            tMax=Math.min(tMax,t2);
            if(tMin>tMax) return -1;
        }
        return tMin>=0 ? tMin : tMax>=0 ? tMax : -1;
    }

    private Vec3 inverseRotate(Vec3 p,double angle) {
        double c=Math.cos(angle),s=Math.sin(angle);
        return new Vec3(p.x*c+p.z*s,p.y,-p.x*s+p.z*c);
    }
    private Vec3 inverseRotateX(Vec3 p,double angle) {
        double c=Math.cos(angle),s=Math.sin(angle);
        return new Vec3(p.x,p.y*c+p.z*s,-p.y*s+p.z*c);
    }
    private Vec3 inverseRotateY(Vec3 p,double angle) {
        double c=Math.cos(angle),s=Math.sin(angle);
        return new Vec3(p.x*c-p.z*s,p.y,p.x*s+p.z*c);
    }

    private record Ray(Vec3 origin,Vec3 direction) {}
    private record Vec3(double x,double y,double z) {
        Vec3 normalize() {
            double length=Math.sqrt(x*x+y*y+z*z);
            return length<1e-12 ? new Vec3(0,0,1) : new Vec3(x/length,y/length,z/length);
        }
    }
}
