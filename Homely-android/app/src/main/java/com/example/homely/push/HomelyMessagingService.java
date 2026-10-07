package com.example.homely.push;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.homely.MainActivity;
import com.example.homely.data.*;
import com.google.firebase.messaging.*;

public class HomelyMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token){
        try { new ApiClient(new SessionStore(this)).registerPush(token); } catch(Exception ignored) { /* Login/onResume retries registration. */ }
    }
    @Override public void onMessageReceived(RemoteMessage message){
        SessionStore session=new SessionStore(this);
        var data=message.getData();
        if(!session.loggedIn() || !session.userId().equals(data.get("recipient_id")))return;
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager manager=getSystemService(NotificationManager.class);
        if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(new NotificationChannel("homely_updates","Tin nhắn và giao dịch",NotificationManager.IMPORTANCE_DEFAULT));
        Intent intent=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("ref_type",data.get("ref_type")).putExtra("ref_id",data.get("ref_id")).putExtra("recipient_id",data.get("recipient_id"));
        int id=String.valueOf(data.get("notification_id")).hashCode();
        PendingIntent pending=PendingIntent.getActivity(this,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.notify(id,new NotificationCompat.Builder(this,"homely_updates").setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(data.getOrDefault("title","Homely")).setContentText(data.getOrDefault("body","Có cập nhật mới"))
                .setContentIntent(pending).setAutoCancel(true).build());
    }
}
