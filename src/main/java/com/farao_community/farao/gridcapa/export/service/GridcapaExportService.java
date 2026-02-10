/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export.service;

import com.farao_community.farao.gridcapa.export.adapter.ClientAdapter;
import com.farao_community.farao.gridcapa.export.config.UnzipExportFileConfiguration;
import com.farao_community.farao.gridcapa.export.exception.ClientAdapterException;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Mohamed Benrejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Service
public class GridcapaExportService {

    public static final String TASKS_PATH = "/tasks/";
    private final RestTemplate restTemplate;
    private final ClientAdapter clientAdapter;
    private final Logger businessLogger;
    private static final Logger LOGGER = LoggerFactory.getLogger(GridcapaExportService.class);

    @Value("${task-manager.base-url}")
    private String taskManagerBaseUrl;
    @Value("${task-manager.fetch-task.reties-number}")
    private int fetchTaskRetriesNumber;
    @Value("${task-manager.fetch-task.interval-in-seconds}")
    private int fetchTaskIntervalInSeconds;
    @Value("${export.seperate-output-files:false}")
    private boolean seperateOutputFiles;
    private final List<String> unzipFiles;

    public GridcapaExportService(RestTemplate restTemplate, ClientAdapter clientAdapter, Logger businessLogger, UnzipExportFileConfiguration unzipConfig) {
        this.restTemplate = restTemplate;
        this.clientAdapter = clientAdapter;
        this.businessLogger = businessLogger;
        this.unzipFiles = unzipConfig.unzipFiles();
    }

    @Bean
    public Consumer<Flux<TaskDto>> consumeTaskDtoUpdate() {
        return f -> f
                .onErrorContinue((t, r) -> LOGGER.error(t.getMessage(), t))
                .subscribe(this::exportOutputsForTask);
    }

