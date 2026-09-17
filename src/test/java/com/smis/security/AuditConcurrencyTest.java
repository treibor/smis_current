package com.smis.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import com.smis.audit.Audit;
import com.smis.dbservice.AuditService;
import com.smis.dbservice.Dbservice;
import com.smis.entity.AuditTrail;
import com.smis.entity.Work;
import com.smis.entity.WorkNew;

class AuditConcurrencyTest {
    @ParameterizedTest
    @ValueSource(strings = {"work", "work-new", "login"})
    void simultaneousAuditWritesRetainEachRequestsIdentityAndDetails(String kind) throws Exception {
        var users = mock(Dbservice.class);
        var storage = mock(AuditService.class);
        var audit = spy(new Audit(users));
        ReflectionTestUtils.setField(audit, "auditservice", storage);
        var identity = new ThreadLocal<String>();
        var barrier = new CyclicBarrier(2);
        when(users.getloggeduser()).thenAnswer(invocation -> identity.get());
        doAnswer(invocation -> {
            barrier.await(5, TimeUnit.SECONDS);
            return "ip-" + identity.get();
        }).when(audit).getRealClientIp();
        Queue<AuditTrail> records = new ConcurrentLinkedQueue<>();
        doAnswer(invocation -> { records.add(invocation.getArgument(0)); return null; })
                .when(storage).updateAudit(any());
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> write(audit, identity, kind, "alice"));
            var second = executor.submit(() -> write(audit, identity, kind, "bob"));
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertEquals(2, records.size());
        var entries = records.toArray(AuditTrail[]::new);
        assertNotSame(entries[0], entries[1]);
        for (String name : new String[]{"alice", "bob"}) {
            var entry = records.stream().filter(record -> record.getAction().equals(name)).findFirst().orElseThrow();
            assertEquals("ip-" + name, entry.getIpAddress());
            assertTrue(entry.getDetails().contains(name));
            assertNotNull(entry.getActionOn());
            if (!kind.equals("login")) assertEquals(name, entry.getActionBy());
        }
    }

    private void write(Audit audit, ThreadLocal<String> identity, String kind, String name) {
        identity.set(name);
        try {
            switch (kind) {
                case "work" -> { var work = new Work(); work.setWorkName(name); audit.saveAudit(work, name); }
                case "work-new" -> { var work = new WorkNew(); work.setWorkName(name); audit.saveAudit(work, name); }
                default -> audit.saveLoginAudit(name, name);
            }
        } finally { identity.remove(); }
    }
}
