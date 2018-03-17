# Advanced Jenkins setup

Creating Jenkins configation as code and applying changes without downtime with Groovy, Java, Docker and Jenkins job.

POC: 
1) Being able to update any Jenkins master or slave immediately - no new image, no redeploy, no downtime 
2) No manual changes through UI - everything is kept as a code, and as a result:
3) Jenkins current state and state of image + config is kept in sync
4) Any change could be tested immediatelly without vicious cycle: create a new image, deploy, test, and if fails - repeat!
5) Creating a configuration that could be applied for specific environment only(prod vs test/dev Jenkins), with inheritence of common config and custom per jenkins config

## Example credentials config

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
