package com.homely.rental.notification.push;
import com.homely.rental.auth.repository.*;
import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.notification.entity.NotificationType;
import com.homely.rental.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="homely.fcm.enabled",havingValue="true")
public class PushWorker {
    private final PushDeliveryRepository deliveries;
    private final DeviceTokenRepository devices;
    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final PushGateway gateway;
    private final PlatformTransactionManager manager;
    record Attempt(Long id,int number,Long deviceId,String token,Map<String,String> data) {}

    @Scheduled(fixedDelayString="${homely.fcm.poll-ms:5000}")
    public void tick() {
        for(Long id:deliveries.due(Instant.now(),PageRequest.of(0,50))) {
            Attempt attempt=new TransactionTemplate(manager).execute(tx -> claim(id));
            if(attempt==null) continue;
            PushGateway.PushFailure failure=null;
            try { gateway.send(attempt.token(),attempt.data()); }
            catch(PushGateway.PushFailure e) { failure=e; }
            catch(RuntimeException e) { failure=new PushGateway.PushFailure("TRANSPORT_ERROR",true,false); }
            PushGateway.PushFailure result=failure;
            new TransactionTemplate(manager).executeWithoutResult(tx -> finish(attempt,result));
        }
    }
    private Attempt claim(Long id) {
        var d=deliveries.lock(id).orElse(null);
        if(d==null || !Set.of("PENDING","PROCESSING").contains(d.getStatus()) || d.getNextAttemptAt().isAfter(Instant.now())) return null;
        var n=notifications.findById(d.getNotificationId()).orElse(null);
        var device=devices.findById(d.getDeviceId()).orElse(null);
        if(d.getAttempts()>=8 || d.getCreatedAt().isBefore(Instant.now().minusSeconds(86400))) {
            d.setStatus("FAILED"); d.setLastError("DELIVERY_EXPIRED"); return null;
        }
        if(n==null || device==null || !device.isActive() || !device.getUser().getId().equals(n.getUser().getId())
                || n.getUser().getStatus()!=UserStatus.ACTIVE || n.getUser().isSuspended()) {
            d.setStatus("SKIPPED"); return null;
        }
        var pref=preferences.findByUserId(n.getUser().getId()).orElse(null);
        if(pref!=null && !(n.getType()==NotificationType.NEW_MESSAGE ? pref.isChatPush() : pref.isTransactionPush())) {
            d.setStatus("SKIPPED"); return null;
        }
        d.setStatus("PROCESSING"); d.setAttempts(d.getAttempts()+1);
        d.setNextAttemptAt(Instant.now().plusSeconds(180)); // lease; crashed workers become eligible again
        return new Attempt(id,d.getAttempts(),device.getId(),device.getToken(),Map.of(
                "notification_id",n.getId().toString(),"recipient_id",n.getUser().getId().toString(),
                "title",n.getTitle(),"body",n.getBody()==null?"":n.getBody(),
                "ref_type",n.getRefType()==null?"":n.getRefType(),"ref_id",n.getRefId()==null?"":n.getRefId().toString()));
    }
    private void finish(Attempt a,PushGateway.PushFailure failure) {
        var d=deliveries.lock(a.id()).orElseThrow();
        if(!"PROCESSING".equals(d.getStatus()) || d.getAttempts()!=a.number()) return;
        if(failure==null) { d.setStatus("SENT"); d.setLastError(null); return; }
        d.setLastError(failure.code);
        if(failure.invalidToken) devices.findById(a.deviceId()).ifPresent(device -> {
            if(device.getToken().equals(a.token())) device.setActive(false);
        });
        if(!failure.retryable || d.getAttempts()>=8) d.setStatus("FAILED");
        else {
            d.setStatus("PENDING");
            long delay=Math.min(3600,60L << Math.min(6,d.getAttempts()-1))+ThreadLocalRandom.current().nextLong(30);
            d.setNextAttemptAt(Instant.now().plusSeconds(delay));
        }
    }
}
