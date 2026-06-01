package com.cxp.platform.governance.controller;

import com.cxp.platform.governance.dto.CityResponse;
import com.cxp.platform.governance.dto.DepartmentResponse;
import com.cxp.platform.governance.dto.DistrictResponse;
import com.cxp.platform.governance.dto.GoverningBodyResponse;
import com.cxp.platform.governance.dto.StateResponse;
import com.cxp.platform.governance.dto.WardResponse;
import com.cxp.platform.governance.repository.CityRepository;
import com.cxp.platform.governance.repository.DepartmentRepository;
import com.cxp.platform.governance.repository.DistrictRepository;
import com.cxp.platform.governance.repository.GoverningBodyRepository;
import com.cxp.platform.governance.repository.StateRepository;
import com.cxp.platform.governance.repository.WardRepository;
import com.cxp.platform.common.openapi.StandardApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Read-only governance hierarchy discovery.
 *
 * Hierarchy traversal order (onboarding, dropdown population):
 *   GET /states
 *     → GET /states/{stateId}/districts
 *       → GET /districts/{districtId}/cities
 *         → GET /cities/{cityId}/governing-bodies
 *           → GET /governing-bodies/{governingBodyId}/wards        (ward selection)
 *           → GET /governing-bodies/{governingBodyId}/departments   (department selection)
 *
 * Intended consumers:
 *   - Onboarding: citizen traverses hierarchy to select their ward during registration.
 *   - Announcement targeting: operator scopes announcements to a ward or governing body.
 *   - Service request routing: ward drives the operational routing path.
 *   - Profile: callers resolve canonical names from IDs stored in UserProfile.
 *   - Future: SDK/OpenAPI publication for NGO and external integrations.
 *
 * Response shape contract (all endpoints):
 *   { "id": "<uuid>", "name": "<string>" }
 *   No additional fields. Timestamps, codes, FKs, and audit metadata must never appear.
 *
 * Ordering contract (all endpoints):
 *   Alphabetical by name (ASC) unless documented otherwise.
 *   Ordering is deterministic and stable — safe for FE dropdown caching.
 *
 * Empty-collection semantics (all child endpoints):
 *   200 + []   → parent is known; no children match the filter criteria.
 *   404        → parent UUID is unknown. Never confuse the two.
 *
 * Cacheability: all endpoints return reference/master data seeded via Flyway and changed
 * only through controlled migrations. Future candidates for Cache-Control / CDN caching
 * once public anonymous exposure is approved (planned for Wave 3+).
 *
 * Security: requires Bearer JWT (inherited from /api/** rule in SecurityConfig).
 * Public anonymous exposure requires explicit governance approval — planned for Wave 3+.
 *
 * Department scoping: departments are governing-body-scoped — each governing body
 * maintains its own department catalogue seeded via Flyway. Department codes (e.g. "WTR")
 * are unique within a governing body but NOT globally. There is no global /departments endpoint.
 */
@Tag(name = "Governance Discovery")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GeographyController {

    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final CityRepository cityRepository;
    private final GoverningBodyRepository governingBodyRepository;
    private final WardRepository wardRepository;
    private final DepartmentRepository departmentRepository;

    @Operation(
            summary     = "List all states",
            description = "Root of the governance hierarchy. Returns all seeded states alphabetically. " +
                          "Start here when traversing the hierarchy for onboarding or dropdown population."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "All states returned; empty list if none are seeded")
    @GetMapping("/states")
    public ResponseEntity<List<StateResponse>> listStates() {
        List<StateResponse> states = stateRepository.findAllByOrderByNameAsc()
                .stream()
                .map(s -> new StateResponse(s.getId(), s.getName()))
                .toList();
        return ResponseEntity.ok(states);
    }

    @Operation(
            summary     = "List districts within a state",
            description = "Returns districts under the given state, alphabetically. " +
                          "404 if the stateId is unknown; 200 + [] if the state has no districts. " +
                          "Next step in hierarchy: GET /districts/{districtId}/cities."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Districts returned; empty list if the state has no seeded districts")
    @ApiResponse(responseCode = "404", description = "stateId not found", content = @Content)
    @GetMapping("/states/{stateId}/districts")
    public ResponseEntity<List<DistrictResponse>> listDistricts(@PathVariable UUID stateId) {
        if (!stateRepository.existsById(stateId)) {
            return ResponseEntity.notFound().build();
        }
        List<DistrictResponse> districts = districtRepository.findByStateIdOrderByNameAsc(stateId)
                .stream()
                .map(d -> new DistrictResponse(d.getId(), d.getName()))
                .toList();
        return ResponseEntity.ok(districts);
    }

    @Operation(
            summary     = "List cities within a district",
            description = "Returns active cities under the given district, alphabetically. " +
                          "Inactive cities are excluded server-side. " +
                          "404 if districtId is unknown; 200 + [] if no active cities exist. " +
                          "Next step in hierarchy: GET /cities/{cityId}/governing-bodies."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Active cities returned; empty list if none are active")
    @ApiResponse(responseCode = "404", description = "districtId not found", content = @Content)
    @GetMapping("/districts/{districtId}/cities")
    public ResponseEntity<List<CityResponse>> listCities(@PathVariable UUID districtId) {
        if (!districtRepository.existsById(districtId)) {
            return ResponseEntity.notFound().build();
        }
        List<CityResponse> cities = cityRepository.findByDistrictIdAndActiveTrueOrderByNameAsc(districtId)
                .stream()
                .map(c -> new CityResponse(c.getId(), c.getName()))
                .toList();
        return ResponseEntity.ok(cities);
    }

    @Operation(
            summary     = "List governing bodies within a city",
            description = "Returns active civic authorities (Municipal Corporations, Nagar Panchayats, etc.) " +
                          "for the given city, alphabetically. The governingBodyId selected here is the " +
                          "tenant anchor for all subsequent ward and department lookups. " +
                          "404 if cityId is unknown; 200 + [] if no active governing bodies exist."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Active governing bodies returned; empty list if none are active")
    @ApiResponse(responseCode = "404", description = "cityId not found", content = @Content)
    @GetMapping("/cities/{cityId}/governing-bodies")
    public ResponseEntity<List<GoverningBodyResponse>> listGoverningBodies(@PathVariable UUID cityId) {
        if (!cityRepository.existsById(cityId)) {
            return ResponseEntity.notFound().build();
        }
        List<GoverningBodyResponse> bodies = governingBodyRepository
                .findByCityIdAndIsActiveTrueOrderByNameAsc(cityId)
                .stream()
                .map(gb -> new GoverningBodyResponse(gb.getId(), gb.getName()))
                .toList();
        return ResponseEntity.ok(bodies);
    }

    @Operation(
            summary     = "List wards within a governing body",
            description = "Returns all wards for the given governing body, alphabetically. " +
                          "Wards are the atomic civic unit — the wardId stored in the citizen profile " +
                          "drives announcement targeting and service-request routing. " +
                          "404 if governingBodyId is unknown; 200 + [] if no wards are seeded."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Wards returned; empty list if the governing body has no seeded wards")
    @ApiResponse(responseCode = "404", description = "governingBodyId not found", content = @Content)
    @GetMapping("/governing-bodies/{governingBodyId}/wards")
    public ResponseEntity<List<WardResponse>> listWards(@PathVariable UUID governingBodyId) {
        if (!governingBodyRepository.existsById(governingBodyId)) {
            return ResponseEntity.notFound().build();
        }
        List<WardResponse> wards = wardRepository
                .findByGoverningBodyIdOrderByNameAsc(governingBodyId)
                .stream()
                .map(w -> new WardResponse(w.getId(), w.getName()))
                .toList();
        return ResponseEntity.ok(wards);
    }

    @Operation(
            summary     = "List departments within a governing body",
            description = "Returns active, non-deleted departments for the given governing body, alphabetically. " +
                          "Department codes (e.g. WTR, SWM) are governing-body-scoped — the same code can exist " +
                          "under multiple governing bodies and must never be resolved globally. " +
                          "Use the returned departmentId when submitting a service request. " +
                          "404 if governingBodyId is unknown; 200 + [] if no active departments exist."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Active departments returned; empty list if none are active")
    @ApiResponse(responseCode = "404", description = "governingBodyId not found", content = @Content)
    @GetMapping("/governing-bodies/{governingBodyId}/departments")
    public ResponseEntity<List<DepartmentResponse>> listDepartments(@PathVariable UUID governingBodyId) {
        if (!governingBodyRepository.existsById(governingBodyId)) {
            return ResponseEntity.notFound().build();
        }
        List<DepartmentResponse> departments = departmentRepository
                .findByGoverningBodyIdAndIsActiveTrueAndIsDeletedFalseOrderByNameAsc(governingBodyId)
                .stream()
                .map(d -> new DepartmentResponse(d.getId(), d.getName()))
                .toList();
        return ResponseEntity.ok(departments);
    }
}
