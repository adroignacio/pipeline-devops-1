import pipeline.*

def call(String chosenStages){

	def utils  = new test.UtilMethods()

	def pipelineStages = (utils.isCIorCD().contains('ci')) ? ['buildAndTest','sonar','nexusCI'] : ['downloadNexus','runJar','rest','nexusCD']

	def stages = utils.getValidatedStages(chosenStages, pipelineStages)

	stages.each{
		stage(it){
			try {
				"${it}"()
			}
			catch(Exception e) {
				error "Stage ${it} tiene problemas: ${e}"
			}
		}
	}
}

def buildAndTest(){
	sh './gradlew clean build'
}

def sonar(){
	def sonarhome = tool 'sonar-scanner'
    sh "${sonarhome}/bin/sonar-scanner -Dsonar.projectKey=ejemplo-gradle -Dsonar.java.binaries=build"
}

def runJar(){
	sh "nohup bash gradlew bootRun &"
	sleep 20
}

def rest(){
	sh "curl -X GET http://localhost:8082/rest/mscovid/test?msg=testing"
}

def nexusCI(){
    nexusPublisher nexusInstanceId: 'nexus', nexusRepositoryId: 'test-nexus', packages: [[$class: 'MavenPackage', mavenAssetList: [[classifier: '', extension: 'jar', filePath: "build/DevOpsUsach2020-0.0.1.jar"]], mavenCoordinate: [artifactId: 'DevOpsUsach2020', groupId: 'com.devopsusach2020', packaging: 'jar', version: "0.0.1-${env.GIT_BRANCH}"]]]  
}

def downloadNexus(){
    sh "curl -X GET -u admin:admin http://localhost:8081/repository/test-nexus/com/devopsusach2020/DevOpsUsach2020/0.0.1/DevOpsUsach2020-0.0.1.jar"
}

def nexusCD(){
    nexusPublisher nexusInstanceId: 'nexus', nexusRepositoryId: 'test-nexus', packages: [[$class: 'MavenPackage', mavenAssetList: [[classifier: '', extension: 'jar', filePath: "DevOpsUsach2020-0.0.1.jar"]], mavenCoordinate: [artifactId: 'DevOpsUsach2020', groupId: 'com.devopsusach2020', packaging: 'jar', version: '1.0.0']]]  
}

return this;