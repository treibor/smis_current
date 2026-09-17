package com.smis.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.*;
import com.vaadin.flow.server.communication.ServerRpcHandler;

class UidlValidationTest {
    private static String body(String id) {
        return "{\"csrfToken\":\"test-only-token\",\"rpc\":[],\"syncId\":0" + id + "}";
    }

    @ParameterizedTest
    @ValueSource(strings = {"2147483648", "4294967297", "-2147483649", "99999999999999999999999999999999", "1.5", "1.0", "\"1\"", "null"})
    void invalidClientIdReturns400ThroughRealFlowUidlHandler(String id) throws Exception {
        VaadinSession session = mock(VaadinSession.class, RETURNS_DEEP_STUBS);
        VaadinRequest request = mock(VaadinRequest.class, RETURNS_DEEP_STUBS);
        UI ui = mock(UI.class);
        when(session.getService().findUI(request)).thenReturn(ui);
        when(request.getReader()).thenReturn(new java.io.BufferedReader(new StringReader(body(",\"clientId\":" + id))));
        VaadinResponse response = mock(VaadinResponse.class);
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(writer));
        assertThat(new ValidatingUidlRequestHandler().synchronizedHandleRequest(session, request, response)).isTrue();
        verify(response).setStatus(400);
        assertThat(writer.toString()).isEqualTo("Invalid request");
        verifyNoInteractions(ui);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "2147483647", "-2147483648", "-1", "-2"})
    void signedProtocolIntegersArePreservedExactly(String id) throws Exception {
        String message = body(",\"clientId\":" + id);
        assertThat(ValidatingUidlRequestHandler.readValidated(new StringReader(message))).isEqualTo(message);
    }

    @Test void legacyMissingAndEmptyArePreserved() throws Exception {
        assertThat(ValidatingUidlRequestHandler.readValidated(new StringReader(body("")))).isEqualTo(body(""));
        assertThat(ValidatingUidlRequestHandler.readValidated(new StringReader(""))).isEmpty();
    }

    @Test void malformedDuplicateAndOversizedMessagesFailClosed() {
        for (String message : new String[]{"{", "[]", "{}{}", "{\"clientId\":0,\"clientId\":1}",
                " ".repeat(ValidatingUidlRequestHandler.MAX_MESSAGE_CHARACTERS + 1)}) {
            assertThatThrownBy(() -> ValidatingUidlRequestHandler.readValidated(new StringReader(message)))
                    .isInstanceOf(RuntimeException.class).hasMessage("Invalid request");
        }
    }

    @Test void baselineReproducesNarrowingAndOriginWithoutLoggingToken() throws Exception {
        VaadinRequest request = mock(VaadinRequest.class, RETURNS_DEEP_STUBS);
        VaadinSession session = mock(VaadinSession.class, RETURNS_DEEP_STUBS);
        UI ui = mock(UI.class, RETURNS_DEEP_STUBS);
        when(ui.getSession()).thenReturn(session);
        when(ui.getInternals().getLastProcessedClientToServerId()).thenReturn(-1);
        String message = body(",\"clientId\":4294967297");
        assertThat(new ServerRpcHandler.RpcRequest(message, request).getClientToServerId()).isEqualTo(Integer.MAX_VALUE);
        Throwable failure = catchThrowable(() -> new ServerRpcHandler().handleRpc(ui, new StringReader(message), request));
        assertThat(failure).isInstanceOf(UnsupportedOperationException.class);
        assertThat(failure.getStackTrace()[0].getClassName()).isEqualTo(ServerRpcHandler.class.getName());
        assertThat(failure.getMessage()).doesNotContain("test-only-token");
        verify(ui.getInternals(), never()).setLastProcessedClientToServerId(anyInt(), any());
    }

    @Test void normalRpcStillProcessesAndOtherTransportsAreNotIntercepted() throws Exception {
        VaadinRequest request = mock(VaadinRequest.class, RETURNS_DEEP_STUBS);
        UI ui = mock(UI.class, RETURNS_DEEP_STUBS);
        when(ui.getInternals().getLastProcessedClientToServerId()).thenReturn(-1);
        new ValidatingUidlRequestHandler().createRpcHandler().handleRpc(ui,
                new StringReader(body(",\"clientId\":0")), request);
        verify(ui.getInternals()).setLastProcessedClientToServerId(eq(0), any());
        var handler = new ValidatingUidlRequestHandler();
        when(request.getMethod()).thenReturn("POST");
        for (String type : new String[]{"heartbeat", "upload", "push", "init", ""}) {
            when(request.getParameter("v-r")).thenReturn(type);
            assertThat(handler.canHandleRequest(request)).isFalse();
        }
        when(request.getParameter("v-r")).thenReturn("uidl");
        assertThat(handler.canHandleRequest(request)).isTrue();
        when(request.getMethod()).thenReturn("GET");
        assertThat(handler.canHandleRequest(request)).isFalse();
    }
}
