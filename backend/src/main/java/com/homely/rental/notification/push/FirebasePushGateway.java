package com.homely.rental.notification.push;
import com.google.firebase.*;
import com.google.firebase.messaging.*;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component @ConditionalOnProperty(name="homely.fcm.enabled",havingValue="true")
public class FirebasePushGateway implements PushGateway {
    private final FirebaseApp app;
    public FirebasePushGateway(@Value("${homely.fcm.project-id}") String projectId) throws java.io.IOException {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("FIREBASE_PROJECT_ID is required when FCM_ENABLED=true");
        }
        app=FirebaseApp.initializeApp(FirebaseOptions.builder().setProjectId(projectId)
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setConnectTimeout(10000).setReadTimeout(20000).build(),"homely-push");
    }
    @jakarta.annotation.PreDestroy void close() { app.delete(); }
    @Override public void send(String token, Map<String,String> data) throws PushFailure {
        try {
            FirebaseMessaging.getInstance(app).send(Message.builder().setToken(token).putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).setTtl(3600000).build()).build());
        } catch(FirebaseMessagingException e) {
            MessagingErrorCode code=e.getMessagingErrorCode();
            boolean invalid=code==MessagingErrorCode.UNREGISTERED;
            boolean retry=code==MessagingErrorCode.UNAVAILABLE || code==MessagingErrorCode.INTERNAL || code==MessagingErrorCode.QUOTA_EXCEEDED;
            throw new PushFailure(code==null?"FCM_ERROR":code.name(),retry,invalid);
        }
    }
}
