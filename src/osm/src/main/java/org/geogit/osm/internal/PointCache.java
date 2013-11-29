package org.geogit.osm.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.geogit.api.CommandLocator;
import org.geogit.api.NodeRef;
import org.geogit.api.RevFeature;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class PointCache {

    LinkedHashMap<Long, Coordinate> pointCache = new LinkedHashMap<Long, Coordinate>() {
        /** serialVersionUID */
        private static final long serialVersionUID = 1277795218777240552L;

        private final int limit = 2 * 1000 * 1000;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Coordinate> eldest) {
            return size() == limit;
        }
    };

    private ObjectDatabase indexDb;

    private CommandLocator cmdLoc;

    private FindTreeChild findTreeChild;

    public PointCache(RevTree root, CommandLocator cmdLoc, ObjectDatabase indexDb) {
        this.cmdLoc = cmdLoc;
        this.indexDb = indexDb;
        this.findTreeChild = cmdLoc.command(FindTreeChild.class).setIndex(true).setParent(root);
    }

    public void put(Long nodeId, Coordinate coord) {
        pointCache.put(nodeId, coord);
    }

    @Nullable
    public Coordinate get(long nodeId) {
        Coordinate coord = pointCache.get(Long.valueOf(nodeId));
        if (coord == null) {
            String fid = String.valueOf(nodeId);
            String path = NodeRef.appendChild(OSMUtils.NODE_TYPE_NAME, fid);
            Optional<NodeRef> ref = findTreeChild.setChildPath(path).call();
            if (ref.isPresent()) {
                NodeRef nodeRef = ref.get();
                RevFeature revFeature = indexDb.getFeature(nodeRef.objectId());
                Point pt = null;
                ImmutableList<Optional<Object>> values = revFeature.getValues();
                for (Optional<Object> opt : values) {
                    if (opt.isPresent()) {
                        Object value = opt.get();
                        if (value instanceof Point) {
                            pt = (Point) value;
                        }
                    }
                }

                if (pt != null) {
                    coord = pt.getCoordinate();
                    pointCache.put(Long.valueOf(nodeId), coord);
                }
            }
        }
        return coord;
    }

}
