/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
    private final Logger businessLogger;
    private static final Logger LOGGER = LoggerFactory.getLogger(GridcapaExportService.class);

    @Value("${task-manager.base-url}")
    private String taskManagerBaseUrl;

    public GridcapaExportService(RestTemplate restTemplate, FtpClientAdapter ftpClientAdapter, Logger businessLogger) {
        this.restTemplate = restTemplate;
        this.ftpClientAdapter = ftpClientAdapter;
        this.businessLogger = businessLogger;
    }

    @Bean
    public Consumer<Flux<TaskDto>> consumeTaskDtoUpdate() {
        return f -> f
            .onErrorContinue((t, r) -> LOGGER.error(t.getMessage(), t))
            .subscribe(this::exportOutputsForSuccessfulTasks);
    }

    void exportOutputsForSuccessfulTasks(TaskDto taskDto) {
        MDC.put("gridcapa-task-id", taskDto.getId().toString());
        boolean taskSuccessful = taskDto.getStatus().equals(TaskStatus.SUCCESS);
        if (taskSuccessful) {
            boolean allOutputsAvailable = false;
            int retryCounter = 0;
            while (retryCounter < 6 && !allOutputsAvailable) {
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Couldn't interrupt thread : {}", e.getMessage());
                }

                allOutputsAvailable = checkAllOutputFileValidated(getUpdatedTaskForTimestamp(taskDto.getTimestamp()));
                retryCounter++;
            }
            if (allOutputsAvailable) {
                businessLogger.info("task success event received, exporting results for: timestamp: {}", taskDto.getTimestamp());
                ResponseEntity<byte[]> responseEntity = getResponseEntity(taskDto.getTimestamp());
                String zipOutputName = getZipNameFromResponseEntity(responseEntity);
                try {
                    ftpClientAdapter.open();
                    ftpClientAdapter.upload(zipOutputName, new ByteArrayInputStream(Objects.requireNonNull(responseEntity.getBody())));
                    ftpClientAdapter.close();
                } catch (IOException e) {
                    throw new RuntimeException("Exception occurred: ", e);
                }
            } else {
                businessLogger.warn("Task success event received with missing output(s) for timestamp: {}. Results will not be exported.", taskDto.getTimestamp());
            }
        }
    }

    ResponseEntity<byte[]> getResponseEntity(OffsetDateTime timestamp) {
        String outputsRestLocation = UriComponentsBuilder.fromHttpUrl(taskManagerBaseUrl + "/tasks/" + timestamp + "/outputs").toUriString();
        return restTemplate.getForEntity(outputsRestLocation, byte[].class);
    }

    String getZipNameFromResponseEntity(ResponseEntity<byte[]> responseEntity) {
        String rawFileName = Optional.ofNullable(responseEntity.getHeaders().get("Content-Disposition")).map(at -> at.get(0)).orElse("outputs.zip");
        // filename coming from response entity header is formatted with double-quotes such as "filename="---real_filename---""
        String fileNameHeaderIdentifier = "filename=";
        return rawFileName.substring(rawFileName.lastIndexOf(fileNameHeaderIdentifier) + fileNameHeaderIdentifier.length() + 1, rawFileName.length() - 1);
    }

    private boolean checkAllOutputFileValidated(TaskDto taskDtoUpdated) {
        return taskDtoUpdated.getOutputs().stream().allMatch(output -> output.getProcessFileStatus().equals(ProcessFileStatus.VALIDATED));
    }

    private TaskDto getUpdatedTaskForTimestamp(OffsetDateTime timestamp) {
        String restLocation = UriComponentsBuilder.fromHttpUrl(taskManagerBaseUrl + "/tasks/" + timestamp).toUriString();
        return restTemplate.getForEntity(restLocation, TaskDto.class).getBody();
    }
}
