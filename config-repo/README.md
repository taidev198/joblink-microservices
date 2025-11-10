# Config Repository

This directory contains configuration files for the JobLink microservices.

## Structure

Each service can have its own configuration file named:
- `{service-name}.yml` or `{service-name}.properties`

For example:
- `vocabulary-service.yml`
- `api-gateway.yml`
- `main-service.yml`
- `matching-service.yml`
- `print-service.yml`

## Usage

The config service will read configurations from this directory. You can use Git to version control these configurations or use file-based storage.

## Configuration Files

Place service-specific configuration files here. The config server will serve them to the respective services.

