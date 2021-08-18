package software.amazon.awscdk.examples;

public enum DeploymentType {

	NATIVE("native"), JVM("jvm");

	private String value;

	DeploymentType(String value) {
		this.value = value;
	}

	public String getValue(){
		return this.value;
	}
}
