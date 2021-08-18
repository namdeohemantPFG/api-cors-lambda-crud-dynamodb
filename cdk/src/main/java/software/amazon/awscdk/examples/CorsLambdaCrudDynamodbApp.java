package software.amazon.awscdk.examples;

import software.amazon.awscdk.core.App;

public class CorsLambdaCrudDynamodbApp {
	public static void main(final String[] args) {
		App app = new App();

		DeploymentType deploymentType = DeploymentType.JVM;

		if (deploymentType == DeploymentType.NATIVE) {
			new CorsLambdaCrudDynamodbStack(app,
                    "cdk-cors-lambda-crud-dynamodb-quarkus-native",
                    deploymentType);
		} else {
			new CorsLambdaCrudDynamodbStack(app,
                    "cdk-cors-lambda-crud-dynamodb-quarkus-jvm",
                    deploymentType);
		}

		app.synth();
	}
}
