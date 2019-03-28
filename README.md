# Artifactory Cleaner

This application help us to clear unused items from artifactory.

## Clear snapshot tags from docker images

When we have some image with tag `image:1.0-SNAPSHOT`, and we release `image:1.0`
then tag `1.0-SNAPSHOT` will be deleted for the image.

## Clear snapshot for maven repository

When we have some artifact `test:test-1.0-SNAPSHOT` in snapshots repository
and we release `test:test-1.0` in release repository then `test:test-1.0-SNAPSHOT` will be deleted.

## How to use

Prepare configuration file, you can use `artifactory-cleaner-example.properties` as template. 

By default application is looking for file `artifactory-cleaner.properties` in working directory.

You can provide configuration file location in property: `artifactory.properties.path`

Configuration items:

| Property                       | Description                                                         |
|--------------------------------| --------------------------------------------------------------------|
| artifactory.url                | artifactory address                                                 | 
| artifactory.user               | user name                                                           |
| artifactory.password           | user password, can be encrypted                                     |
| artifactory.docker.repo.name   | repository name with docker image                                   |
| artifactory.snapshot.repo.name | repository name with snapshot versions                              |
| artifactory.release.repo.name  | repository name with release versions                               |
| artifactory.retry.count        | how many time retry failed request to artifactory - default 12      |
| artifactory.retry.sleep        | sleep in second  between each retry for failed request - default 15 |

Run

    mvn clean package
    java -jar target/artifactory-cleaner-1.0-SNAPSHOT-jar-with-dependencies.jar

When you have configuration file in some other location, you can run

    java -Dartifactory.properties.path=path_to_config \
        -jar target/artifactory-cleaner-1.0-SNAPSHOT-jar-with-dependencies.jar

You can also provided some or all configuration in java properties

    java -Dartifactory.url=http://artifactory.example.com \
        -Dartifactory.user=user_name \
        ...
        -jar target/artifactory-cleaner-1.0-SNAPSHOT-jar-with-dependencies.jar

# Reporting bugs and feature requests
    We use github issues to track bugs, improvements and feature requests.
    If you find security bug you can also send info to <security@payu.com>
