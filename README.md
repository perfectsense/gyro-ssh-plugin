<img src="https://github.com/perfectsense/gyro/blob/master/etc/gyro.png" height="200"/>

[![Gitter](https://img.shields.io/gitter/room/perfectsense/gyro)](https://gitter.im/perfectsense/gyro)
[![TravisCI](https://api.travis-ci.com/perfectsense/gyro-ssh-plugin.svg?branch=master)](https://travis-ci.org/perfectsense/gyro-ssh-plugin)
[![Apache License 2.0](https://img.shields.io/github/license/perfectsense/gyro-ssh-plugin)](https://github.com/perfectsense/gyro-ssh-plugin/blob/master/LICENSE)


The **SSH Plugin for Gyro** enables users to access virtual machines managed by gyro seamlessly.

Features: [WIP]
* List - See what how many virtual machines are up in a project and what are their status.
* SSH - Ssh into one of the listed virtual machines.
* Tunnel - Tunnel into one of the listed virtual machines.

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

If want to access a virtual machine that is behind a private network and, you don't want to use a vpn then, jump hosts can cover that for you.

All you need is to specify a virtual machine that is accessible and has access to the private hosts you will need to access.

Provide the jump hosts by defining the following in your `.gyro/init.gyro` file:

```
@jump-host
    jump-hosts: $(aws::instance gateway-*)
    regions: ["$(primary-region)"]
@end
```

#### Usage ####

There are 3 commands that you have access to:

 - list: Lists all the virtual machines that are available in your project.
 - ssh: Allows you to ssh into a virtual machine, without prior knowledge of the ip of the machine. 
 - tunnel: Allows you to tunnel into a virtual machine, without prior knowledge of the ip of the machine. 

If the virtual machine is behind a private network and, the jump-host is configured, then ssh/tunnel wil automatically use one of the jump hosts specified to provide you access to that virtual machine. 

## License

[Apache License 2.0](https://github.com/perfectsense/gyro-ssh-plugin/blob/master/LICENSE) 
