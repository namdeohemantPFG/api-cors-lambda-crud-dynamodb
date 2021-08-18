package software.amazon.awscdk.examples.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.HashMap;
import java.util.Map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.inject.Named;

@Named("getAllItems")
public class GetAllItems implements RequestHandler<Object, GatewayResponse>{


    @Override
    public GatewayResponse handleRequest(Object input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Inside software.amazon.awscdk.examples.lambda: getAllItems ");

        String output = getData(context);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        //output = callPFGRest();
        return new GatewayResponse(output, headers, 200);
    }

    private String getData(Context context) {
        DynamoDbClient ddb = DynamoDbClient.builder()
                //.httpClient(ApacheHttpClient.create())
                .build();
        String tableName= System.getenv("TABLE_NAME");
        ScanRequest scanRequest= ScanRequest.builder()
                .tableName(tableName)
                .build();
        ScanResponse response = ddb.scan(scanRequest);
        return response.toString();
    }

    private String callPFGRest() {

		try {
                URL url = new URL("https://api.pilot.theprincipal.net/retirement/v1/benefitevent/actuator/health");
                HttpURLConnection con;
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                con.disconnect();
                System.out.println("Response: ****************** => "+content.toString());
                return content.toString();
            } catch (IOException e) {
                System.out.println("Response: ****************** => Error");
                e.printStackTrace();
                return e.getMessage();
            }
    }

}