    void exportOutputsForTask(final TaskDto taskDto) {
        try {
            MDC.put("gridcapa-task-id", taskDto.getId().toString());
            final boolean isTaskFinished = taskDto.getStatus().equals(TaskStatus.SUCCESS) || taskDto.getStatus().equals(TaskStatus.ERROR);
            if (isTaskFinished) {
                LOGGER.info("Received a task status {} event for timestamp: {}, trying to export result within the configured interval.", taskDto.getStatus(), taskDto.getTimestamp());
                final TaskDto taskDtoUpdated = fetchOutputsAvailable(taskDto);
                exportValidatedOutputsAndLog(taskDtoUpdated);
            }
        } catch (final Exception e) {
            //this exeption block avoids gridcapa export from deconnecting from spring cloud stream !
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void exportValidatedOutputsAndLog(final TaskDto taskDto) {
        businessLogger.info("Task status {}, exporting results for timestamp: {}", taskDto.getStatus(), taskDto.getTimestamp());
        if (seperateOutputFiles) {
            taskDto.getOutputs().stream().filter(processFileDto -> processFileDto.getProcessFileStatus().equals(ProcessFileStatus.VALIDATED))
                    .forEach(processFileDto -> {
                        final ResponseEntity<byte[]> responseEntity = getResponseEntityByFileType(taskDto.getTimestamp(), processFileDto.getFileType());
                        uploadToFtpFromResponseEntity(responseEntity, mustUnzip(processFileDto.getFileType()));
                    });
            final ResponseEntity<byte[]> responseEntity = getResponseEntityByFileType(taskDto.getTimestamp(), "LOGS");
            uploadToFtpFromResponseEntity(responseEntity, false);
        } else {
            final ResponseEntity<byte[]> responseEntity = getResponseEntity(taskDto.getTimestamp());
            uploadToFtpFromResponseEntity(responseEntity, false);
        }
    }

    private boolean mustUnzip(final String fileType) {
        return unzipFiles != null && unzipFiles.contains(fileType);
    }

    private void uploadToFtpFromResponseEntity(final ResponseEntity<byte[]> responseEntity, final boolean unzip) {
        String fileOutputName = getFileNameFromResponseEntity(responseEntity);
        // When not separating output files, append runtime UTC timestamp suffix before the extension
        if (!seperateOutputFiles) {
            final String suffix = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss").format(ZonedDateTime.now(ZoneOffset.UTC));
            if (fileOutputName.endsWith(".zip")) {
                final int dotIndex = fileOutputName.lastIndexOf('.');
                if (dotIndex > 0) {
                    fileOutputName = fileOutputName.substring(0, dotIndex) + "_" + suffix + fileOutputName.substring(dotIndex);
                } else {
                    fileOutputName = fileOutputName + "_" + suffix;
                }
            } else {
                fileOutputName = fileOutputName + "_" + suffix;
            }
        }
        try {
            LOGGER.info("Uploading file {} to ftp", fileOutputName);
            clientAdapter.upload(fileOutputName, unzip, new ByteArrayInputStream(Objects.requireNonNull(responseEntity.getBody())));
        } catch (final ClientAdapterException e) {
            businessLogger.error("Exception occurred while uploading generated results to server, details: {}", e.getMessage());
        }
    }

    /**
     * Sometimes the files are not validated immediately with task status update, we retry to fetch task
     */
    private TaskDto fetchOutputsAvailable(final TaskDto taskDto) {
        LOGGER.info("Received a task status {} event for timestamp: {}, trying to fetch result within the configured interval.", taskDto.getStatus(), taskDto.getTimestamp());
        TaskDto updatedTaskDto;
        boolean allOutputsAvailable = checkAllOutputFileValidated(taskDto);
        int retryCounter = 0;
        do {
            try {
                TimeUnit.SECONDS.sleep(fetchTaskIntervalInSeconds);
                LOGGER.info("Fetching outputs for iteration number {}", retryCounter);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Couldn't interrupt thread : {}", e.getMessage());
            }
            updatedTaskDto = getUpdatedTaskForTimestamp(taskDto.getTimestamp());
            if (updatedTaskDto != null) {
                allOutputsAvailable = checkAllOutputFileValidated(updatedTaskDto);
            }
            retryCounter++;
        } while (retryCounter < fetchTaskRetriesNumber && !allOutputsAvailable);
        return updatedTaskDto != null ? updatedTaskDto : taskDto;
    }

    ResponseEntity<byte[]> getResponseEntity(final OffsetDateTime timestamp) {
        final String outputsRestLocation = UriComponentsBuilder.fromUriString(taskManagerBaseUrl + TASKS_PATH + timestamp + "/outputs").toUriString();
        return restTemplate.getForEntity(outputsRestLocation, byte[].class);
    }

    ResponseEntity<byte[]> getResponseEntityByFileType(final OffsetDateTime timestamp, final String fileType) {
        final String outputsRestLocation = UriComponentsBuilder.fromUriString(taskManagerBaseUrl + TASKS_PATH + timestamp + "/file/" + fileType).toUriString();
        return restTemplate.getForEntity(outputsRestLocation, byte[].class);
    }

    String getFileNameFromResponseEntity(final ResponseEntity<byte[]> responseEntity) {
        final String rawFileName = Optional.ofNullable(responseEntity.getHeaders().get("Content-Disposition")).map(List::getFirst).orElse("outputs.zip");
        // filename coming from response entity header is formatted with double-quotes such as "filename="---real_filename---""
        final String fileNameHeaderIdentifier = "filename=";
        return rawFileName.substring(rawFileName.lastIndexOf(fileNameHeaderIdentifier) + fileNameHeaderIdentifier.length() + 1, rawFileName.length() - 1);
    }

    private boolean checkAllOutputFileValidated(final TaskDto taskDtoUpdated) {
        return taskDtoUpdated.getOutputs().stream().allMatch(output -> output.getProcessFileStatus().equals(ProcessFileStatus.VALIDATED));
    }

    private TaskDto getUpdatedTaskForTimestamp(final OffsetDateTime timestamp) {
        final String restLocation = UriComponentsBuilder.fromUriString(taskManagerBaseUrl + TASKS_PATH + timestamp).toUriString();
        return restTemplate.getForEntity(restLocation, TaskDto.class).getBody();
    }
}
