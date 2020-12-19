
def call(){

    pipeline {
        agent any

        parameters { choice(name: 'buildtool', choices: ['gradle','maven'], description: 'Elección de herramienta de construcción para aplicación covid') }

        stages {
            stage('Hello') {
                steps {
                    script{
                        println 'Herramienta de ejecución seleccionada: ' + params.buildtool

                        if (params.buildtool == 'gradle'){
                            gradle.call()  
                        } else {
                            maven.call()  
                        }

                    }
                }
            }
        }
    }  

}

return this;
