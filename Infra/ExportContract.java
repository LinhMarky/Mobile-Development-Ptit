import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import java.nio.file.*;
import java.util.*;

/** Usage: java -cp <snakeyaml.jar> Infra/ExportContract.java <runtime.json> <openapi.yaml> */
public class ExportContract {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Yaml yaml=new Yaml();
        Map<String,Object> current=yaml.load(Files.readString(Path.of(args[0])));
        Path target=Path.of(args[1]);
        Map<String,Object> previous=yaml.load(Files.readString(target));
        current.put("servers",List.of(Map.of("url","http://localhost:8080","description","Local backend; paths include /api/v1")));
        current.put("x-stomp",previous.get("x-stomp"));
        var components=(Map<String,Object>)current.get("components");
        var schemas=(Map<String,Object>)components.get("schemas");
        var oldComponents=(Map<String,Object>)previous.get("components");
        ((Map<String,Object>)oldComponents.get("schemas")).forEach(schemas::putIfAbsent);
        schemas.put("ChatProblem", Map.of("$ref", "#/components/schemas/ProblemDTO"));
        schemas.put("ProblemDetails", Map.of("$ref", "#/components/schemas/ProblemDTO"));
        schemas.put("WebhookPayload",Map.of("type","object","required",List.of("provider_payment_id","event_id","status"),"properties",Map.of(
                "provider_payment_id",Map.of("type","string","maxLength",100),
                "event_id",Map.of("type","string","format","uuid"),
                "status",Map.of("type","string","enum",List.of("SUCCEEDED","FAILED")),
                "failure_code",Map.of("type","string","maxLength",100))));
        ((Map<String,Object>)current.get("paths")).put("/webhooks/payments/mock",Map.of("post",Map.of(
                "operationId","handleMockWebhook","summary","Signed sandbox webhook (only when MOCK_PAYMENT_ENABLED=true)",
                "parameters",List.of(Map.of("name","X-Mock-Signature","in","header","required",true,"schema",Map.of("type","string"),
                        "description","Hex HMAC-SHA256 over the exact request bytes, using MOCK_WEBHOOK_SECRET")),
                "requestBody",Map.of("required",true,"content",Map.of("application/json",Map.of("schema",Map.of("$ref","#/components/schemas/WebhookPayload")))),
                "responses",Map.of("200",Map.of("description","Processed or exact duplicate",
                                "content",Map.of("application/json",Map.of("schema",Map.of("$ref","#/components/schemas/RestResponseVoid")))),
                        "default",Map.of("description","Wrapped error; data contains ProblemDTO",
                                "content",Map.of("application/json",Map.of("schema",Map.of("$ref","#/components/schemas/RestResponseProblemDTO"))))))));
        verifyReferences(current,current);
        DumperOptions options=new DumperOptions();options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);options.setPrettyFlow(true);options.setSplitLines(false);
        Files.writeString(target,new Yaml(options).dump(current));
        System.out.println("Exported "+((Map<?,?>)current.get("paths")).size()+" paths; retained STOMP extension.");
    }
    static void verifyReferences(Object node, Map<String,Object> root) {
        if(node instanceof Map<?,?> map) {
            if(map.get("$ref") instanceof String ref && ref.startsWith("#/")) {
                Object current=root;
                for(String key:ref.substring(2).split("/")) {
                    if(!(current instanceof Map<?,?> values))throw new IllegalStateException("Broken reference: "+ref);
                    current=values.get(key.replace("~1","/").replace("~0","~"));
                }
                if(current==null)throw new IllegalStateException("Broken reference: "+ref);
            }
            map.values().forEach(value->verifyReferences(value,root));
        } else if(node instanceof List<?> list)list.forEach(value->verifyReferences(value,root));
    }
}
