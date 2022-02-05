import pipeline.*

def call(String chosenStages){

	def utils  = new test.UtilMethods()

	def pipelineStages = (utils.isCIorCD().contains('ci')) ? ['buildAndTest','sonar','runJar','rest','nexusCI'] : ['downloadNexus','runDownloadedJar','rest','nexusCD']

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
	sh "echo 'Build && Test!'"
    sh "gradle clean build"
}

def sonar(){
	sh "echo 'Análisis Estático!'"
        withSonarQubeEnv('sonarqube') {
            sh './gradlew sonarqube -Dsonar.projectKey=ejemplo-gradle -Dsonar.java.binaries=build'
        }
		}

def runJar(){
	sh "gradle bootRun&"
	sleep 20
}

def rest(){
	sh "curl -X GET 'http://localhost:8081/rest/mscovid/test?msg=testing'"
}

def nexusCI(){
            nexusPublisher nexusInstanceId: 'nexus',
        nexusRepositoryId: 'devops-usach-nexus',
        packages: [
            [$class: 'MavenPackage',
                mavenAssetList: [
                    [classifier: '',
                    extension: 'jar',
                    filePath: 'build/libs/DevOpsUsach2020-0.0.1.jar'
                ]
            ],
                mavenCoordinate: [
                    artifactId: 'DevOpsUsach2020',
                    groupId: 'com.devopsusach2020',
                    packaging: 'jar',
                    version: '0.0.1'
                ]
            ]
        ]}

def downloadNexus(){
    sh ' curl -X GET -u $NEXUS_USER:$NEXUS_PASSWORD "http://nexus:8081/repository/devops-usach-nexus/com/devopsusach2020/DevOpsUsach2020/0.0.1/DevOpsUsach2020-0.0.1.jar" -O'
}

def runDownloadedJar(){
    sh 'nohup bash java -jar DevOpsUsach2020-0.0.1.jar & >/dev/null'
    sleep 20
}

def nexusCD(){
    nexusPublisher nexusInstanceId: 'nexus', nexusRepositoryId: 'test-nexus', packages: [[$class: 'MavenPackage', mavenAssetList: [[classifier: '', extension: 'jar', filePath: "DevOpsUsach2020-0.0.1-develop.jar"]], mavenCoordinate: [artifactId: 'DevOpsUsach2020', groupId: 'com.devopsusach2020', packaging: 'jar', version: '1.0.0']]]  
}

return this;