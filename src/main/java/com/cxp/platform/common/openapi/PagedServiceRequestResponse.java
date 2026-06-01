package com.cxp.platform.common.openapi;

import com.cxp.platform.servicerequest.dto.ServiceRequestResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Documentation-only schema for the paginated service request list response.
 * Never instantiated at runtime — exists to give SDK generators a precise model.
 *
 * Inherits the pagination envelope fields from PagedResponse.
 * The content field is overridden to carry the specific item type.
 */
@Schema(description = "Paginated list of service requests. " +
                      "Use 'page' for the canonical page index. " +
                      "The 'number' field is a deprecated alias for 'page' and will be removed in a future release.")
public final class PagedServiceRequestResponse extends PagedResponse<ServiceRequestResponse> {

    @Schema(description = "Service request items for this page.")
    public List<ServiceRequestResponse> content;
}
