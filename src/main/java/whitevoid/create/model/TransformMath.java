package whitevoid.create.model;

public final class TransformMath {
    private TransformMath() {}

    public record Point(double x, double y, double z) {}

    public static Point apply(Point point, Transform transform) {
        double x = point.x() * transform.scaleX();
        double y = point.y() * transform.scaleY();
        double z = point.z() * transform.scaleZ();

        double rx = Math.toRadians(transform.rotationX());
        double ry = Math.toRadians(transform.rotationY());
        double rz = Math.toRadians(transform.rotationZ());

        double cos = Math.cos(rx);
        double sin = Math.sin(rx);
        double ny = y * cos - z * sin;
        double nz = y * sin + z * cos;
        y = ny;
        z = nz;

        cos = Math.cos(ry);
        sin = Math.sin(ry);
        double nx = x * cos + z * sin;
        nz = -x * sin + z * cos;
        x = nx;
        z = nz;

        cos = Math.cos(rz);
        sin = Math.sin(rz);
        nx = x * cos - y * sin;
        ny = x * sin + y * cos;
        x = nx;
        y = ny;

        return new Point(x + transform.x(), y + transform.y(), z + transform.z());
    }

    public static Point applyHierarchy(Point point, ModelNode node) {
        Point result = apply(point, node.transform());
        ModelNode parent = node.parent();
        while (parent != null) {
            result = apply(result, parent.transform());
            parent = parent.parent();
        }
        return result;
    }

    /**
     * Applies the node hierarchy to a direction/vector.
     * Unlike applyHierarchy, translation is deliberately ignored.
     * This is used by viewport gizmos so their axes follow the model's
     * local rotation and hierarchy instead of behaving like world axes.
     */
    public static Point applyDirectionHierarchy(Point direction, ModelNode node) {
        Point result = applyDirection(direction, node.transform());
        ModelNode parent = node.parent();
        while (parent != null) {
            result = applyDirection(result, parent.transform());
            parent = parent.parent();
        }
        return result;
    }

    public static Point applyDirection(Point direction, Transform transform) {
        double x = direction.x() * transform.scaleX();
        double y = direction.y() * transform.scaleY();
        double z = direction.z() * transform.scaleZ();

        double rx = Math.toRadians(transform.rotationX());
        double ry = Math.toRadians(transform.rotationY());
        double rz = Math.toRadians(transform.rotationZ());

        double cos = Math.cos(rx);
        double sin = Math.sin(rx);
        double ny = y * cos - z * sin;
        double nz = y * sin + z * cos;
        y = ny;
        z = nz;

        cos = Math.cos(ry);
        sin = Math.sin(ry);
        double nx = x * cos + z * sin;
        nz = -x * sin + z * cos;
        x = nx;
        z = nz;

        cos = Math.cos(rz);
        sin = Math.sin(rz);
        nx = x * cos - y * sin;
        ny = x * sin + y * cos;
        x = nx;
        y = ny;

        return new Point(x, y, z);
    }
}
