/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export;

import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

/**
 * @author Mohamed Benrejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Service
public class GridcapaExportService {

    private final RestTemplate restTemplate;
    private final FtpClientAdapter ftpClientAdapter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(GridcapaExportService.class);

    @Value("${task-manager.base-url}")
    private String taskManagerBaseUrl;

    public GridcapaExportService(RestTemplate restTemplate, FtpClientAdapter ftpClientAdapter) {
        this.restTemplate = restTemplate;
        this.ftpClientAdapter = ftpClientAdapter;
    }

    @Bean
    public Consumer<Flux<String>> consumeTaskStatusUpdate() {
        return f -> f
            .onErrorContinue((t, r) -> LOGGER.error(t.getMessage(), t))
            .map(this::convertEvent)
            .subscribe(this::transferSuccessfulTasksOutputs);
    }

    TaskStatusUpdate convertEvent(String eventString) {
        try {
            LOGGER.info("event received: {}", eventString);
            return objectMapper.readValue(eventString, TaskStatusUpdate.class);
        } catch (JsonProcessingException e) {
            String errorMessage = String.format("parsing exception occurred while reading TaskStatusUpdate event '%s', event will be ignored", eventString);
            LOGGER.error(errorMessage);
            throw new RuntimeException(errorMessage, e);
        }
    }

    void transferSuccessfulTasksOutputs(TaskStatusUpdate taskStatusUpdate) {
        if (taskStatusUpdate.getTaskStatus().equals(TaskStatus.SUCCESS)) {
            String outputsRestLocation = UriComponentsBuilder.fromHttpUrl(taskManagerBaseUrl + "/tasks/" + taskStatusUpdate.getId() + "/outputs-by-id").toUriString();
            ResponseEntity<byte[]> responseEntity = restTemplate.getForEntity(outputsRestLocation, byte[].class);
            String rawFileName = Optional.ofNullable(responseEntity.getHeaders().get("Content-Disposition")).map(at -> at.get(0)).orElse("outputs.zip");
            String fileNameHeaderIdentifier = "filename=";
            String zipOutputName = rawFileName.substring(rawFileName.lastIndexOf(fileNameHeaderIdentifier) + fileNameHeaderIdentifier.length() + 1, rawFileName.length() - 1);
            try {
                ftpClientAdapter.open();
                ftpClientAdapter.upload(zipOutputName, new ByteArrayInputStream(Objects.requireNonNull(responseEntity.getBody())));
                ftpClientAdapter.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                throw new RuntimeException("Runtime exception: ", e);
            }
        }
    }

}
