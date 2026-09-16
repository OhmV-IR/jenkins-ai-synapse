pipeline {
    agent none

    stages {
        stage('Build matrix') {
            matrix {
                axes {
                    axis {
                        name 'JDK_VERSION'
                        values '21', '25'
                    }
                }

                agent { label 'linux' }
                tools { 
                    jdk "${JDK_VERSION}"
                    maven '3.9.14'
                }

                environment {
                    MAVEN_SETTINGS = credentials('nexus-maven-settings-file')
                }

                stages {
                    stage("Checkout") {
                        steps {
                            checkout scm
                        }
                    }

                    stage("Compile plugin") {
                        steps {
                            sh "mvn -DforkCount=1C clean compile"
                        }
                    }

                    stage("Test plugin"){
                        steps {
                            sh "mvn -DforkCount=1C test"
                            junit '**/target/surefire-reports/*.xml'
                        }
                    }

                    stage("Verify plugin"){
                        steps {
                            sh "mvn -DforkCount=1C verify"
                        }
                    }

                    stage("Upload artifact") {
                        steps {
                            sh "mkdir -p output"
                            sh "cp target/jenkinsaisynapse.hpi output/jenkinsaisynapse-jvm-${JDK_VERSION}.hpi"
                            archiveArtifacts artifacts: "output/jenkinsaisynapse-jvm-${JDK_VERSION}.hpi", fingerprint: true
                            sh "rm -rf output"
                        }
                    }

                    stage('Deploy Snapshot') {
                         when {
                              allOf {
                                   branch 'master'
                                   not { buildingTag() }
                                   environment name: 'JDK_VERSION', value: '25'
                              }
                         }
                         steps {
                             script {
                                    def snapshotVersion = "1.0.0-BUILD-${BUILD_NUMBER}-SNAPSHOT"
                                    sh "mvn versions:set -DnewVersion=${snapshotVersion} -DgenerateBackupPoms=false"
                                    sh 'mvn clean deploy -s "$MAVEN_SETTINGS" -DskipTests'
                             }
                        }
                    }

                    stage("Deploy tagged release"){
                        when {
                            allOf {
                                buildingTag()
                                environment name: 'JDK_VERSION', value: '25'
                            }
                        }
                        steps {
                            script {
                                def releaseVersion = TAG_NAME.replaceAll(/^[a-zA-Z_-]+/, '')
                                echo "Setting release version to ${releaseVersion} from tag ${TAG_NAME}..."
                                sh "mvn versions:set -DnewVersion=${releaseVersion} -DgenerateBackupPoms=false"
                                sh 'mvn clean deploy -s "$MAVEN_SETTINGS" -DskipTests'
                            }
                        }
                    }
                }
            }
        }
    }
}

