/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export.service;

import com.farao_community.farao.gridcapa.export.adapter.FtpClientAdapter;
import com.farao_community.farao.gridcapa.export.exception.ClientAdapterException;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Mohamed Benrejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */

@SpringBootTest
class GridcapaExportServiceTest {

    @MockBean
    FtpClientAdapter ftpClientAdapter;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private GridcapaExportService outputsToFtpService;

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
        Assertions.assertEquals("out.zip", outputsToFtpService.getFileNameFromResponseEntity(responseEntity));
    }

    @Test
    void checkTaskManagerCallWithAllOutputsForSuccessTask() {
        ReflectionTestUtils.setField(outputsToFtpService, "seperateOutputFiles", false);
        TaskDto taskDto = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f6"), OffsetDateTime.parse("2022-04-27T10:10Z"), TaskStatus.SUCCESS, createProcessFileList(2, 2), createProcessFileList(2, 2), new ArrayList<>());
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/outputs", byte[].class)).thenReturn(ResponseEntity.ok("test".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z", TaskDto.class)).thenReturn(ResponseEntity.of(Optional.of(taskDto)));
        outputsToFtpService.exportOutputsForTask(taskDto);
        Mockito.verify(restTemplate, Mockito.atLeastOnce()).getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/outputs", byte[].class);
    }

    @Test
    void checkTaskManagerCallWithMissingFileForSuccessTask() {
        ReflectionTestUtils.setField(outputsToFtpService, "seperateOutputFiles", false);
        TaskDto taskDto = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f6"), OffsetDateTime.parse("2022-04-27T10:11Z"), TaskStatus.SUCCESS, createProcessFileList(2, 1), createProcessFileList(2, 1), new ArrayList<>());
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z/outputs", byte[].class)).thenReturn(ResponseEntity.ok("test".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z", TaskDto.class)).thenReturn(ResponseEntity.of(Optional.of(taskDto)));
        outputsToFtpService.exportOutputsForTask(taskDto);
        Mockito.verify(restTemplate, Mockito.times(1)).getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z/outputs", byte[].class);
    }

    @Test
    void checkTaskManagerCallWithMissingFileForErrorTask() {
        ReflectionTestUtils.setField(outputsToFtpService, "seperateOutputFiles", false);
        TaskDto taskDto = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f6"), OffsetDateTime.parse("2022-04-27T10:11Z"), TaskStatus.ERROR, createProcessFileList(2, 1), createProcessFileList(2, 1), new ArrayList<>());
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z/outputs", byte[].class)).thenReturn(ResponseEntity.ok("test".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z", TaskDto.class)).thenReturn(ResponseEntity.of(Optional.of(taskDto)));
        outputsToFtpService.exportOutputsForTask(taskDto);
        Mockito.verify(restTemplate, Mockito.times(1)).getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z/outputs", byte[].class);
    }

    @Test
    void checkTaskManagerCallWithLogFileForErrorTask() {
        ReflectionTestUtils.setField(outputsToFtpService, "seperateOutputFiles", false);
        TaskDto taskDto = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f6"), OffsetDateTime.parse("2022-04-27T10:11Z"), TaskStatus.ERROR, createProcessFileList(2, 0), createProcessFileList(2, 0), new ArrayList<>());
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z/outputs", byte[].class)).thenReturn(ResponseEntity.ok("test".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z", TaskDto.class)).thenReturn(ResponseEntity.of(Optional.of(taskDto)));
        outputsToFtpService.exportOutputsForTask(taskDto);
        Mockito.verify(restTemplate, Mockito.times(1)).getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z/outputs", byte[].class);
    }

    @Test
    void checkTaskManagerCallWithMissingFileForErrorTaskWithoutOutputs() {
        ReflectionTestUtils.setField(outputsToFtpService, "seperateOutputFiles", false);
        TaskDto taskDto = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f6"), OffsetDateTime.parse("2022-04-27T10:11Z"), TaskStatus.ERROR, createProcessFileList(2, 0), createProcessFileList(2, 0), new ArrayList<>());
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z/outputs", byte[].class)).thenReturn(ResponseEntity.ok("test".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z", TaskDto.class)).thenReturn(ResponseEntity.of(Optional.of(taskDto)));
        outputsToFtpService.exportOutputsForTask(taskDto);
        Mockito.verify(restTemplate, Mockito.times(1)).getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z/outputs", byte[].class);
    }

    @Test
    void checkTaskManagerCallForSeperateZipFilesForSuccessTask() {
        ReflectionTestUtils.setField(outputsToFtpService, "seperateOutputFiles", true);
        TaskDto taskDto = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f6"), OffsetDateTime.parse("2022-04-27T10:10Z"), TaskStatus.SUCCESS, createProcessFileList(3, 3), createProcessFileList(3, 3), new ArrayList<>());
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/file/AA0", byte[].class)).thenReturn(ResponseEntity.ok("test1".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/file/AA1", byte[].class)).thenReturn(ResponseEntity.ok("test2".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/file/AA2", byte[].class)).thenReturn(ResponseEntity.ok("test3".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/file/LOGS", byte[].class)).thenReturn(ResponseEntity.ok("rao-logs.zip".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z", TaskDto.class)).thenReturn(ResponseEntity.of(Optional.of(taskDto)));
        outputsToFtpService.exportOutputsForTask(taskDto);
        Mockito.verify(restTemplate, Mockito.atLeastOnce()).getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/file/AA0", byte[].class);
        Mockito.verify(restTemplate, Mockito.atLeastOnce()).getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/file/AA1", byte[].class);
        Mockito.verify(restTemplate, Mockito.atLeastOnce()).getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/file/AA2", byte[].class);
        Mockito.verify(restTemplate, Mockito.atLeastOnce()).getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/file/LOGS", byte[].class);
        try {
            Mockito.verify(ftpClientAdapter, Mockito.times(4)).upload(Mockito.anyString(), Mockito.any());
        } catch (ClientAdapterException e) {
            Assertions.fail("checkTaskManagerCallForSeperateZipFiles : ftpClientAdapter should not throw IOException!");
        }
    }

    @Test
    void checkTaskManagerCallForLogsForErrorTask() {
        ReflectionTestUtils.setField(outputsToFtpService, "seperateOutputFiles", true);
        TaskDto taskDto = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f6"), OffsetDateTime.parse("2022-04-27T10:10Z"), TaskStatus.ERROR, createProcessFileList(3, 0), createProcessFileList(3, 0), new ArrayList<>());
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/file/LOGS", byte[].class)).thenReturn(ResponseEntity.ok("rao-logs.zip".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z", TaskDto.class)).thenReturn(ResponseEntity.of(Optional.of(taskDto)));
        outputsToFtpService.exportOutputsForTask(taskDto);
        Mockito.verify(restTemplate, Mockito.never()).getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/file/AA0", byte[].class);
        Mockito.verify(restTemplate, Mockito.atLeastOnce()).getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/file/LOGS", byte[].class);
        try {
            Mockito.verify(ftpClientAdapter, Mockito.times(1)).upload(Mockito.anyString(), Mockito.any());
        } catch (ClientAdapterException e) {
            Assertions.fail("checkTaskManagerCallForSeperateZipFiles : ftpClientAdapter should not throw IOException!");
        }
    }

    private List<ProcessFileDto> createProcessFileList(int total, int nbValidated) {
        List<ProcessFileDto> result = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            ProcessFileDto file = new ProcessFileDto("/AA" + i, "/AA" + i,
                    (i < nbValidated) ? ProcessFileStatus.VALIDATED : ProcessFileStatus.NOT_PRESENT,
                    "aa" + i, OffsetDateTime.now());
            result.add(file);
        }
        return result;
    }
}
