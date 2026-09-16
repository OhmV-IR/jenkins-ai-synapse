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
                }
            }
        }

        stage('Execute Maven Release') {
            when {
                allOf {
                    branch 'master'
                    not { buildingTag() }
                    changelog '.*\\[release\\].*'
                }
            }
            agent { label 'linux' }
            tools {
                jdk '25'
                maven '3.9.14'
            }
            environment {
                MAVEN_SETTINGS = credentials('nexus-maven-settings-file')
            }
            steps {
                cleanWs()
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: 'master']],
                    userRemoteConfigs: scm.userRemoteConfigs,
                    extensions: [
                        [$class: 'LocalBranch', localBranch: 'master'],
                        [$class: 'CloneOption', noTags: false, shallow: false]
                    ]
                ])

                withCredentials([usernamePassword(credentialsId: 'ghpat_personal',
                                                  usernameVariable: 'GH_USER',
                                                  passwordVariable: 'GH_TOKEN')]) {
                    sh '''
                        set -eu
                        git config user.name "Jenkins CI"
                        git config user.email "jenkins-ci@ohmvir.dev"
                        git remote set-url origin "https://${GH_USER}:${GH_TOKEN}@github.com/OhmV-IR/jenkins-ai-synapse.git"
                        git fetch --tags --prune --prune-tags origin

                        mvn --batch-mode release:clean release:prepare release:perform \
                            -s "$MAVEN_SETTINGS" \
                            -Dresume=false \
                            -Darguments="-DskipTests"
                    '''
                }
            }
            post {
                always {
                    sh 'git remote set-url origin "https://github.com/OhmV-IR/jenkins-ai-synapse.git" || true'
                }
            }
        }
    }
}