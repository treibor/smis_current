package com.smis.security.richtext;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/** Explicit offline maintenance tool; never registered as a startup runner or HTTP endpoint. */
public final class RichTextMaintenance {
    private static final List<String> TABLES = List.of("installment", "installment_new");
    private static final int BATCH_SIZE = 200;
    public record Summary(long examined, long changed, long blocked, long updated) {}
    private record Row(long id, String html) {}

    public static void main(String[] args) {
        try {
            List<String> options = List.of(args);
            boolean apply = options.contains("--apply");
            Path propertiesPath = Path.of(argument(options, "--properties"));
            if (apply) {
                Path backup = Path.of(argument(options, "--backup"));
                if (!options.contains("--confirm-backup") || !Files.isRegularFile(backup) || Files.size(backup) == 0) {
                    throw new IllegalArgumentException("Apply requires a verified nonempty backup file and --confirm-backup.");
                }
            }
            Properties properties = new Properties();
            try (var input = Files.newInputStream(propertiesPath)) { properties.load(input); }
            try (Connection connection = DriverManager.getConnection(properties.getProperty("spring.datasource.url"),
                    properties.getProperty("spring.datasource.username"), properties.getProperty("spring.datasource.password"))) {
                Summary dryRun = run(connection, false, System.out);
                if (dryRun.blocked() > 0) throw new IllegalArgumentException("Oversized records require review before cleanup.");
                if (apply) {
                    long expected = Long.parseLong(argument(options, "--expected-changed"));
                    if (expected != dryRun.changed()) throw new IllegalArgumentException("Changed count differs from the reviewed dry run.");
                    run(connection, true, System.out);
                }
            }
        } catch (Exception exception) {
            // JDBC exceptions can contain row content and credentials: output only the type/state.
            System.err.println("Maintenance failed: type=" + exception.getClass().getSimpleName()
                    + (exception instanceof SQLException sql ? ", SQLState=" + sql.getSQLState() : "")
                    + ". Check arguments, connection access, backup confirmation and dry-run blocked counts.");
            System.exit(1);
        }
    }

    private static String argument(List<String> options, String key) {
        int index = options.indexOf(key);
        if (index < 0 || index + 1 >= options.size()) throw new IllegalArgumentException("Missing maintenance argument.");
        return options.get(index + 1);
    }

    /** Each write batch commits independently; compare-and-set prevents overwriting concurrent edits. */
    public static Summary run(Connection connection, boolean apply, PrintStream output) throws SQLException {
        if (!connection.getAutoCommit()) throw new IllegalArgumentException("Use a dedicated maintenance connection.");
        boolean oldReadOnly = connection.isReadOnly();
        long examined = 0, changed = 0, blocked = 0, updated = 0;
        connection.setReadOnly(!apply);
        connection.setAutoCommit(false);
        try {
            for (String table : TABLES) {
                // The table names are constants, never provided by an HTTP/CLI caller.
                try (var metadata = connection.getMetaData().getColumns(null, connection.getSchema(), table, "copy_to")) {
                    if (!metadata.next()) {
                        output.println("SKIP table=" + table + " reason=missing-column");
                        continue;
                    }
                    output.println("COLUMN table=" + table + " column=copy_to capacity=" + metadata.getInt("COLUMN_SIZE"));
                }
                long lastId = Long.MIN_VALUE;
                long upperId;
                try (var statement = connection.createStatement();
                     var result = statement.executeQuery("select max(installment_id) from " + table)) {
                    result.next();
                    upperId = result.getLong(1);
                }
                long images = 0, dataImages = 0, externalImages = 0, frames = 0;
                while (true) {
                    List<Row> rows = new ArrayList<>();
                    try (var select = connection.prepareStatement("select installment_id, copy_to from " + table
                            + " where installment_id > ? and installment_id <= ? order by installment_id limit " + BATCH_SIZE)) {
                        select.setLong(1, lastId);
                        select.setLong(2, upperId);
                        try (var results = select.executeQuery()) {
                            while (results.next()) rows.add(new Row(results.getLong(1), results.getString(2)));
                        }
                    }
                    if (rows.isEmpty()) break;
                    long batchUpdated = 0;
                    for (Row row : rows) {
                        lastId = row.id();
                        examined++;
                        String safe;
                        try { safe = RichTextHtmlSanitizer.INSTANCE.sanitizeForStorage(row.html()); }
                        catch (IllegalArgumentException exception) {
                            blocked++;
                            output.println("BLOCKED table=" + table + " id=" + row.id() + " reason=length-limit");
                            if (apply) throw new SQLException("Blocked maintenance batch.");
                            continue;
                        }
                        if (row.html() != null) {
                            var parsed = org.jsoup.Jsoup.parseBodyFragment(row.html());
                            images += parsed.select("img").size();
                            frames += parsed.select("iframe").size();
                            for (var image : parsed.select("img")) {
                                String src = image.attr("src").toLowerCase(java.util.Locale.ROOT);
                                if (src.startsWith("data:")) dataImages++;
                                if (src.startsWith("http:") || src.startsWith("https:") || src.startsWith("//")) externalImages++;
                            }
                        }
                        if (Objects.equals(row.html(), safe)) continue;
                        changed++;
                        output.println("NORMALIZED table=" + table + " id=" + row.id());
                        if (apply) {
                            try (var update = connection.prepareStatement("update " + table
                                    + " set copy_to = ? where installment_id = ? and copy_to is not distinct from ?")) {
                                update.setString(1, safe);
                                update.setLong(2, row.id());
                                update.setString(3, row.html());
                                if (update.executeUpdate() != 1) throw new SQLException("Concurrent edit; batch rolled back.");
                                batchUpdated++;
                            }
                        }
                    }
                    if (apply) connection.commit(); else connection.rollback();
                    updated += batchUpdated;
                    output.println("BATCH table=" + table + " throughId=" + lastId + " committedUpdates=" + batchUpdated);
                }
                output.println("CONTENT table=" + table + " images=" + images + " dataImages=" + dataImages
                        + " externalImages=" + externalImages + " iframes=" + frames);
            }
            output.println("SUMMARY mode=" + (apply ? "apply" : "dry-run") + " examined=" + examined
                    + " changed=" + changed + " blocked=" + blocked + " updated=" + updated);
            return new Summary(examined, changed, blocked, updated);
        } finally {
            connection.rollback();
            connection.setAutoCommit(true);
            connection.setReadOnly(oldReadOnly);
        }
    }
}
