# Advanced Jenkins config as code setup with 4 steps

Creating Jenkins configation as code and applying changes without downtime with Groovy, Java, Docker and Jenkins job.

POC: 
1) Being able to update any Jenkins master or slave immediately - no new image, no redeploy, no downtime 
2) No manual changes through UI - everything is kept as a code, and as a result:
3) Jenkins current state and state of image + config is kept in sync
4) Any change could be tested immediatelly without vicious cycle: create a new image, deploy, test, and if fails - repeat!
5) Creating a configuration that could be applied for specific environment only(prod vs test/dev Jenkins), with inheritence of common config and custom per jenkins config

#### Step 1: Write groovy to interact with Java API
```
import hudson.model.*
import jenkins.model.*
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl

def domain = Domain.global()
def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

def instance = System.getenv("JENKINS_INSTANCE_NAME").replaceAll('-','_')

ConfigObject conf = new ConfigSlurper().parse(new File(System.getenv("JENKINS_HOME")+'/jenkins_config/credentials.txt').text)

conf.common_credentials.each { key, credentials ->
    println("Adding common credential ${key}")
    store.addCredentials(domain, new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, key, credentials.description, credentials.username, credentials.password))
}


conf."${instance}_credentials".each { key, credentials ->
    println("Adding ${instance} credential ${key}")
    store.addCredentials(domain, new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, key, credentials.description, credentials.username, credentials.password))
}

println("Successfully configured credentials")

```

#### Step 2: Create config for the script

```
common_credentials {

    exclude{
        tyrion-jenkins
    }

    data{
        jenkins_service_user = [
             username: 'jenkins_service_user',
             password: '{{with $secret := secret "secret/jenkins/jenkins_service_user" }}{{ $secret.Data.value }}{{end}}',
             description :'for automated jenkins jobs'
         ]

         slack = [
             username: '{{with $secret := secret "secret/slack/user" }}{{ $secret.Data.value }}{{end}}',
             password: '{{with $secret := secret "secret/slack/pass" }}{{ $secret.Data.value }}{{end}}',
             description: 'slack credentials'
         ]
    }

}

custom_credentials  {

    include{
        john-snow-jenkins
        arya-jenkins
        sansa-jenkins
    }

    data{
        artifactory = [
                username: 'arti',
                password: '{{with $secret := secret "secret/jenkins/artifactory" }}{{ $secret.Data.artifactory_password }}{{end}}',
                description: 'Artifactory credentials'
        ]

    }

}

tyrion-jenkins_credentials  {

    data{
       nexus=[
                'username':'deployment',
                'password':'{{with $secret := secret "secret/jenkins/nexus" }}{{ $secret.Data.nexus_password }}{{end}}',
                'description':'Nexus credentials'
        ]

    }

}
```

#### Step 3: Checkout config and script and inject secrets and other variables with consul-template in container:
```
#!/usr/bin/env bash

git clone ssh://git@your_scm_here/jenkins_config_as_code.git ${JENKINS_HOME}/jenkins_config
mv ${JENKINS_HOME}/jenkins_config/*.groovy ${JENKINS_HOME}/init.groovy.d/

consul-template \
  -consul-addr "$CONSUL_ADDR" \
  -vault-addr "$VAULT_ADDR" \
  -config "jenkins_config.hcl" \
  -once


```


#### Step 4: Update continuously with Jenkins job without downtime
```
node {
    stage('checkout') {

        sh '''

		    git clone ssh://git@your_scm_here/jenkins_config_as_code.git ${JENKINS_HOME}/jenkins_config
			mv ${JENKINS_HOME}/jenkins_config/*.groovy ${JENKINS_HOME}/init.groovy.d/

		'''
    }

    stage('run consul template'){
        sh '''
			consul-template \
			  -consul-addr "$CONSUL_ADDR" \
			  -vault-addr "$VAULT_ADDR" \
			  -config "jenkins_config.hcl" \
			  -once        
        '''
    }

    stage('update credentials') {
        load("/var/jenkins_home/init.groovy.d/credentials.groovy")
    }

    stage('update k8s') {
        load("/var/jenkins_home/init.groovy.d/kubernetes.groovy")
    }

}


```