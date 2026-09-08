package com.smis.security.richtext;

import static org.assertj.core.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;

class RichTextMaintenanceTest {
    @Test
    void dryRunDoesNotWriteAndApplyIsIdempotentWithoutLoggingPayloads() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:maintenance;DATABASE_TO_LOWER=TRUE")) {
            try (var sql = connection.createStatement()) {
                sql.execute("create table installment (installment_id bigint primary key, copy_to varchar(2000))");
                sql.execute("insert into installment values (1, '<p onclick=secretPayload()>Safe</p>'), (2, '<p>Unchanged</p>')");
            }
            var bytes = new ByteArrayOutputStream();
            var log = new PrintStream(bytes);
            assertThat(RichTextMaintenance.run(connection, false, log))
                    .isEqualTo(new RichTextMaintenance.Summary(2, 1, 0, 0));
            try (var sql = connection.createStatement(); var rows = sql.executeQuery("select copy_to from installment where installment_id=1")) {
                rows.next();
                assertThat(rows.getString(1)).contains("secretPayload");
            }
            assertThat(RichTextMaintenance.run(connection, true, log))
                    .isEqualTo(new RichTextMaintenance.Summary(2, 1, 0, 1));
            assertThat(RichTextMaintenance.run(connection, true, log))
                    .isEqualTo(new RichTextMaintenance.Summary(2, 0, 0, 0));
            assertThat(bytes.toString()).contains("NORMALIZED table=installment id=1").doesNotContain("secretPayload", "<p>");
        }
    }
}
