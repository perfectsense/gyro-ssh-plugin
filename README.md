<img src="https://github.com/perfectsense/gyro/blob/master/etc/gyro.png" height="200"/>

[![Gitter](https://img.shields.io/gitter/room/perfectsense/gyro)](https://gitter.im/perfectsense/gyro)
[![TravisCI](https://api.travis-ci.com/perfectsense/gyro-ssh-plugin.svg?branch=master)](https://travis-ci.org/perfectsense/gyro-ssh-plugin)
[![Apache License 2.0](https://img.shields.io/github/license/perfectsense/gyro-ssh-plugin)](https://github.com/perfectsense/gyro-ssh-plugin/blob/master/LICENSE)


The **SSH Plugin for Gyro** enables users to ssh into virtual machines managed by Gyro seamlessly. The plugin looks for configured virtual machines inside the Gyro configuration files and then provides you with a list specifying *name*, *location* and *hostname* for each. You can choose to ssh into any of the listed machines and upon selection the plugin will automatically execute the **ssh** cmd with the appropriate options to connect to the virtual machine. The plugin can either look at specific Gyro configurations or all the available configurations of a project to generate the list of virtual machines. 


To learn more about Gyro see [getgyro.io](https://getgyro.io) and [gyro](https://github.com/perfectsense/gyro). 

* [Submit an Issue](https://github.com/perfectsense/gyro-aws-provider/issues)
* [Getting Help](#getting-help)

## Using the SSH Plugin

#### Import ####

Load the SSH plugin in your project by consuming it as a `plugin` directive in your `.gyro/init.gyro` file. It uses the format `@plugin: gyro:gyro-ssh-plugin:<version>`.

```shell
@repository: 'https://artifactory.psdops.com/gyro-releases'
@plugin: 'gyro:gyro-ssh-plugin:1.0.0'
```

#### Configuration ####

The gyro-ssh-plugin works out of the box to access Virtual machines, unless they are behind a private network.

To access virtual machines residing behind a private network without using a vpn, jump hosts needs to be configured.

Specify one or more virtual machines that are accessible publicly and has access to the private hosts you will need to access.

Provide the jump hosts by defining the following in your `.gyro/init.gyro` file:

```
@jump-host
    jump-hosts: $(aws::instance gateway-*)
    regions: ["$(primary-region)"]
@end
```

#### Usage ####

There are 3 commands that you have access to:

* list - Lists all the virtual machines that are available in your project.

* ssh - Allows you to ssh into a virtual machine, without prior knowledge of the ip of the machine. 

* tunnel - Allows you to tunnel into a virtual machine, without prior knowledge of the ip of the machine. 


If the virtual machine is behind a private network and, the jump-host is configured, then ssh/tunnel wil automatically use one of the jump hosts specified to provide you access to that virtual machine. 

## License

[Apache License 2.0](https://github.com/perfectsense/gyro-ssh-plugin/blob/master/LICENSE) 
