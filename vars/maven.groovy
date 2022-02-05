import pipeline.*

def call(String chosenStages){

    def utils  = new test.UtilMethods()

    def pipelineStages = (utils.isCIorCD().contains('ci')) ? ['compile','test','jar','sonar','runJar','rest','nexusCI','crearRelease'] : ['downloadNexus','runDownloadedJar','rest','nexusCD'] 

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

def crearRelease(){
    if (env.GIT_BRANCH.contains('develop')){
        
        def git = new git.GitMethods()

        if (git.checkIfBranchExists('release-v1-0-0')){
            println 'La rama existe'
            git.deleteBranch('release-v1-0-0')
            println 'Rama eliminada. Se crea nuevamente.'
            git.createBranch(env.GIT_BRANCH,'release-v1-0-0')
            println 'Rama creada con éxito.'
        } else {
            git.createBranch(env.GIT_BRANCH,'release-v1-0-0')
            println 'Rama creada con éxito.'
        }

    } else {
        println "La rama ${env.GIT_BRANCH} no corresponde como rama de orígen para la creación de un Release."
    }
}

def compile(){
    sh './mvnw clean compile -e'
}

def test(){
    sh './mvnw clean test -e'
}

def jar(){
    sh './mvnw clean package -e'
}

def sonar(){
    sh "echo 'Análisis Estático!'"
      withSonarQubeEnv('sonarqube') {
          sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=ejemplo-gradle -Dsonar.java.binaries=build'
      }
}

def runJar(){
    sh 'mvn spring-boot:run &'
    sleep 20
}

def rest(){
    sh "curl -X GET http://localhost:8082/rest/mscovid/test?msg=testing"
}

def nexusCI(){
          nexusPublisher nexusInstanceId: 'nexus',
      nexusRepositoryId: 'devops-usach-nexus',
      packages: [
          [$class: 'MavenPackage',
              mavenAssetList: [
                  [classifier: '',
                  extension: 'jar',
                  filePath: 'build/DevOpsUsach2020-0.0.1.jar'
              ]
          ],
              mavenCoordinate: [
                  artifactId: 'DevOpsUsach2020',
                  groupId: 'com.devopsusach2020',
                  packaging: 'jar',
                  version: '0.0.1'
              ]
          ]
      ]
}

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