package com.cxp.platform.governance.controller;

import com.cxp.platform.governance.domain.City;
import com.cxp.platform.governance.domain.Department;
import com.cxp.platform.governance.domain.District;
import com.cxp.platform.governance.domain.GoverningBody;
import com.cxp.platform.governance.domain.State;
import com.cxp.platform.governance.domain.Ward;
import com.cxp.platform.governance.repository.CityRepository;
import com.cxp.platform.governance.repository.DepartmentRepository;
import com.cxp.platform.governance.repository.DistrictRepository;
import com.cxp.platform.governance.repository.GoverningBodyRepository;
import com.cxp.platform.governance.repository.StateRepository;
import com.cxp.platform.governance.repository.WardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit tests for the full governance hierarchy discovery:
 *   /states → /districts → /cities → /governing-bodies → /wards
 *
 * Uses standaloneSetup MockMvc — no Spring application context, no Flyway, no JPA.
 * Security posture (Bearer JWT required on /api/**) is enforced by SecurityConfig
 * and is not repeated here.
 */
@ExtendWith(MockitoExtension.class)
class GeographyControllerTest {

    private MockMvc mockMvc;

    @Mock private StateRepository stateRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private CityRepository cityRepository;
    @Mock private GoverningBodyRepository governingBodyRepository;
    @Mock private WardRepository wardRepository;
    @Mock private DepartmentRepository departmentRepository;

    @BeforeEach
    void setUp() {
        GeographyController controller = new GeographyController(
                stateRepository, districtRepository, cityRepository,
                governingBodyRepository, wardRepository, departmentRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── State discovery ───────────────────────────────────────────────────────

    @Test
    void listStates_returnsAlphabeticalList() throws Exception {
        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        when(stateRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(makeState(id1, "Rajasthan"), makeState(id2, "Uttarakhand")));

        mockMvc.perform(get("/api/v1/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(id1.toString()))
                .andExpect(jsonPath("$[0].name").value("Rajasthan"))
                .andExpect(jsonPath("$[1].id").value(id2.toString()))
                .andExpect(jsonPath("$[1].name").value("Uttarakhand"));
    }

    @Test
    void listStates_exposesOnlyIdAndName() throws Exception {
        when(stateRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(makeState(UUID.randomUUID(), "Uttarakhand")));

        mockMvc.perform(get("/api/v1/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].code").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].isActive").doesNotExist());
    }

    @Test
    void listStates_returnsEmptyListWhenNoneSeeded() throws Exception {
        when(stateRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── District discovery ────────────────────────────────────────────────────

    @Test
    void listDistricts_returnsDistrictsForKnownState() throws Exception {
        UUID stateId = UUID.randomUUID();
        UUID d1 = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID d2 = UUID.fromString("00000000-0000-0000-0000-000000000011");

        when(stateRepository.existsById(stateId)).thenReturn(true);
        when(districtRepository.findByStateIdOrderByNameAsc(stateId))
                .thenReturn(List.of(
                        makeDistrict(d1, "Dehradun", stateId),
                        makeDistrict(d2, "Haridwar", stateId)));

        mockMvc.perform(get("/api/v1/states/{stateId}/districts", stateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(d1.toString()))
                .andExpect(jsonPath("$[0].name").value("Dehradun"))
                .andExpect(jsonPath("$[1].id").value(d2.toString()))
                .andExpect(jsonPath("$[1].name").value("Haridwar"));
    }

    @Test
    void listDistricts_returns404ForUnknownState() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(stateRepository.existsById(unknownId)).thenReturn(false);

        mockMvc.perform(get("/api/v1/states/{stateId}/districts", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listDistricts_returnsEmptyListForStateWithNoDistricts() throws Exception {
        UUID stateId = UUID.randomUUID();
        when(stateRepository.existsById(stateId)).thenReturn(true);
        when(districtRepository.findByStateIdOrderByNameAsc(stateId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/states/{stateId}/districts", stateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── City discovery ────────────────────────────────────────────────────────

    @Test
    void listCities_returnsActiveCitiesForKnownDistrict() throws Exception {
        UUID districtId = UUID.randomUUID();
        UUID stateId    = UUID.randomUUID();
        UUID c1 = UUID.fromString("00000000-0000-0000-0000-000000000020");
        UUID c2 = UUID.fromString("00000000-0000-0000-0000-000000000021");

        when(districtRepository.existsById(districtId)).thenReturn(true);
        when(cityRepository.findByDistrictIdAndActiveTrueOrderByNameAsc(districtId))
                .thenReturn(List.of(
                        makeCity(c1, "Dehradun",  districtId, stateId),
                        makeCity(c2, "Rishikesh", districtId, stateId)));

        mockMvc.perform(get("/api/v1/districts/{districtId}/cities", districtId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(c1.toString()))
                .andExpect(jsonPath("$[0].name").value("Dehradun"))
                .andExpect(jsonPath("$[1].id").value(c2.toString()))
                .andExpect(jsonPath("$[1].name").value("Rishikesh"));
    }

    @Test
    void listCities_returns404ForUnknownDistrict() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(districtRepository.existsById(unknownId)).thenReturn(false);

        mockMvc.perform(get("/api/v1/districts/{districtId}/cities", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listCities_returnsEmptyListWhenNoActiveCities() throws Exception {
        UUID districtId = UUID.randomUUID();
        when(districtRepository.existsById(districtId)).thenReturn(true);
        when(cityRepository.findByDistrictIdAndActiveTrueOrderByNameAsc(districtId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/districts/{districtId}/cities", districtId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listCities_doesNotLeakPersistenceFields() throws Exception {
        UUID districtId = UUID.randomUUID();
        when(districtRepository.existsById(districtId)).thenReturn(true);
        when(cityRepository.findByDistrictIdAndActiveTrueOrderByNameAsc(districtId))
                .thenReturn(List.of(makeCity(UUID.randomUUID(), "Dehradun", districtId, UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/districts/{districtId}/cities", districtId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].code").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    // ── Governing body discovery ──────────────────────────────────────────────

    @Test
    void listGoverningBodies_returnsActiveBodiesForKnownCity() throws Exception {
        UUID cityId = UUID.randomUUID();
        UUID gb1 = UUID.fromString("00000000-0000-0000-0000-000000000030");
        UUID gb2 = UUID.fromString("00000000-0000-0000-0000-000000000031");

        when(cityRepository.existsById(cityId)).thenReturn(true);
        when(governingBodyRepository.findByCityIdAndIsActiveTrueOrderByNameAsc(cityId))
                .thenReturn(List.of(
                        makeGoverningBody(gb1, "Dehradun Municipal Corporation"),
                        makeGoverningBody(gb2, "Rishikesh Municipal Council")));

        mockMvc.perform(get("/api/v1/cities/{cityId}/governing-bodies", cityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(gb1.toString()))
                .andExpect(jsonPath("$[0].name").value("Dehradun Municipal Corporation"))
                .andExpect(jsonPath("$[1].id").value(gb2.toString()))
                .andExpect(jsonPath("$[1].name").value("Rishikesh Municipal Council"));
    }

    @Test
    void listGoverningBodies_returns404ForUnknownCity() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(cityRepository.existsById(unknownId)).thenReturn(false);

        mockMvc.perform(get("/api/v1/cities/{cityId}/governing-bodies", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listGoverningBodies_returnsEmptyListForCityWithNoBodies() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(cityRepository.existsById(cityId)).thenReturn(true);
        when(governingBodyRepository.findByCityIdAndIsActiveTrueOrderByNameAsc(cityId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cities/{cityId}/governing-bodies", cityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listGoverningBodies_doesNotLeakPersistenceFields() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(cityRepository.existsById(cityId)).thenReturn(true);
        when(governingBodyRepository.findByCityIdAndIsActiveTrueOrderByNameAsc(cityId))
                .thenReturn(List.of(makeGoverningBody(UUID.randomUUID(), "Dehradun Municipal Corporation")));

        mockMvc.perform(get("/api/v1/cities/{cityId}/governing-bodies", cityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                // Internal fields must never leak: code, stateId, districtId, cityId, isActive
                .andExpect(jsonPath("$[0].code").doesNotExist())
                .andExpect(jsonPath("$[0].stateId").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    // ── Ward discovery ────────────────────────────────────────────────────────

    @Test
    void listWards_returnsWardsForKnownGoverningBody() throws Exception {
        UUID governingBodyId = UUID.randomUUID();
        UUID w1 = UUID.fromString("00000000-0000-0000-0000-000000000040");
        UUID w2 = UUID.fromString("00000000-0000-0000-0000-000000000041");

        when(governingBodyRepository.existsById(governingBodyId)).thenReturn(true);
        when(wardRepository.findByGoverningBodyIdOrderByNameAsc(governingBodyId))
                .thenReturn(List.of(
                        makeWard(w1, "Balawala",  governingBodyId),
                        makeWard(w2, "Dalanwala", governingBodyId)));

        mockMvc.perform(get("/api/v1/governing-bodies/{governingBodyId}/wards", governingBodyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(w1.toString()))
                .andExpect(jsonPath("$[0].name").value("Balawala"))
                .andExpect(jsonPath("$[1].id").value(w2.toString()))
                .andExpect(jsonPath("$[1].name").value("Dalanwala"));
    }

    @Test
    void listWards_returns404ForUnknownGoverningBody() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(governingBodyRepository.existsById(unknownId)).thenReturn(false);

        mockMvc.perform(get("/api/v1/governing-bodies/{governingBodyId}/wards", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listWards_returnsEmptyListForBodyWithNoWards() throws Exception {
        UUID governingBodyId = UUID.randomUUID();
        when(governingBodyRepository.existsById(governingBodyId)).thenReturn(true);
        when(wardRepository.findByGoverningBodyIdOrderByNameAsc(governingBodyId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/governing-bodies/{governingBodyId}/wards", governingBodyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listWards_doesNotLeakPersistenceFields() throws Exception {
        UUID governingBodyId = UUID.randomUUID();
        when(governingBodyRepository.existsById(governingBodyId)).thenReturn(true);
        when(wardRepository.findByGoverningBodyIdOrderByNameAsc(governingBodyId))
                .thenReturn(List.of(makeWard(UUID.randomUUID(), "Defence Colony", governingBodyId)));

        mockMvc.perform(get("/api/v1/governing-bodies/{governingBodyId}/wards", governingBodyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                // Ward entity has zoneId and governingBody — neither must appear in response
                .andExpect(jsonPath("$[0].zoneId").doesNotExist())
                .andExpect(jsonPath("$[0].governingBody").doesNotExist());
    }

    // ── Department discovery ──────────────────────────────────────────────────

    @Test
    void listDepartments_returnsDepartmentsForKnownGoverningBody() throws Exception {
        UUID governingBodyId = UUID.randomUUID();
        UUID d1 = UUID.fromString("00000000-0000-0000-0000-000000000050");
        UUID d2 = UUID.fromString("00000000-0000-0000-0000-000000000051");

        when(governingBodyRepository.existsById(governingBodyId)).thenReturn(true);
        when(departmentRepository.findByGoverningBodyIdAndIsActiveTrueAndIsDeletedFalseOrderByNameAsc(governingBodyId))
                .thenReturn(List.of(
                        makeDepartment(d1, "Solid Waste Management", "SWM", governingBodyId),
                        makeDepartment(d2, "Water Supply",          "WTR", governingBodyId)));

        mockMvc.perform(get("/api/v1/governing-bodies/{governingBodyId}/departments", governingBodyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(d1.toString()))
                .andExpect(jsonPath("$[0].name").value("Solid Waste Management"))
                .andExpect(jsonPath("$[1].id").value(d2.toString()))
                .andExpect(jsonPath("$[1].name").value("Water Supply"));
    }

    @Test
    void listDepartments_returns404ForUnknownGoverningBody() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(governingBodyRepository.existsById(unknownId)).thenReturn(false);

        mockMvc.perform(get("/api/v1/governing-bodies/{governingBodyId}/departments", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listDepartments_returnsEmptyListForBodyWithNoDepartments() throws Exception {
        UUID governingBodyId = UUID.randomUUID();
        when(governingBodyRepository.existsById(governingBodyId)).thenReturn(true);
        when(departmentRepository.findByGoverningBodyIdAndIsActiveTrueAndIsDeletedFalseOrderByNameAsc(governingBodyId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/governing-bodies/{governingBodyId}/departments", governingBodyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listDepartments_doesNotLeakPersistenceFields() throws Exception {
        UUID governingBodyId = UUID.randomUUID();
        when(governingBodyRepository.existsById(governingBodyId)).thenReturn(true);
        when(departmentRepository.findByGoverningBodyIdAndIsActiveTrueAndIsDeletedFalseOrderByNameAsc(governingBodyId))
                .thenReturn(List.of(makeDepartment(UUID.randomUUID(), "Water Supply", "WTR", governingBodyId)));

        mockMvc.perform(get("/api/v1/governing-bodies/{governingBodyId}/departments", governingBodyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                // Internal fields must never leak: code, governingBodyId, isActive, isDeleted
                .andExpect(jsonPath("$[0].code").doesNotExist())
                .andExpect(jsonPath("$[0].governingBodyId").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist())
                .andExpect(jsonPath("$[0].deleted").doesNotExist());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private State makeState(UUID id, String name) {
        State s = new State();
        s.setId(id);
        s.setName(name);
        return s;
    }

    private District makeDistrict(UUID id, String name, UUID stateId) {
        District d = new District();
        d.setId(id);
        d.setName(name);
        d.setStateId(stateId);
        return d;
    }

    private City makeCity(UUID id, String name, UUID districtId, UUID stateId) {
        City c = new City();
        c.setId(id);
        c.setName(name);
        c.setDistrictId(districtId);
        c.setStateId(stateId);
        c.setActive(true);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    private GoverningBody makeGoverningBody(UUID id, String name) {
        GoverningBody gb = new GoverningBody();
        gb.setId(id);
        gb.setName(name);
        gb.setActive(true);
        return gb;
    }

    private Ward makeWard(UUID id, String name, UUID governingBodyId) {
        GoverningBody gb = new GoverningBody();
        gb.setId(governingBodyId);
        gb.setName("Test Governing Body");
        Ward w = new Ward();
        w.setId(id);
        w.setName(name);
        w.setGoverningBody(gb);
        return w;
    }

    private Department makeDepartment(UUID id, String name, String code, UUID governingBodyId) {
        Department d = new Department();
        d.setId(id);
        d.setName(name);
        d.setCode(code);
        d.setGoverningBodyId(governingBodyId);
        d.setActive(true);
        d.setDeleted(false);
        return d;
    }
}
