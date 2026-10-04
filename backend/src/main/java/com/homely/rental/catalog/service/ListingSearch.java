package com.homely.rental.catalog.service;
import com.homely.rental.catalog.dto.request.ListingSearchQuery;
import com.homely.rental.catalog.entity.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.Instant;
import java.util.*;

public final class ListingSearch {
    private ListingSearch() {}
    public static Specification<Listing> matching(ListingSearchQuery f) {
        f.validateRanges();
        return (root, query, cb) -> {
            var room = root.join("room");
            List<Predicate> p=new ArrayList<>();
            p.add(cb.equal(root.get("status"),ListingStatus.PUBLISHED));
            p.add(cb.equal(room.get("availability"),RoomAvailability.AVAILABLE));
            p.add(cb.or(cb.isNull(root.get("expiresAt")),cb.greaterThan(root.get("expiresAt"),Instant.now())));
            if (f.getQuery()!=null && !f.getQuery().isBlank()) {
                String text="%"+f.getQuery().trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")),text,'\\'),cb.like(cb.lower(root.get("description")),text,'\\'),
                        cb.like(cb.lower(room.get("address").get("line")),text,'\\')));
            }
            if(f.getRoom_type()!=null) p.add(cb.equal(room.get("roomType"),RoomType.valueOf(f.getRoom_type())));
            if(f.getProvince_code()!=null) p.add(cb.equal(room.get("address").get("provinceCode"),f.getProvince_code()));
            if(f.getMin_rent_vnd()!=null) p.add(cb.ge(root.get("rentVnd"),f.getMin_rent_vnd()));
            if(f.getMax_rent_vnd()!=null) p.add(cb.le(root.get("rentVnd"),f.getMax_rent_vnd()));
            if(f.getMin_area_m2()!=null) p.add(cb.ge(room.get("areaM2"),f.getMin_area_m2()));
            if(f.getMax_area_m2()!=null) p.add(cb.le(room.get("areaM2"),f.getMax_area_m2()));
            if(f.getAmenity_ids()!=null && !f.getAmenity_ids().isEmpty()) {
                var ids = new HashSet<>(f.getAmenity_ids());
                Subquery<Long> sub=query.subquery(Long.class);
                var other=sub.from(Room.class);
                var amenities=other.join("amenities");
                sub.select(cb.countDistinct(amenities.get("id"))).where(cb.equal(other.get("id"),room.get("id")),amenities.get("id").in(ids));
                p.add(cb.equal(sub,(long)ids.size()));
            }
            Expression<Double> distance=null;
            if (f.getLatitude()!=null) {
                var lat=cb.function("radians",Double.class,room.get("location").get("latitude"));
                var lon=cb.function("radians",Double.class,room.get("location").get("longitude"));
                var a=cb.prod(cb.function("sin",Double.class,lat),Math.sin(Math.toRadians(f.getLatitude())));
                var b=cb.prod(cb.prod(cb.function("cos",Double.class,lat),Math.cos(Math.toRadians(f.getLatitude()))),
                        cb.function("cos",Double.class,cb.diff(lon,Math.toRadians(f.getLongitude()))));
                var clamp=cb.function("least",Double.class,cb.literal(1.0),
                        cb.function("greatest",Double.class,cb.literal(-1.0),cb.sum(a,b)));
                distance=cb.prod(cb.function("acos",Double.class,clamp),6371.0088);
                p.add(cb.isNotNull(room.get("location").get("latitude")));
                if(f.getRadius_km()!=null) p.add(cb.le(distance,f.getRadius_km()));
            }
            if(query.getResultType()!=Long.class && query.getResultType()!=long.class) {
                Order order=switch(f.getSort()) {
                    case "rent_asc" -> cb.asc(root.get("rentVnd"));
                    case "rent_desc" -> cb.desc(root.get("rentVnd"));
                    case "nearest" -> cb.asc(distance);
                    default -> cb.desc(root.get("publishedAt"));
                };
                query.orderBy(order,cb.desc(root.get("id")));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
