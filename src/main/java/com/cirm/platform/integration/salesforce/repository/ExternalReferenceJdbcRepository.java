package com.cirm.platform.integration.salesforce.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ExternalReferenceJdbcRepository implements ExternalReferenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExternalReferenceJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<String> findExternalId(String entityType, UUID entityUuid, UUID governingBodyId) {
        return jdbcTemplate.query(
                """
                SELECT er.external_id
                FROM external_reference er
                WHERE er.entity_type = ?
                  AND er.entity_uuid = ?
                  AND er.governing_body_id = ?
                ORDER BY er.created_at DESC NULLS LAST
                LIMIT 1
                """,
                ps -> {
                    ps.setString(1, entityType);
                    ps.setObject(2, entityUuid);
                    ps.setObject(3, governingBodyId);
                },
                rs -> rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty()
        );
    }
}
