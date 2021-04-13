pipeline {

  agent none

  stages {

    stage('Deploy DAGS') {
      agent { label 'sbt-template' }
      environment {
       NEXUS = 'gateway.interop.pdnd.dev'
       NEXUS_CREDENTIALS = credentials('pdnd-nexus')
       PDND_TRUST_STORE_PSW = credentials('pdnd-interop-trust-psw')
      }
      steps {
        container('sbt-container') {
          withCredentials([file(credentialsId: 'pdnd-interop-trust-cert', variable: 'pdnd_certificate')]) {
               sh "cat \$pdnd_certificate > gateway.interop.pdnd.dev.cer"
          }
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
            pwd
            keytool -import -file gateway.interop.pdnd.dev.cer -alias pdnd-interop-gateway -keystore PDNDTrustStore -storepass ${PDND_TRUST_STORE_PSW} -noprompt
            cp $JAVA_HOME/jre/lib/security/cacerts main_certs
            keytool -importkeystore -srckeystore main_certs -destkeystore PDNDTrustStore -srcstorepass ${PDND_TRUST_STORE_PSW} -deststorepass ${PDND_TRUST_STORE_PSW}
            sbt generateCode
            sbt -Djavax.net.ssl.trustStore=./PDNDTrustStore -Djavax.net.ssl.trustStorePassword=${PDND_TRUST_STORE_PSW} docker:publish
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
        PDND_TRUST_STORE_PSW = credentials('pdnd-interop-trust-psw')
      }
      steps {
        container('sbt-container') {
          withCredentials([file(credentialsId: 'pdnd-interop-trust-cert', variable: 'pdnd_certificate')]) {
               sh "cat \$pdnd_certificate > gateway.interop.pdnd.dev.cer"
          }
          script {
            sh '''#!/bin/bash
            export NEXUS_HOST=${NEXUS}
            export NEXUS_USER=${NEXUS_CREDENTIALS_USR}
            export NEXUS_PASSWORD=${NEXUS_CREDENTIALS_PSW}
            keytool -import -file gateway.interop.pdnd.dev.cer -alias pdnd-interop-gateway -keystore PDNDTrustStore -storepass ${PDND_TRUST_STORE_PSW} -noprompt
            cp $JAVA_HOME/jre/lib/security/cacerts main_certs
            keytool -importkeystore -srckeystore main_certs -destkeystore PDNDTrustStore -srcstorepass ${PDND_TRUST_STORE_PSW} -deststorepass ${PDND_TRUST_STORE_PSW}
            sbt clean compile
            sbt -Djavax.net.ssl.trustStore=./PDNDTrustStore -Djavax.net.ssl.trustStorePassword=${PDND_TRUST_STORE_PSW} "project client" publish
            '''
          }
        }
      }
    }
  }

}
