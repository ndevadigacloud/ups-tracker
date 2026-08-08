package com.ups.shipment.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import com.ups.shipment.dto.DashboardResponse;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final MongoTemplate mongoTemplate;

    public DashboardService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public DashboardResponse buildDashboard() {
        // Single-round-trip aggregation using $facet: counts by status, daily volume,
        // and top destination facilities are computed in parallel branches server-side.
        Document facetStage = new Document("$facet", new Document()
                .append("byStatus", List.of(
                        new Document("$group", new Document("_id", "$status").append("count", new Document("$sum", 1)))))
                .append("byDay", List.of(
                        new Document("$group", new Document()
                                .append("_id", new Document("$dateToString",
                                        new Document("format", "%Y-%m-%d").append("date", "$createdAt")))
                                .append("count", new Document("$sum", 1))),
                        new Document("$sort", new Document("_id", 1))))
                .append("topDestinations", List.of(
                        new Document("$group", new Document("_id", "$destinationFacilityId").append("count", new Document("$sum", 1))),
                        new Document("$sort", new Document("count", -1)),
                        new Document("$limit", 5))));

        log.debug("Running dashboard $facet aggregation against 'shipments' collection");
        long startNanos = System.nanoTime();

        Aggregation aggregation = Aggregation.newAggregation(context -> facetStage);
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "shipments", Document.class);
        Document facetResult = results.getUniqueMappedResult();

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        log.info("Dashboard aggregation completed in {} ms", elapsedMs);

        Map<String, Long> countsByStatus = new LinkedHashMap<>();
        if (facetResult != null) {
            for (Document doc : facetResult.getList("byStatus", Document.class)) {
                countsByStatus.put(doc.getString("_id"), doc.getInteger("count").longValue());
            }
        }

        List<Map<String, Object>> volumeByDay = facetResult == null
                ? List.of()
                : facetResult.getList("byDay", Document.class).stream()
                    .map(d -> Map.<String, Object>of("date", d.getString("_id"), "count", d.getInteger("count")))
                    .toList();

        List<Map<String, Object>> topDestinations = facetResult == null
                ? List.of()
                : facetResult.getList("topDestinations", Document.class).stream()
                    .map(d -> Map.<String, Object>of("facilityId", d.getString("_id"), "count", d.getInteger("count")))
                    .toList();

        log.debug("Dashboard result: {} status buckets, {} days of volume, {} top destinations",
                countsByStatus.size(), volumeByDay.size(), topDestinations.size());

        return new DashboardResponse(countsByStatus, volumeByDay, topDestinations);
    }
}
