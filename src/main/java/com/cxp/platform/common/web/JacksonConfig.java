package com.cxp.platform.common.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;

import java.io.IOException;

/**
 * Platform-wide Jackson serialization customizations.
 *
 * Pagination contract (PR Sprint-2/PR1):
 *   "page" is the canonical field for the page index.
 *   "number" is a transitional compatibility alias — deprecated, remove after
 *   all clients have migrated to "page".
 *
 * Registered globally via Jackson2ObjectMapperBuilderCustomizer so every
 * Page<T> controller return type is covered without per-controller changes.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer pageSerializerCustomizer() {
        return builder -> builder.serializerByType(Page.class, new PageSerializer());
    }

    /**
     * Serializes Spring Data Page<T> with a canonical "page" field for the page index.
     *
     * Fields emitted:
     *   content          — page items (serialized via registered type serializers)
     *   page             — canonical page index (0-based)
     *   number           — transitional alias for page; deprecated
     *   size             — requested page size
     *   totalElements    — total record count across all pages
     *   totalPages       — total page count
     *   numberOfElements — items in this page (may be < size on the last page)
     *   first            — true if this is the first page
     *   last             — true if this is the last page
     *   empty            — true if content is empty
     *
     * Omitted intentionally:
     *   pageable         — Spring-internal, not part of the public API contract
     *   sort             — omitted until sort exposure is governed; clients should
     *                      not parse Spring's internal sort representation
     */
    @SuppressWarnings("unchecked")
    private static class PageSerializer extends StdSerializer<Page<?>> {

        PageSerializer() {
            super((Class<Page<?>>) (Class<?>) Page.class);
        }

        @Override
        public void serialize(Page<?> page, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();

            provider.defaultSerializeField("content", page.getContent(), gen);

            // Canonical field — this is the platform contract going forward.
            gen.writeNumberField("page", page.getNumber());
            // Transitional alias — deprecated, removal target: Sprint 3.
            // Kept while clients migrate from Spring's default "number" to "page".
            gen.writeNumberField("number", page.getNumber());

            gen.writeNumberField("size", page.getSize());
            gen.writeNumberField("totalElements", page.getTotalElements());
            gen.writeNumberField("totalPages", page.getTotalPages());
            gen.writeNumberField("numberOfElements", page.getNumberOfElements());
            gen.writeBooleanField("first", page.isFirst());
            gen.writeBooleanField("last", page.isLast());
            gen.writeBooleanField("empty", page.isEmpty());

            gen.writeEndObject();
        }
    }
}
