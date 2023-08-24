package me.fengyj.common.exceptions;

/**
 * Used for the exceptions relevant to the resources.
 * @param type Get the resource type. like DB, File, Service, etc.
 * @param name Get the resource name. for a file, it could be the path of the file, or for web service, could be the API.
 */
public record ResourceInfo(String type, String name) { }
