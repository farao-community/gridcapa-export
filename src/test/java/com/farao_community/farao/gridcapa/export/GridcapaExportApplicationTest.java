/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@SpringBootTest
class GridcapaExportApplicationTest {

    @Autowired
    private StreamBridge streamBridge;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void whenSendMessages() {

        TaskDto taskDto1 = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f6"), OffsetDateTime.parse("2022-04-27T10:10Z"), TaskStatus.SUCCESS, createProcessFileList(1, 1), createProcessFileList(1, 1), new ArrayList<>(), new ArrayList<>());
        TaskDto taskDto2 = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f7"), OffsetDateTime.parse("2022-04-27T10:11Z"), TaskStatus.SUCCESS, createProcessFileList(1, 1), createProcessFileList(1, 1), new ArrayList<>(), new ArrayList<>());
        TaskDto taskDto3 = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f8"), OffsetDateTime.parse("2022-04-27T10:12Z"), TaskStatus.SUCCESS, createProcessFileList(1, 1), createProcessFileList(1, 1), new ArrayList<>(), new ArrayList<>());
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/file/AA0", byte[].class)).thenReturn(ResponseEntity.ok("test1".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:11Z/file/AA0", byte[].class)).thenThrow(RuntimeException.class);
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:12Z/file/AA0", byte[].class)).thenReturn(ResponseEntity.ok("test3".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z", TaskDto.class)).thenReturn(ResponseEntity.of(Optional.of(taskDto1)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:10Z/outputs", byte[].class)).thenReturn(ResponseEntity.ok("test1".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:12Z", TaskDto.class)).thenReturn(ResponseEntity.of(Optional.of(taskDto1)));
        Mockito.when(restTemplate.getForEntity("http://localhost:8080/tasks/2022-04-27T10:12Z/outputs", byte[].class)).thenReturn(ResponseEntity.ok("test1".getBytes(StandardCharsets.UTF_8)));

        assertTrue(streamBridge.send("consumeTaskDtoUpdate-in-0", MessageBuilder.withPayload(taskDto1)
                        .build()));
        assertTrue(streamBridge.send("consumeTaskDtoUpdate-in-0", MessageBuilder.withPayload(taskDto2)
                .build()));
        /* if we remove the new catch exception block in GridcapaExportService.exportOutputsForTask(), then this next line fails ! */
        assertTrue(streamBridge.send("consumeTaskDtoUpdate-in-0", MessageBuilder.withPayload(taskDto3)
                .build()));
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
