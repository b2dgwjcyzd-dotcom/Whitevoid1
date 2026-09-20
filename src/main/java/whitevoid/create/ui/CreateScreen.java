                var wb=whitevoid.create.model.TransformMath.applyHierarchy(
                        new whitevoid.create.model.TransformMath.Point(b.x(),b.y(),b.z()),node);
                var pa=projector.project(wa.x(),wa.y(),wa.z(),cx,cy,300);
                var pb=projector.project(wb.x(),wb.y(),wb.z(),cx,cy,300);
                if(pa!=null && pb!=null && pointInsideBox(pa.x(),pa.y(),left,top,right,bottom)
                        && pointInsideBox(pb.x(),pb.y(),left,top,right,bottom))
                    if (hasAltDown()) selection.removeEdge(node, edge[0], edge[1]);
                    else selection.addEdge(node, edge[0], edge[1]);
            }
        } else {
            for (int i=0;i<mesh.faces().size();i++) {
                int[] ids=mesh.faces().get(i).vertices();
                double sx=0,sy=0; int count=0;
                for(int id:ids) {
                    var v=mesh.vertices().get(id);
                    var w=whitevoid.create.model.TransformMath.applyHierarchy(
                            new whitevoid.create.model.TransformMath.Point(v.x(),v.y(),v.z()),node);
                    var p=projector.project(w.x(),w.y(),w.z(),cx,cy,300);
                    if(p!=null){sx+=p.x();sy+=p.y();count++;}
                }
                if(count>0 && pointInsideBox(sx/count,sy/count,left,top,right,bottom)) {
                    if (hasAltDown()) selection.removeFace(node, i);
                    else selection.addFace(node, i);
                }
            }
        }
    }