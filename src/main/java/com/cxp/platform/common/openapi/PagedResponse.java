package com.cxp.platform.common.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Documentation-only schema class describing the canonical platform pagination envelope.
 *
 * This class is never instantiated at runtime. It exists solely to give SDK generators
 * and Swagger UI a precise, governed model for paginated list responses.
 *
 * The actual serialization is performed by PageSerializer in JacksonConfig, which
 * overrides Spring Data's default Page<T> representation by:
 *   - Emitting "page" as the canonical 0-based page index
 *   - Emitting "number" as a deprecated compatibility alias for "page"
 *   - Excluding Spring-internal "pageable" and "sort" fields
 *
 * Usage in controllers: reference this class via @Schema(implementation = PagedServiceRequestResponse.class)
 * or a domain-specific subclass on the paginated @ApiResponse.
 */
@Schema(description = "Canonical platform pagination envelope. " +
                      "The 'page' field is canonical (0-based index). " +
                      "The 'number' field is a deprecated backward-compatibility alias for 'page' " +
                      "and will be removed in a future release — migrate SDK consumers to 'page'.")
public abstract class PagedResponse<T> {

    @Schema(description = "Page items.")
    public List<T> content;

    @Schema(description = "Canonical 0-based page index. Use this field — 'number' is a deprecated alias.",
            example = "0")
    public int page;

    @Schema(description = "Backward-compatibility alias for 'page'. Deprecated — use 'page' instead. " +
                          "Will be removed in a future release.",
            deprecated = true,
            example = "0")
    public int number;

    @Schema(description = "Number of items requested per page.", example = "20")
    public int size;

    @Schema(description = "Total number of items across all pages.", example = "142")
    public long totalElements;

    @Schema(description = "Total number of pages available.", example = "8")
    public int totalPages;

    @Schema(description = "Number of items in this page. May be less than 'size' on the last page.", example = "20")
    public int numberOfElements;

    @Schema(description = "True when this is the first page (page == 0).")
    public boolean first;

    @Schema(description = "True when this is the last page (page == totalPages - 1).")
    public boolean last;

    @Schema(description = "True when 'content' is empty.")
    public boolean empty;
}
