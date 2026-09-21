package whitevoid.create.core.history.commands;

import java.util.Objects;
import whitevoid.create.core.history.Command;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.Transform;

public final class SetTransformCommand implements Command {
    private final ModelNode node;
    private final double oldX, oldY, oldZ, oldRx, oldRy, oldRz, oldSx, oldSy, oldSz;
    private final double newX, newY, newZ, newRx, newRy, newRz, newSx, newSy, newSz;

    public SetTransformCommand(ModelNode node,
                               double newX, double newY, double newZ,
                               double newRx, double newRy, double newRz,
                               double newSx, double newSy, double newSz) {
        this.node = Objects.requireNonNull(node, "node");
        Transform t = node.transform();
        oldX=t.x(); oldY=t.y(); oldZ=t.z();
        oldRx=t.rotationX(); oldRy=t.rotationY(); oldRz=t.rotationZ();
        oldSx=t.scaleX(); oldSy=t.scaleY(); oldSz=t.scaleZ();
        validate(newX, newY, newZ, newRx, newRy, newRz, newSx, newSy, newSz);
        this.newX=newX; this.newY=newY; this.newZ=newZ;
        this.newRx=newRx; this.newRy=newRy; this.newRz=newRz;
        this.newSx=newSx; this.newSy=newSy; this.newSz=newSz;
    }

    public SetTransformCommand(ModelNode node,
                               double oldX, double oldY, double oldZ,
                               double oldRx, double oldRy, double oldRz,
                               double oldSx, double oldSy, double oldSz,
                               double newX, double newY, double newZ,
                               double newRx, double newRy, double newRz,
                               double newSx, double newSy, double newSz,
                               boolean explicitSnapshot) {
        this.node = Objects.requireNonNull(node, "node");
        validate(oldX, oldY, oldZ, oldRx, oldRy, oldRz, oldSx, oldSy, oldSz);
        validate(newX, newY, newZ, newRx, newRy, newRz, newSx, newSy, newSz);
        this.oldX=oldX; this.oldY=oldY; this.oldZ=oldZ;
        this.oldRx=oldRx; this.oldRy=oldRy; this.oldRz=oldRz;
        this.oldSx=oldSx; this.oldSy=oldSy; this.oldSz=oldSz;
        this.newX=newX; this.newY=newY; this.newZ=newZ;
        this.newRx=newRx; this.newRy=newRy; this.newRz=newRz;
        this.newSx=newSx; this.newSy=newSy; this.newSz=newSz;
    }

    @Override public void execute() { apply(newX,newY,newZ,newRx,newRy,newRz,newSx,newSy,newSz); }
    @Override public void undo() { apply(oldX,oldY,oldZ,oldRx,oldRy,oldRz,oldSx,oldSy,oldSz); }
    @Override public String name() { return "Set Transform"; }

    private void apply(double x,double y,double z,double rx,double ry,double rz,double sx,double sy,double sz) {
        node.transform().position(x,y,z);
        node.transform().rotation(rx,ry,rz);
        node.transform().scale(sx,sy,sz);
    }

    private static void validate(double x,double y,double z,double rx,double ry,double rz,double sx,double sy,double sz) {
        double[] values = {x,y,z,rx,ry,rz,sx,sy,sz};
        for (double value : values) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Transform values must be finite");
            }
        }
        if (sx == 0.0 || sy == 0.0 || sz == 0.0) {
            throw new IllegalArgumentException("Transform scale cannot be zero");
        }
    }
}
