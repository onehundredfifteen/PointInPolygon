package org.geotools.PointInPolygon;

import java.util.List;

import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;

/*
     * CountryVisitor
     * Class discards all points that don't belong to the country's area
*/
public class CountryVisitor implements ItemVisitor {

    private List<Object> result;
    private MultiPolygon country;

    public CountryVisitor(MultiPolygon country, List<Object> result) {
        this.result = result;
        this.country = country;
    }

    @Override
    public void visitItem(Object obj) {
         if(country.contains((Geometry) obj))
            result.add(obj);
    }
}