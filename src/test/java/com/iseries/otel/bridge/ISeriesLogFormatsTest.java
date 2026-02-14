package com.iseries.otel.bridge;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for iSeries-specific log formats.
 * Covers QHST (history log), job log, and message queue log formats.
 */
@SpringBootTest(args = "--spring.batch.job.enabled=false", properties = "spring.main.allow-bean-definition-overriding=true")
class ISeriesLogFormatsTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job logConversionJob;

    @Autowired
    private SdkLoggerProvider sdkLoggerProvider;

    private static final List<LogRecordData> EXPORTED_LOGS = Collections.synchronizedList(new ArrayList<>());

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public LogRecordExporter logRecordExporter() {
            return new LogRecordExporter() {
                @Override
                public CompletableResultCode export(Collection<LogRecordData> logs) {
                    EXPORTED_LOGS.addAll(logs);
                    return CompletableResultCode.ofSuccess();
                }

                @Override
                public CompletableResultCode flush() {
                    return CompletableResultCode.ofSuccess();
                }

                @Override
                public CompletableResultCode shutdown() {
                    return CompletableResultCode.ofSuccess();
                }
            };
        }
    }

    @BeforeEach
    void clearLogs() {
        EXPORTED_LOGS.clear();
    }

    @Test
    void testISeriesQhstLog() throws Exception {
        File sampleFile = new ClassPathResource("iseries_qhst.log").getFile();

        // Pattern: TIMESTAMP MSGID MESSAGE
        // Captures the iSeries message ID (e.g., CPI1125, CPF9801) as a separate attribute
        String grokPattern = "(?s)%{TIMESTAMP_ISO8601:timestamp} %{WORD:msgid} %{GREEDYDATA:message}";
        String filePattern = "^\\d{4}-\\d{2}-\\d{2}";

        JobParameters params = new JobParametersBuilder()
                .addString("input.file.path", sampleFile.getAbsolutePath())
                .addString("input.file.pattern", filePattern)
                .addString("parser.grok.pattern", grokPattern)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(logConversionJob, params);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        sdkLoggerProvider.forceFlush().join(5, TimeUnit.SECONDS);

        // 8 log entries: 6 single-line + 2 multiline (CPF9801 and CPD0034 have continuation lines)
        assertEquals(8, EXPORTED_LOGS.size(), "Should have processed 8 QHST log entries");

        // Verify first log has message ID as attribute
        LogRecordData firstLog = EXPORTED_LOGS.get(0);
        assertEquals("CPI1125",
                firstLog.getAttributes().asMap()
                        .get(io.opentelemetry.api.common.AttributeKey.stringKey("msgid")).toString());
        assertTrue(firstLog.getBody().asString().contains("Job 012345/QSYS/QSTRUPJD started"),
                "Body should contain job start message");

        // Verify multiline log (CPF9801 with library list)
        LogRecordData multilineLog = EXPORTED_LOGS.get(4);
        assertEquals("CPF9801",
                multilineLog.getAttributes().asMap()
                        .get(io.opentelemetry.api.common.AttributeKey.stringKey("msgid")).toString());
        assertTrue(multilineLog.getBody().asString().contains("Library list searched"),
                "Multiline body should contain diagnostic info");
    }

    @Test
    void testISeriesJobLog() throws Exception {
        File sampleFile = new ClassPathResource("iseries_joblog.log").getFile();

        // Job log has a header line followed by column header, then message entries
        // We parse the message entries using the MSGID TYPE SEV DATE TIME pattern
        // The first two lines (header + column header) will be parsed with fallback
        String grokPattern = "(?s)%{WORD:msgid} %{SPACE}%{WORD:msgtype} %{SPACE}%{INT:severity} %{SPACE}%{DATA:date} %{SPACE}%{DATA:time} %{SPACE}%{DATA:from_pgm} %{SPACE}%{DATA:from_inst} %{SPACE}%{DATA:to_pgm} %{SPACE}%{GREEDYDATA:message}";
        String filePattern = "^[A-Z]{3}\\d{4}|^5770SS1|^MSGID";

        JobParameters params = new JobParametersBuilder()
                .addString("input.file.path", sampleFile.getAbsolutePath())
                .addString("input.file.pattern", filePattern)
                .addString("parser.grok.pattern", grokPattern)
                .addLong("timestamp", System.currentTimeMillis() + 1)
                .toJobParameters();

        JobExecution execution = jobLauncher.run(logConversionJob, params);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        sdkLoggerProvider.forceFlush().join(5, TimeUnit.SECONDS);

        // At minimum, we should have parsed some records (header lines may fallback)
        assertTrue(EXPORTED_LOGS.size() >= 4,
                "Should have processed at least 4 job log entries, got: " + EXPORTED_LOGS.size());
    }

    @Test
    void testISeriesMessageQueueLog() throws Exception {
        File sampleFile = new ClassPathResource("iseries_msgq.log").getFile();

        // Pattern: TIMESTAMP MSGID MSGQ MESSAGE
        String grokPattern = "(?s)%{TIMESTAMP_ISO8601:timestamp} %{WORD:msgid} %{WORD:msgq} %{GREEDYDATA:message}";
        String filePattern = "^\\d{4}-\\d{2}-\\d{2}";

        JobParameters params = new JobParametersBuilder()
                .addString("input.file.path", sampleFile.getAbsolutePath())
                .addString("input.file.pattern", filePattern)
                .addString("parser.grok.pattern", grokPattern)
                .addLong("timestamp", System.currentTimeMillis() + 2)
                .toJobParameters();

        JobExecution execution = jobLauncher.run(logConversionJob, params);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        sdkLoggerProvider.forceFlush().join(5, TimeUnit.SECONDS);

        assertEquals(8, EXPORTED_LOGS.size(), "Should have processed 8 message queue log entries");

        // Verify message ID extraction
        LogRecordData diskWarning = EXPORTED_LOGS.get(4);
        assertEquals("CPF1816",
                diskWarning.getAttributes().asMap()
                        .get(io.opentelemetry.api.common.AttributeKey.stringKey("msgid")).toString());
        assertTrue(diskWarning.getBody().asString().contains("storage threshold"),
                "Disk warning body should mention storage threshold");

        // Verify multiline (disk warning with capacity details)
        assertTrue(diskWarning.getBody().asString().contains("Capacity:"),
                "Multiline body should contain capacity details");

        // Verify message queue attribute
        assertEquals("QSYSOPR",
                diskWarning.getAttributes().asMap()
                        .get(io.opentelemetry.api.common.AttributeKey.stringKey("msgq")).toString());
    }
}
