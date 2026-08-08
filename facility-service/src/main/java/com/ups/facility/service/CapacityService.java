package com.ups.facility.service;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.ups.facility.model.Facility;

@Service
public class CapacityService {

    private static final Logger log = LoggerFactory.getLogger(CapacityService.class);

    private final MongoTemplate mongoTemplate;

    public CapacityService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Atomically reserves capacity: the $expr guard makes the load+weight check
     * and the increment happen as one operation, so concurrent reservations for
     * the same facility can't both pass the check before either writes back.
     */
    public boolean tryReserve(String facilityId, double weightKg) {
        log.debug("Attempting to reserve {} kg at facility {}", weightKg, facilityId);

        Document filter = new Document("_id", facilityId)
                .append("$expr", new Document("$lte", java.util.List.of(
                        new Document("$add", java.util.List.of("$currentLoadKg", weightKg)),
                        "$capacityKg")));

        BasicQuery query = new BasicQuery(filter);
        Update update = new Update().inc("currentLoadKg", weightKg);

        Facility updated = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Facility.class);

        if (updated != null) {
            log.info("Reserved {} kg at facility {} (new load: {}/{} kg)",
                    weightKg, facilityId, updated.getCurrentLoadKg(), updated.getCapacityKg());
        } else {
            log.warn("Could not reserve {} kg at facility {} - at or over capacity, or facility not found", weightKg, facilityId);
        }

        return updated != null;
    }
}
