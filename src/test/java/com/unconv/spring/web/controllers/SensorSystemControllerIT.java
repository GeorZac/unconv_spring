package com.unconv.spring.web.controllers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.unconv.spring.common.AbstractIntegrationTest;
import com.unconv.spring.consts.SensorLocationType;
import com.unconv.spring.domain.SensorLocation;
import com.unconv.spring.domain.SensorSystem;
import com.unconv.spring.persistence.SensorSystemRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class SensorSystemControllerIT extends AbstractIntegrationTest {
    @Autowired private WebApplicationContext webApplicationContext;

    @Autowired private SensorSystemRepository sensorSystemRepository;

    private List<SensorSystem> sensorSystemList = null;

    private final SensorLocation sensorLocation =
            new SensorLocation(
                    UUID.randomUUID(), "Parthenon", 37.9715, 23.7269, SensorLocationType.OUTDOOR);

    @BeforeEach
    void setUp() {
        this.mockMvc =
                MockMvcBuilders.webAppContextSetup(webApplicationContext)
                        .defaultRequest(
                                MockMvcRequestBuilders.get("/SensorSystem")
                                        .with(user("username").roles("USER")))
                        .apply(springSecurity())
                        .build();

        sensorSystemRepository.deleteAllInBatch();

        sensorSystemList = new ArrayList<>();
        sensorSystemList.add(new SensorSystem(null, "First SensorSystem", sensorLocation));
        sensorSystemList.add(new SensorSystem(null, "Second SensorSystem", sensorLocation));
        sensorSystemList.add(new SensorSystem(null, "Third SensorSystem", sensorLocation));
        sensorSystemList = sensorSystemRepository.saveAll(sensorSystemList);
    }

    @Test
    void shouldFetchAllSensorSystemsInAscendingOrder() throws Exception {
        this.mockMvc
                .perform(get("/SensorSystem").param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size()", is(sensorSystemList.size())))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.pageNumber", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.isFirst", is(true)))
                .andExpect(jsonPath("$.isLast", is(true)))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.hasPrevious", is(false)));
    }

    @Test
    void shouldFetchAllSensorSystemsInDescendingOrder() throws Exception {
        this.mockMvc
                .perform(get("/SensorSystem").param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size()", is(sensorSystemList.size())))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.pageNumber", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.isFirst", is(true)))
                .andExpect(jsonPath("$.isLast", is(true)))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.hasPrevious", is(false)));
    }

    @Test
    void shouldFindSensorSystemById() throws Exception {
        SensorSystem sensorSystem = sensorSystemList.get(0);
        UUID sensorSystemId = sensorSystem.getId();

        this.mockMvc
                .perform(get("/SensorSystem/{id}", sensorSystemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sensorSystem.getId().toString())))
                .andExpect(jsonPath("$.text", is(sensorSystem.getSensorName())));
    }

    @Test
    void shouldCreateNewSensorSystem() throws Exception {
        SensorSystem sensorSystem = new SensorSystem(null, "New SensorSystem", sensorLocation);
        this.mockMvc
                .perform(
                        post("/SensorSystem")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sensorSystem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.sensorName", is(sensorSystem.getSensorName())));
    }

    @Test
    void shouldReturn400WhenCreateNewSensorSystemWithoutText() throws Exception {
        SensorSystem sensorSystem = new SensorSystem(null, null, null);

        this.mockMvc
                .perform(
                        post("/SensorSystem")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sensorSystem)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", is("application/problem+json")))
                .andExpect(
                        jsonPath(
                                "$.type",
                                is("https://zalando.github.io/problem/constraint-violation")))
                .andExpect(jsonPath("$.title", is("Constraint Violation")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.violations", hasSize(1)))
                .andExpect(jsonPath("$.violations[0].field", is("text")))
                .andExpect(jsonPath("$.violations[0].message", is("Text cannot be empty")))
                .andReturn();
    }

    @Test
    void shouldUpdateSensorSystem() throws Exception {
        SensorSystem sensorSystem = sensorSystemList.get(0);
        sensorSystem.setSensorName("Updated SensorSystem");

        this.mockMvc
                .perform(
                        put("/SensorSystem/{id}", sensorSystem.getId())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sensorSystem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sensorSystem.getId().toString())))
                .andExpect(jsonPath("$.text", is(sensorSystem.getSensorName())));
    }

    @Test
    void shouldDeleteSensorSystem() throws Exception {
        SensorSystem sensorSystem = sensorSystemList.get(0);

        this.mockMvc
                .perform(delete("/SensorSystem/{id}", sensorSystem.getId()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sensorSystem.getId().toString())))
                .andExpect(jsonPath("$.text", is(sensorSystem.getSensorName())));
    }

    @Test
    void shouldReturn404WhenFetchingNonExistingSensorSystem() throws Exception {
        UUID sensorSystemId = UUID.randomUUID();
        this.mockMvc
                .perform(get("/SensorSystem/{id}", sensorSystemId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistingSensorSystem() throws Exception {
        UUID sensorSystemId = UUID.randomUUID();
        SensorSystem sensorSystem = sensorSystemList.get(1);

        this.mockMvc
                .perform(
                        put("/SensorSystem/{id}", sensorSystemId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sensorSystem)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistingSensorSystem() throws Exception {
        UUID sensorSystemId = UUID.randomUUID();
        this.mockMvc
                .perform(delete("/SensorSystem/{id}", sensorSystemId).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
