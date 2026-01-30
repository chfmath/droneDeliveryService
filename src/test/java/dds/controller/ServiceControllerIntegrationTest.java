package dds.controller;

import dds.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.google.gson.Gson;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
public class ServiceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final Gson gson = new Gson();

    @Test
    @DisplayName("Index endpoint should return welcome message")
    public void indexShouldReturnWelcomeMessage() throws Exception {
        mockMvc.perform(get("/api/v1/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Welcome from ILP")));
    }

    @Test
    @DisplayName("Distance endpoint should calculate distance correctly")
    public void distanceToShouldCalculateDistance() throws Exception {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(5.0, 12.0);
        DistanceRequest request = new DistanceRequest();
        request.setPosition1(p1);
        request.setPosition2(p2);

        mockMvc.perform(post("/api/v1/distanceTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("13.0"));
    }

    @Test
    @DisplayName("IsCloseTo endpoint should return correct proximity result")
    public void isCloseToShouldCheckProximity() throws Exception {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(0.0001, 0.0001);
        DistanceRequest closeRequest = new DistanceRequest();
        closeRequest.setPosition1(p1);
        closeRequest.setPosition2(p2);

        mockMvc.perform(post("/api/v1/isCloseTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(closeRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // Distant positions test
        Position p3 = new Position(0.0, 0.0);
        Position p4 = new Position(0.1, 0.1);
        DistanceRequest farRequest = new DistanceRequest();
        farRequest.setPosition1(p3);
        farRequest.setPosition2(p4);

        mockMvc.perform(post("/api/v1/isCloseTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(farRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("NextPosition endpoint should calculate next position correctly")
    public void nextPositionShouldCalculateCorrectly() throws Exception {
        Position start = new Position(0.0, 0.0);
        NextPositionRequest request = new NextPositionRequest();
        request.setStart(start);
        request.setAngle(0.0); // East

        mockMvc.perform(post("/api/v1/nextPosition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IsInRegion endpoint should determine if position is in region")
    public void isInRegionShouldCheckContainment() throws Exception {
        Position position = new Position(1.0, 1.0);
        Position[] vertices = {
                new Position(0.0, 0.0),
                new Position(2.0, 0.0),
                new Position(2.0, 2.0),
                new Position(0.0, 2.0),
                new Position(0.0, 0.0)
        };

        Region region = new Region("Test Region", vertices);
        RegionRequest request = new RegionRequest();
        request.setPosition(position);
        request.setRegion(region);

        mockMvc.perform(post("/api/v1/isInRegion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // Position outside region
        Position outsidePosition = new Position(3.0, 3.0);
        request.setPosition(outsidePosition);

        mockMvc.perform(post("/api/v1/isInRegion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
