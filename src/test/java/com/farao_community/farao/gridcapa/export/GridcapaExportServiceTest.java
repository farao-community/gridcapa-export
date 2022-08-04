package com.farao_community.farao.gridcapa.export;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class GridcapaExportServiceTest {

    @Autowired
    private GridcapaExportService outputsToFtpService;

    @MockBean
    FtpClientAdapter ftpClientAdapter;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void checkFileNameRetrievedCorrectlyFromHeader() {
        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON);
        header.put("Content-Disposition", List.of("filename=\"out.zip\""));
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(
            "test-body".getBytes(StandardCharsets.UTF_8),
            header,
            HttpStatus.OK
        );
        Assertions.assertEquals("out.zip", outputsToFtpService.getZipNameFromResponseEntity(responseEntity));
    }

    @Test
    void checkTaskManagerCall() {
        TaskDto taskDto = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f6"), OffsetDateTime.parse("2022-04-27T10:10Z"), TaskStatus.SUCCESS, new ArrayList<>(), new ArrayList<>(), createProcessFileList(2, 2), new ArrayList<>());
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/outputs", byte[].class)).thenReturn(ResponseEntity.ok("test".getBytes(StandardCharsets.UTF_8)));
        outputsToFtpService.exportOutputsForSuccessfulTasks(taskDto);
        Mockito.verify(restTemplate, Mockito.atLeastOnce()).getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/outputs", byte[].class);
    }

    @Test
    void checkTaskManagerCallMissingFile() {
        TaskDto taskDto = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f6"), OffsetDateTime.parse("2022-04-27T10:11Z"), TaskStatus.SUCCESS, new ArrayList<>(), new ArrayList<>(), createProcessFileList(2, 1), new ArrayList<>());
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z/outputs", byte[].class)).thenReturn(ResponseEntity.ok("test".getBytes(StandardCharsets.UTF_8)));
        outputsToFtpService.exportOutputsForSuccessfulTasks(taskDto);
        Mockito.verify(restTemplate, Mockito.never()).getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z/outputs", byte[].class);
    }

    private List<ProcessFileDto> createProcessFileList(int total, int nbValidated) {

        List<ProcessFileDto> result = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            ProcessFileDto file = new ProcessFileDto("AA" + i,
                    (i < nbValidated) ? ProcessFileStatus.VALIDATED : ProcessFileStatus.NOT_PRESENT,
                    "aa" + i, OffsetDateTime.now(),
                    "http://fakeUrl/aa" + 1
            );
            result.add(file);
        }
        return result;
    }
}
