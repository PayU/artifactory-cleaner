# Artifactory Cleaner

This application help us to clear unused items from artifactory.

## Delete tags from docker repository

Delete all tags except the newest ones. By default 5 newest tags of each image are kept. Filter mechanism
is provided based on regular expressions.

## Delete snapshots from maven repository

When a release of `test:test-1.5` is done, all snapshots with maven version lower than 1.5 will be deleted.

## How to use

Prepare configuration file, you can use `artifactory-cleaner-example.properties` as template.

By default, application is looking for file `artifactory-cleaner.properties` in working directory.

You can provide configuration file location in property: `artifactory.properties.path`

Configuration items:

| Property                        | Description                                                               |
|---------------------------------|---------------------------------------------------------------------------|
| artifactory.url                 | artifactory address                                                       | 
| artifactory.user                | user name                                                                 |
| artifactory.password            | user password, can be encrypted                                           |
| artifactory.docker.repo.name    | repository name with docker image                                         |
| artifactory.docker.tags.to.keep | number of newest tags to keep in each image - default 5                   |
| artifactory.docker.filter.file  | path to file with filters, each line is a regexp to image path            |
| artifactory.snapshot.repo.name  | repository name with snapshot versions                                    |
| artifactory.release.repo.name   | repository name with release versions                                     |
| artifactory.retry.count         | how many time retry failed request to artifactory - default 12            |
| artifactory.retry.sleep         | sleep in second  between each retry for failed request - default 15       |
| artifactory.release.clean.<N>   | clean old released components, it can ba many config items indexed by `N` |

### artifactory.release.clean.<N> - format

This configuration item contains elements separated by colon `:`, eg:

```
artifactory.release.clean.1 = repository-name:root-path-of-maven-project:days:remain:limit
```

where:

| Item                       | Description                                                                                | Defaults |
|----------------------------|--------------------------------------------------------------------------------------------|----------|
| repository-name            | remote repository name                                                                     |          |
| root-path-of-maven-project | Maven groupId exposed as path, that will be used for searching a versions list for removal |          |
| days                       | artifacts version older than `days` will be scheduled for removal                          | 365      |
| remain                     | how many versions must always be preserved                                                 | 3        |
| limit                      | how many versions should be removed in one execution                                       | 128      |

Run

    mvn clean package
    java -jar target/artifactory-cleaner-1.0-SNAPSHOT-jar-with-dependencies.jar

When you have configuration file in some other location, you can run

    java -Dartifactory.properties.path=path_to_config \
        -jar target/artifactory-cleaner-1.0-SNAPSHOT-jar-with-dependencies.jar

You can also provide some or all configuration in java properties

    java -Dartifactory.url=http://artifactory.example.com \
        -Dartifactory.user=user_name \
        ...
        -jar target/artifactory-cleaner-1.0-SNAPSHOT-jar-with-dependencies.jar

# Reporting bugs and feature requests

    We use github issues to track bugs, improvements and feature requests.
    If you find security bug you can also send info to <security@payu.com>
