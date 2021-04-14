pipeline {

  agent none

  stages {

    stage('Initialize') {
      agent { label 'sbt-template' }
      environment {
       SBT_FOLDER = ".sbt"
       PDND_TRUST_STORE_PSW = credentials('pdnd-interop-trust-psw')
      }
      steps {
        withCredentials([file(credentialsId: 'pdnd-interop-trust-cert', variable: 'pdnd_certificate')]) {
          sh '''
          cat \$pdnd_certificate > gateway.interop.pdnd.dev.cer
          keytool -import -file gateway.interop.pdnd.dev.cer -alias pdnd-interop-gateway -keystore PDNDTrustStore -storepass ${PDND_TRUST_STORE_PSW} -noprompt
          cp $JAVA_HOME/jre/lib/security/cacerts main_certs
          keytool -importkeystore -srckeystore main_certs -destkeystore PDNDTrustStore -srcstorepass ${PDND_TRUST_STORE_PSW} -deststorepass ${PDND_TRUST_STORE_PSW}
          '''
          stash includes: "PDNDTrustStore", name: "pdnd_trust_store"
        }
        script {
          sh 'if [ ! -d ${SBT_FOLDER} ]; then mkdir ${SBT_FOLDER}; fi'
          sh '''
          echo "realm=Sonatype Nexus Repository Manager
          host=https://gateway.interop.pdnd.dev/nexus
          user=${NEXUS_CREDENTIALS_USR}
          password=${NEXUS_CREDENTIALS_PSW}" > ${SBT_FOLDER}/.credentials
          '''
          stash includes: "${SBT_FOLDER}/*", name: "sbt_folder"
        }
      }
    }

    stage('Deploy DAGS') {
      agent { label 'sbt-template' }
      environment {
       NEXUS = 'gateway.interop.pdnd.dev'
       NEXUS_CREDENTIALS = credentials('pdnd-nexus')
       PDND_TRUST_STORE_PSW = credentials('pdnd-interop-trust-psw')
      }
      steps {
        container('sbt-container') {
          unstash "pdnd_trust_store"
          unstash "sbt_folder"
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
            sbt -Djavax.net.ssl.trustStore=./PDNDTrustStore -Djavax.net.ssl.trustStorePassword=${PDND_TRUST_STORE_PSW} generateCode "project root" docker:publish
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
  }

}
