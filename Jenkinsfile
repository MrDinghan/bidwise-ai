pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        // Frontend codegen reads the spec exported by the backend build.
        OPENAPI_SPEC = "${WORKSPACE}/backend/target/openapi.json"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend - build') {
            steps {
                dir('backend') {
                    sh './mvnw -B -q -DskipTests package'
                }
            }
        }

        stage('Backend - checkstyle & spotbugs') {
            steps {
                dir('backend') {
                    sh './mvnw -B -q checkstyle:check spotbugs:check'
                }
            }
        }

        stage('Backend - test, coverage & export OpenAPI') {
            steps {
                dir('backend') {
                    // `verify` runs unit tests, JaCoCo gate, and the OpenApiSpecExportTest
                    // which writes target/openapi.json (the contract for the frontend).
                    sh './mvnw -B verify'
                }
            }
            post {
                always {
                    junit testResults: 'backend/target/surefire-reports/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: 'backend/target/openapi.json', allowEmptyArchive: false
                }
            }
        }

        stage('Frontend - codegen, lint & test') {
            steps {
                dir('frontend') {
                    sh 'corepack enable'
                    sh 'pnpm install --frozen-lockfile'
                    // Regenerate the client from the freshly exported spec — fails the
                    // build if backend contract and frontend code have drifted.
                    sh 'pnpm run gen:api'
                    sh 'pnpm run lint'
                    sh 'pnpm test'
                    sh 'pnpm run build'
                }
            }
        }

        stage('Docker - build images') {
            steps {
                sh 'docker build -t bidwise/backend:${BUILD_NUMBER} ./backend'
                sh 'docker build -t bidwise/frontend:${BUILD_NUMBER} ./frontend'
            }
        }

        stage('Publish & deploy') {
            steps {
                // P0 placeholder: push images / deploy to AWS in a later milestone.
                echo 'Publish & deploy: not configured for P0.'
            }
        }
    }

    post {
        always {
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}
