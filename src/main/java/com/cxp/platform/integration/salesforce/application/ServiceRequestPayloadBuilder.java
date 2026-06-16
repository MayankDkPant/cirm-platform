package com.cxp.platform.integration.salesforce.application;

import com.cxp.platform.servicerequest.entity.ServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceRequestPayloadBuilder {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> buildPayload(ServiceRequest request) {
        String governingBodyCode = findCodeById("governing_body", request.getGoverningBodyId());

        Map<String, Object> wardInfo = findWardCodeAndZone(request.getWardId());
        String wardCode = (String) wardInfo.get("wardCode");
        UUID zoneId = (UUID) wardInfo.get("zoneId");
        String zoneCode = zoneId == null ? null : findCodeById("zone", zoneId);

        String departmentCode = request.getDepartmentId() != null
                ? findCodeById("department", request.getDepartmentId())
                : null;

        Map<String, Object> payload = new HashMap<>();
        payload.put("Subject", request.getTitle());
        payload.put("Description", request.getDescription());
        payload.put("Origin", "CXP");
        payload.put("Status", "New");
        payload.put("Priority", request.getPriority() != null ? request.getPriority().name() : null);
        payload.put("Type", request.getType().name());

        payload.put("Governing_Body__r", relationByCode(governingBodyCode));
        payload.put("Ward__r", relationByCode(wardCode));
        payload.put("Department__r", relationByCode(departmentCode));
        payload.put("Zone__r", relationByCode(zoneCode));

        return payload;
    }

    private String findCodeById(String tableName, UUID id) {
        if (id == null) {
            return null;
        }
        return jdbcTemplate.query(
                "SELECT \"Code__c\" FROM " + tableName + " WHERE id = ? LIMIT 1",
                ps -> ps.setObject(1, id),
                rs -> rs.next() ? rs.getString(1) : null
        );
    }

    private Map<String, Object> findWardCodeAndZone(UUID wardId) {
        Map<String, Object> result = new HashMap<>();
        if (wardId == null) {
            return result;
        }
        jdbcTemplate.query(
                "SELECT \"Code__c\", zone_id FROM ward WHERE id = ? LIMIT 1",
                ps -> ps.setObject(1, wardId),
                rs -> {
                    if (rs.next()) {
                        result.put("wardCode", rs.getString(1));
                        Object zoneId = rs.getObject(2);
                        if (zoneId instanceof UUID uuid) {
                            result.put("zoneId", uuid);
                        }
                    }
                    return null;
                }
        );
        return result;
    }

    private Map<String, Object> relationByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Map<String, Object> relation = new HashMap<>();
        relation.put("Code__c", code);
        return relation;
    }
}
