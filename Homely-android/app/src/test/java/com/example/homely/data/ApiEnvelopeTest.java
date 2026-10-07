package com.example.homely.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ApiEnvelopeTest {
    @Test public void objectAndLoginTokensAreUnwrappedOnce() throws Exception {
        JSONObject payload = ApiEnvelope.data("{\"statusCode\":200,\"message\":\"OK\",\"data\":{\"access_token\":\"access\",\"user\":{\"id\":7}}}", 200);
        assertEquals("access", payload.getString("access_token"));
        assertEquals(7, payload.getJSONObject("user").getInt("id"));
        assertFalse(payload.has("statusCode"));
    }

    @Test public void arraysAndPagesKeepTheShapeUsedByScreens() throws Exception {
        assertEquals(7, ApiEnvelope.data("{\"statusCode\":200,\"message\":\"OK\",\"data\":[{\"id\":7}]}", 200)
                .getJSONArray("data").getJSONObject(0).getInt("id"));
        JSONObject page = ApiEnvelope.data("{\"statusCode\":200,\"message\":\"OK\",\"data\":{\"data\":[],\"total_elements\":0,\"page_size\":20}}", 200);
        assertEquals(0, page.getJSONArray("data").length());
        assertEquals(20, page.getInt("page_size"));
    }

    @Test public void nullAndNoContentSuccessProduceEmptyLocalObjects() throws Exception {
        assertEquals(0, ApiEnvelope.data("{\"statusCode\":200,\"message\":\"OK\",\"data\":null}", 200).length());
        assertEquals(0, ApiEnvelope.data("", 204).length());
    }

    @Test public void wrappedErrorsExposeSafeDetailAndNonJsonErrorsUseFallback() {
        assertEquals("Account is inactive", ApiEnvelope.errorMessage("{\"statusCode\":403,\"message\":\"Forbidden\",\"data\":{\"code\":\"ACCOUNT_INACTIVE\",\"detail\":\"Account is inactive\"}}", 403, "Fallback"));
        assertEquals("Fallback", ApiEnvelope.errorMessage("<html>proxy error</html>", 502, "Fallback"));
    }

    @Test public void missingEnvelopeAndWrongHttpStatusAreRejected() {
        for (String raw : new String[]{"{\"id\":7}", "{\"statusCode\":201,\"message\":\"Created\",\"data\":{}}"}) {
            try { ApiEnvelope.data(raw, 200); fail("Invalid envelope accepted"); }
            catch (JSONException expected) { }
        }
    }
}
