import hudson.model.*
import jenkins.model.*
import org.csanchez.jenkins.plugins.kubernetes.*
import org.csanchez.jenkins.plugins.kubernetes.volumes.workspace.EmptyDirWorkspaceVolume
import org.csanchez.jenkins.plugins.kubernetes.volumes.HostPathVolume

//since kubernetes-1.0
//import org.csanchez.jenkins.plugins.kubernetes.model.KeyValueEnvVar
import org.csanchez.jenkins.plugins.kubernetes.PodEnvVar

//change after testing
ConfigObject conf = new ConfigSlurper().parse(new File(System.getenv("JENKINS_HOME") + '/jenkins_config/kubernetes.txt').text)

def kc
try {
    println("Configuring k8s")


    if (Jenkins.instance.clouds) {
        kc = Jenkins.instance.clouds.get(0)
        println "cloud found: ${Jenkins.instance.clouds}"
    } else {
        kc = new KubernetesCloud(conf.kubernetes.name)
        Jenkins.instance.clouds.add(kc)
        println "cloud added: ${Jenkins.instance.clouds}"
    }

    kc.setContainerCapStr(conf.kubernetes.containerCapStr)
    kc.setServerUrl(conf.kubernetes.serverUrl)
    kc.setSkipTlsVerify(conf.kubernetes.skipTlsVerify)
    kc.setNamespace(conf.kubernetes.namespace)
    kc.setJenkinsUrl(conf.kubernetes.jenkinsUrl)
    kc.setCredentialsId(conf.kubernetes.credentialsId)
    kc.setRetentionTimeout(conf.kubernetes.retentionTimeout)
    //since kubernetes-1.0
//    kc.setConnectTimeout(conf.kubernetes.connectTimeout)
    kc.setReadTimeout(conf.kubernetes.readTimeout)
    //since kubernetes-1.0
//    kc.setMaxRequestsPerHostStr(conf.kubernetes.maxRequestsPerHostStr)

    println "set templates"
    kc.templates.clear()

    conf.kubernetes.podTemplates.each { podTemplateConfig ->

        def podTemplate = new PodTemplate()
        podTemplate.setLabel(podTemplateConfig.label)
        podTemplate.setName(podTemplateConfig.name)

        if (podTemplateConfig.inheritFrom) podTemplate.setInheritFrom(podTemplateConfig.inheritFrom)
        if (podTemplateConfig.slaveConnectTimeout) podTemplate.setSlaveConnectTimeout(podTemplateConfig.slaveConnectTimeout)
        if (podTemplateConfig.idleMinutes) podTemplate.setIdleMinutes(podTemplateConfig.idleMinutes)
        if (podTemplateConfig.nodeSelector) podTemplate.setNodeSelector(podTemplateConfig.nodeSelector)
        //
        //since kubernetes-1.0
//        if (podTemplateConfig.nodeUsageMode) podTemplate.setNodeUsageMode(podTemplateConfig.nodeUsageMode)
        if (podTemplateConfig.customWorkspaceVolumeEnabled) podTemplate.setCustomWorkspaceVolumeEnabled(podTemplateConfig.customWorkspaceVolumeEnabled)

        if (podTemplateConfig.workspaceVolume) {
            if (podTemplateConfig.workspaceVolume.type == 'EmptyDirWorkspaceVolume') {
                podTemplate.setWorkspaceVolume(new EmptyDirWorkspaceVolume(podTemplateConfig.workspaceVolume.memory))
            }
        }

        if (podTemplateConfig.volumes) {
            def volumes = []
            podTemplateConfig.volumes.each { volume ->
                if (volume.type == 'HostPathVolume') {
                    volumes << new HostPathVolume(volume.hostPath, volume.mountPath)

                }
            }
            podTemplate.setVolumes(volumes)
        }

        if (podTemplateConfig.keyValueEnvVar) {
            def envVars = []
            podTemplateConfig.keyValueEnvVar.each { keyValueEnvVar ->

                //since kubernetes-1.0
//                envVars << new KeyValueEnvVar(keyValueEnvVar.key, keyValueEnvVar.value)
                envVars << new PodEnvVar(keyValueEnvVar.key, keyValueEnvVar.value)
            }
            podTemplate.setEnvVars(envVars)
        }


        if (podTemplateConfig.containerTemplate) {
            println "containerTemplate: ${podTemplateConfig.containerTemplate}"

            ContainerTemplate ct = new ContainerTemplate(
                    podTemplateConfig.containerTemplate.name ?: conf.kubernetes.containerTemplateDefaults.name,
                    podTemplateConfig.containerTemplate.image)

            ct.setAlwaysPullImage(podTemplateConfig.containerTemplate.alwaysPullImage ?: conf.kubernetes.containerTemplateDefaults.alwaysPullImage)
            ct.setPrivileged(podTemplateConfig.containerTemplate.privileged ?: conf.kubernetes.containerTemplateDefaults.privileged)
            ct.setTtyEnabled(podTemplateConfig.containerTemplate.ttyEnabled ?: conf.kubernetes.containerTemplateDefaults.ttyEnabled)
            ct.setWorkingDir(podTemplateConfig.containerTemplate.workingDir ?: conf.kubernetes.containerTemplateDefaults.workingDir)
            ct.setArgs(podTemplateConfig.containerTemplate.args ?: conf.kubernetes.containerTemplateDefaults.args)
            ct.setResourceRequestCpu(podTemplateConfig.containerTemplate.resourceRequestCpu ?: conf.kubernetes.containerTemplateDefaults.resourceRequestCpu)
            ct.setResourceLimitCpu(podTemplateConfig.containerTemplate.resourceLimitCpu ?: conf.kubernetes.containerTemplateDefaults.resourceLimitCpu)
            ct.setResourceRequestMemory(podTemplateConfig.containerTemplate.resourceRequestMemory ?: conf.kubernetes.containerTemplateDefaults.resourceRequestMemory)
            ct.setResourceLimitMemory(podTemplateConfig.containerTemplate.resourceLimitMemory ?: conf.kubernetes.containerTemplateDefaults.resourceLimitMemory)
            ct.setCommand(podTemplateConfig.containerTemplate.command ?: conf.kubernetes.containerTemplateDefaults.command)
            podTemplate.setContainers([ct])
        }

        println "adding ${podTemplateConfig.name}"
        kc.templates << podTemplate

    }

    kc = null
    println("Configuring k8s completed")
}
finally {
    //if we don't null kc, jenkins will try to serialise k8s objects and that will fail, so we won't see actual error
    kc = null
}







