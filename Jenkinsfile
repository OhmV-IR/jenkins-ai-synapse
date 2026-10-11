pipeline {
    agent none

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '30'))
    }

    stages {
        stage('Build') {
            agent { label 'linux' }
            tools {
                jdk '25'
                maven '3.9.14'
            }

            stages {
                stage('Checkout') {
                    steps {
                        checkout scm
                    }
                }

                stage('Build and verify') {
                    steps {
                        sh 'mvn --batch-mode -DforkCount=1C clean verify'
                    }
                    post {
                        always {
                            junit allowEmptyResults: true,
                                  testResults: '**/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Upload artifact') {
                    steps {
                        archiveArtifacts artifacts: 'target/jenkinsaisynapse.hpi',
                                         fingerprint: true
                    }
                }

                stage('Deploy Snapshot') {
                    when {
                        allOf {
                            branch 'master'
                            not { buildingTag() }
                        }
                    }
                    steps {
                        withCredentials([usernamePassword(credentialsId: 'maven-central-token',
                                                          usernameVariable: 'CENTRAL_TOKEN_USERNAME',
                                                          passwordVariable: 'CENTRAL_TOKEN_PASSWORD')]) {
                            sh '''
                                mvn --batch-mode -s .ci/central-settings.xml \
                                    -Dchangelist="-BUILD-${BUILD_NUMBER}-SNAPSHOT" \
                                    -DskipTests \
                                    clean deploy
                            '''
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
                                                  passwordVariable: 'GH_TOKEN'),
                                 usernamePassword(credentialsId: 'maven-central-token',
                                                  usernameVariable: 'CENTRAL_TOKEN_USERNAME',
                                                  passwordVariable: 'CENTRAL_TOKEN_PASSWORD'),
                                 file(credentialsId: 'maven-gpg-signing-key',
                                      variable: 'GPG_KEY_FILE'),
                                 string(credentialsId: 'maven-gpg-passphrase',
                                        variable: 'MAVEN_GPG_PASSPHRASE')]) {
                    sh '''
                        set -eu

                        git config user.name "Jenkins CI"
                        git config user.email "jenkins-ci@ohmvir.dev"

                        # The release plugin pushes to the URL in <developerConnection>,
                        # not to "origin", so auth has to be attached to git itself.
                        ASKPASS="${WORKSPACE_TMP:-/tmp}/git-askpass.sh"
                        cat > "$ASKPASS" <<'SCRIPT'
#!/bin/sh
case "$1" in
  Username*) echo "$GH_USER" ;;
  Password*) echo "$GH_TOKEN" ;;
  *) exit 1 ;;
esac
SCRIPT
                        chmod 700 "$ASKPASS"
                        export GIT_ASKPASS="$ASKPASS"

                        # maven-gpg-plugin's bc signer reads the armored private key from this variable
                        MAVEN_GPG_KEY="$(cat "$GPG_KEY_FILE")"
                        export MAVEN_GPG_KEY

                        # Absolute path, because release:perform builds from target/checkout
                        MAVEN_SETTINGS="$WORKSPACE/.ci/central-settings.xml"

                        git fetch --tags --prune --prune-tags origin

                        mvn --batch-mode release:clean release:prepare release:perform \
                            -s "$MAVEN_SETTINGS" \
                            -Dresume=false \
                            -DlocalCheckout=true \
                            -Darguments="-DskipTests -s $MAVEN_SETTINGS"
                    '''
                }
            }
            post {
                always {
                    sh 'rm -f "${WORKSPACE_TMP:-/tmp}/git-askpass.sh" || true'
                }
            }
        }
    }
}
