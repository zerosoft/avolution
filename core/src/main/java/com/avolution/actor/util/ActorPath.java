package com.avolution.actor.util;

public class ActorPath {
    private static final String PATH_SEPARATOR = "/";
    private static final String ID_SEPARATOR = "#";

    /**
     * 解析完整的Actor路径
     * @param fullPath 完整路径 (e.g. "/user/parent/child#123")
     * @return ActorPath对象
     */
    public static ActorPath fromString(String fullPath) {
        return new ActorPath(fullPath);
    }

    private final String fullPath;

    private ActorPath(String fullPath) {
        this.fullPath = fullPath;
    }

    /**
     * 获取父Actor路径
     * @return 父Actor路径，如果是根Actor则返回null
     */
    public String getParentPath() {
        int lastSlash = fullPath.lastIndexOf(PATH_SEPARATOR);
        return lastSlash > 0 ? fullPath.substring(0, lastSlash) : null;
    }

    /**
     * 获取Actor名称（不包含ID）
     * @return Actor名称
     */
    public String getName() {
        String lastSegment = getLastPathSegment();
        int hashIndex = lastSegment.indexOf(ID_SEPARATOR);
        return hashIndex >= 0 ? lastSegment.substring(0, hashIndex) : lastSegment;
    }

    /**
     * 获取Actor ID
     * @return Actor ID，如果没有则返回null
     */
    public String getId() {
        String lastSegment = getLastPathSegment();
        int hashIndex = lastSegment.indexOf(ID_SEPARATOR);
        return hashIndex >= 0 ? lastSegment.substring(hashIndex + 1) : null;
    }

    /**
     * 获取路径中的最后一段
     */
    private String getLastPathSegment() {
        int lastSlash = fullPath.lastIndexOf(PATH_SEPARATOR);
        return lastSlash >= 0 ? fullPath.substring(lastSlash + 1) : fullPath;
    }

    /**
     * 获取完整路径
     */
    public String getFullPath() {
        return fullPath;
    }

    /**
     * 判断是否是系统Actor
     */
    public boolean isSystem() {
        return fullPath.startsWith("/system");
    }

    /**
     * 判断是否是用户Actor
     */
    public boolean isUser() {
        return fullPath.startsWith("/user");
    }

    /**
     * 获取子Actor路径
     * @param childName 子Actor名称
     * @param id 子Actor ID
     */
    public String child(String childName, String id) {
        return fullPath + PATH_SEPARATOR + childName +
                (id != null ? ID_SEPARATOR + id : "");
    }

    public boolean isSamePath(String fullPath){
        return this.fullPath.equals(fullPath);
    }

    @Override
    public String toString() {
        return fullPath;
    }
}