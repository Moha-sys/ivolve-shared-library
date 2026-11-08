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
    def deploymentName   = config.get('deploymentName', 'demoapp-deployment')
    def localRepoPath    = config.get('localRepoPath', '/home/moha/Downloads/iVolve_tasks')

    echo "=========================================================="
    echo " [Shared Library] GitOps Manifest Update & Push          "
    echo " Target App:   ${deploymentName}                         "
    echo " New Image:    ${imageName}:${imageTag}                  "
    echo " Manifest:     ${manifestPath}                           "
    echo "=========================================================="

    stage("Update Deployment File") {
        echo "Updating container image tag in ${manifestPath}..."
        sh """
        cd ${localRepoPath}
        sed -i 's|image: .*|image: ${imageName}:${imageTag}|g' ${manifestPath}
        echo "--- Updated Manifest ---"
        grep -n "image:" ${manifestPath}
        """
    }

    stage("Push Changes to GitHub") {
        echo "Pushing updated deployment manifest to GitHub repository for Argo CD sync..."
        sh """
        cd ${localRepoPath}
        git config user.name "Jenkins CI"
        git config user.email "jenkins@ivolve.com"
        git add ${manifestPath}

        if git diff --staged --quiet; then
            echo "No changes detected to commit."
        else
            git commit -m "chore(gitops): update ${deploymentName} image to ${imageName}:${imageTag} [build #${imageTag}]"
            git push origin main
            echo "Successfully pushed updated manifest to GitHub!"
        fi
        """
    }
}
