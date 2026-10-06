package com.example.homely.data;
import okhttp3.*;
import org.json.JSONObject;
import com.example.homely.BuildConfig;

public class ChatSocket extends WebSocketListener {
    public interface Listener { void ready(); void changed(JSONObject message); void error(String message); }
    private final ApiClient api;
    private final Listener listener;
    private WebSocket socket;
    private boolean connected;
    private final StringBuilder buffer=new StringBuilder();
    public ChatSocket(ApiClient api,Listener listener){this.api=api;this.listener=listener;}
    public void connect(){
        String url=BuildConfig.API_URL.substring(0,BuildConfig.API_URL.lastIndexOf("/api/"))+"/ws";
        socket=api.http.newWebSocket(new Request.Builder().url(url).header("Authorization","Bearer "+api.session.token()).build(),this);
    }
    @Override public void onOpen(WebSocket ws,Response response){ws.send("CONNECT\naccept-version:1.2\nhost:homely\nheart-beat:0,0\n\n\u0000");}
    @Override public synchronized void onMessage(WebSocket ws,String text){
        buffer.append(text);
        int end;
        while((end=buffer.indexOf("\u0000"))>=0){
            String frame=buffer.substring(0,end).stripLeading();buffer.delete(0,end+1);
            if(frame.startsWith("CONNECTED")){
                connected=true;
                String[] queues={"chat.messages","chat.acks","chat.errors"};
                for(int i=0;i<queues.length;i++) ws.send("SUBSCRIBE\nid:"+i+"\ndestination:/user/queue/"+queues[i]+"\nack:auto\n\n\u0000");
                listener.ready();
            } else if(frame.startsWith("MESSAGE") || frame.startsWith("ERROR")){
                int split=frame.indexOf("\n\n");
                try{
                    JSONObject data=new JSONObject(frame.substring(split+2));
                    if(frame.contains("/chat.errors") || data.has("code"))listener.error(data.optString("detail","Không gửi được tin nhắn."));
                    else listener.changed(data);
                }catch(Exception e){listener.error("Không đọc được phản hồi chat. Hãy tải lại lịch sử.");}
            }
        }
    }
    public boolean send(JSONObject payload){
        return connected && socket!=null && socket.send("SEND\ndestination:/app/chat.send\ncontent-type:application/json\n\n"+payload+"\u0000");
    }
    public void close(){connected=false;if(socket!=null)socket.close(1000,"Leaving chat");}
    @Override public void onFailure(WebSocket ws,Throwable error,Response response){connected=false;listener.error("Chat mất kết nối. Bấm kết nối lại; tin đã gửi vẫn nằm trong lịch sử.");}
    @Override public void onClosed(WebSocket ws,int code,String reason){connected=false;}
}
