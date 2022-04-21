package com.farao_community.farao.gridcapa.export;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GridcapaExportServiceTest {

    @Autowired
    private GridcapaExportService outputsToFtpService;

    @Test
    void checkUnknownEventThrowsException() {
        String taskStatusUpdateEvent = "{\n" +
            "  \"id\": \"1fdda469-53e9-4d63-a533-b935cffdd2f6\",\n" +
            "  \"taskStatus\": \"UNKNOWN\"\n" +
            "}";
        assertThrows(RuntimeException.class, () -> outputsToFtpService.convertEvent(taskStatusUpdateEvent));
    }

    @Test
    void checkTaskConversion() {
        String taskStatusUpdateEvent = "{\n" +
            "  \"id\": \"1fdda469-53e9-4d63-a533-b935cffdd2f6\",\n" +
            "  \"taskStatus\": \"SUCCESS\"\n" +
            "}";
        outputsToFtpService.convertEvent(taskStatusUpdateEvent);
        assertDoesNotThrow(() -> RuntimeException.class);
    }
}
