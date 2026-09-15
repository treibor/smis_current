package com.smis.security;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.ServerRpcHandler;
import com.vaadin.flow.server.communication.UidlRequestHandler;

/** Version-pinned Flow hook: no servlet body wrapper, no change to other transports. */
public final class ValidatingUidlRequestHandler extends UidlRequestHandler {
    // Bounds buffering before Flow reads the same message. Rich-text input is capped at 100k characters.
    static final int MAX_MESSAGE_CHARACTERS = 1_048_576;
    private static final JsonFactory JSON = new JsonFactory();

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return "POST".equals(request.getMethod()) && super.canHandleRequest(request);
    }

    @Override
    protected ServerRpcHandler createRpcHandler() {
        return new ServerRpcHandler() {
            @Override
            public void handleRpc(UI ui, Reader reader, VaadinRequest request)
                    throws IOException, InvalidUIDLSecurityKeyException {
                String body = readValidated(reader);
                super.handleRpc(ui, new StringReader(body), request);
            }
        };
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        try {
            return super.synchronizedHandleRequest(session, request, response);
        } catch (InvalidRequest ignored) {
            response.setStatus(400);
            response.setContentType("text/plain;charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write("Invalid request");
            return true;
        }
    }

    static String readValidated(Reader reader) throws IOException {
        StringBuilder body = new StringBuilder();
        char[] buffer = new char[4096];
        int count;
        while ((count = reader.read(buffer)) != -1) {
            if (body.length() + count > MAX_MESSAGE_CHARACTERS) throw new InvalidRequest();
            body.append(buffer, 0, count);
        }
        // Flow explicitly tolerates empty messages. Do not change that behavior.
        if (body.length() == 0) return "";
        try (JsonParser parser = JSON.createParser(body.toString())) {
            if (parser.nextToken() != JsonToken.START_OBJECT) throw new InvalidRequest();
            boolean found = false;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.FIELD_NAME) throw new InvalidRequest();
                String name = parser.currentName();
                JsonToken value = parser.nextToken();
                if ("clientId".equals(name)) {
                    if (found || value != JsonToken.VALUE_NUMBER_INT) throw new InvalidRequest();
                    found = true;
                    // Flow's client and server both use signed int counters, including rollover.
                    parser.getIntValue();
                } else {
                    parser.skipChildren();
                }
            }
            // Missing clientId is Flow 24.2's explicit legacy -1 sentinel. Preserve it.
            if (parser.nextToken() != null) throw new InvalidRequest();
        } catch (com.fasterxml.jackson.core.JacksonException e) {
            // Parser exceptions may include input excerpts: never log or propagate them.
            throw new InvalidRequest();
        }
        return body.toString();
    }

    private static final class InvalidRequest extends RuntimeException {
        private InvalidRequest() { super("Invalid request", null, false, false); }
    }
}
