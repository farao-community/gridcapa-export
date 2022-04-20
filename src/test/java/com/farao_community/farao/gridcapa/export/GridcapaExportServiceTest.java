package com.farao_community.farao.gridcapa.export;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GridcapaExportServiceTest {

    @Autowired
    private GridcapaExportService outputsToFtpService;

    @MockBean
    private FtpClientAdapter ftpClientAdapter;

    @MockBean
    private RestTemplate restTemplate;

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

    //TODO
    /*
    @Test
    void checkSuccessfulTaskHandling() {
        TaskStatusUpdate taskStatusUpdate = new TaskStatusUpdate(UUID.randomUUID(), TaskStatus.SUCCESS);
        try {
            Mockito.when(restTemplate.getForEntity(Mockito.any(), Mockito.any())).thenReturn(ResponseEntity.accepted().build());
            Mockito.doNothing().when(ftpClientAdapter).open();
            Mockito.doNothing().when(ftpClientAdapter).upload(Mockito.any(), Mockito.any());
            Mockito.doNothing().when(ftpClientAdapter).close();
        } catch (IOException ignored) {
        }
        outputsToFtpService.transferSuccessfulTasksOutputs(taskStatusUpdate);
        assertDoesNotThrow(() -> RuntimeException.class);
    }*/
}
