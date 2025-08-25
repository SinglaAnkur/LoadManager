package com.venn.LoadManager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import com.venn.LoadManager.dto.LoadRequestDTO;
import com.venn.LoadManager.dto.LoadResponseDTO;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FileProcessingService implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(FileProcessingService.class);

    private final TransactionProcessingService service;
    private final ObjectMapper objectMapper;

    @Value( "${app.input.directory}")
    private String inputDirectory;

    private String backupDirectory = "src/main/resources/backup";

    private boolean running = true;

    public FileProcessingService(TransactionProcessingService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        Path inputDir = Paths.get(inputDirectory);
        Path backupDir = Paths.get(backupDirectory);
        Files.createDirectories(backupDir);

        log.info("Starting polling directory: {}", inputDir.toAbsolutePath());
        pollInputDirectory(inputDir, backupDir);
    }

    private void pollInputDirectory(Path inputDir, Path backupDir) {
        while (running) {
            try {
                processAvailableFiles(inputDir, backupDir);
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.info("Shutting down polling");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processAvailableFiles(Path inputDir, Path backupDir) {
        try {
            List<Path> files = listFilesByModifiedDate(inputDir);
            for (Path filePath : files) {
                processFile(filePath, backupDir);
            }

        } catch (Exception e) {
            log.error("Unexpected error during directory scan or file processing", e);
        }
    }

    private List<Path> listFilesByModifiedDate(Path inputDir) throws IOException {
        return Files.list(inputDir)
                .filter(Files::isRegularFile)
                .sorted(Comparator.comparingLong(this::getLastModifiedTime))
                .collect(Collectors.toList());
    }

    private void processFile(Path filePath, Path backupDir) {
        String fileName = filePath.getFileName().toString();
        log.info("Processing file: {}", fileName);

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            long read = 0;
            long processedWithResponse = 0;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    read++;
                    continue;
                }
                try {
                    LoadRequestDTO request = objectMapper.readValue(line, LoadRequestDTO.class);
                    Optional<LoadResponseDTO> responseOpt = service.process(request);
                    if (responseOpt.isPresent()) {
                        String out = objectMapper.writeValueAsString(responseOpt.get());
                        System.out.println(out);
                        processedWithResponse++;
                    }
                } catch (Exception e) {
                    log.warn("Skipping malformed line: {} | reason: {} \n", line, e.getMessage());
                }
                read++;
            }

            log.info("Finished file: {} | Lines Read={}, Responses Processed={}", fileName, read, processedWithResponse);
            backupFile(filePath, backupDir);
        } catch (Exception e) {
            log.error("Error processing file: {}. Will retry in next iteration.", fileName, e);
        }
    }

    private long getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }

    private void backupFile(Path sourceFile, Path backupDir) throws IOException {
        Path target = backupDir.resolve(sourceFile.getFileName().toString());
        Files.move(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Backup at: {}", backupDir.toAbsolutePath());
    }

    @javax.annotation.PreDestroy
    public void shutdown() {
        running = false;
        Thread.currentThread().interrupt();
    }
}
