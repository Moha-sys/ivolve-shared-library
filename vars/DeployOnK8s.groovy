#!/usr/bin/env groovy

/**
 * Shared Library Step: DeployOnK8s
 * GitOps-enabled step that updates the Kubernetes deployment manifest with the newly built
 * image tag and pushes the changes to GitHub so Argo CD can automatically synchronize the cluster.
 */
def call(Map config = [:]) {
    def imageName        = config.get('imageName', 'mohadevv/demoapp')
    def imageTag         = config.get('imageTag', "${env.BUILD_NUMBER}")
    def manifestPath     = config.get('manifestPath', 'GitOps/Lab23/k8s/deployment.yaml')
    def gitRepo          = config.get('gitRepo', 'github.com/Moha-sys/iVolve_tasks.git')
    def gitBranch        = config.get('gitBranch', 'main')
    def gitCredentialsId = config.get('gitCredentialsId', 'jenkins')
    def deploymentName   = config.get('deploymentName', 'demoapp-deployment')
    def gitUserName      = config.get('gitUserName', 'Jenkins CI')
    def gitUserEmail     = config.get('gitUserEmail', 'jenkins@ivolve.com')
    def manifestsDir     = config.get('manifestsDir', 'gitops-repo')

    echo "=========================================================="
    echo " [Shared Library] GitOps Manifest Update & Push          "
    echo " Target App:   ${deploymentName}                         "
    echo " New Image:    ${imageName}:${imageTag}                  "
    echo " Manifest:     ${manifestPath}                           "
    echo " Target Repo:  https://${gitRepo}                        "
    echo " Target Branch:${gitBranch}                              "
    echo "=========================================================="

    withCredentials([usernamePassword(
        credentialsId: gitCredentialsId,
        usernameVariable: 'GIT_USER',
        passwordVariable: 'GIT_PASS'
    )]) {
        stage("Update Deployment File") {
            echo "Cloning manifests repository and updating image tag in ${manifestPath}..."
            sh """
            rm -rf ${manifestsDir}
            git clone --depth 1 https://\${GIT_USER}:\${GIT_PASS}@${gitRepo} ${manifestsDir}
            cd ${manifestsDir}
            git checkout ${gitBranch}

            if [ -f "${manifestPath}" ]; then
                sed -i 's|image: .*|image: ${imageName}:${imageTag}|g' ${manifestPath}
                echo "--- Manifest Diff ---"
                git diff ${manifestPath} || true
            else
                echo "ERROR: Manifest file ${manifestPath} not found in repository!"
                ls -la
                exit 1
            fi
            """
        }

        stage("Push Changes to GitHub") {
            echo "Pushing updated deployment manifest to GitHub repository for Argo CD sync..."
            sh """
            cd ${manifestsDir}
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
            git add ${manifestPath}

            if git diff --staged --quiet; then
                echo "No changes detected to commit."
            else
                git commit -m "chore(gitops): update ${deploymentName} image to ${imageName}:${imageTag} [build #${imageTag}]"
                git push origin ${gitBranch}
                echo "Successfully pushed updated manifest to GitHub branch ${gitBranch}!"
            fi
            """
        }
    }
}
