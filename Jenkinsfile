pipeline {

  agent none

  stages {

    stage('Deploy DAGS') {
      agent { label 'sbt-template' }
      environment {
        NEXUS = 'gateway.interop.pdnd.dev'
        NEXUS_CREDENTIALS = credentials('pdnd-nexus')
      }
      steps {
        container('sbt-container') {

          script {

            sh '''
              curl -sL https://deb.nodesource.com/setup_10.x | bash -
              apt-get install -y nodejs
              npm install @openapitools/openapi-generator-cli -g
              openapi-generator-cli version
              curl -fsSL https://get.docker.com -o get-docker.sh
              sh get-docker.sh

              docker login $NEXUS -u $NEXUS_CREDENTIALS_USR -p $NEXUS_CREDENTIALS_PSW


            '''

            sh '''#!/bin/bash
            export DOCKER_REPO=$NEXUS
            export NEXUS_HOST=${NEXUS}
            export NEXUS_USER=${NEXUS_CREDENTIALS_USR}
            export NEXUS_PASSWORD=${NEXUS_CREDENTIALS_PSW}
            sbt -Djavax.net.ssl.trustStore=$JENKINS_HOME/PDNDTrustStore -Djavax.net.ssl.trustStorePassword=${PDND_TRUST_STORE_PSW} generateCode docker:publish

            '''

          }
        }
      }
    }

    stage('Apply Kubernetes files') {
      agent { label 'sbt-template' }
      steps{
        // we should use a container with kubectl preinstalled
        container('sbt-container') {

          withKubeConfig([credentialsId: 'kube-config']) {

            sh '''
            cd kubernetes
            curl -LO "https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl"
            chmod u+x ./kubectl
            chmod u+x undeploy.sh
            chmod u+x deploy.sh
            cp kubectl /usr/local/bin/
            ./undeploy.sh
            ./deploy.sh
            '''

          }
        }
      }
    }

    stage('Publish client') {
      agent { label 'sbt-template' }
      environment {
        NEXUS = 'gateway.interop.pdnd.dev'
        NEXUS_CREDENTIALS = credentials('pdnd-nexus')
        PDND_TRUST_STORE_PSW = credentials('pdnd-trust-store')
      }
      steps {
        container('sbt-container') {
          script {
            sh '''#!/bin/bash
            export NEXUS_HOST=${NEXUS}
            export NEXUS_USER=${NEXUS_CREDENTIALS_USR}
            export NEXUS_PASSWORD=${NEXUS_CREDENTIALS_PSW}
            sbt -Djavax.net.ssl.trustStore=$JENKINS_HOME/PDNDTrustStore -Djavax.net.ssl.trustStorePassword=${PDND_TRUST_STORE_PSW} clean compile "project client" publish
            '''
          }
        }
      }
    }
  }

}
