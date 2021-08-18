package software.amazon.awscdk.examples;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.lambda.VersionOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CorsLambdaCrudDynamodbStack CDK example for Java!
 */
class CorsLambdaCrudDynamodbStack extends Stack {
	public CorsLambdaCrudDynamodbStack(final Construct parent, final String name, final DeploymentType depType) {
		super(parent, name);

		final String prefix = depType.getValue()+"-";
		final String tableName = prefix+"items";
		final String apiName = prefix+"itemsApi";

		TableProps tableProps;
		Attribute partitionKey = Attribute.builder()
				.name("itemId")
				.type(AttributeType.STRING)
				.build();
		tableProps = TableProps.builder()
				.tableName(tableName)
				.partitionKey(partitionKey)
				// The default removal policy is RETAIN, which means that cdk destroy will not attempt to delete
				// the new table, and it will remain in your account until manually deleted. By setting the policy to
				// DESTROY, cdk destroy will delete the table (even if it has data in it)
				.removalPolicy(RemovalPolicy.DESTROY)
				.build();
		Table dynamodbTable = new Table(this, tableName, tableProps);


		Map<String, String> lambdaEnvMap = new HashMap<>();
		lambdaEnvMap.put("TABLE_NAME", dynamodbTable.getTableName());
		lambdaEnvMap.put("PRIMARY_KEY", "itemId");
		lambdaEnvMap.put("DISABLE_SIGNAL_HANDLERS", "true");


		Function getOneItemFunction = new Function(this, prefix+"getOneItemFunction",
				getLambdaFunctionProps(lambdaEnvMap,
						"software.amazon.awscdk.examples.lambda.GetOneItem",
						"./lambda-get/target/function.zip",
						depType));
		Function getAllItemsFunction = new Function(this, prefix+"getAllItemsFunction",
				getLambdaFunctionProps(lambdaEnvMap,
						"software.amazon.awscdk.examples.lambda.GetAllItems",
						"./lambda/target/function.zip",
						depType));
		Function createItemFunction = new Function(this, prefix+"createItemFunction",
				getLambdaFunctionProps(lambdaEnvMap,
						"software.amazon.awscdk.examples.lambda.CreateItem",
						"./lambda-add/target/function.zip",
						depType));
		Function updateItemFunction = new Function(this, prefix+"updateItemFunction",
				getLambdaFunctionProps(lambdaEnvMap,
						"software.amazon.awscdk.examples.lambda.UpdateItem",
						"./lambda-update/target/function.zip",
						depType));
		Function deleteItemFunction = new Function(this, prefix+"deleteItemFunction",
				getLambdaFunctionProps(lambdaEnvMap,
						"software.amazon.awscdk.examples.lambda.DeleteItem",
						"./lambda-delete/target/function.zip",
						depType));


		dynamodbTable.grantReadWriteData(getOneItemFunction);
		dynamodbTable.grantReadWriteData(getAllItemsFunction);
		dynamodbTable.grantReadWriteData(createItemFunction);
		dynamodbTable.grantReadWriteData(updateItemFunction);
		dynamodbTable.grantReadWriteData(deleteItemFunction);

		RestApi api = new RestApi(this, apiName,
				RestApiProps.builder().restApiName("Items Service").build());

		IResource items = api.getRoot().addResource("items");

		Integration getAllIntegration = new LambdaIntegration(getAllItemsFunction);
		items.addMethod("GET", getAllIntegration);

		Integration createOneIntegration = new LambdaIntegration(createItemFunction);
		items.addMethod("POST", createOneIntegration);
		addCorsOptions(items);


		IResource singleItem = items.addResource("{id}");
		Integration getOneIntegration = new LambdaIntegration(getOneItemFunction);
		singleItem.addMethod("GET", getOneIntegration);

		Integration updateOneIntegration = new LambdaIntegration(updateItemFunction);
		singleItem.addMethod("PATCH", updateOneIntegration);

		Integration deleteOneIntegration = new LambdaIntegration(deleteItemFunction);
		singleItem.addMethod("DELETE", deleteOneIntegration);
		addCorsOptions(singleItem);
	}


	private void addCorsOptions(IResource item) {
		List<MethodResponse> methoedResponses = new ArrayList<>();

		Map<String, Boolean> responseParameters = new HashMap<>();
		responseParameters.put("method.response.header.Access-Control-Allow-Headers", Boolean.TRUE);
		responseParameters.put("method.response.header.Access-Control-Allow-Methods", Boolean.TRUE);
		responseParameters.put("method.response.header.Access-Control-Allow-Credentials", Boolean.TRUE);
		responseParameters.put("method.response.header.Access-Control-Allow-Origin", Boolean.TRUE);
		methoedResponses.add(MethodResponse.builder()
				.responseParameters(responseParameters)
				.statusCode("200")
				.build());
		MethodOptions methodOptions = MethodOptions.builder()
				.methodResponses(methoedResponses)
				.build();

		Map<String, String> requestTemplate = new HashMap<>();
		requestTemplate.put("application/json", "{\"statusCode\": 200}");
		List<IntegrationResponse> integrationResponses = new ArrayList<>();

		Map<String, String> integrationResponseParameters = new HashMap<>();
		integrationResponseParameters.put("method.response.header.Access-Control-Allow-Headers", "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token,X-Amz-User-Agent'");
		integrationResponseParameters.put("method.response.header.Access-Control-Allow-Origin", "'*'");
		integrationResponseParameters.put("method.response.header.Access-Control-Allow-Credentials", "'false'");
		integrationResponseParameters.put("method.response.header.Access-Control-Allow-Methods", "'OPTIONS,GET,PUT,POST,DELETE'");
		integrationResponses.add(IntegrationResponse.builder()
				.responseParameters(integrationResponseParameters)
				.statusCode("200")
				.build());
		Integration methodIntegration = MockIntegration.Builder.create()
				.integrationResponses(integrationResponses)
				.passthroughBehavior(PassthroughBehavior.NEVER)
				.requestTemplates(requestTemplate)
				.build();

		item.addMethod("OPTIONS", methodIntegration, methodOptions);
	}

	private FunctionProps getLambdaFunctionProps(Map<String, String> lambdaEnvMap,
												 String handler,
												 String assetPath,
												 DeploymentType deploymentType) {
		return FunctionProps.builder()
				.code(Code.fromAsset(assetPath))
				.handler(DeploymentType.NATIVE == deploymentType ? "not.used.in.provided.runtime" : handler)
				.runtime(DeploymentType.NATIVE == deploymentType ? Runtime.PROVIDED : Runtime.JAVA_11)
				.environment(lambdaEnvMap)
				.timeout(Duration.seconds(30))
				.memorySize(1024)
				.description("Built on " + java.time.LocalDateTime.now())
				.currentVersionOptions(VersionOptions.builder().build())
				.tracing(Tracing.ACTIVE)
				.build();
	}

}
